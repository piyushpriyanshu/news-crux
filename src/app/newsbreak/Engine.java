package app.newsbreak;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Xml;
import app.newsbreak.core.Ai;
import app.newsbreak.core.EditionBuilder;
import app.newsbreak.core.ExamMeta;
import app.newsbreak.core.Harvest;
import app.newsbreak.core.Ist;
import app.newsbreak.core.Kb;
import app.newsbreak.core.Net;
import app.newsbreak.core.Pyq;
import app.newsbreak.core.RssParser;
import app.newsbreak.core.Sentinel;
import app.newsbreak.core.Text;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

/* loaded from: classes.dex */
public final class Engine {
    static final int AI_RUNS_PER_EDITION = 8;
    static final long COACH_KEEP_MS = 1123200000;
    static final long KEEP_MS = 604800000;
    static final int RETIRE_MIN_FAILS = 6;
    static final long RETIRE_MS = 604800000;
    static volatile boolean deliverPending;
    private static volatile List<JSONObject> itemsCache;
    private static Kb kb;
    public static volatile Listener listener;
    private static Pyq pyq;
    private static boolean pyqFailed;
    static final AtomicBoolean running = new AtomicBoolean(false);
    static final AtomicBoolean aiRunning = new AtomicBoolean(false);
    public static volatile boolean fastFirst = false;
    public static final Object EDLOCK = new Object();
    static volatile String aiAfterDeliver = null;
    static final AtomicBoolean aiCardRunning = new AtomicBoolean(false);

    public interface Listener {
        void onEvent(String str);
    }

    private Engine() {
    }

    static boolean firstRunFast(List<JSONObject> list) {
        return list.isEmpty() && fastFirst;
    }

    static synchronized Kb kb(Context context) throws Exception {
        Kb kb2;
        synchronized (Engine.class) {
            if (kb == null) {
                kb = new Kb(Store.asset(context, "www/data/kb.json"));
            }
            kb2 = kb;
        }
        return kb2;
    }

    /** Gives older cards the links to their full PYQs (see Pyq.addKeys). */
    static boolean addPyqKeys(Context c, JSONObject ed) {
        boolean any = false;
        try {
            Pyq p = pyq(c);
            Kb k = kb(c);
            if (p == null || ed == null) return false;
            for (String key : new String[]{"topics", "catchup"}) {
                JSONArray a = ed.optJSONArray(key);
                for (int i = 0; a != null && i < a.length(); i++) {
                    if (p.addKeys(a.optJSONObject(i), k)) any = true;
                    if ("topics".equals(key) && p.addAll(a.optJSONObject(i))) any = true; // every exam's related questions
                }
            }
        } catch (Throwable ignored) {
        }
        return any;
    }

    /**
     * A feed entry's "keep" (address pattern, e.g. only /news/national/), "hours" (age limit, default 36) and "max"
     * (newest N) limits. Used for news sitemaps, which list every article of the site.
     */
    static List<JSONObject> keepOnly(List<JSONObject> items, JSONObject feed, long now) {
        java.util.regex.Pattern keep = feed.has("keep") ? java.util.regex.Pattern.compile(feed.optString("keep"), java.util.regex.Pattern.CASE_INSENSITIVE) : null;
        long oldest = now - feed.optLong("hours", 36) * 3600000L;
        List<JSONObject> out = new ArrayList<JSONObject>();
        for (JSONObject it : items) {
            if (keep != null && !keep.matcher(it.optString("u")).find()) continue;
            if (it.optLong("p") > 0 && it.optLong("p") < oldest) continue;
            out.add(it);
        }
        Collections.sort(out, new Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) { return Long.compare(b.optLong("p"), a.optLong("p")); } });
        int max = feed.optInt("max", 150);
        return out.size() > max ? new ArrayList<JSONObject>(out.subList(0, max)) : out;
    }

    static synchronized Pyq pyq(Context context) {
        Pyq pyq2;
        synchronized (Engine.class) {
            if (pyq == null && !pyqFailed) {
                try {
                    Pyq p = new Pyq(Store.asset(context, "www/data/pyq.js"), Store.asset(context, "www/data/pyq_text.js"), kb(context));
                    try { p.addMoreText(Store.asset(context, "www/data/pyq_more_text.js")); } catch (Throwable t) {} // other UPSC papers: full questions first (their answers feed the matching)
                    try { p.addMore(Store.asset(context, "www/data/pyq_more.js")); } catch (Throwable t) {} // CDS, CAPF, NDA, Geo-Scientist, EPFO, ESE
                    pyq = p;
                } catch (Throwable th) {
                    pyqFailed = true;
                    pyq2 = null;
                }
            }
            pyq2 = pyq;
        }
        return pyq2;
    }

    static EditionBuilder builder(Context context) throws Exception {
        EditionBuilder editionBuilder = new EditionBuilder(kb(context), pyq(context));
        editionBuilder.deliverOffsetMs = Math.max(0, Math.min(Store.prefs(context).getInt("deliverAt", 360), 660)) * 60000;
        JSONObject readObj = Store.readObj(context, "adapt.json");
        if (readObj != null) {
            Iterator<String> keys = readObj.keys();
            while (keys.hasNext()) {
                String next = keys.next();
                editionBuilder.boost.put(next, Double.valueOf(readObj.optDouble(next, 0.0d)));
            }
        }
        return editionBuilder;
    }

    public static void learn(Context context, JSONObject jSONObject) {
        try {
            JSONObject readObj = Store.readObj(context, "adapt.json");
            JSONObject jSONObject2 = readObj == null ? new JSONObject() : readObj;
            JSONObject jSONObject3 = new JSONObject();
            Iterator<String> keys = jSONObject2.keys();
            while (keys.hasNext()) {
                String next = keys.next();
                double v = jSONObject2.optDouble(next, 0.0d) * 0.85d;
                if (v >= 0.05d) jSONObject3.put(next, Math.round(v * 100.0d) / 100.0d);
            }
            JSONArray optJSONArray = jSONObject.optJSONArray("gapThemes");
            for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
                String optString = optJSONArray.optString(i);
                jSONObject3.put(optString, Math.min(1.5d, jSONObject3.optDouble(optString, 0.0d) + 0.3d));
            }
            Store.write(context, "adapt.json", jSONObject3.toString());
        } catch (Exception e) {
        }
    }

    static void threads(Context context, JSONObject jSONObject, Map<String, JSONObject> map) {
        JSONObject edition;
        try {
            String string = jSONObject.getString("date");
            JSONArray jSONArray = jSONObject.getJSONArray("topics");
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject2 = jSONArray.getJSONObject(i);
                Set<String> set = Text.tokens(jSONObject2.optString("head"));
                HashSet hashSet = new HashSet();
                JSONArray optJSONArray = jSONObject2.optJSONArray("ents");
                for (int i2 = 0; optJSONArray != null && i2 < optJSONArray.length(); i2++) {
                    if (!Pyq.GENERIC_IDS.contains("e:" + optJSONArray.optString(i2))) {
                        hashSet.add(optJSONArray.optString(i2));
                    }
                }
                JSONArray jSONArray2 = new JSONArray();
                HashSet hashSet2 = new HashSet();
                JSONArray optJSONArray2 = jSONObject2.optJSONArray("branch");
                for (int i3 = 0; optJSONArray2 != null && i3 < optJSONArray2.length() && jSONArray2.length() < 5; i3++) {
                    if (hashSet2.add(optJSONArray2.getJSONObject(i3).optString("d"))) {
                        jSONArray2.put(optJSONArray2.get(i3));
                    }
                }
                for (int i4 = 1; i4 <= 30 && jSONArray2.length() < 5; i4++) {
                    String addDays = Ist.addDays(string, -i4);
                    if (!hashSet2.contains(addDays)) {
                        if (map.containsKey(addDays)) {
                            edition = map.get(addDays);
                        } else {
                            edition = Store.edition(context, addDays);
                            map.put(addDays, edition);
                        }
                        if (edition != null) {
                            JSONArray optJSONArray3 = edition.optJSONArray("topics");
                            for (int i5 = 0; optJSONArray3 != null && i5 < optJSONArray3.length(); i5++) {
                                JSONObject jSONObject3 = optJSONArray3.getJSONObject(i5);
                                double jaccard = Text.jaccard(set, Text.tokens(jSONObject3.optString("head")));
                                boolean z = false;
                                JSONArray optJSONArray4 = jSONObject3.optJSONArray("ents");
                                for (int i6 = 0; optJSONArray4 != null && i6 < optJSONArray4.length(); i6++) {
                                    if (hashSet.contains(optJSONArray4.optString(i6))) {
                                        z = true;
                                    }
                                }
                                if (jaccard >= 0.34d || (z && jaccard >= 0.18d)) {
                                    JSONArray optJSONArray5 = jSONObject3.optJSONArray("bullets");
                                    JSONObject put = new JSONObject().put("d", addDays).put("h", jSONObject3.optString("head"));
                                    if (optJSONArray5 != null && optJSONArray5.length() > 0) {
                                        put.put("b", optJSONArray5.optString(0));
                                    }
                                    jSONArray2.put(put);
                                    break;
                                }
                            }
                        }
                    }
                }
                ArrayList arrayList = new ArrayList();
                for (int i7 = 0; i7 < jSONArray2.length(); i7++) {
                    arrayList.add(jSONArray2.getJSONObject(i7));
                }
                Collections.sort(arrayList, new Comparator<JSONObject>() { // from class: app.newsbreak.Engine.1
                    @Override // java.util.Comparator
                    public int compare(JSONObject jSONObject4, JSONObject jSONObject5) {
                        return jSONObject5.optString("d").compareTo(jSONObject4.optString("d"));
                    }
                });
                if (arrayList.isEmpty()) {
                    jSONObject2.remove("thread");
                } else {
                    jSONObject2.put("thread", new JSONArray((Collection) arrayList));
                }
            }
        } catch (Exception e) {
        }
    }

    static void migrate(SharedPreferences sharedPreferences) {
        if ("google/gemini-3.5-flash-lite".equals(sharedPreferences.getString("model_openrouter", ""))) {
            sharedPreferences.edit().remove("model_openrouter").apply();
        }
        String string = sharedPreferences.getString("apiKey", "");
        if (string.length() > 10 && sharedPreferences.getString("key_claude", "").length() == 0) {
            sharedPreferences.edit().putString("key_claude", string).putString("model_claude", sharedPreferences.getString("model", Ai.defaultModel("claude"))).putString("aiProvider", sharedPreferences.getString("aiProvider", "claude")).putString("apiKey", "").apply();
        }
    }

    public static String keyOf(SharedPreferences sharedPreferences, String str) {
        return sharedPreferences.getString("key_" + str, "").trim();
    }

    public static String modelOf(SharedPreferences sharedPreferences, String str) {
        String trim = sharedPreferences.getString("model_" + str, "").trim();
        return trim.length() > 0 ? trim : Ai.defaultModel(str);
    }

    public static List<String> chain(SharedPreferences sharedPreferences) {
        ArrayList arrayList = new ArrayList();
        for (String str : sharedPreferences.getString("aiOrder", "").split(",")) {
            String trim = str.trim();
            if (trim.length() > 0 && !arrayList.contains(trim) && keyOf(sharedPreferences, trim).length() > 10) {
                arrayList.add(trim);
            }
        }
        String string = sharedPreferences.getString("aiProvider", "");
        if (arrayList.isEmpty() && string.length() > 0 && keyOf(sharedPreferences, string).length() > 10) {
            arrayList.add(string);
        }
        for (String str2 : Ai.PROVIDERS) {
            if (!arrayList.contains(str2) && keyOf(sharedPreferences, str2).length() > 10) {
                arrayList.add(str2);
            }
        }
        return arrayList;
    }

    public static String provider(SharedPreferences sharedPreferences) {
        List<String> chain = chain(sharedPreferences);
        return chain.isEmpty() ? sharedPreferences.getString("aiProvider", "gemini") : chain.get(0);
    }

    public static String aiKey(SharedPreferences sharedPreferences) {
        return keyOf(sharedPreferences, provider(sharedPreferences));
    }

    public static String aiModel(SharedPreferences sharedPreferences) {
        return modelOf(sharedPreferences, provider(sharedPreferences));
    }

    static void emit(String str, JSONObject jSONObject) {
        Listener listener2 = listener;
        if (listener2 != null) {
            try {
                jSONObject.put("type", str);
                listener2.onEvent(jSONObject.toString());
            } catch (Exception e) {
            }
        }
    }

    public static boolean isRunning() {
        return running.get();
    }

    static List<JSONObject> loadItems(Context context) {
        List<JSONObject> list = itemsCache;
        if (list != null) {
            return new ArrayList(list);
        }
        JSONArray readArr = Store.readArr(context, "items.json");
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < readArr.length(); i++) {
            JSONObject optJSONObject = readArr.optJSONObject(i);
            if (optJSONObject != null) {
                if (!optJSONObject.has("noise") && ExamMeta.title(optJSONObject.optString("t"))) { // stored before the exam-news filter
                    try { optJSONObject.put("noise", new JSONArray().put("exam")).put("sc", Math.min(optJSONObject.optDouble("sc", 0), 0)); } catch (Exception ignored) {}
                }
                arrayList.add(optJSONObject);
            }
        }
        itemsCache = new ArrayList(arrayList);
        return arrayList;
    }

    /** One sync: all feeds in parallel, coaching pages, full articles, editions of the last 3 days, live board, alerts. */
    public static JSONObject sync(Context ctx, String reason) {
        final Context c = ctx.getApplicationContext();
        JSONObject result = new JSONObject();
        if (!running.compareAndSet(false, true)) {
            try { result.put("busy", true); } catch (Exception ignored) {}
            return result;
        }
        final long now = System.currentTimeMillis();
        String aiDate = null;
        try {
            emit("sync", new JSONObject().put("state", "start").put("reason", reason));
            final Kb k = kb(c);
            SharedPreferences sp = Store.prefs(c);
            Set<String> disabled = new HashSet<String>();
            for (String s : sp.getString("disabledSources", "").split(",")) if (s.trim().length() > 0) disabled.add(s.trim());

            JSONArray sources = new JSONArray(Store.asset(c, "www/data/sources.json"));
            JSONObject fs0 = Store.readObj(c, "feedstate.json");
            final JSONObject fstate = fs0 != null ? fs0 : new JSONObject();
            final List<JSONObject> fetched = new ArrayList<JSONObject>();
            final JSONArray health = new JSONArray();
            final AtomicBoolean closed = new AtomicBoolean(false);
            final List<String> failedUrls = new ArrayList<String>();
            ExecutorService pool = Executors.newFixedThreadPool(6);
            final AtomicInteger done = new AtomicInteger();
            int total = 0;
            for (int i = 0; i < sources.length(); i++) {
                final JSONObject src = sources.getJSONObject(i);
                final String code = src.getString("code");
                if (disabled.contains(code)) continue;
                JSONArray feeds = src.getJSONArray("feeds");
                for (int f = 0; f < feeds.length(); f++) {
                    final JSONObject feed = feeds.getJSONObject(f);
                    JSONObject fst;
                    synchronized (fstate) { fst = fstate.optJSONObject(feed.optString("url")); }
                    if (fst != null && fst.optLong("lastTry") > now + 3600000L) fst.put("lastTry", now); // clock moved back: don't skip for weeks
                    if (fst != null && fst.optBoolean("retired") && now - fst.optLong("lastTry") < RETIRE_MS) { // dead feed: not fetched, re-checked once a week
                        JSONObject rh = new JSONObject().put("code", code).put("label", feed.optString("label")).put("url", feed.optString("url")).put("gn", feed.optBoolean("gn"))
                                .put("ok", false).put("retired", true).put("since", fst.optLong("since")).put("retiredAt", fst.optLong("retiredAt")).put("fails", fst.optInt("fails"));
                        synchronized (health) { health.put(rh); }
                        continue;
                    }
                    total++;
                    final int tot = total;
                    pool.submit(new Runnable() {
                        public void run() {
                            JSONObject h = new JSONObject();
                            long t0 = System.currentTimeMillis();
                            final String url = feed.optString("url");
                            final boolean gnFeed = feed.optBoolean("gn");
                            try {
                                h.put("code", code); h.put("label", feed.optString("label")); h.put("url", url);
                                h.put("gn", gnFeed);
                                Net.Resp r = null; Exception last = null;
                                for (int attempt = 0; attempt < 2; attempt++) { // a failed feed is retried at once — never paused
                                    try { r = Net.get(url, attempt == 0 ? 20000 : 15000); if (r.code == 200) break; last = new Exception("HTTP " + r.code); }
                                    catch (Exception e) { last = e; r = null; }
                                    if (attempt == 0) try { Thread.sleep(1200); } catch (InterruptedException ie) { break; }
                                }
                                if (r == null || r.code != 200) throw last != null ? last : new Exception("No response");
                                h.put("http", r.code);
                                List<RssParser.Raw> raws = RssParser.parse(r.body, new RssParser.ParserFactory() {
                                    public XmlPullParser create() { return Xml.newPullParser(); }
                                });
                                List<JSONObject> items = RssParser.normalise(raws, code, feed.optString("label"), feed.optBoolean("gn"), now);
                                if (feed.has("keep") || feed.has("max")) items = keepOnly(items, feed, now); // news sitemaps: only the sections that matter, newest first
                                for (JSONObject it : items) k.score(it);
                                synchronized (fetched) { if (!closed.get()) fetched.addAll(items); }
                                h.put("ok", !raws.isEmpty());
                                h.put("count", items.size());
                                if (raws.isEmpty()) throw new Exception("No items parsed"); // a sitemap filtered to nothing new is still a working feed
                                synchronized (fstate) { fstate.remove(url); }
                            } catch (Throwable e) {
                                try {
                                    h.put("ok", false);
                                    h.put("err", e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + Text.cap(e.getMessage(), 90) : ""));
                                    synchronized (failedUrls) { failedUrls.add(url); } // Google News feeds count too: empty for a week = dead
                                } catch (Exception ignored) {}
                            }
                            try { h.put("ms", System.currentTimeMillis() - t0); h.put("at", System.currentTimeMillis()); } catch (Exception ignored) {}
                            synchronized (health) { if (!closed.get()) health.put(h); }
                            int d = done.incrementAndGet();
                            try { emit("sync", new JSONObject().put("state", "progress").put("done", d).put("total", tot)); } catch (Exception ignored) {}
                        }
                    });
                }
            }
            pool.shutdown();
            pool.awaitTermination(150, TimeUnit.SECONDS);
            pool.shutdownNow();
            closed.set(true);
            List<JSONObject> got;
            synchronized (fetched) { got = new ArrayList<JSONObject>(fetched); }
            int okFeeds = 0;
            synchronized (health) { for (int i = 0; i < health.length(); i++) if (health.optJSONObject(i).optBoolean("ok")) okFeeds++; }
            // A failed feed is retried on every sync. Only when it has returned nothing for 7 days in a row (counted only on syncs
            // where the network worked, at least 6 tries) is it removed; a removed feed is re-checked once a week and comes back if it answers.
            synchronized (fstate) {
                if (okFeeds >= 3) {
                    synchronized (failedUrls) {
                        for (String url : failedUrls) {
                            JSONObject st = fstate.optJSONObject(url);
                            if (st == null) st = new JSONObject();
                            st.put("fails", st.optInt("fails", 0) + 1);
                            st.remove("until");
                            // the 7 days must be watched, not assumed: a gap of 2+ days without a counted try restarts the count
                            if (!st.optBoolean("retired") && st.has("lastTry") && now - st.optLong("lastTry") > 2 * 24 * 3600 * 1000L) { st.put("since", now); st.put("fails", 1); }
                            if (!st.has("since")) st.put("since", now);
                            st.put("lastTry", now);
                            if (!st.optBoolean("retired") && now - st.optLong("since") >= RETIRE_MS && st.optInt("fails") >= RETIRE_MIN_FAILS) st.put("retired", true).put("retiredAt", now);
                            fstate.put(url, st);
                        }
                    }
                }
                synchronized (health) {
                    for (int i = 0; i < health.length(); i++) {
                        JSONObject h = health.optJSONObject(i);
                        JSONObject st = fstate.optJSONObject(h.optString("url"));
                        if (st != null && !h.optBoolean("ok")) {
                            h.put("fails", st.optInt("fails")).put("since", st.optLong("since"));
                            if (st.optBoolean("retired")) h.put("retired", true).put("retiredAt", st.optLong("retiredAt"));
                        }
                    }
                }
                Store.write(c, "feedstate.json", fstate.toString());
            }

            // merge with store
            List<JSONObject> old = loadItems(c);
            Map<String, JSONObject> byId = new LinkedHashMap<String, JSONObject>();
            for (JSONObject o : old) byId.put(o.optString("id"), o);
            List<JSONObject> added = new ArrayList<JSONObject>();
            boolean firstRun = old.isEmpty();
            for (JSONObject it : got) {
                String id = it.optString("id");
                JSONObject ex = byId.get(id);
                if (ex == null) {
                    if (it.optDouble("sc", 0) < -2) continue; // don't store obvious noise
                    byId.put(id, it);
                    added.add(it);
                } else {
                    it.put("fs", ex.optLong("fs", now));
                    if (!it.optBoolean("pd")) it.put("p", ex.optLong("p", it.optLong("p")));
                    for (String key : new String[]{"x", "xd", "xb", "xu", "cx", "full", "xa"}) if (!it.has(key) && ex.has(key)) it.put(key, ex.get(key)); // full article read earlier
                    if (it.optString("d").length() < ex.optString("d").length()) { it.put("d", ex.optString("d")); k.score(it); }
                    byId.put(id, it);
                }
            }
            // Background reading 1: coaching pages of the last 10 days, read in full (priority source of topics)
            Set<String> enabled = new HashSet<String>();
            for (int i = 0; i < sources.length(); i++) { String sc = sources.getJSONObject(i).optString("code"); if (!disabled.contains(sc)) enabled.add(sc); }
            if (!firstRunFast(old)) {
                emit("sync", new JSONObject().put("state", "stage").put("stage", "coaching"));
                JSONObject hs = Store.readObj(c, "harvest.json");
                if (hs == null) hs = new JSONObject();
                try {
                    for (JSONObject it : Harvest.coaching(hs, enabled, now, k, 75000L)) {
                        JSONObject ex = byId.get(it.optString("id"));
                        if (ex != null) it.put("fs", ex.optLong("fs", now)); else added.add(it);
                        byId.put(it.optString("id"), it);
                    }
                } catch (Throwable ignored) {}
                synchronized (hs) { Store.write(c, "harvest.json", hs.toString()); }
            }
            List<JSONObject> all = new ArrayList<JSONObject>();
            for (JSONObject it : byId.values()) if (it.optLong("p") >= now - (EditionBuilder.COACHING.contains(it.optString("s")) ? COACH_KEEP_MS : 7 * Ist.DAY)) all.add(it);
            // Background reading 2: the full articles of the strongest stories
            if (!firstRunFast(old)) {
                emit("sync", new JSONObject().put("state", "stage").put("stage", "articles"));
                try { Harvest.articles(all, now, 70000L, 28); } catch (Throwable ignored) {}
            }
            JSONArray ia = new JSONArray();
            for (JSONObject it : all) ia.put(it);
            synchronized (Harvest.WRITE) { Store.write(c, "items.json", ia.toString()); }
            itemsCache = new ArrayList<JSONObject>(all);
            synchronized (health) { Store.write(c, "health.json", health.toString()); }

            EditionBuilder eb = builder(c);
            String today = Ist.dateKey(now);
            List<String> events = new ArrayList<String>();
            JSONObject ydayEd = buildEditions(c, eb, all, System.currentTimeMillis(), events); // time of build, not of sync start (morning cut-off)
            try { Store.write(c, "live.json", eb.live(all, today, now).toString()); } catch (Exception ignored) {}

            // notifications
            Notifier.ensureChannels(c);
            if (sp.getBoolean("notif", true)) {
                if (events.contains("delivered") && ydayEd != null && !ydayEd.optBoolean("notifyPending")) Notifier.edition(c, ydayEd);
                int amendN = 0;
                for (String e : events) if (e.startsWith("amend:") && amendN++ < 2) {
                    String[] p = e.split(":", 3);
                    Notifier.amendment(c, p[1], p[2]);
                }
                if (sp.getBoolean("alerts", true) && !firstRun && !quiet(sp, now)) {
                    List<JSONObject> hot = new ArrayList<JSONObject>();
                    long dayStart = Ist.startOf(today);
                    List<Set<String>> seen = new ArrayList<Set<String>>();
                    Collections.sort(added, new Comparator<JSONObject>() {
                        public int compare(JSONObject a, JSONObject b) { return Double.compare(b.optDouble("sc"), a.optDouble("sc")); }
                    });
                    for (JSONObject it : added) {
                        if (it.optLong("p") < dayStart - 6 * 3600000L || it.optDouble("sc", 0) < Kb.ALERT_MIN || it.has("noise") || EditionBuilder.COACHING.contains(it.optString("s"))) continue;
                        Set<String> tk = Text.tokens(it.optString("t"));
                        boolean dup = false;
                        for (Set<String> o : seen) if (Text.jaccard(o, tk) >= 0.34) { dup = true; break; }
                        if (dup) continue;
                        seen.add(tk); hot.add(it);
                        if (hot.size() >= 6) break;
                    }
                    if (!hot.isEmpty()) Notifier.alerts(c, hot);
                }
            }

            // AI enrichment of the edition being compiled (and of a just-delivered one) — runs in its own job
            migrate(sp);
            aiDate = aiDue(c, sp, now);
            releaseHeld(c, sp, now);

            int ok = okFeeds, active = 0;
            synchronized (health) { for (int i = 0; i < health.length(); i++) if (!health.optJSONObject(i).optBoolean("retired")) active++; } // removed feeds don't count
            sp.edit().putLong("lastSync", now).putString("lastReason", reason).putInt("feedsOk", ok).putInt("feedsTotal", active).putInt("lastAdded", added.size()).apply();
            result.put("ok", true).put("added", added.size()).put("feedsOk", ok).put("feedsTotal", active).put("items", all.size());
            emit("sync", new JSONObject().put("state", "done").put("added", added.size()).put("feedsOk", ok).put("feedsTotal", active));
        } catch (Throwable e) {
            try {
                result.put("ok", false).put("error", String.valueOf(e));
                emit("sync", new JSONObject().put("state", "error").put("error", String.valueOf(e)));
            } catch (Exception ignored) {}
        } finally {
            running.set(false);
        }
        if (deliverPending) { deliverPending = false; deliver(c); } // morning alarm fired while this sync ran
        if (aiAfterDeliver != null) { if (aiDate == null) aiDate = aiAfterDeliver; aiAfterDeliver = null; }
        if (aiDate != null) try { result.put("aiDate", aiDate); } catch (Exception ignored) {} // caller runs the AI edition in its own job/service
        return result;
    }

    public static JSONObject buildEditions(Context context, EditionBuilder editionBuilder, List<JSONObject> list, long j, List<String> list2) throws Exception {
        JSONObject jSONObject;
        String dateKey = Ist.dateKey(j);
        JSONObject jSONObject2 = null;
        HashMap hashMap = new HashMap();
        int i = 3;
        while (i >= 1) {
            String addDays = Ist.addDays(dateKey, -i);
            synchronized (EDLOCK) {
                JSONObject edition = Store.edition(context, addDays);
                if (edition == null || edition.optLong("deliveredAt") <= 0 || i <= 1) {
                    JSONObject edition2 = Store.edition(context, Ist.addDays(addDays, -1));
                    editionBuilder.history = new ArrayList();
                    for (int i2 = 1; i2 <= 10; i2++) {
                        String addDays2 = Ist.addDays(addDays, -i2);
                        JSONObject edition3 = hashMap.containsKey(addDays2) ? (JSONObject) hashMap.get(addDays2) : Store.edition(context, addDays2);
                        hashMap.put(addDays2, edition3);
                        for (String str : new String[]{"topics", "catchup"}) {
                            JSONArray optJSONArray = edition3 == null ? null : edition3.optJSONArray(str);
                            for (int i3 = 0; optJSONArray != null && i3 < optJSONArray.length(); i3++) {
                                JSONObject jSONObject3 = new JSONObject(optJSONArray.getJSONObject(i3).toString());
                                jSONObject3.put("date", addDays2);
                                editionBuilder.history.add(jSONObject3);
                            }
                        }
                    }
                    JSONObject build = editionBuilder.build(list, addDays, edition, edition2, j);
                    if (build.getJSONArray("topics").length() == 0 && edition == null) {
                        jSONObject = jSONObject2;
                    } else {
                        if ((edition == null || edition.optLong("deliveredAt") == 0) && build.optLong("deliveredAt") > 0) {
                            patternNote(context, build);
                            appendFacts(context, EditionBuilder.facts(build), "auto");
                            learn(context, build);
                            if (i == 1) {
                                list2.add("delivered");
                            }
                        }
                        JSONArray jSONArray = build.getJSONArray("topics");
                        for (int i4 = 0; i4 < jSONArray.length(); i4++) {
                            JSONObject jSONObject4 = jSONArray.getJSONObject(i4);
                            if (jSONObject4.optBoolean("amend") && jSONObject4.optLong("addedAt") == j) {
                                list2.add("amend:" + addDays + ":" + jSONObject4.optString("head"));
                                ArrayList arrayList = new ArrayList();
                                arrayList.addAll(EditionBuilder.facts(new JSONObject().put("date", addDays).put("topics", new JSONArray().put(jSONObject4))));
                                appendFacts(context, arrayList, "auto");
                            }
                        }
                        threads(context, build, hashMap);
                        Store.saveEdition(context, build);
                        hashMap.put(addDays, build);
                        jSONObject = i == 1 ? build : jSONObject2;
                    }
                } else {
                    jSONObject = jSONObject2;
                }
            }
            i--;
            jSONObject2 = jSONObject;
        }
        return jSONObject2;
    }

    /** Morning delivery (alarm): finalise yesterday's edition from items already fetched, notify on time; AI polish after. */
    public static void deliver(Context ctx) {
        Context c = ctx.getApplicationContext();
        if (!running.compareAndSet(false, true)) { deliverPending = true; return; } // the running sync delivers when it finishes
        try {
            long now = System.currentTimeMillis();
            List<JSONObject> all = loadItems(c);
            if (all.isEmpty()) return;
            List<String> events = new ArrayList<String>();
            JSONObject ydayEd = buildEditions(c, builder(c), all, now, events);
            SharedPreferences sp = Store.prefs(c);
            Notifier.ensureChannels(c);
            if (sp.getBoolean("notif", true) && events.contains("delivered") && ydayEd != null) {
                Notifier.edition(c, ydayEd); // on time, never held for the AI
                if (needsAi(sp, ydayEd)) aiAfterDeliver = ydayEd.optString("date"); // optional polish, in the background
            }
            emit("sync", new JSONObject().put("state", "done").put("added", 0).put("feedsOk", sp.getInt("feedsOk", 0)).put("feedsTotal", sp.getInt("feedsTotal", 0)));
        } catch (Throwable ignored) {
        } finally {
            running.set(false);
        }
    }

    static boolean quiet(SharedPreferences sharedPreferences, long j) {
        if (!sharedPreferences.getBoolean("quiet", true)) {
            return false;
        }
        int minuteOfDay = Ist.minuteOfDay(j);
        return minuteOfDay >= 1350 || minuteOfDay < 390;
    }

    static void patternNote(Context context, JSONObject jSONObject) {
        JSONObject edition;
        boolean z;
        try {
            JSONArray readArr = Store.readArr(context, "checklog.json");
            JSONArray jSONArray = jSONObject.getJSONArray("check");
            String string = jSONObject.getString("date");
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject2 = jSONArray.getJSONObject(i);
                if (!jSONObject2.optBoolean("ok")) {
                    arrayList.add(Integer.valueOf(jSONObject2.getInt("n")));
                    readArr.put(new JSONObject().put("date", string).put("rule", jSONObject2.getInt("n")).put("fix", jSONObject2.optString("msg")));
                }
            }
            while (readArr.length() > 400) {
                readArr.remove(0);
            }
            Store.write(context, "checklog.json", readArr.toString());
            JSONArray jSONArray2 = new JSONArray();
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                int intValue = ((Integer) it.next()).intValue();
                int i2 = 1;
                int i3 = 1;
                while (i2 <= 2 && (edition = Store.edition(context, Ist.addDays(string, -i2))) != null) {
                    JSONArray optJSONArray = edition.optJSONArray("check");
                    if (optJSONArray != null) {
                        z = false;
                        for (int i4 = 0; i4 < optJSONArray.length(); i4++) {
                            if (optJSONArray.getJSONObject(i4).getInt("n") == intValue && !optJSONArray.getJSONObject(i4).optBoolean("ok")) {
                                z = true;
                            }
                        }
                    } else {
                        z = false;
                    }
                    if (!z) {
                        break;
                    }
                    i2++;
                    i3++;
                }
                if (i3 >= 3) {
                    jSONArray2.put("Rule " + intValue + " tripped 3 editions in a row — pattern flagged for re-examination.");
                }
            }
            if (jSONArray2.length() > 0) {
                jSONObject.put("patternNotes", jSONArray2);
            }
        } catch (Exception e) {
        }
    }

    static String normFact(String str) {
        return str.replaceAll("^\\[[0-9\\-]+\\]\\s*", "").toLowerCase().replaceAll("[^a-z0-9%₹]+", " ").trim();
    }

    public static int appendFacts(Context context, List<String> list, String str) {
        int i;
        int i2 = 0;
        synchronized (Store.LOCK) {
            JSONArray readArr = Store.readArr(context, "factbank.json");
            HashSet hashSet = new HashSet();
            for (int i3 = 0; i3 < readArr.length(); i3++) {
                hashSet.add(normFact(readArr.optJSONObject(i3).optString("t")));
            }
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                String replaceAll = it.next().trim().replaceAll("^[-•*]\\s*", "");
                if (replaceAll.length() >= 12) {
                    String normFact = normFact(replaceAll);
                    if (normFact.length() >= AI_RUNS_PER_EDITION && hashSet.add(normFact)) {
                        try {
                            JSONObject jSONObject = new JSONObject();
                            Matcher matcher = Pattern.compile("^\\[(\\d\\d-\\d\\d-\\d{4})\\]\\s*").matcher(replaceAll);
                            if (matcher.find()) {
                                jSONObject.put("d", matcher.group(1));
                                replaceAll = replaceAll.substring(matcher.end());
                            }
                            jSONObject.put("t", replaceAll);
                            jSONObject.put("src", str);
                            jSONObject.put("at", System.currentTimeMillis());
                            readArr.put(jSONObject);
                            i = i2 + 1;
                        } catch (Exception e) {
                            i = i2;
                        }
                        i2 = i;
                    }
                }
            }
            if (i2 > 0) {
                Store.write(context, "factbank.json", readArr.toString());
            }
        }
        return i2;
    }

    static void stage(String str, String str2) {
        try {
            emit("ai", new JSONObject().put("state", "stage").put("date", str).put("stage", str2));
        } catch (Exception e) {
        }
    }

    static boolean topicNeedsAi(JSONObject jSONObject) {
        if (EditionBuilder.selfSufficient(jSONObject)) {
            return false;
        }
        JSONObject optJSONObject = jSONObject.optJSONObject("ai");
        if (optJSONObject != null) {
            return (optJSONObject.optBoolean("stale") || !Ai.sig(jSONObject).equals(optJSONObject.optString("sig"))) && optJSONObject.optInt("n") < 3;
        }
        return true;
    }

    static boolean needsAi(SharedPreferences sharedPreferences, JSONObject jSONObject) {
        if (chain(sharedPreferences).isEmpty() || !sharedPreferences.getBoolean("autoAI", true) || jSONObject.optInt("aiRuns") >= AI_RUNS_PER_EDITION) {
            return false;
        }
        JSONArray optJSONArray = jSONObject.optJSONArray("topics");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            if (topicNeedsAi(optJSONArray.optJSONObject(i))) {
                return true;
            }
        }
        return false;
    }

    static String aiDue(Context context, SharedPreferences sharedPreferences, long j) {
        String dateKey = Ist.dateKey(j);
        for (int i = 1; i <= 2; i++) {
            JSONObject edition = Store.edition(context, Ist.addDays(dateKey, -i));
            if (edition != null && needsAi(sharedPreferences, edition)) {
                return edition.optString("date");
            }
        }
        return null;
    }

    static void releaseHeld(Context context, SharedPreferences sharedPreferences, long j) {
        for (int i = 1; i <= 2; i++) {
            try {
                String addDays = Ist.addDays(Ist.dateKey(System.currentTimeMillis()), -i);
                synchronized (EDLOCK) {
                    JSONObject edition = Store.edition(context, addDays);
                    if (edition != null && edition.optBoolean("notifyPending")) {
                        if (!needsAi(sharedPreferences, edition) || j - edition.optLong("deliveredAt") > 2400000) {
                            releaseNow(context, sharedPreferences, edition);
                        }
                    }
                }
            } catch (Exception e) {
                return;
            }
        }
    }

    static void releaseNow(Context context, SharedPreferences sharedPreferences, JSONObject jSONObject) {
        try {
            jSONObject.remove("notifyPending");
            Store.saveEdition(context, jSONObject);
            Notifier.ensureChannels(context);
            if (sharedPreferences.getBoolean("notif", true)) {
                Notifier.edition(context, jSONObject);
            }
        } catch (Exception e) {
        }
    }

    public static void releaseAll(Context context) {
        releaseHeld(context, Store.prefs(context), System.currentTimeMillis() + 3600000);
    }

    public static void runAi(Context context, String str, boolean z) {
        int i;
        Ai.Result result;
        String str2;
        Object obj;
        JSONObject jSONObject;
        boolean z2;
        boolean z3;
        int i2;
        Object obj2;
        if (!aiRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            {
                emit("ai", new JSONObject().put("state", "start").put("date", str));
                SharedPreferences prefs = Store.prefs(context);
                migrate(prefs);
                List<String> chain = chain(prefs);
                if (chain.isEmpty()) {
                    emit("ai", new JSONObject().put("state", "error").put("date", str).put("error", "Add an AI API key in Settings first (Gemini, Groq, OpenRouter, OpenAI, Grok or Claude)."));
                    releaseHeld(context, prefs, System.currentTimeMillis() + 3600000);
                    return;
                }
                JSONObject edition;
                synchronized (EDLOCK) {
                    edition = Store.edition(context, str);
                    if (edition == null) {
                        emit("ai", new JSONObject().put("state", "error").put("date", str).put("error", "No edition for " + str + " yet."));
                        return;
                    }
                    try {
                        Sentinel.cleanEdition(edition, kb(context));
                    } catch (Throwable th) {
                    }
                    edition.put("aiRuns", edition.optInt("aiRuns", 0) + 1);
                    edition.remove("aiError");
                    Store.saveEdition(context, edition);
                }
                {
                    boolean z4 = prefs.getBoolean("webSearch", true);
                    boolean z5 = prefs.getBoolean("verifyPass", true);
                    HashMap hashMap = new HashMap();
                    for (JSONObject jSONObject2 : loadItems(context)) {
                        hashMap.put(jSONObject2.optString("id"), jSONObject2);
                    }
                    JSONArray jSONArray = edition.getJSONArray("topics");
                    ArrayList arrayList = new ArrayList();
                    for (int i3 = 0; i3 < jSONArray.length(); i3++) {
                        JSONObject jSONObject3 = jSONArray.getJSONObject(i3);
                        if (z || topicNeedsAi(jSONObject3)) {
                            arrayList.add(jSONObject3);
                        }
                    }
                    Kb kb2 = kb(context);
                    String str3 = chain.get(0);
                    boolean equals = "groq".equals(str3);
                    int i4 = equals ? 2 : 4;
                    ArrayList arrayList2 = new ArrayList();
                    StringBuilder sb = new StringBuilder();
                    LinkedHashMap linkedHashMap = new LinkedHashMap();
                    LinkedHashMap<String, String> linkedHashMap2 = new LinkedHashMap<String, String>();
                    HashMap hashMap2 = new HashMap();
                    int i5 = 0;
                    String str4 = null;
                    String str5 = null;
                    boolean z6 = false;
                    boolean z7 = false;
                    int i6 = 0;
                    int i7 = 0;
                    String str6 = str3;
                    while (i5 < arrayList.size()) {
                        List<JSONObject> subList = arrayList.subList(i5, Math.min(arrayList.size(), i5 + i4));
                        for (JSONObject jSONObject4 : subList) {
                            hashMap2.put(jSONObject4.optString("id"), Ai.sig(jSONObject4));
                        }
                        stage(str, "draft");
                        String enrichMessage = Ai.enrichMessage(str, subList, hashMap, kb2, equals);
                        Ai.Result result2 = null;
                        Iterator<String> it = chain.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                result = result2;
                                str2 = str6;
                                break;
                            }
                            str2 = it.next();
                            if (!arrayList2.contains(str2)) {
                                if (!str2.equals(chain.get(0))) {
                                    stage(str, "fallback:" + Ai.providerName(str2));
                                }
                                result2 = Ai.call(str2, keyOf(prefs, str2), modelOf(prefs, str2), false, false, Ai.ENRICH_SYSTEM, (!"groq".equals(str2) || equals) ? enrichMessage : Ai.enrichMessage(str, subList, hashMap, kb2, true), 150);
                                if (result2.error == null && Ai.parseJson(result2.text) != null) {
                                    result = result2;
                                    break;
                                }
                                if (result2.error == null) {
                                    result2.error = Ai.providerName(str2) + " returned text that isn't the expected format.";
                                }
                                arrayList2.add(str2);
                                sb.append(sb.length() > 0 ? "\n" : "").append("• ").append(result2.error);
                            }
                        }
                        if (result == null || result.error != null) {
                            break;
                        }
                        String str7 = result.model;
                        JSONObject parseJson = Ai.parseJson(result.text);
                        if (z5) {
                            stage(str, "verify");
                            boolean z8 = z4 && Ai.canSearch(str2, result.model) && !"groq".equals(str2);
                            String verifyMessage = Ai.verifyMessage(str, parseJson, subList, hashMap, kb2, equals);
                            Ai.Result call = z8 ? Ai.call(str2, keyOf(prefs, str2), result.model, true, true, Ai.VERIFY_JSON_SYSTEM, verifyMessage, 180) : null;
                            if (call == null || call.error != null || call.searchDropped || ((!call.usedSearch && call.citations.isEmpty()) || Ai.parseJson(call.text) == null)) {
                                Ai.Result callAny = callAny(prefs, chain, str2, arrayList2, Ai.SOURCECHECK_JSON_SYSTEM, verifyMessage, 150);
                                if (callAny == null || callAny.error != null || Ai.parseJson(callAny.text) == null) {
                                    obj2 = "none";
                                    jSONObject = parseJson;
                                    z2 = z6;
                                    z3 = z7;
                                } else {
                                    jSONObject = Ai.parseJson(callAny.text);
                                    obj2 = "sources";
                                    z2 = true;
                                    z3 = z7;
                                }
                            } else {
                                jSONObject = Ai.parseJson(call.text);
                                linkedHashMap2.putAll(call.citations);
                                obj2 = "web";
                                z2 = z6;
                                z3 = true;
                            }
                            i2 = jSONObject.optInt("corrections", 0) + i6;
                            obj = obj2;
                        } else {
                            obj = "none";
                            jSONObject = parseJson;
                            z2 = z6;
                            z3 = z7;
                            i2 = i6;
                        }
                        JSONArray optJSONArray = jSONObject.optJSONArray("topics");
                        for (int i8 = 0; optJSONArray != null && i8 < optJSONArray.length(); i8++) {
                            JSONObject cleanCard = Ai.cleanCard(optJSONArray.optJSONObject(i8));
                            if (cleanCard != null) {
                                if (!cleanCard.has("un")) {
                                    JSONArray unrelated = Ai.unrelated(parseJson, cleanCard.optString("id"));
                                    if (unrelated.length() > 0) {
                                        cleanCard.put("un", unrelated);
                                    }
                                }
                                cleanCard.put("chk", obj).put("by", Ai.providerName(str2));
                                linkedHashMap.put(cleanCard.optString("id"), cleanCard);
                            }
                        }
                        i5 += i4;
                        str4 = str7;
                        str5 = str2;
                        z6 = z2;
                        z7 = z3;
                        i6 = i2;
                        i7 += subList.size();
                        str6 = str2;
                    }
                    long currentTimeMillis = System.currentTimeMillis();
                    ArrayList arrayList3 = new ArrayList();
                    synchronized (EDLOCK) {
                        JSONObject edition2 = Store.edition(context, str);
                        JSONObject jSONObject5 = edition2 == null ? edition : edition2;
                        JSONArray jSONArray2 = jSONObject5.getJSONArray("topics");
                        int i9 = 0;
                        int i10 = 0;
                        while (i10 < jSONArray2.length()) {
                            JSONObject jSONObject6 = jSONArray2.getJSONObject(i10);
                            JSONObject jSONObject7 = (JSONObject) linkedHashMap.get(jSONObject6.optString("id"));
                            if (jSONObject7 == null) {
                                i = i9;
                            } else {
                                JSONObject optJSONObject = jSONObject6.optJSONObject("ai");
                                String sig = hashMap2.containsKey(jSONObject6.optString("id")) ? (String) hashMap2.get(jSONObject6.optString("id")) : Ai.sig(jSONObject6);
                                jSONObject7.put("sig", sig).put("n", (optJSONObject == null ? 0 : optJSONObject.optInt("n")) + 1).put("at", currentTimeMillis);
                                if (!sig.equals(Ai.sig(jSONObject6))) {
                                    jSONObject7.put("stale", true);
                                }
                                jSONObject7.remove("id");
                                jSONObject6.put("ai", jSONObject7);
                                Sentinel.clean(jSONObject6, kb2);
                                int i11 = i9 + 1;
                                JSONArray optJSONArray2 = jSONObject7.optJSONArray("b");
                                int i12 = 0;
                                for (int i13 = 0; optJSONArray2 != null && i13 < optJSONArray2.length() && i12 < 2; i13++) {
                                    if (Text.hasFigure(optJSONArray2.optString(i13))) {
                                        arrayList3.add("[" + Ist.tag(str) + "] " + Text.cap(jSONObject6.optString("head"), 70) + " — " + optJSONArray2.optString(i13));
                                        i12++;
                                    }
                                }
                                i = i11;
                            }
                            i10++;
                            i9 = i;
                        }
                        JSONObject optJSONObject2 = jSONObject5.optJSONObject("ai");
                        JSONObject jSONObject8 = (optJSONObject2 == null || optJSONObject2.has("text")) ? new JSONObject() : optJSONObject2;
                        if (str5 != null) {
                            jSONObject8.put("provider", Ai.providerName(str5)).put("model", str4);
                        }
                        jSONObject8.put("verified", z7 || jSONObject8.optBoolean("verified")).put("crossChecked", z6 || jSONObject8.optBoolean("crossChecked")).put("at", currentTimeMillis);
                        jSONObject8.put("fixes", jSONObject8.optInt("fixes") + i6);
                        int i14 = 0;
                        for (int i15 = 0; i15 < jSONArray2.length(); i15++) {
                            if (jSONArray2.getJSONObject(i15).has("ai")) {
                                i14++;
                            }
                        }
                        jSONObject8.put("cards", i14).put("of", jSONArray2.length());
                        JSONArray jSONArray3 = new JSONArray();
                        for (Map.Entry<String, String> entry : linkedHashMap2.entrySet()) {
                            if (jSONArray3.length() >= 30) {
                                break;
                            } else {
                                jSONArray3.put(new JSONObject().put("u", entry.getKey()).put("t", entry.getValue()));
                            }
                        }
                        if (jSONArray3.length() > 0) {
                            jSONObject8.put("cites", jSONArray3);
                        }
                        jSONObject5.put("ai", jSONObject8);
                        if (i9 != 0 || sb.length() <= 0) {
                            jSONObject5.remove("aiError");
                        } else {
                            jSONObject5.put("aiError", chain.size() > 1 ? "All AI services failed:\n" + ((Object) sb) : sb.toString().replaceFirst("^• ", ""));
                        }
                        Store.saveEdition(context, jSONObject5);
                    }
                    if (!arrayList3.isEmpty()) {
                        appendFacts(context, arrayList3, "ai");
                    }
                    synchronized (EDLOCK) {
                        JSONObject edition3 = Store.edition(context, str);
                        if (edition3 != null && edition3.optBoolean("notifyPending")) {
                            releaseNow(context, prefs, edition3);
                        }
                    }
                    if (!linkedHashMap.isEmpty() || sb.length() <= 0) {
                        emit("ai", new JSONObject().put("state", "done").put("date", str).put("cards", linkedHashMap.size()).put("verified", z7).put("crossChecked", z6));
                    } else {
                        emit("ai", new JSONObject().put("state", "error").put("date", str).put("error", chain.size() > 1 ? "All AI services failed:\n" + ((Object) sb) : sb.toString().replaceFirst("^• ", "")));
                    }
                    releaseHeld(context, prefs, System.currentTimeMillis());
                }
            }
        } catch (Throwable th2) {
            fail(context, str, String.valueOf(th2));
        } finally {
            aiRunning.set(false);
        }
    }

    /** Catch-up card opened by the reader: write + check that one card with the AI (only thin cards get here). */
    public static void runAiCard(Context c, String date, String id) {
        if (!aiCardRunning.compareAndSet(false, true)) {
            try { emit("aicard", new JSONObject().put("state", "busy").put("date", date).put("id", id)); } catch (Exception ignored) {}
            return;
        }
        try {
            emit("aicard", new JSONObject().put("state", "start").put("date", date).put("id", id));
            SharedPreferences sp = Store.prefs(c);
            migrate(sp);
            List<String> ch = chain(sp);
            if (ch.isEmpty()) { cardFail(date, id, "Add an AI key in Settings to have this card rewritten."); return; }
            JSONObject ed = Store.edition(c, date);
            try { Sentinel.cleanEdition(ed, kb(c)); } catch (Throwable ignored) {}
            JSONObject t = catchupCard(ed, id);
            if (t == null) { cardFail(date, id, "This catch-up item is no longer in the edition."); return; }
            Map<String, JSONObject> items = new HashMap<String, JSONObject>();
            for (JSONObject it : loadItems(c)) items.put(it.optString("id"), it);
            Kb k = kb(c);
            List<JSONObject> part = new ArrayList<JSONObject>();
            part.add(t);
            String sig = Ai.sig(t);
            Ai.Result d = null;
            String provider = null;
            StringBuilder errs = new StringBuilder();
            List<String> failed = new ArrayList<String>();
            for (String pv : ch) {
                Ai.Result r = Ai.call(pv, keyOf(sp, pv), modelOf(sp, pv), false, false, Ai.ENRICH_SYSTEM, Ai.enrichMessage(date, part, items, k, "groq".equals(pv)), 120);
                if (r.error == null && Ai.parseJson(r.text) != null) { provider = pv; d = r; break; }
                if (r.error == null) r.error = Ai.providerName(pv) + " returned text that isn't the expected format.";
                failed.add(pv);
                errs.append(errs.length() > 0 ? "\n" : "").append("• ").append(r.error);
            }
            if (provider == null) { cardFail(date, id, ch.size() > 1 ? "All AI services failed:\n" + errs : errs.toString().replaceFirst("^• ", "")); return; }
            JSONObject fin = Ai.parseJson(d.text);
            String chk = "none";
            if (sp.getBoolean("verifyPass", true)) {
                boolean small = "groq".equals(provider);
                String vu = Ai.verifyMessage(date, fin, part, items, k, small);
                boolean web = sp.getBoolean("webSearch", true) && Ai.canSearch(provider, d.model) && !small;
                Ai.Result v = web ? Ai.call(provider, keyOf(sp, provider), d.model, true, true, Ai.VERIFY_JSON_SYSTEM, vu, 150) : null;
                if (v != null && v.error == null && !v.searchDropped && (v.usedSearch || !v.citations.isEmpty()) && Ai.parseJson(v.text) != null) { fin = Ai.parseJson(v.text); chk = "web"; }
                else {
                    Ai.Result v2 = callAny(sp, ch, provider, failed, Ai.SOURCECHECK_JSON_SYSTEM, vu, 120);
                    if (v2 != null && v2.error == null && Ai.parseJson(v2.text) != null) { fin = Ai.parseJson(v2.text); chk = "sources"; }
                }
            }
            JSONArray ra = fin.optJSONArray("topics");
            JSONObject r = null;
            for (int i = 0; ra != null && i < ra.length() && r == null; i++) { JSONObject x = Ai.cleanCard(ra.optJSONObject(i)); if (x != null && (id.equals(x.optString("id")) || ra.length() == 1)) r = x; }
            if (r == null) { cardFail(date, id, "The AI reply couldn't be used; the card shows the coaching notes instead."); return; }
            if (!r.has("un")) { // a checker may drop the AI's verdict on static cards: keep the draft's
                JSONObject dj = Ai.parseJson(d.text);
                JSONArray un = Ai.unrelated(dj, id);
                if (un.length() == 0 && dj != null && dj.optJSONArray("topics") != null && dj.optJSONArray("topics").length() == 1) un = Ai.unrelated(dj.optJSONArray("topics").optJSONObject(0));
                if (un.length() > 0) r.put("un", un);
            }
            r.remove("id");
            r.put("chk", chk).put("by", Ai.providerName(provider)).put("sig", sig).put("n", 1).put("at", System.currentTimeMillis());
            List<String> facts = new ArrayList<String>();
            synchronized (EDLOCK) {
                JSONObject fresh = Store.edition(c, date);
                JSONObject ft = catchupCard(fresh, id);
                if (ft == null) { cardFail(date, id, "This catch-up item is no longer in the edition."); return; }
                if (!sig.equals(Ai.sig(ft))) r.put("stale", true);
                ft.put("ai", r);
                Sentinel.clean(ft, k);
                Store.saveEdition(c, fresh);
                JSONArray b = r.optJSONArray("b");
                for (int q = 0, n = 0; b != null && q < b.length() && n < 2; q++) if (Text.hasFigure(b.optString(q))) { facts.add("[" + Ist.tag(date) + "] " + Text.cap(ft.optString("head"), 70) + " — " + b.optString(q)); n++; }
            }
            if (!facts.isEmpty()) appendFacts(c, facts, "ai");
            emit("aicard", new JSONObject().put("state", "done").put("date", date).put("id", id).put("chk", chk));
        } catch (Throwable e) {
            cardFail(date, id, String.valueOf(e));
        } finally {
            aiCardRunning.set(false);
        }
    }

    static JSONObject catchupCard(JSONObject jSONObject, String str) {
        JSONArray optJSONArray = jSONObject == null ? null : jSONObject.optJSONArray("catchup");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            if (str.equals(optJSONArray.optJSONObject(i).optString("id"))) {
                return optJSONArray.optJSONObject(i);
            }
        }
        return null;
    }

    static void cardFail(String str, String str2, String str3) {
        try {
            emit("aicard", new JSONObject().put("state", "error").put("date", str).put("id", str2).put("error", str3));
        } catch (Exception e) {
        }
    }

    static String firstLine(String str) {
        String replaceFirst = str.replaceFirst("^• ", "");
        int indexOf = replaceFirst.indexOf(10);
        return indexOf > 0 ? replaceFirst.substring(0, indexOf) : replaceFirst;
    }

    static Ai.Result callAny(SharedPreferences sharedPreferences, List<String> list, String str, List<String> list2, String str2, String str3, int i) {
        ArrayList<String> arrayList = new ArrayList();
        arrayList.add(str);
        for (String str4 : list) {
            if (!arrayList.contains(str4) && !list2.contains(str4)) {
                arrayList.add(str4);
            }
        }
        Ai.Result result = null;
        for (String str5 : arrayList) {
            result = Ai.call(str5, keyOf(sharedPreferences, str5), modelOf(sharedPreferences, str5), false, false, str2, str3, i);
            if (result.error == null) {
                break;
            }
        }
        return result;
    }

    /** Settings → Test key: one tiny request, then (where the model can) a web-search check. */
    public static void testAi(Context c) {
        JSONArray lines = new JSONArray();
        boolean ok = true;
        try {
            SharedPreferences sp = Store.prefs(c);
            String p = sp.getString("aiProvider", provider(sp));
            String key = keyOf(sp, p), model = modelOf(sp, p);
            if (key.length() < 10) { lines.put("✗ No " + Ai.providerName(p) + " key saved."); ok = false; }
            else {
                String guess = Ai.detectProvider(key);
                if (guess != null && !guess.equals(p)) lines.put("⚠ This key looks like a " + Ai.providerName(guess) + " key, but the provider selected is " + Ai.providerName(p) + ".");
                Ai.Result r = Ai.call(p, key, model, false, false, "You are a connection test. Reply with exactly: OK", "Reply with exactly: OK", 45);
                if (r.error != null) { lines.put("✗ " + r.error); ok = false; }
                else {
                    lines.put("✓ Key works · model " + r.model + (r.model.equals(model) ? "" : " (fallback — '" + model + "' not available)"));
                    if (!r.model.equals(model)) sp.edit().putString("model_" + p, r.model).apply();
                    if (Ai.canSearch(p, r.model) && !"groq".equals(p)) {
                        Ai.Result w = Ai.call(p, key, r.model, true, true, "Answer in one short line.", "Using web search, name one headline from today's Press Information Bureau (pib.gov.in).", 60);
                        if (w.error != null) lines.put("⚠ Web search check failed: " + w.error);
                        else if (w.searchDropped || (!w.usedSearch && w.citations.isEmpty())) lines.put("• Web search is not available on this key/plan — editions will be cross-checked against the fetched source articles instead (still accurate, just not a live web check).");
                        else lines.put("✓ Web search works — editions will be web fact-checked.");
                    } else if ("openrouter".equals(p)) {
                        lines.put("• OpenRouter free router: no credits needed (about 50 requests/day — an edition uses 2–3). Editions are cross-checked against the fetched source articles; live web search needs credits.");
                    } else if ("groq".equals(p)) {
                        lines.put("• Groq free tier = 8,000 tokens/minute: News Crux sends the cards in small batches and cross-checks against fetched sources (live web check only on paid tiers).");
                    }
                }
            }
        } catch (Throwable t) {
            lines.put("✗ " + t);
            ok = false;
        }
        try { emit("aitest", new JSONObject().put("ok", ok).put("lines", lines)); } catch (Exception ignored) {}
    }

    static void fail(Context context, String str, String str2) {
        try {
            synchronized (EDLOCK) {
                JSONObject edition = Store.edition(context, str);
                if (edition != null) {
                    edition.put("aiError", str2);
                    Store.saveEdition(context, edition);
                }
            }
            emit("ai", new JSONObject().put("state", "error").put("date", str).put("error", str2));
            releaseHeld(context, Store.prefs(context), System.currentTimeMillis() + 3600000);
        } catch (Exception e) {
        }
    }

    public static String live(Context context) {
        String read = Store.read(context, "live.json");
        if (read != null) {
            try {
                if (new JSONObject(read).optString("date").equals(Ist.dateKey(System.currentTimeMillis()))) {
                    return read;
                }
            } catch (Exception e) {
            }
        }
        try {
            long currentTimeMillis = System.currentTimeMillis();
            String jSONObject = builder(context).live(loadItems(context), Ist.dateKey(currentTimeMillis), currentTimeMillis).toString();
            Store.write(context, "live.json", jSONObject);
            return jSONObject;
        } catch (Exception e2) {
            return "{\"topics\":[],\"error\":\"" + e2.getClass().getSimpleName() + "\"}";
        }
    }

    public static String feed(Context context, String str, int i) {
        try {
            List<JSONObject> loadItems = loadItems(context);
            Collections.sort(loadItems, new Comparator<JSONObject>() { // from class: app.newsbreak.Engine.4
                @Override // java.util.Comparator
                public int compare(JSONObject jSONObject, JSONObject jSONObject2) {
                    return Long.compare(jSONObject2.optLong("p"), jSONObject.optLong("p"));
                }
            });
            JSONArray jSONArray = new JSONArray();
            for (JSONObject jSONObject : loadItems) {
                if (str == null || str.length() <= 0 || str.equals(jSONObject.optString("s"))) {
                    if (!jSONObject.has("noise")) {
                        JSONObject jSONObject2 = new JSONObject(jSONObject.toString());
                        jSONObject2.remove("xb");
                        jSONObject2.remove("cx");
                        jSONObject2.remove("xd");
                        jSONArray.put(jSONObject2);
                        if (jSONArray.length() >= i) {
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            }
            return jSONArray.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    static Map<String, Object> unused() {
        return new HashMap();
    }
}
