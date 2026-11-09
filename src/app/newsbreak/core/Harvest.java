package app.newsbreak.core;

import app.newsbreak.core.EditionBuilder;
import app.newsbreak.core.Extract;
import app.newsbreak.core.Html;
import app.newsbreak.core.Net;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class Harvest {
    public static final int LOOKBACK_DAYS = 10;
    static final long REFRESH_MS = 9000000;
    public static final String VAJIRAM_INDEX = "https://vajiramandravi.com/current-affairs/";
    public static final String VISION_INDEX = "https://visionias.in/current-affairs/news-today";
    public static final Object WRITE = new Object();
    static final String[] MONTHS = {"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"};

    private Harvest() {
    }

    public static String drishtiUrl(String str) {
        String[] split = str.split("-");
        return "https://www.drishtiias.com/current-affairs-news-analysis-editorials/news-analysis/" + split[2] + "-" + split[1] + "-" + split[0];
    }

    public static String insightsUrl(String str) {
        String[] split = str.split("-");
        return "https://www.insightsonindia.com/" + split[0] + "/" + split[1] + "/" + split[2] + "/upsc-current-affairs-" + Integer.parseInt(split[2]) + "-" + MONTHS[Integer.parseInt(split[1]) - 1] + "-" + split[0] + "/";
    }

    static final class Job {
        int age;
        String code;
        boolean daily;
        String date;
        String url;

        Job(String str, String str2, String str3, boolean z, int i) {
            this.code = str;
            this.url = str2;
            this.date = str3;
            this.daily = z;
            this.age = i;
        }
    }

    static JSONObject page(JSONObject jSONObject, String str) throws Exception {
        JSONObject optJSONObject = jSONObject.optJSONObject("pages");
        if (optJSONObject == null) {
            optJSONObject = new JSONObject();
            jSONObject.put("pages", optJSONObject);
        }
        JSONObject optJSONObject2 = optJSONObject.optJSONObject(str);
        if (optJSONObject2 != null) {
            return optJSONObject2;
        }
        JSONObject jSONObject2 = new JSONObject();
        optJSONObject.put(str, jSONObject2);
        return jSONObject2;
    }

    static boolean due(JSONObject jSONObject, int i, long j) {
        boolean optBoolean = jSONObject.optBoolean("ok");
        long optLong = jSONObject.optLong("at");
        int optInt = jSONObject.optInt("tries");
        if (optBoolean && jSONObject.optInt("n") > 0) {
            return i <= 1 && j - optLong >= REFRESH_MS;
        }
        if (!optBoolean || i < 2 || optInt < 3) {
            return i < 2 || optInt < 4;
        }
        return false;
    }

    public static List<JSONObject> coaching(final JSONObject jSONObject, Set<String> set, final long j, final Kb kb, long j2) throws Exception {
        ArrayList arrayList;
        final List synchronizedList = Collections.synchronizedList(new ArrayList());
        String dateKey = Ist.dateKey(j);
        ArrayList<Job> arrayList2 = new ArrayList();
        for (int i = 0; i <= 10; i++) {
            String addDays = Ist.addDays(dateKey, -i);
            if (set.contains("Drishti")) {
                String drishtiUrl = drishtiUrl(addDays);
                synchronized (jSONObject) {
                    if (due(page(jSONObject, drishtiUrl), i, j)) {
                        arrayList2.add(new Job("Drishti", drishtiUrl, addDays, true, i));
                    }
                }
            }
            if (set.contains("Insights")) {
                String insightsUrl = insightsUrl(addDays);
                synchronized (jSONObject) {
                    if (due(page(jSONObject, insightsUrl), i, j)) {
                        arrayList2.add(new Job("Insights", insightsUrl, addDays, true, i));
                    }
                }
            }
        }
        if (set.contains("Vision")) {
            arrayList2.addAll(indexJobs(jSONObject, "Vision", VISION_INDEX, j, Pattern.compile("href=[\"']((?:https?://(?:www\\.)?visionias\\.in)?/current-affairs/news-today/(\\d{4}-\\d{2}-\\d{2})/[a-z0-9\\-]+/[a-z0-9\\-]+)[\"'#?]"), 12));
        }
        if (set.contains("Vajiram")) {
            arrayList2.addAll(indexJobs(jSONObject, "Vajiram", VAJIRAM_INDEX, j, Pattern.compile("href=[\"']((?:https?://(?:www\\.)?vajiramandravi\\.com)?/current-affairs/([a-z0-9\\-]{6,})/)[\"']"), 10));
        }
        final long currentTimeMillis = System.currentTimeMillis() + j2;
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(4);
        for (final Job job : arrayList2) {
            newFixedThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() > currentTimeMillis) return;
                    JSONObject pg;
                    synchronized (jSONObject) {
                        try { pg = Harvest.page(jSONObject, job.url); } catch (Exception e) { return; }
                    }
                    boolean ok = false;
                    int n = 0;
                    try {
                        Net.Resp r = Harvest.fetch(job.url, 20000);
                        // a day page that redirects to another day's page (not published yet) is not that day's page
                        boolean rightPage = !job.daily || r.finalUrl == null || r.finalUrl.equals(job.url) || r.finalUrl.contains(Harvest.dateToken(job.code, job.date));
                        if (r.code == 200 && r.body.length() > 500 && rightPage) {
                            ok = true;
                            List<Extract.CoachArticle> arts = new ArrayList<Extract.CoachArticle>();
                            if (job.daily) arts.addAll(Extract.daily(r.body, job.url));
                            else {
                                Extract.CoachArticle a = Extract.single(r.body, job.url);
                                if (!a.sections.isEmpty()) arts.add(a);
                            }
                            String date = job.date;
                            if (date == null) { // single article: its own published date
                                Html.Doc d = Html.parse(r.body);
                                long pt = Ist.parse(Html.first(d.ldDate, d.published));
                                date = Ist.dateKey(pt > 0 ? pt : j);
                                if (pt > 0 && pt < j - 11 * Ist.DAY) arts.clear(); // too old for the look-back
                            }
                            for (Extract.CoachArticle a : arts) {
                                if (EditionBuilder.promo(a.title)) continue;
                                JSONObject it = Harvest.item(job.code, a, date, j);
                                if (kb != null) kb.score(it);
                                Harvest.coachFloor(it);
                                synchronized (synchronizedList) {
                                    if (atomicBoolean.get()) break;
                                    synchronizedList.add(it);
                                }
                                n++;
                            }
                        }
                    } catch (Throwable t) {
                        // ok stays as it was: a read error after a good fetch still counts as "page seen"
                    }
                    synchronized (jSONObject) {
                        if (atomicBoolean.get()) return;
                        try {
                            pg.put("at", j).put("ok", ok).put("n", n).put("tries", ok && n > 0 ? 0 : pg.optInt("tries") + 1).put("code", job.code);
                            if (job.date != null) pg.put("date", job.date);
                        } catch (Exception ignored) {}
                    }
                }
            });
        }
        newFixedThreadPool.shutdown();
        newFixedThreadPool.awaitTermination(Math.max(1000L, 5000 + j2), TimeUnit.MILLISECONDS);
        newFixedThreadPool.shutdownNow();
        synchronized (synchronizedList) {
            atomicBoolean.set(true);
            arrayList = new ArrayList(synchronizedList);
        }
        synchronized (jSONObject) {
            prune(jSONObject, j);
        }
        return arrayList;
    }

    static String dateToken(String str, String str2) {
        if (str2 == null) {
            return "";
        }
        String[] split = str2.split("-");
        return "Drishti".equals(str) ? split[2] + "-" + split[1] + "-" + split[0] : "/" + split[0] + "/" + split[1] + "/" + split[2] + "/";
    }

    /** Article links from a coaching site's index page (refreshed every 2.5 h); unread links of the last 10 days become jobs. */
    static List<Job> indexJobs(JSONObject state, String code, String indexUrl, long now, Pattern linkRe, int max) {
        List<Job> out = new ArrayList<Job>();
        try {
            JSONObject pg;
            synchronized (state) { pg = page(state, indexUrl); }
            JSONArray links = pg.optJSONArray("links");
            if (now - pg.optLong("at") >= REFRESH_MS || links == null) {
                Net.Resp r = fetch(indexUrl, 20000);
                synchronized (state) { pg.put("at", now).put("ok", r.code == 200).put("n", 1).put("tries", r.code == 200 ? 0 : pg.optInt("tries") + 1); }
                if (r.code == 200) {
                    JSONArray fresh = new JSONArray();
                    Set<String> seen = new HashSet<String>();
                    Matcher m = linkRe.matcher(r.body);
                    while (m.find() && fresh.length() < 60) {
                        String u = m.group(1);
                        if (u.startsWith("/")) u = indexUrl.replaceFirst("^(https?://[^/]+).*$", "$1") + u;
                        String l = u.toLowerCase(Locale.ROOT);
                        if (l.contains("quiz") || l.contains("/page/") || l.contains("/category/") || l.contains("/tag/") || l.endsWith("/current-affairs/")) continue;
                        if (seen.add(u)) fresh.put(u);
                    }
                    synchronized (state) { pg.put("links", fresh); }
                    links = fresh;
                }
            }
            String today = Ist.dateKey(now);
            for (int i = 0; links != null && i < links.length() && out.size() < max; i++) {
                String u = links.optString(i);
                Matcher dm = Pattern.compile("/(\\d{4}-\\d{2}-\\d{2})/").matcher(u);
                String date = dm.find() ? dm.group(1) : null;
                int age = 0;
                if (date != null) {
                    long days = (Ist.startOf(today) - Ist.startOf(date)) / Ist.DAY;
                    if (days > 10 || days < -1) continue; // outside the look-back
                    age = (int) Math.max(0, days);
                }
                JSONObject ap;
                synchronized (state) { ap = page(state, u); }
                if (!ap.optBoolean("ok") && ap.optInt("tries") < 3) out.add(new Job(code, u, date, false, age));
            }
        } catch (Throwable ignored) {}
        return out;
    }

    static void coachFloor(JSONObject jSONObject) throws Exception {
        if (!jSONObject.has("noise") && jSONObject.optDouble("sc", 0.0d) < 4.0d) {
            jSONObject.put("sc", 4.0d);
        }
    }

    public static JSONObject item(String str, Extract.CoachArticle coachArticle, String str2, long j) throws Exception {
        JSONObject coachJson = Extract.coachJson(coachArticle);
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("id", Text.hash(str + "|" + coachArticle.title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim()));
        jSONObject.put("t", coachArticle.title);
        StringBuilder sb = new StringBuilder();
        JSONArray optJSONArray = coachJson.optJSONArray("news");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length() && sb.length() < 600; i++) {
            sb.append(optJSONArray.optString(i)).append(' ');
        }
        JSONObject background = Extract.background(coachJson, coachArticle.title, 3);
        if (background != null) {
            JSONArray jSONArray = background.getJSONArray("pts");
            for (int i2 = 0; i2 < jSONArray.length() && sb.length() < 900; i2++) {
                sb.append(jSONArray.optString(i2)).append(' ');
            }
        }
        jSONObject.put("d", Text.cap(sb.toString().trim(), 900));
        jSONObject.put("u", coachArticle.url);
        jSONObject.put("s", str);
        jSONObject.put("f", "Daily page");
        jSONObject.put("p", Math.min(Ist.startOf(str2) + 32400000, j));
        jSONObject.put("pd", true);
        jSONObject.put("fs", j);
        jSONObject.put("cx", coachJson);
        jSONObject.put("full", true);
        return jSONObject;
    }

    static void prune(JSONObject jSONObject, long j) {
        try {
            JSONObject optJSONObject = jSONObject.optJSONObject("pages");
            if (optJSONObject != null) {
                JSONArray names = optJSONObject.names();
                int i = 0;
                while (names != null) {
                    if (i < names.length()) {
                        JSONObject optJSONObject2 = optJSONObject.optJSONObject(names.optString(i));
                        long j2 = (optJSONObject2 == null || !optJSONObject2.has("date")) ? 5184000000L : 1036800000L;
                        if (optJSONObject2 != null && j - optJSONObject2.optLong("at") > j2) {
                            optJSONObject.remove(names.optString(i));
                        }
                        i++;
                    } else {
                        return;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    static Net.Resp fetch(String str, int i) throws Exception {
        Net.Resp page = Net.page(str, i);
        if (page.code >= 500 || page.code == 429) {
            try {
                Thread.sleep(1500L);
            } catch (InterruptedException e) {
            }
            return Net.page(str, i);
        }
        return page;
    }

    public static int articles(List<JSONObject> list, long j, long j2, int i) throws Exception {
        boolean z;
        ArrayList arrayList = new ArrayList();
        for (JSONObject jSONObject : list) {
            if (jSONObject.optLong("p") >= j - 187200000 && jSONObject.optDouble("sc", 0.0d) >= 2.0d && !jSONObject.has("noise")) {
                arrayList.add(jSONObject);
            }
        }
        List<EditionBuilder.Cluster> cluster = EditionBuilder.cluster(arrayList);
        ArrayList<List<JSONObject>> arrayList2 = new ArrayList<List<JSONObject>>();
        int i2 = 0;
        for (EditionBuilder.Cluster cluster2 : cluster) {
            int i3 = i2 + 1;
            if (i2 >= 40 || cluster2.score < 5.0d) {
                break;
            }
            boolean z2 = false;
            Iterator<JSONObject> it = cluster2.items.iterator();
            while (true) {
                z = z2;
                if (!it.hasNext()) {
                    break;
                }
                JSONObject next = it.next();
                if (next.optJSONArray("x") != null && next.optJSONArray("x").length() >= 2 && !EditionBuilder.isCoaching(next)) {
                    z = true;
                }
                z2 = z;
            }
            if (!z) {
                ArrayList<JSONObject> arrayList3 = new ArrayList(cluster2.items);
                Collections.sort(arrayList3, new Comparator<JSONObject>() { // from class: app.newsbreak.core.Harvest.2
                    @Override // java.util.Comparator
                    public int compare(JSONObject jSONObject2, JSONObject jSONObject3) {
                        int i4 = GNews.isGn(jSONObject2.optString("u")) ? 1 : 0;
                        int i5 = GNews.isGn(jSONObject3.optString("u")) ? 1 : 0;
                        return i4 != i5 ? i4 - i5 : Integer.compare(EditionBuilder.srcRank(jSONObject2.optString("s")), EditionBuilder.srcRank(jSONObject3.optString("s")));
                    }
                });
                ArrayList arrayList4 = new ArrayList();
                for (JSONObject jSONObject2 : arrayList3) {
                    if (jSONObject2.optInt("xa") < 2 && jSONObject2.optString("u").length() >= 10 && (!EditionBuilder.isCoaching(jSONObject2) || !jSONObject2.has("cx"))) {
                        arrayList4.add(jSONObject2);
                        if (arrayList4.size() >= 3) {
                            break;
                        }
                    }
                }
                if (!arrayList4.isEmpty()) {
                    arrayList2.add(arrayList4);
                }
                if (arrayList2.size() >= i) {
                    break;
                }
                i2 = i3;
            } else {
                i2 = i3;
            }
        }
        final long currentTimeMillis = System.currentTimeMillis() + j2;
        final int[] iArr = {0};
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(5);
        for (final List<JSONObject> pick : arrayList2) {
            newFixedThreadPool.submit(new Runnable() {
                public void run() {
                    for (JSONObject it : pick) { // first outlet that can be read wins
                        if (System.currentTimeMillis() > currentTimeMillis) return;
                        if (atomicBoolean.get()) return;
                        if (Harvest.readOne(it, atomicBoolean)) { synchronized (iArr) { iArr[0]++; } return; }
                    }
                }
            });
        }
        newFixedThreadPool.shutdown();
        newFixedThreadPool.awaitTermination(Math.max(1000L, 5000 + j2), TimeUnit.MILLISECONDS);
        newFixedThreadPool.shutdownNow();
        synchronized (WRITE) {
            atomicBoolean.set(true);
        }
        return iArr[0];
    }

    /** Reads one item's full article; true on success. */
    static boolean readOne(JSONObject it, java.util.concurrent.atomic.AtomicBoolean shut) {
        try {
            synchronized (WRITE) { if (shut.get()) return false; it.put("xa", it.optInt("xa") + 1); }
            String u = GNews.resolve(it.optString("u"), 12000);
            if (u == null) return false;
            Net.Resp r = fetch(u, 15000);
            if (r.code != 200 || r.body.length() < 500) return false;
            if (EditionBuilder.isCoaching(it)) {
                Extract.CoachArticle a = Extract.single(r.body, u);
                if (a.sections.isEmpty()) return false;
                synchronized (WRITE) { if (shut.get()) return false; it.put("cx", Extract.coachJson(a)); it.put("full", true); coachFloor(it); }
            } else {
                Html.Article a = Html.article(r.body, u);
                if (a.paras.isEmpty()) {
                    if (a.desc.length() > 60) synchronized (WRITE) { if (shut.get()) return false; if (it.optString("d").length() < a.desc.length()) it.put("d", Text.cap(a.desc, 600)); }
                    return false;
                }
                List<String> ks = Extract.keySentences(a, 6), ds = Extract.definitions(a, 3);
                StringBuilder b = new StringBuilder();
                for (String p : a.paras) { if (b.length() > 1800) break; b.append(p).append('\n'); }
                synchronized (WRITE) { if (shut.get()) return false;
                    it.put("x", new JSONArray(ks));
                    if (!ds.isEmpty()) it.put("xd", new JSONArray(ds));
                    it.put("xb", Text.cap(b.toString().trim(), 1900));
                    if (!u.equals(it.optString("u"))) it.put("xu", u);
                    if (it.optString("d").length() < 40 && a.desc.length() > 60) it.put("d", Text.cap(a.desc, 600));
                }
            }
            return true;
        } catch (Throwable ignored) { return false; }
    }

    static Map<String, Object> unused() {
        return new LinkedHashMap();
    }
}
