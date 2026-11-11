import app.newsbreak.core.*;
import app.newsbreak.core.GNewsAccess;
import java.util.*;

/** Article reading: structured data first, page containers next, Google News addresses intact (run by tests/run.sh). */
public class ReaderTest {
    static int pass = 0, fail = 0;
    static void ok(boolean c, String w) { if (c) pass++; else { fail++; System.out.println("FAIL: " + w); } }
    static String para(String s, int n) { StringBuilder b = new StringBuilder(); for (int i = 0; i < n; i++) b.append("<p>").append(s).append(" Sentence number ").append(i).append(" adds one more fact about the scheme and its outlay of Rs 1,200 crore.</p>"); return b.toString(); }
    static String sentences(String s, int n) { StringBuilder b = new StringBuilder(); for (int i = 0; i < n; i++) b.append(s).append(", point ").append(i + 1).append(". "); return b.toString(); }
    public static void main(String[] a) throws Exception {
        String menu = "<nav><ul><li><a href='/'>Home</a></li><li><a href='/x'>India news and politics today in the capital</a></li></ul></nav>";
        // 1. JSON-LD article text wins
        String ld = "<html><head><script type='application/ld+json'>{\"@type\":\"NewsArticle\",\"headline\":\"Cabinet approves mission\",\"datePublished\":\"2026-11-10T10:00:00+05:30\",\"articleBody\":\"" + sentences("The Union Cabinet approved the National Critical Mineral Mission with an outlay of Rs 16,300 crore", 12) + "\"}</script></head><body>" + menu + "<p>Short teaser only.</p></body></html>";
        Reader.Result r = Reader.read(ld, "https://example.com/a", null);
        ok("ld".equals(r.method) && r.chars > 800, "ld wins: " + r.method + " " + r.chars);
        ok(r.article.published > 0, "date from JSON-LD");
        // 2. PIB-style container without <p> structure that the generic method misses
        String pib = "<html><body>" + menu + "<div class='innner-page-main-about-us-content-right-part'><div>Ministry of Mines</div>" + para("The Ministry of Mines launched the auction of 20 critical mineral blocks.", 8) + "</div><div class='footer'>Visitor counter</div></body></html>";
        r = Reader.read(pib, "https://pib.gov.in/PressReleaseIframePage.aspx?PRID=1", null);
        ok(r.chars > 600 && !r.article.paras.toString().contains("Visitor"), "PIB container read: " + r.method + " " + r.chars);
        // 3. Next.js data
        String body = "<p>" + sentences("RBI kept the repo rate unchanged at 5.5 per cent and retained the neutral stance", 10) + "</p>";
        String next = "<html><body><div id='__next'></div><script id='__NEXT_DATA__' type='application/json'>{\"props\":{\"pageProps\":{\"story\":{\"title\":\"x\",\"content\":" + org.json.JSONObject.quote(body) + "}}}}</script></body></html>";
        r = Reader.read(next, "https://example.com/n", null);
        ok("next".equals(r.method) && r.chars > 600, "Next.js data read: " + r.method + " " + r.chars);
        // 4. promo lines never become paragraphs
        String promo = "<html><body><article class='story-details'>" + para("India and Japan signed a pact on semiconductors.", 6) + "<p>Also read: Ten things to know before you buy a phone this festive season, click here now.</p><p>Subscribe to our newsletter to get the top stories in your inbox every morning, it is free.</p></article></body></html>";
        r = Reader.read(promo, "https://example.com/p", null);
        String all = String.join(" ", r.article.paras);
        ok(!all.contains("Also read") && !all.contains("Subscribe"), "promo dropped");
        // 5. a PDF is not a page
        r = Reader.read("%PDF-1.7 binary", "https://isro.gov.in/x.pdf", null);
        ok(r.article.paras.isEmpty() && r.chars == 0, "PDF skipped");
        // 6. AMP copy when the page gives too little
        String thin = "<html><head><link rel='amphtml' href='/amp/a'></head><body>" + menu + "<p>Teaser.</p></body></html>";
        final String amp = "<html><body><div class='article-body'>" + para("The Supreme Court upheld the validity of the Act.", 7) + "</div></body></html>";
        r = Reader.read(thin, "https://example.com/a", new Reader.Fetcher() { public String get(String u) { return u.endsWith("/amp/a") ? amp : null; } });
        ok("amp".equals(r.method) && r.chars > 600, "AMP fallback: " + r.method + " " + r.chars);
        // 7. Google News reply: the address keeps its query string
        String reply = ")]}'\n\n[[\"wrb.fr\",\"Fbv4je\",\"[\\\"garturlres\\\",\\\"https://www.pib.gov.in/PressReleaseDetail.aspx?PRID\\\\u003d2335185\\\\u0026reg\\\\u003d3\\\",1]\",null,null,null,\"generic\"]]";
        java.util.regex.Matcher m = GNewsAccess.res().matcher(reply);
        ok(m.find() && GNewsAccess.unescape(m.group(1)).equals("https://www.pib.gov.in/PressReleaseDetail.aspx?PRID=2335185&reg=3"), "Google News address intact: " + (m.find(0) ? GNewsAccess.unescape(m.group(1)) : "no match"));
        System.out.println("ReaderTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
