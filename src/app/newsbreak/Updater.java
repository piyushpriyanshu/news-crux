package app.newsbreak;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * App updates from GitHub Releases.
 *
 * The app asks GitHub for the latest release of {@link #REPO} when it opens (at most every 30 min) and every 6 hours in
 * the background. A newer version shows in Settings, as a toast on open, and as one notification per version. "Install"
 * downloads the release APK and hands it to Android's installer (same signing key, so data stays).
 */
final class Updater {
    private Updater() {}

    static final String REPO = "piyushpriyanshu/news-crux";
    static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    static final String PAGE = "https://github.com/" + REPO + "/releases/latest";
    static final long OPEN_GAP = 30 * 60000L, BG_GAP = 6 * 3600000L;
    static final AtomicBoolean busy = new AtomicBoolean(false);

    /** "1.6.0" (or "v1.6.0", "1.6.0-beta") → comparable parts. */
    static int[] parts(String v) {
        String s = v == null ? "" : v.trim().replaceFirst("^[vV]", "");
        int cut = 0;
        while (cut < s.length() && (Character.isDigit(s.charAt(cut)) || s.charAt(cut) == '.')) cut++;
        String[] p = s.substring(0, cut).split("\\.");
        int[] o = new int[3];
        for (int i = 0; i < 3 && i < p.length; i++) {
            try { o[i] = Integer.parseInt(p[i]); } catch (Exception e) { o[i] = 0; }
        }
        return o;
    }

    static boolean newer(String latest, String current) {
        int[] a = parts(latest), b = parts(current);
        for (int i = 0; i < 3; i++) if (a[i] != b[i]) return a[i] > b[i];
        return false;
    }

    static String current(Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; } catch (Exception e) { return "0"; }
    }

    /** What the UI shows: current, latest, newer, notes, checkedAt, error. */
    static JSONObject info(Context c) {
        SharedPreferences sp = Store.prefs(c);
        JSONObject o = new JSONObject();
        try {
            String cur = current(c), lat = sp.getString("updLatest", "");
            o.put("current", cur).put("latest", lat).put("newer", lat.length() > 0 && newer(lat, cur))
                    .put("notes", sp.getString("updNotes", "")).put("checkedAt", sp.getLong("updCheckedAt", 0))
                    .put("error", sp.getString("updError", "")).put("busy", busy.get()).put("page", PAGE);
        } catch (Exception e) {}
        return o;
    }

    /** Checks in a background thread when the last check is older than {@code gap} ms (0 = always). */
    static void checkAsync(final Context ctx, final long gap, final boolean notify) {
        final Context c = ctx.getApplicationContext();
        if (System.currentTimeMillis() - Store.prefs(c).getLong("updCheckedAt", 0) < gap) return;
        new Thread(new Runnable() {
            public void run() { check(c, notify); }
        }, "nc-update").start();
    }

    /** Asks GitHub for the latest release. Returns {@link #info}. Never throws. */
    static JSONObject check(Context c, boolean notify) {
        if (!busy.compareAndSet(false, true)) return info(c);
        SharedPreferences sp = Store.prefs(c);
        try {
            HttpURLConnection h = open(API);
            int code = h.getResponseCode();
            if (code == 404) { // no release published yet
                sp.edit().putLong("updCheckedAt", System.currentTimeMillis()).putString("updError", "").apply();
                return info(c);
            }
            if (code != 200) throw new Exception("GitHub replied " + code);
            JSONObject r = new JSONObject(read(h.getInputStream(), 1 << 20));
            if (r.optBoolean("draft") || r.optBoolean("prerelease")) throw new Exception("Latest release is not final");
            String tag = r.optString("tag_name", "").replaceFirst("^[vV]", "");
            String url = "", name = ""; long size = 0;
            JSONArray as = r.optJSONArray("assets");
            for (int i = 0; as != null && i < as.length(); i++) {
                JSONObject a = as.optJSONObject(i);
                if (a != null && a.optString("name").toLowerCase().endsWith(".apk")) {
                    url = a.optString("browser_download_url"); name = a.optString("name"); size = a.optLong("size"); break;
                }
            }
            String notes = r.optString("body", "").trim();
            if (notes.length() > 1500) notes = notes.substring(0, 1500) + "…";
            sp.edit().putString("updLatest", tag).putString("updUrl", url).putString("updName", name).putLong("updSize", size)
                    .putString("updNotes", notes).putLong("updCheckedAt", System.currentTimeMillis()).putString("updError", "").apply();
            String cur = current(c);
            if (notify && url.length() > 0 && newer(tag, cur) && !tag.equals(sp.getString("updNotified", ""))) {
                sp.edit().putString("updNotified", tag).apply();
                Notifier.update(c, tag, notes);
            }
        } catch (Throwable t) {
            sp.edit().putLong("updCheckedAt", System.currentTimeMillis()).putString("updError", String.valueOf(t.getMessage())).apply();
        } finally {
            busy.set(false);
        }
        JSONObject o = info(c);
        try { Engine.emit("update", new JSONObject(o.toString())); } catch (Exception e) {}
        return o;
    }

    static HttpURLConnection open(String u) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
        h.setConnectTimeout(15000);
        h.setReadTimeout(30000);
        h.setInstanceFollowRedirects(true);
        h.setRequestProperty("Accept", "application/vnd.github+json");
        h.setRequestProperty("User-Agent", "NewsCrux-Android");
        return h;
    }

    static String read(InputStream in, int max) throws Exception {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) { b.write(buf, 0, n); if (b.size() > max) throw new Exception("too large"); }
            return b.toString("UTF-8");
        } finally { in.close(); }
    }

    static File dir(Context c) {
        File d = new File(c.getCacheDir(), "update");
        d.mkdirs();
        return d;
    }

    /**
     * Downloads the latest APK and opens Android's installer. Runs in the caller's thread (call from a worker).
     * @return "ok", "perm" (allow installs from this app first), "none" (no newer release) or an error message.
     */
    static String install(Activity0 a) {
        Context c = a.context();
        SharedPreferences sp = Store.prefs(c);
        String url = sp.getString("updUrl", ""), tag = sp.getString("updLatest", "");
        if (url.length() == 0 || !newer(tag, current(c))) return "none";
        if (Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls()) {
            a.start(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.getPackageName())));
            return "perm";
        }
        try {
            File[] old = dir(c).listFiles();
            for (int i = 0; old != null && i < old.length; i++) old[i].delete();
            File f = new File(dir(c), "NewsCrux-" + tag.replaceAll("[^0-9A-Za-z.]", "") + ".apk");
            HttpURLConnection h = open(url);
            h.setRequestProperty("Accept", "application/octet-stream");
            if (h.getResponseCode() != 200) throw new Exception("download failed (" + h.getResponseCode() + ")");
            InputStream in = h.getInputStream();
            FileOutputStream out = new FileOutputStream(f);
            long got = 0;
            try {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) { out.write(buf, 0, n); got += n; }
            } finally { out.close(); in.close(); }
            long want = sp.getLong("updSize", 0);
            if (got < 100000 || (want > 0 && got != want)) { f.delete(); throw new Exception("download incomplete"); }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(ShareProvider.uri(f), "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            a.start(i);
            return "ok";
        } catch (Throwable t) {
            return "Update failed: " + t.getMessage();
        }
    }

    /** The bits of an Activity the installer needs (keeps this class testable). */
    interface Activity0 {
        Context context();
        void start(Intent i);
    }
}
