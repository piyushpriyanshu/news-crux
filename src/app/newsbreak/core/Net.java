package app.newsbreak.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/* loaded from: classes.dex */
public final class Net {
    static final int MAX = 4194304;
    public static final String UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36 NewsBreak/1.0";
    public static volatile String testBase = null;
    private static final Pattern CT = Pattern.compile("charset=([\\w\\-]+)", 2);
    private static final Pattern DECL = Pattern.compile("encoding=[\"']([\\w\\-]+)[\"']", 2);

    public static final class Resp {
        public String body;
        public int code;
        public String finalUrl;
    }

    private Net() {
    }

    public static Resp get(String str, int i) throws Exception {
        return get(str, i, "application/rss+xml, application/atom+xml, application/xml;q=0.9, text/xml;q=0.9, */*;q=0.8");
    }

    public static Resp get(String str, int i, String str2) throws Exception {
        String str3;
        for (int i2 = 0; i2 < 6; i2++) {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(route(str)).openConnection();
            httpURLConnection.setInstanceFollowRedirects(false);
            httpURLConnection.setConnectTimeout(i);
            httpURLConnection.setReadTimeout(i);
            httpURLConnection.setRequestProperty("User-Agent", UA);
            httpURLConnection.setRequestProperty("Accept", str2);
            httpURLConnection.setRequestProperty("Accept-Language", "en-IN,en;q=0.9");
            httpURLConnection.setRequestProperty("Accept-Encoding", "gzip");
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                String headerField = httpURLConnection.getHeaderField("Location");
                httpURLConnection.disconnect();
                if (headerField == null) {
                    break;
                }
                if (testBase == null || !headerField.startsWith(testBase + "/h/")) {
                    str3 = (testBase == null || !headerField.startsWith("/h/")) ? headerField : "https://" + headerField.substring(3);
                } else {
                    str3 = "https://" + headerField.substring(testBase.length() + 3);
                }
                str = new URL(new URL(str), str3).toString();
            } else {
                Resp resp = new Resp();
                resp.code = responseCode;
                resp.finalUrl = str;
                InputStream errorStream = responseCode >= 400 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
                if (errorStream == null) {
                    resp.body = "";
                    httpURLConnection.disconnect();
                    return resp;
                }
                InputStream gZIPInputStream = "gzip".equalsIgnoreCase(httpURLConnection.getContentEncoding()) ? new GZIPInputStream(errorStream) : errorStream;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[16384];
                while (true) {
                    int read = gZIPInputStream.read(bArr);
                    if (read <= 0 || byteArrayOutputStream.size() >= MAX) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr, 0, read);
                }
                gZIPInputStream.close();
                httpURLConnection.disconnect();
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                resp.body = new String(byteArray, charset(httpURLConnection.getContentType(), byteArray));
                return resp;
            }
        }
        throw new Exception("Too many redirects");
    }

    static String route(String str) {
        return (testBase == null || str.startsWith(testBase) || str.contains("://127.0.0.1") || str.contains("://localhost")) ? str : testBase + "/h/" + str.replaceFirst("^https?://", "");
    }

    public static Resp post(String str, String str2, String str3, int i) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(route(str)).openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setConnectTimeout(i);
        httpURLConnection.setReadTimeout(i);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestProperty("User-Agent", UA);
        httpURLConnection.setRequestProperty("Content-Type", str3);
        httpURLConnection.setRequestProperty("Accept-Language", "en-IN,en;q=0.9");
        OutputStream outputStream = httpURLConnection.getOutputStream();
        outputStream.write(str2.getBytes("UTF-8"));
        outputStream.close();
        Resp resp = new Resp();
        resp.code = httpURLConnection.getResponseCode();
        resp.finalUrl = str;
        InputStream errorStream = resp.code >= 400 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
        if (errorStream == null) {
            resp.body = "";
            httpURLConnection.disconnect();
            return resp;
        }
        InputStream gZIPInputStream = "gzip".equalsIgnoreCase(httpURLConnection.getContentEncoding()) ? new GZIPInputStream(errorStream) : errorStream;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[16384];
        while (true) {
            int read = gZIPInputStream.read(bArr);
            if (read <= 0 || byteArrayOutputStream.size() >= MAX) {
                break;
            }
            byteArrayOutputStream.write(bArr, 0, read);
        }
        gZIPInputStream.close();
        httpURLConnection.disconnect();
        resp.body = new String(byteArrayOutputStream.toByteArray(), "UTF-8");
        return resp;
    }

    public static Resp page(String str, int i) throws Exception {
        return get(str, i, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    }

    static String charset(String str, byte[] bArr) {
        if (str != null) {
            Matcher matcher = CT.matcher(str);
            if (matcher.find()) {
                return norm(matcher.group(1));
            }
        }
        Matcher matcher2 = DECL.matcher(new String(bArr, 0, Math.min(200, bArr.length)));
        return matcher2.find() ? norm(matcher2.group(1)) : "UTF-8";
    }

    static String norm(String str) {
        try {
            Charset.forName(str);
            return str;
        } catch (Exception e) {
            return "UTF-8";
        }
    }
}
