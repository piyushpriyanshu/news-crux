package app.newsbreak.core;

/** Test access to package-private parts of the core. */
public final class GNewsAccess {
    public static java.util.regex.Pattern res() { return GNews.RES; }
    public static String unescape(String s) { return GNews.unescape(s); }
    public static org.json.JSONObject figureSources(java.util.List<org.json.JSONObject> items) { return EditionBuilder.figureSources(items); }
    public static java.util.List<RssParser.Raw> sitemap(String s) { return RssParser.sitemap(s); }
    public static boolean devanagari(String s) { return RssParser.devanagari(s); }
}
