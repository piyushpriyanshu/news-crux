package app.newsbreak.core;

import app.newsbreak.core.Net;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class GNews {
    private static final Pattern SG = Pattern.compile("data-n-a-sg=\"([^\"]+)\"");
    private static final Pattern TS = Pattern.compile("data-n-a-ts=\"([^\"]+)\"");
    /** The real address inside Google's reply. It is escaped twice ("=" arrives as \\u003d), so read up to the closing escaped quote, not the first backslash. */
    static final Pattern RES = Pattern.compile("garturlres\\\\+\",\\s*\\\\+\"(https?:.*?)\\\\+\"");

    private GNews() {
    }

    public static boolean isGn(String str) {
        return str != null && str.contains("news.google.com/") && (str.contains("/articles/") || str.contains("/read/"));
    }

    static String id(String str) {
        Matcher matcher = Pattern.compile("/(?:rss/)?(?:articles|read)/([A-Za-z0-9_\\-]+)").matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    static String fromBase64(String str) {
        int indexOf;
        try {
            String replace = str.replace('-', '+').replace('_', '/');
            while (replace.length() % 4 != 0) {
                replace = replace + "=";
            }
            String str2 = new String(base64(replace), "ISO-8859-1");
            if (str2.contains("AU_yqL") || (indexOf = str2.indexOf("http")) < 0) {
                return null;
            }
            int i = indexOf;
            while (i < str2.length() && str2.charAt(i) > ' ' && str2.charAt(i) < 127) {
                i++;
            }
            String substring = str2.substring(indexOf, i);
            if (!substring.matches("https?://[^\\s]+\\.[^\\s]+")) {
                substring = null;
            }
            return substring;
        } catch (Exception e) {
            return null;
        }
    }

    static byte[] base64(String str) {
        char charAt;
        int i;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (i2 < str.length() && (charAt = str.charAt(i2)) != '=') {
            int indexOf = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(charAt);
            if (indexOf < 0) {
                i = i3;
            } else {
                i4 = (i4 << 6) | indexOf;
                i = i3 + 6;
                if (i >= 8) {
                    i -= 8;
                    byteArrayOutputStream.write((i4 >> i) & 255);
                }
            }
            i2++;
            i3 = i;
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static String resolve(String str, int i) {
        if (isGn(str)) {
            String id = id(str);
            if (id == null) {
                return null;
            }
            String fromBase64 = fromBase64(id);
            if (fromBase64 == null) {
                try {
                    Net.Resp page = Net.page("https://news.google.com/rss/articles/" + id, i);
                    if (page.code != 200) {
                        page = Net.page("https://news.google.com/articles/" + id, i);
                    }
                    if (page.code != 200) {
                        return null;
                    }
                    Matcher matcher = SG.matcher(page.body);
                    Matcher matcher2 = TS.matcher(page.body);
                    if (!matcher.find() || !matcher2.find()) {
                        return null;
                    }
                    Net.Resp post = Net.post("https://news.google.com/_/DotsSplashUi/data/batchexecute", "f.req=" + URLEncoder.encode("[[[\"Fbv4je\"," + JSONObject.quote("[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1],\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"" + id + "\"," + matcher2.group(1) + ",\"" + matcher.group(1) + "\"]") + ",null,\"generic\"]]]", "UTF-8"), "application/x-www-form-urlencoded;charset=UTF-8", i);
                    if (post.code != 200) {
                        return null;
                    }
                    Matcher matcher3 = RES.matcher(post.body);
                    if (matcher3.find()) {
                        return unescape(matcher3.group(1));
                    }
                    return null;
                } catch (Exception e) {
                    return null;
                }
            }
            return fromBase64;
        }
        return str;
    }

    static String unescape(String str) {
        Matcher matcher = Pattern.compile("\\\\+u([0-9a-fA-F]{4})").matcher(str);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(matcher.group(1), 16))));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString().replaceAll("\\\\+/", "/").replaceAll("\\\\+$", "");
    }
}
