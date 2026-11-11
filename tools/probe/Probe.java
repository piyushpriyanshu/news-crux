import app.newsbreak.core.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.json.*;

/**
 * Live check of every source, run on GitHub (tools/probe/run.sh). For each feed: does it answer, how many items, how
 * fresh. For sample articles: how the app reads them (method, length, date) and which structured data the page carries.
 * Also checks the coaching pages, news sitemaps and candidate direct feeds. Writes probe/report.json and probe/report.md.
 *
 * The report carries only counts, URLs and a short opening snippet of each article (never full text).
 */
public class Probe {
    static final Pattern CLS = Pattern.compile("<(div|section|article)[^>]*\\b(class|id)=\"([^\"]*(article|story|content|body|detail|post|entry|text)[^\"]*)\"", Pattern.CASE_INSENSITIVE);

    public static void main(String[] a) throws Exception {
        String root = a.length > 0 ? a[0] : ".";
        JSONArray sources = new JSONArray(new String(Files.readAllBytes(Paths.get(root, "assets/www/data/sources.json")), "UTF-8"));
        JSONObject cand = new JSONObject(new String(Files.readAllBytes(Paths.get(root, "tools/probe/candidates.json")), "UTF-8"));
        long now = System.currentTimeMillis();
        JSONObject rep = new JSONObject().put("at", now);
        ExecutorService pool = Executors.newFixedThreadPool(8);

        // 1. feeds in the app
        List<Future<JSONObject>> fs = new ArrayList<Future<JSONObject>>();
        for (int i = 0; i < sources.length(); i++) {
            final JSONObject s = sources.getJSONObject(i);
            JSONArray feeds = s.getJSONArray("feeds");
            for (int j = 0; j < feeds.length(); j++) {
                final JSONObject f = feeds.getJSONObject(j);
                fs.add(pool.submit(new Callable<JSONObject>() { public JSONObject call() { return feed(s.getString("code"), f.getString("url"), f.optBoolean("gn"), 3); } }));
            }
        }
        // 2. candidate direct feeds and sitemaps
        JSONArray cf = cand.optJSONArray("feeds");
        for (int i = 0; cf != null && i < cf.length(); i++) {
            final JSONObject f = cf.getJSONObject(i);
            fs.add(pool.submit(new Callable<JSONObject>() { public JSONObject call() { JSONObject o = feed(f.getString("code"), f.getString("url"), false, 2); try { o.put("candidate", true); } catch (Exception e) {} return o; } }));
        }
        JSONArray feedsOut = new JSONArray();
        for (Future<JSONObject> f : fs) { try { feedsOut.put(f.get(150, TimeUnit.SECONDS)); } catch (Exception e) { feedsOut.put(new JSONObject().put("error", String.valueOf(e))); } }
        rep.put("feeds", feedsOut);

        JSONArray hosts = cand.optJSONArray("robots");
        List<Future<JSONObject>> ss = new ArrayList<Future<JSONObject>>();
        for (int i = 0; hosts != null && i < hosts.length(); i++) {
            final String h = hosts.getString(i);
            ss.add(pool.submit(new Callable<JSONObject>() { public JSONObject call() { return robots(h); } }));
        }
        JSONArray sm = new JSONArray();
        for (Future<JSONObject> f : ss) { try { sm.put(f.get(120, TimeUnit.SECONDS)); } catch (Exception e) { sm.put(new JSONObject().put("error", String.valueOf(e))); } }
        rep.put("sitemaps", sm);

        // 3. coaching pages
        JSONArray co = new JSONArray();
        String today = Ist.dateKey(now), yday = Ist.dateKey(now - Ist.DAY);
        for (String u : new String[]{Harvest.drishtiUrl(today), Harvest.drishtiUrl(yday), Harvest.insightsUrl(today), Harvest.insightsUrl(yday), Harvest.VAJIRAM_INDEX, Harvest.VISION_INDEX}) co.put(coach(u));
        rep.put("coaching", co);
        pool.shutdownNow();

        Files.createDirectories(Paths.get(root, "probe"));
        Files.write(Paths.get(root, "probe/report.json"), rep.toString(1).getBytes("UTF-8"));
        Files.write(Paths.get(root, "probe/report.md"), md(rep).getBytes("UTF-8"));
        System.out.println(md(rep));
    }

    static JSONObject feed(String code, String url, boolean gn, int samples) {
        JSONObject o = new JSONObject();
        try {
            o.put("code", code).put("url", url);
            long t = System.currentTimeMillis();
            Net.Resp r = Net.get(url, 20000);
            o.put("http", r.code).put("ms", System.currentTimeMillis() - t).put("bytes", r.body == null ? 0 : r.body.length());
            if (r.code != 200 || r.body == null) return o;
            boolean sitemap = r.body.contains("<urlset") || r.body.contains("<sitemapindex");
            if (sitemap) { o.put("sitemap", sitemapInfo(r.body)); return o; }
            List<RssParser.Raw> raws = RssParser.parse(r.body, new RssParser.ParserFactory() {
                public org.xmlpull.v1.XmlPullParser create() { return new org.kxml2.io.KXmlParser(); }
            });
            List<JSONObject> items = RssParser.normalise(raws, code, "probe", gn, System.currentTimeMillis());
            o.put("items", items.size());
            long newest = 0; int desc = 0;
            for (JSONObject it : items) { newest = Math.max(newest, it.optLong("p")); desc += it.optString("d").length(); }
            o.put("newestHours", newest == 0 ? -1 : Math.round((System.currentTimeMillis() - newest) / 360000.0) / 10.0);
            o.put("avgDesc", items.isEmpty() ? 0 : desc / items.size());
            JSONArray arts = new JSONArray();
            for (int i = 0; i < items.size() && arts.length() < samples; i++) arts.put(article(items.get(i).optString("u"), items.get(i).optString("t")));
            o.put("articles", arts);
        } catch (Throwable e) {
            try { o.put("error", String.valueOf(e)); } catch (Exception ignored) {}
        }
        return o;
    }

    static JSONObject article(String u, String title) {
        JSONObject o = new JSONObject();
        try {
            o.put("title", Text.cap(title, 90));
            String real = GNews.isGn(u) ? GNews.resolve(u, 12000) : u;
            o.put("url", real == null ? u : real);
            if (real == null) { o.put("error", "google news link not resolved"); return o; }
            Net.Resp r = Net.page(real, 20000);
            o.put("http", r.code).put("bytes", r.body == null ? 0 : r.body.length());
            if (r.code != 200 || r.body == null) return o;
            String b = r.body;
            Html.Article old = Html.article(b, real);
            int oc = 0; for (String p : old.paras) oc += p.length();
            o.put("old", new JSONObject().put("paras", old.paras.size()).put("chars", oc).put("date", old.published > 0).put("start", old.paras.isEmpty() ? "" : Text.cap(old.paras.get(0), 140)));
            try {
                Reader.Result nr = Reader.read(b, real, null);
                o.put("new", new JSONObject().put("method", nr.method).put("paras", nr.article.paras.size()).put("chars", nr.chars).put("score", nr.score).put("date", nr.article.published > 0).put("start", nr.article.paras.isEmpty() ? "" : Text.cap(nr.article.paras.get(0), 140)).put("tried", nr.tried));
            } catch (Throwable e) { o.put("newError", String.valueOf(e)); }
            JSONObject sig = new JSONObject();
            sig.put("ldBody", Html.parse(b).ldBody.length());
            sig.put("nextData", b.contains("__NEXT_DATA__"));
            sig.put("nuxt", b.contains("__NUXT__"));
            sig.put("wp", b.contains("api.w.org") || b.contains("/wp-content/"));
            Matcher am = Pattern.compile("<link[^>]+rel=[\"']amphtml[\"'][^>]*>", Pattern.CASE_INSENSITIVE).matcher(b);
            sig.put("amp", am.find() ? Html.attrOf(am.group(), "href") : "");
            sig.put("itemprop", b.contains("itemprop=\"articleBody\"") || b.contains("itemprop='articleBody'"));
            sig.put("paywall", b.contains("isAccessibleForFree\":false") || b.contains("\"isAccessibleForFree\": false") || b.contains("isAccessibleForFree\":\"False\""));
            Map<String, Integer> cls = new LinkedHashMap<String, Integer>();
            Matcher m = CLS.matcher(b);
            while (m.find() && cls.size() < 12) { String c = m.group(3).trim(); if (c.length() < 60) cls.put(c, cls.containsKey(c) ? cls.get(c) + 1 : 1); }
            sig.put("classes", new JSONObject(cls));
            o.put("sig", sig);
            if (!o.has("new") || o.getJSONObject("new").optInt("chars") < 600) o.put("outline", outline(b));
        } catch (Throwable e) {
            try { o.put("error", String.valueOf(e)); } catch (Exception ignored) {}
        }
        return o;
    }

    /** Where the text sits on a page we could not read: tag.class of elements with the most direct text (lengths only). */
    static JSONArray outline(String html) {
        List<String[]> rows = new ArrayList<String[]>();
        Matcher m = Pattern.compile("<(div|section|article|td|span|main|font|p)\\b([^>]*)>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (m.find() && rows.size() < 4000) {
            String attrs = m.group(2);
            Matcher c = Pattern.compile("(class|id)=[\"']([^\"']{1,60})").matcher(attrs);
            String name = m.group(1).toLowerCase() + (c.find() ? "." + c.group(2).trim().replace(' ', '.') : "");
            int end = html.indexOf('<', m.end());
            String txt = end > m.end() ? html.substring(m.end(), end).replaceAll("&nbsp;|\\s+", " ").trim() : "";
            int next = m.end(); int tot = 0; int k = 0;
            // text up to 30 tags further: a rough measure of the prose inside
            Matcher t = Pattern.compile(">([^<]{20,})<").matcher(html);
            t.region(m.end() - 1, Math.min(html.length(), m.end() + 6000));
            while (t.find() && k++ < 30) tot += t.group(1).trim().length();
            if (tot > 300) rows.add(new String[]{name, String.valueOf(tot), String.valueOf(txt.length())});
        }
        Collections.sort(rows, new Comparator<String[]>() { public int compare(String[] x, String[] y) { return Integer.parseInt(y[1]) - Integer.parseInt(x[1]); } });
        JSONArray o = new JSONArray();
        Set<String> seen = new HashSet<String>();
        for (String[] r : rows) { if (seen.add(r[0]) && o.length() < 12) o.put(r[0] + " " + r[1] + "/" + r[2]); }
        return o;
    }

    static JSONObject sitemapInfo(String b) throws Exception {
        JSONObject o = new JSONObject();
        int urls = 0, news = 0, dates = 0;
        Matcher m = Pattern.compile("<url>(.*?)</url>", Pattern.DOTALL).matcher(b);
        JSONArray first = new JSONArray();
        while (m.find()) { urls++; String u = m.group(1); if (u.contains("news:title")) news++; if (u.contains("publication_date") || u.contains("lastmod")) dates++; if (first.length() < 2) { Matcher l = Pattern.compile("<loc>\\s*(?:<!\\[CDATA\\[)?([^<\\]]+)").matcher(u); if (l.find()) first.put(l.group(1).trim()); } }
        int subs = 0; Matcher s = Pattern.compile("<sitemap>").matcher(b); while (s.find()) subs++;
        return o.put("urls", urls).put("newsTitles", news).put("dated", dates).put("subSitemaps", subs).put("first", first);
    }

    static JSONObject robots(String host) {
        JSONObject o = new JSONObject();
        try {
            o.put("host", host);
            Net.Resp r = Net.get("https://" + host + "/robots.txt", 15000, "text/plain,*/*");
            o.put("http", r.code);
            JSONArray maps = new JSONArray();
            if (r.code == 200 && r.body != null) for (String l : r.body.split("\n")) if (l.toLowerCase().startsWith("sitemap:")) maps.put(l.substring(8).trim());
            o.put("listed", maps);
            JSONArray checked = new JSONArray();
            for (int i = 0; i < maps.length() && checked.length() < 4; i++) {
                String u = maps.getString(i);
                if (!u.toLowerCase().contains("news") && maps.length() > 3) continue;
                try {
                    Net.Resp sr = Net.get(u, 20000);
                    JSONObject c = new JSONObject().put("url", u).put("http", sr.code);
                    if (sr.code == 200 && sr.body != null) c.put("info", sitemapInfo(sr.body));
                    checked.put(c);
                } catch (Throwable e) { checked.put(new JSONObject().put("url", u).put("error", String.valueOf(e))); }
            }
            o.put("checked", checked);
        } catch (Throwable e) {
            try { o.put("error", String.valueOf(e)); } catch (Exception ignored) {}
        }
        return o;
    }

    static JSONObject coach(String u) {
        JSONObject o = new JSONObject();
        try {
            o.put("url", u);
            Net.Resp r = Net.page(u, 25000);
            o.put("http", r.code).put("bytes", r.body == null ? 0 : r.body.length());
            if (r.code == 200 && r.body != null) {
                List<Extract.CoachArticle> d = Extract.daily(r.body, u);
                int secs = 0; for (Extract.CoachArticle x : d) secs += x.sections.size();
                o.put("topics", d.size()).put("sections", secs);
                JSONArray t = new JSONArray(); for (int i = 0; i < d.size() && i < 4; i++) t.put(Text.cap(d.get(i).title, 80));
                o.put("titles", t);
                o.put("nextData", r.body.contains("__NEXT_DATA__")).put("wp", r.body.contains("api.w.org"));
            }
        } catch (Throwable e) {
            try { o.put("error", String.valueOf(e)); } catch (Exception ignored) {}
        }
        return o;
    }

    static String md(JSONObject rep) throws Exception {
        StringBuilder b = new StringBuilder("# Source check\n\n| Source | Feed | HTTP | Items | Newest (h) | Articles read (old → new) |\n|---|---|---|---|---|---|\n");
        JSONArray f = rep.getJSONArray("feeds");
        for (int i = 0; i < f.length(); i++) {
            JSONObject o = f.getJSONObject(i);
            JSONArray arts = o.optJSONArray("articles");
            StringBuilder ar = new StringBuilder();
            for (int j = 0; arts != null && j < arts.length(); j++) {
                JSONObject x = arts.getJSONObject(j);
                JSONObject ol = x.optJSONObject("old"), nw = x.optJSONObject("new");
                ar.append(ol == null ? "x" : ol.optInt("chars")).append("→").append(nw == null ? "x" : nw.optInt("chars") + " " + nw.optString("method")).append("; ");
            }
            String u = o.optString("url");
            b.append("| ").append(o.optString("code")).append(o.optBoolean("candidate") ? " (candidate)" : "").append(" | ").append(u.length() > 70 ? u.substring(0, 70) + "…" : u)
             .append(" | ").append(o.optInt("http")).append(" | ").append(o.has("sitemap") ? "sitemap " + o.getJSONObject("sitemap").optInt("urls") : String.valueOf(o.optInt("items")))
             .append(" | ").append(o.optDouble("newestHours", -1)).append(" | ").append(ar).append(o.has("error") ? " ERR " + o.optString("error") : "").append(" |\n");
        }
        b.append("\n## Coaching pages\n\n");
        JSONArray c = rep.getJSONArray("coaching");
        for (int i = 0; i < c.length(); i++) { JSONObject o = c.getJSONObject(i); b.append("- ").append(o.optString("url")).append(": HTTP ").append(o.optInt("http")).append(", ").append(o.optInt("topics")).append(" topics, ").append(o.optInt("sections")).append(" sections").append(o.has("error") ? " ERR " + o.optString("error") : "").append('\n'); }
        b.append("\n## Sitemaps\n\n");
        JSONArray s = rep.getJSONArray("sitemaps");
        for (int i = 0; i < s.length(); i++) b.append("- ").append(s.getJSONObject(i).toString()).append('\n');
        return b.toString();
    }
}
