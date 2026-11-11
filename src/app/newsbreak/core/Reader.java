package app.newsbreak.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Reads the text of a news article, trying the most reliable source inside the page first:
 * <ol>
 * <li><b>ld</b>: the article text the publisher gives search engines (JSON-LD articleBody);</li>
 * <li><b>next</b>: the article data a Next.js site embeds in the page (__NEXT_DATA__);</li>
 * <li><b>body</b>: the page's article container (itemprop="articleBody", "story-details", "entry-content" …);</li>
 * <li><b>blocks</b>: the densest run of paragraphs (the old generic method);</li>
 * <li><b>amp</b> / <b>wp</b>: when the page itself gives too little, its AMP copy or the WordPress post data.</li>
 * </ol>
 * Every result is scored (length, sentences, no menu or promo lines) and the best one wins, so a redesign that breaks
 * one method falls back to the next instead of returning page furniture.
 */
public final class Reader {
    private Reader() {}

    /** Fetches a follow-up URL (AMP copy, WordPress post data); returns the body or null. */
    public interface Fetcher { String get(String url); }

    public static final class Result {
        public Html.Article article = new Html.Article();
        public String method = "none";
        public int chars;
        public double score;
        public String tried = "";
    }

    /** Containers that hold the article text on most news and coaching sites. */
    static final Pattern BODY = Pattern.compile("<(div|section|article|main)\\b[^>]*?(?:itemprop=[\"']articleBody[\"']|\\b(?:class|id)=[\"'][^\"']*?\\b("
            + "articlebodycontent|article-body|articlebody|article_body|article__body|article-content|article__content|articlecontent|story-body|storybody|story_body|"
            + "story-details|storydetails|storyDetails|story-content|story_content|full-details|fullstory|artText|art_text|entry-content|post-content|post_content|"
            + "td-post-content|content-body|detail-body|detail_body|news-detail|newsdetail|article-text|main-content|innner-page-main-about-us-content-right-part|"
            + "ReleaseText|releaseText|pressrelease|press-release|_s30J|clearfix\\s+article)\\b[^\"']*[\"'])[^>]*>", Pattern.CASE_INSENSITIVE);
    static final Pattern AMP = Pattern.compile("<link[^>]+rel=[\"']amphtml[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    static final Pattern WPJSON = Pattern.compile("<link[^>]+type=[\"']application/json[\"'][^>]*href=[\"']([^\"']+/wp-json/wp/v2/(?:posts|pages)/\\d+)[\"']|<link[^>]+href=[\"']([^\"']+/wp-json/wp/v2/(?:posts|pages)/\\d+)[\"'][^>]*type=[\"']application/json[\"']", Pattern.CASE_INSENSITIVE);
    static final Pattern NEXT = Pattern.compile("<script[^>]+id=[\"']__NEXT_DATA__[\"'][^>]*>(.*?)</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    /** Lines that are page furniture or promotion, not the story. */
    static final Pattern PROMO = Pattern.compile("(?i)(also read|read more|read also|click here|subscribe|sign up|newsletter|download the app|follow us|join our|whatsapp|telegram|"
            + "premium|unlock this|log ?in to|already a subscriber|advertisement|sponsored|copyright|all rights reserved|cookie|privacy policy|terms of use|"
            + "share this|comments? \\(|you may also like|recommended for you|trending now|watch:|listen to this|for more news|catch the latest)");

    public static Result read(String html, String url, Fetcher f) {
        Html.Doc doc = Html.parse(html);
        List<Result> rs = new ArrayList<Result>();
        rs.add(fromText(doc.ldBody, "ld"));
        rs.add(fromNext(html));
        rs.add(fromBody(html));
        rs.add(fromBlocks(doc));
        Result best = best(rs);
        if (best.score < 3.0 && f != null) {
            // too little from the page itself: the AMP copy or the WordPress post data often has the full text
            Matcher am = AMP.matcher(html);
            if (am.find()) {
                String au = Html.attr(am.group(), "href");
                if (au != null && au.length() > 8) {
                    try { au = new java.net.URL(new java.net.URL(url), au).toString(); } catch (Exception e) {}
                    String ab = f.get(au);
                    if (ab != null && ab.length() > 500) {
                        Html.Doc ad = Html.parse(ab);
                        Result a1 = best(listOf(fromText(ad.ldBody, "amp"), fromBody(ab), fromBlocks(ad)));
                        a1.method = "amp";
                        rs.add(a1);
                    }
                }
            }
            Matcher wm = WPJSON.matcher(html);
            if (wm.find()) {
                String wu = wm.group(1) != null ? wm.group(1) : wm.group(2);
                String wb = f.get(wu.replace("&amp;", "&"));
                if (wb != null) {
                    try {
                        JSONObject p = new JSONObject(wb);
                        JSONObject c = p.optJSONObject("content");
                        if (c != null) rs.add(fromHtml(c.optString("rendered"), "wp"));
                    } catch (Exception e) {}
                }
            }
            best = best(rs);
        }
        StringBuilder tried = new StringBuilder();
        for (Result r : rs) tried.append(r.method).append(':').append(r.chars).append(' ');
        best.tried = tried.toString().trim();
        Html.Article a = best.article;
        a.url = url;
        a.title = Html.first(doc.ldHeadline, doc.ogTitle, doc.title);
        a.desc = Html.first(doc.desc, doc.ldDesc);
        a.published = Ist.parse(Html.first(doc.ldDate, doc.published));
        if (a.published <= 0) a.published = RssParser.urlDate(url);
        return best;
    }

    static List<Result> listOf(Result... r) { List<Result> l = new ArrayList<Result>(); for (Result x : r) l.add(x); return l; }

    /** Highest score wins; on a near tie the more reliable method (earlier in the list) is kept. */
    static Result best(List<Result> rs) {
        Result b = null;
        for (Result r : rs) if (r != null && (b == null || r.score > b.score + 0.4)) b = r;
        return b == null ? new Result() : b;
    }

    /** Plain article text (JSON-LD) → paragraphs of a few sentences. */
    static Result fromText(String raw, String method) {
        Result r = new Result();
        r.method = method;
        String t = Text.clean(String.valueOf(raw == null ? "" : raw).replace("\\n", "\n"));
        if (t.contains("<p") || t.contains("<br")) return fromHtml(t, method);
        if (t.length() < 200) return r;
        StringBuilder b = new StringBuilder();
        for (String s : Text.sentences(t)) {
            if (PROMO.matcher(s).find() && s.length() < 160) continue;
            b.append(s).append(' ');
            if (b.length() > 320) { r.article.paras.add(b.toString().trim()); b.setLength(0); }
        }
        if (b.length() > 0) r.article.paras.add(b.toString().trim());
        return scored(r);
    }

    /** An HTML fragment of the article (WordPress content, Next.js body) → paragraphs. */
    static Result fromHtml(String frag, String method) {
        Result r = new Result();
        r.method = method;
        if (frag == null || frag.length() < 100) return r;
        Html.Doc d = Html.parse("<html><body><article>" + frag + "</article></body></html>");
        for (Html.Block bl : d.blocks) if (!"h".equals(bl.tag)) add(r, bl.text);
        return scored(r);
    }

    static Result fromBlocks(Html.Doc doc) {
        Result r = new Result();
        r.method = "blocks";
        for (Html.Block bl : Html.mainBlocks(doc)) if (!"h".equals(bl.tag)) add(r, bl.text);
        return scored(r);
    }

    /** The page's own article container, cut out with its nested tags. */
    static Result fromBody(String html) {
        Result best = new Result();
        best.method = "body";
        Matcher m = BODY.matcher(html);
        int tries = 0;
        while (m.find() && tries++ < 6) {
            String frag = element(html, m.start(), m.group(1).toLowerCase(Locale.ROOT));
            if (frag == null) continue;
            Result r = fromHtml(frag, "body");
            if (r.score > best.score) best = r;
        }
        return best;
    }

    /** The element starting at {@code start} with its whole content (matching nested open/close tags). */
    static String element(String html, int start, String tag) {
        Pattern p = Pattern.compile("<(/?)" + tag + "\\b[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        int depth = 0;
        if (!m.find(start)) return null;
        int open = m.end();
        depth = 1;
        while (m.find()) {
            if (m.group(1).length() == 0) depth++;
            else if (--depth == 0) return html.substring(open, m.start());
            if (m.end() - start > 800000) break;
        }
        return null;
    }

    /** Next.js pages carry the article in a JSON block: take the longest HTML-like or prose string. */
    static Result fromNext(String html) {
        Result r = new Result();
        r.method = "next";
        Matcher m = NEXT.matcher(html);
        if (!m.find()) return r;
        try {
            Object root = new JSONTokener(m.group(1)).nextValue();
            String[] longest = {""};
            walk(root, longest, 0);
            if (longest[0].length() < 300) return r;
            Result x = longest[0].contains("<p") || longest[0].contains("<br") ? fromHtml(longest[0], "next") : fromText(longest[0], "next");
            x.method = "next";
            return x;
        } catch (Throwable e) {
            return r;
        }
    }

    static void walk(Object o, String[] longest, int depth) {
        if (depth > 14 || o == null) return;
        if (o instanceof JSONObject) {
            JSONObject j = (JSONObject) o;
            JSONArray names = j.names();
            for (int i = 0; names != null && i < names.length(); i++) {
                String k = names.optString(i);
                Object v = j.opt(k);
                if (v instanceof String) {
                    String s = (String) v;
                    String kl = k.toLowerCase(Locale.ROOT);
                    boolean bodyKey = kl.contains("body") || kl.contains("content") || kl.contains("description") || kl.contains("text") || kl.equals("html");
                    if (bodyKey && s.length() > longest[0].length() && !s.startsWith("http") && s.indexOf(' ') > 0) longest[0] = s;
                } else walk(v, longest, depth + 1);
            }
        } else if (o instanceof JSONArray) {
            JSONArray a = (JSONArray) o;
            for (int i = 0; i < a.length() && i < 400; i++) walk(a.opt(i), longest, depth + 1);
        }
    }

    static void add(Result r, String text) {
        String t = text == null ? "" : text.trim();
        if (t.length() < 40 || Html.boiler(t)) return;
        if (PROMO.matcher(t).find() && t.length() < 200) return;
        if (r.article.paras.contains(t)) return;
        r.article.paras.add(t);
    }

    /**
     * Score = how much real prose came out: characters (up to ~5,000), sentences per paragraph, and penalties for
     * promo lines, very short lines and link lists. Under 400 characters is not an article.
     */
    static Result scored(Result r) {
        int chars = 0, sent = 0, shortP = 0, promo = 0;
        Set<String> seen = new HashSet<String>();
        List<String> keep = new ArrayList<String>();
        for (String p : r.article.paras) {
            String k = p.toLowerCase(Locale.ROOT);
            if (!seen.add(k)) continue;
            keep.add(p);
            chars += p.length();
            sent += Math.max(1, p.split("[.!?](\\s|$)").length);
            if (p.length() < 70) shortP++;
            if (PROMO.matcher(p).find()) promo++;
        }
        r.article.paras.clear();
        r.article.paras.addAll(keep);
        r.chars = chars;
        if (chars < 400) { r.score = chars / 400.0; return r; }
        double s = Math.min(chars, 5000) / 1000.0 + Math.min(sent, 30) / 10.0;
        s -= promo * 0.6 + Math.max(0, shortP - keep.size() / 3) * 0.3;
        r.score = Math.round(s * 100) / 100.0;
        return r;
    }
}
