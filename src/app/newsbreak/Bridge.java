package app.newsbreak;

import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import app.newsbreak.core.Ai;
import app.newsbreak.core.Ist;
import app.newsbreak.core.Sentinel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class Bridge {
    final MainActivity act;
    final Context c;

    Bridge(MainActivity mainActivity) {
        this.act = mainActivity;
        this.c = mainActivity.getApplicationContext();
    }

    @JavascriptInterface
    public String boot() {
        try {
            SharedPreferences prefs = Store.prefs(this.c);
            JSONObject jSONObject = new JSONObject();
            long currentTimeMillis = System.currentTimeMillis();
            jSONObject.put("today", Ist.dateKey(currentTimeMillis));
            jSONObject.put("yesterday", Ist.addDays(Ist.dateKey(currentTimeMillis), -1));
            jSONObject.put("now", currentTimeMillis);
            jSONObject.put("lastSync", prefs.getLong("lastSync", 0L));
            jSONObject.put("feedsOk", prefs.getInt("feedsOk", 0));
            jSONObject.put("feedsTotal", prefs.getInt("feedsTotal", 0));
            jSONObject.put("syncing", Engine.isRunning());
            jSONObject.put("aiRunning", Engine.aiRunning.get());
            jSONObject.put("settings", settingsJson(prefs));
            jSONObject.put("editions", editionIndex());
            jSONObject.put("version", this.c.getPackageManager().getPackageInfo(this.c.getPackageName(), 0).versionName);
            jSONObject.put("update", Updater.info(this.c));
            jSONObject.put("notifPermission", Build.VERSION.SDK_INT < 33 || this.c.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == 0);
            PowerManager powerManager = (PowerManager) this.c.getSystemService("power");
            jSONObject.put("batteryUnrestricted", powerManager != null && powerManager.isIgnoringBatteryOptimizations(this.c.getPackageName()));
            NotificationManager notificationManager = (NotificationManager) this.c.getSystemService("notification");
            jSONObject.put("notifEnabled", notificationManager == null || notificationManager.areNotificationsEnabled());
            jSONObject.put("maker", (Build.MANUFACTURER + "").toLowerCase());
            jSONObject.put("systemDark", (this.c.getResources().getConfiguration().uiMode & 48) == 32);
            jSONObject.put("hasData", (Store.read(this.c, "items.json") == null && Store.editionDates(this.c).isEmpty()) ? false : true);
            return jSONObject.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    JSONObject settingsJson(SharedPreferences sharedPreferences) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("notif", sharedPreferences.getBoolean("notif", true));
        jSONObject.put("alerts", sharedPreferences.getBoolean("alerts", true));
        jSONObject.put("quiet", sharedPreferences.getBoolean("quiet", true));
        jSONObject.put("intervalHours", sharedPreferences.getInt("intervalHours", 3));
        jSONObject.put("disabledSources", sharedPreferences.getString("disabledSources", ""));
        Engine.migrate(sharedPreferences);
        String provider = Engine.provider(sharedPreferences);
        jSONObject.put("provider", sharedPreferences.getString("aiProvider", provider));
        jSONObject.put("primary", provider);
        JSONObject jSONObject2 = new JSONObject();
        for (String str : Ai.PROVIDERS) {
            String trim = sharedPreferences.getString("key_" + str, "").trim();
            String trim2 = sharedPreferences.getString("model_" + str, "").trim();
            jSONObject2.put(str, new JSONObject().put("hasKey", trim.length() > 10).put("keyHint", trim.length() > 10 ? trim.substring(0, Math.min(6, trim.length() - 4)) + "…" + trim.substring(trim.length() - 4) : "").put("model", trim2.length() > 0 ? trim2 : Ai.defaultModel(str)).put("name", Ai.providerName(str)));
        }
        jSONObject.put("providers", jSONObject2);
        jSONObject.put("order", new JSONArray((Collection) Engine.chain(sharedPreferences)));
        jSONObject.put("editing", sharedPreferences.getString("aiProvider", provider));
        jSONObject.put("hasKey", Engine.aiKey(sharedPreferences).length() > 10);
        jSONObject.put("model", Engine.aiModel(sharedPreferences));
        jSONObject.put("webSearch", sharedPreferences.getBoolean("webSearch", true));
        jSONObject.put("verifyPass", sharedPreferences.getBoolean("verifyPass", true));
        jSONObject.put("autoAI", sharedPreferences.getBoolean("autoAI", true));
        jSONObject.put("theme", sharedPreferences.getString("theme", "auto"));
        jSONObject.put("textScale", sharedPreferences.getInt("textScale", 100));
        jSONObject.put("lastSeen", sharedPreferences.getLong("lastSeen", 0L));
        jSONObject.put("deliverAt", sharedPreferences.getInt("deliverAt", 360));
        jSONObject.put("setupDone", sharedPreferences.getBoolean("setupDone", false));
        return jSONObject;
    }

    JSONArray editionIndex() throws Exception {
        return Store.editionIndex(this.c, 120);
    }

    @JavascriptInterface
    public String edition(String str) {
        JSONObject edition = Store.edition(this.c, str);
        if (edition == null) {
            return "null";
        }
        String raw = edition.toString();
        String ck = str + ":" + raw.length() + ":" + raw.hashCode();
        synchronized (edCache) { String hit = edCache.get(ck); if (hit != null) return hit; }
        try {
            Sentinel.cleanEdition(edition, Engine.kb(this.c));
        } catch (Throwable th) {
        }
        Engine.addPyqKeys(this.c, edition);
        String out = edition.toString();
        synchronized (edCache) { if (edCache.size() > 6) edCache.clear(); edCache.put(ck, out); }
        return out;
    }

    /** Cleaned editions by date + saved content: reopening a day doesn't redo the checks. */
    static final java.util.Map<String, String> edCache = new java.util.HashMap<String, String>();

    @JavascriptInterface
    public String live() {
        String s = Engine.live(this.c);
        try {
            JSONObject o = new JSONObject(s);
            boolean ch = Sentinel.dropMeta(o);
            if (app.newsbreak.core.EditionBuilder.consolidate(o.optJSONArray("topics"))) ch = true;
            if (Engine.addPyqKeys(this.c, o) || ch) return o.toString();
        } catch (Throwable ignored) {
        }
        return s;
    }

    @JavascriptInterface
    public String search(String q) {
        JSONArray out = new JSONArray();
        try {
            String[] terms = q.toLowerCase().trim().split("\\s+");
            if (q.trim().length() < 2) return "[]";
            for (String d : Store.editionDates(c)) {
                JSONObject ed = Store.edition(c, d);
                if (ed == null) continue;
                JSONArray t = new JSONArray();
                for (String key : new String[]{"topics", "catchup"}) { JSONArray x = ed.optJSONArray(key); for (int k = 0; x != null && k < x.length(); k++) t.put(x.get(k)); }
                for (int i = 0; i < t.length(); i++) {
                    JSONObject tp = t.getJSONObject(i);
                    String hay = (tp.optString("head") + " " + tp.optString("bullets") + " " + tp.optString("pyq") + " " + tp.optString("gs") + " " + tp.optString("bg")).toLowerCase();
                    boolean all = true;
                    for (String term : terms) if (!hay.contains(term)) { all = false; break; }
                    if (all) { try { Sentinel.clean(tp, Engine.kb(c)); } catch (Throwable ignored) {} tp.put("date", d); out.put(tp); if (out.length() >= 80) return out.toString(); } // only the matches are checked: fast per keystroke
                }
            }
        } catch (Exception ignored) {}
        return out.toString();
    }


    @JavascriptInterface
    public String feed(String str, int i) {
        return Engine.feed(this.c, str, i);
    }

    @JavascriptInterface
    public String factbank() {
        return Store.readArr(this.c, "factbank.json").toString();
    }

    @JavascriptInterface
    public String health() {
        return Store.readArr(this.c, "health.json").toString();
    }

    @JavascriptInterface
    public String checklog() {
        return Store.readArr(this.c, "checklog.json").toString();
    }

    @JavascriptInterface
    public String asset(String str) {
        String asset = Store.asset(this.c, "www/" + str);
        return asset == null ? "" : asset;
    }

    @JavascriptInterface
    public String bookmarks() {
        return Store.readArr(this.c, "bookmarks.json").toString();
    }

    /** Your PYQ attempts: {"year|q": [1 right / 0 wrong, time, option]}. */
    @JavascriptInterface
    public String pyqTries() {
        JSONObject o = Store.readObj(this.c, "pyq_tries.json");
        return o == null ? "{}" : o.toString();
    }

    @JavascriptInterface
    public void savePyqTries(String str) {
        try {
            if (str.length() < 400000) { new JSONObject(str); Store.write(this.c, "pyq_tries.json", str); }
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public void saveBookmarks(String str) {
        try {
            new JSONArray(str);
            Store.write(this.c, "bookmarks.json", str);
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public void saveFactbank(String str) {
        try {
            new JSONArray(str);
            synchronized (Store.LOCK) {
                Store.write(this.c, "factbank.json", str);
            }
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public String sync() {
        if (Engine.isRunning()) {
            return "busy";
        }
        WorkService.start(this.c, "sync", null, "manual");
        return "started";
    }

    /** The full PYQ for "year|q" from the app's own data (no file loading in the page). */
    @JavascriptInterface
    public String pyqQuestion(String key) {
        try {
            app.newsbreak.core.Pyq p = Engine.pyq(this.c);
            org.json.JSONArray q = p == null ? null : p.question(key);
            return q == null ? "" : q.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    static volatile org.json.JSONObject pwCache;

    /** Your private explanation for a PYQ ("year|q"), imported with Restore; "" when not imported. */
    @JavascriptInterface
    public String pwExplain(String key) {
        try {
            org.json.JSONObject pw = pwCache;
            if (pw == null) {
                String raw = Store.read(this.c, "pw.json");
                if (raw == null) return "";
                pw = new org.json.JSONObject(raw);
                pwCache = pw;
            }
            org.json.JSONObject e = pw.optJSONObject(key);
            return e == null ? "" : e.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    @JavascriptInterface
    public String updateInfo() {
        return Updater.info(this.c).toString();
    }

    @JavascriptInterface
    public void checkUpdate() {
        Updater.checkAsync(this.c, 0, false);
    }

    /** Downloads the newer release and opens the installer; the result arrives as an "update" event (state: install). */
    @JavascriptInterface
    public void installUpdate() {
        final MainActivity a = this.act;
        new Thread(new Runnable() {
            public void run() {
                String r = Updater.install(new Updater.Activity0() {
                    public Context context() { return a.getApplicationContext(); }
                    public void start(final android.content.Intent i) {
                        a.runOnUiThread(new Runnable() { public void run() { try { a.startActivity(i); } catch (Exception e) {} } });
                    }
                });
                try { Engine.emit("update", Updater.info(a.getApplicationContext()).put("state", "install").put("result", r)); } catch (Exception e) {}
            }
        }, "nc-install").start();
    }

    @JavascriptInterface
    public void testAi() {
        WorkService.start(this.c, "aitest", null, null);
    }

    @JavascriptInterface
    public String detectProvider(String str) {
        String detectProvider = Ai.detectProvider(str);
        return detectProvider == null ? "" : detectProvider;
    }

    @JavascriptInterface
    public String generateAi(String str) {
        if (Engine.aiRunning.get()) {
            return "busy";
        }
        WorkService.start(this.c, "ai", str, null);
        return "started";
    }

    @JavascriptInterface
    public String generateAiCard(String str, String str2) {
        if (Engine.aiCardRunning.get()) {
            return "busy";
        }
        WorkService.start(this.c, "aicard", str, str2);
        return "started";
    }

    @JavascriptInterface
    public void setString(String str, String str2) {
        Store.prefs(this.c).edit().putString(str, str2 == null ? "" : str2.trim()).apply();
    }

    @JavascriptInterface
    public void setBool(String str, boolean z) {
        Store.prefs(this.c).edit().putBoolean(str, z).apply();
    }

    @JavascriptInterface
    public void setInt(String str, int i) {
        Store.prefs(this.c).edit().putInt(str, i).commit();
        if ("intervalHours".equals(str)) {
            SyncJobService.schedule(this.c, true);
        }
        if ("deliverAt".equals(str)) {
            DeliverReceiver.arm(this.c);
        }
    }

    @JavascriptInterface
    public void setLong(String str, String str2) {
        try {
            Store.prefs(this.c).edit().putLong(str, Long.parseLong(str2)).apply();
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public void openUrl(String str) {
        if (str != null) {
            if (str.startsWith("https://") || str.startsWith("http://")) {
                try {
                    Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(str));
                    intent.addFlags(268435456);
                    this.c.startActivity(intent);
                } catch (Exception e) {
                }
            }
        }
    }

    @JavascriptInterface
    public void share(String str) {
        try {
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            intent.putExtra("android.intent.extra.TEXT", str);
            Intent createChooser = Intent.createChooser(intent, "Share via");
            createChooser.addFlags(268435456);
            this.c.startActivity(createChooser);
        } catch (Exception e) {
        }
    }

    static byte[] png(String str) {
        int indexOf = str.indexOf("base64,");
        if (indexOf >= 0) {
            str = str.substring(indexOf + 7);
        }
        return Base64.decode(str, 0);
    }

    static String safeName(String str) {
        if (str == null) {
            str = "NewsCrux";
        }
        String replaceAll = str.replaceAll("[^A-Za-z0-9_-]+", "-");
        return replaceAll.length() > 60 ? replaceAll.substring(0, 60) : replaceAll;
    }

    @JavascriptInterface
    public String shareImage(String str, String str2, String str3) {
        try {
            File dir = ShareProvider.dir(this.c);
            File[] listFiles = dir.listFiles();
            if (listFiles != null) {
                for (File file : listFiles) {
                    if (System.currentTimeMillis() - file.lastModified() > Ist.DAY) {
                        file.delete();
                    }
                }
            }
            File file2 = new File(dir, safeName(str2) + ".png");
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                fileOutputStream.write(png(str));
                fileOutputStream.close();
                Uri uri = ShareProvider.uri(file2);
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("image/png");
                intent.putExtra("android.intent.extra.STREAM", uri);
                if (str3 != null && str3.length() > 0) {
                    intent.putExtra("android.intent.extra.TEXT", str3);
                }
                intent.setClipData(ClipData.newRawUri("News Crux card", uri));
                intent.addFlags(1);
                Intent createChooser = Intent.createChooser(intent, "Share card");
                createChooser.addFlags(268435457);
                this.c.startActivity(createChooser);
                return "ok";
            } catch (Throwable th) {
                fileOutputStream.close();
                throw th;
            }
        } catch (Exception e) {
            return "error";
        }
    }

    @JavascriptInterface
    public String saveImage(String str, String str2) {
        try {
            byte[] png = png(str);
            String str3 = safeName(str2) + ".png";
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("_display_name", str3);
                contentValues.put("mime_type", "image/png");
                contentValues.put("relative_path", Environment.DIRECTORY_PICTURES + "/News Crux");
                contentValues.put("is_pending", (Integer) 1);
                ContentResolver contentResolver = this.c.getContentResolver();
                Uri insert = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (insert == null) {
                    return "error";
                }
                try {
                    OutputStream openOutputStream = contentResolver.openOutputStream(insert);
                    if (openOutputStream == null) {
                        throw new IOException("no stream");
                    }
                    try {
                        openOutputStream.write(png);
                        contentValues.clear();
                        contentValues.put("is_pending", (Integer) 0);
                        contentResolver.update(insert, contentValues, null, null);
                        return "ok";
                    } finally {
                        openOutputStream.close();
                    }
                } catch (Exception e) {
                    try {
                        contentResolver.delete(insert, null, null);
                    } catch (Exception e2) {
                    }
                    return "error";
                }
            }
            if (this.c.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
                this.act.runOnUiThread(new Runnable() { // from class: app.newsbreak.Bridge.1
                    @Override // java.lang.Runnable
                    public void run() {
                        Bridge.this.act.requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 8);
                    }
                });
                return "perm";
            }
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "News Crux");
            file.mkdirs();
            File file2 = new File(file, str3);
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                fileOutputStream.write(png);
                fileOutputStream.close();
                MediaScannerConnection.scanFile(this.c, new String[]{file2.getAbsolutePath()}, new String[]{"image/png"}, null);
                return "ok";
            } catch (Throwable th) {
                fileOutputStream.close();
                throw th;
            }
        } catch (Exception e3) {
            return "error";
        }
    }

    @JavascriptInterface
    public void copy(String str) {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) this.c.getSystemService("clipboard");
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("News Crux", str));
            }
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public void setBars(String str, boolean z) {
        this.act.setBars(str, z);
    }

    @JavascriptInterface
    public void testNotification() {
        Notifier.test(this.c);
    }

    @JavascriptInterface
    public void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            this.act.runOnUiThread(new Runnable() { // from class: app.newsbreak.Bridge.2
                @Override // java.lang.Runnable
                public void run() {
                    Bridge.this.act.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 7);
                }
            });
        }
    }

    @JavascriptInterface
    public void openBatterySettings() {
        try {
            Intent intent = new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", Uri.parse("package:" + this.c.getPackageName()));
            intent.addFlags(268435456);
            this.c.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent2 = new Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS");
                intent2.addFlags(268435456);
                this.c.startActivity(intent2);
            } catch (Exception e2) {
            }
        }
    }

    @JavascriptInterface
    public void openNotifSettings() {
        try {
            Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", this.c.getPackageName());
            intent.addFlags(268435456);
            this.c.startActivity(intent);
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public String reported() {
        return Store.readArr(this.c, "reported.json").toString();
    }

    @JavascriptInterface
    public void saveReported(String str) {
        try {
            new JSONArray(str);
            Store.write(this.c, "reported.json", str);
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public void backup() {
        this.act.startBackup();
    }

    @JavascriptInterface
    public void restore() {
        this.act.startRestore();
    }

    @JavascriptInterface
    public void printHtml(String str, String str2) {
        this.act.printHtml(str, str2);
    }

    @JavascriptInterface
    public void openAppSettings() {
        try {
            Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + this.c.getPackageName()));
            intent.addFlags(268435456);
            this.c.startActivity(intent);
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public void resetData() {
        synchronized (Store.LOCK) {
            for (String str : new String[]{"items.json", "health.json"}) {
                new File(this.c.getFilesDir(), str).delete();
            }
        }
    }
}
