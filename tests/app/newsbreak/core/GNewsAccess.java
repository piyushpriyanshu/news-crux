package app.newsbreak.core;

/** Test access to package-private parts of GNews. */
public final class GNewsAccess {
    public static java.util.regex.Pattern res() { return GNews.RES; }
    public static String unescape(String s) { return GNews.unescape(s); }
}
