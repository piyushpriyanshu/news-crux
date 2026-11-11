package app.newsbreak;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class Backup {
    static final String[] FILES = {"factbank.json", "bookmarks.json", "reported.json", "adapt.json", "checklog.json", "pw.json", "pyq_tries.json"};

    private Backup() {
    }

    public static String export(Context context) throws Exception {
        String str;
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("app", "newsbreak").put("v", 1).put("at", System.currentTimeMillis());
        JSONObject jSONObject2 = new JSONObject();
        for (Map.Entry<String, ?> entry : Store.prefs(context).getAll().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                str = "b";
            } else {
                str = value instanceof Integer ? "i" : value instanceof Long ? "l" : value instanceof String ? "s" : null;
            }
            if (str != null) {
                jSONObject2.put(entry.getKey(), new JSONObject().put("t", str).put("v", value));
            }
        }
        jSONObject.put("prefs", jSONObject2);
        JSONObject jSONObject3 = new JSONObject();
        for (String str2 : FILES) {
            String read = Store.read(context, str2);
            if (read != null) {
                jSONObject3.put(str2, read);
            }
        }
        jSONObject.put("files", jSONObject3);
        JSONObject jSONObject4 = new JSONObject();
        List<String> editionDates = Store.editionDates(context);
        for (int i = 0; i < editionDates.size() && i < 90; i++) {
            JSONObject edition = Store.edition(context, editionDates.get(i));
            if (edition != null) {
                jSONObject4.put(editionDates.get(i), edition);
            }
        }
        jSONObject.put("editions", jSONObject4);
        return jSONObject.toString();
    }

    /** Restores a backup made by News Break / News Crux: settings, lists (merged) and editions (a richer copy on the phone is kept). */
    public static String restore(Context c, String json) throws Exception {
        JSONObject o;
        try { o = new JSONObject(json); } catch (Exception e) { throw new Exception("Not a News Crux backup file"); }
        if (!"newsbreak".equals(o.optString("app"))) throw new Exception("Not a News Crux backup file");
        int prefsN = 0, filesN = 0, edN = 0, pwN = 0;
        JSONObject prefs = o.optJSONObject("prefs");
        if (prefs != null) {
            SharedPreferences.Editor ed = Store.prefs(c).edit();
            Iterator<String> it = prefs.keys();
            while (it.hasNext()) {
                String k = it.next();
                if (k.equals("lastSync") || k.equals("feedsOk") || k.equals("feedsTotal")) continue;
                JSONObject v = prefs.optJSONObject(k);
                if (v == null) continue;
                String t = v.optString("t");
                if ("b".equals(t)) ed.putBoolean(k, v.optBoolean("v"));
                else if ("i".equals(t)) ed.putInt(k, v.optInt("v"));
                else if ("l".equals(t)) ed.putLong(k, v.optLong("v"));
                else if ("s".equals(t)) ed.putString(k, v.optString("v"));
                prefsN++;
            }
            ed.commit();
        }
        JSONObject files = o.optJSONObject("files");
        if (files != null) {
            for (String f : FILES) {
                String v = files.optString(f, null);
                if (v == null) continue;
                if (f.equals("factbank.json")) v = mergeFacts(Store.readArr(c, f), new JSONArray(v)).toString();
                else if (f.equals("bookmarks.json")) v = mergeBy(Store.readArr(c, f), new JSONArray(v), "id").toString();
                else if (f.equals("reported.json")) v = mergeBy(Store.readArr(c, f), new JSONArray(v), "k").toString();
                else if (f.equals("checklog.json")) v = mergeBy(Store.readArr(c, f), new JSONArray(v), null).toString();
                else if (f.equals("adapt.json")) {
                    JSONObject cur = Store.readObj(c, f), in = new JSONObject(v);
                    if (cur != null) { Iterator<String> it = cur.keys(); while (it.hasNext()) { String k = it.next(); in.put(k, Math.max(in.optDouble(k, 0), cur.optDouble(k, 0))); } }
                    v = in.toString();
                }
                else if (f.equals("pyq_tries.json")) { // your PYQ attempts: keep the latest try of each question
                    JSONObject cur = Store.readObj(c, f), in = new JSONObject(v);
                    if (cur != null) { Iterator<String> it = cur.keys(); while (it.hasNext()) { String k = it.next(); JSONArray a = cur.optJSONArray(k), b = in.optJSONArray(k); if (a != null && (b == null || a.optLong(1) > b.optLong(1))) in.put(k, a); } }
                    v = in.toString();
                }
                if (f.equals("pw.json")) {
                    JSONObject pw = new JSONObject(v); // your private PYQ explanations: validated, then kept on this phone only
                    Store.write(c, f, v);
                    Bridge.pwCache = null;
                    pwN = pw.length();
                    continue;
                }
                Store.write(c, f, v);
                filesN++;
            }
        }
        JSONObject eds = o.optJSONObject("editions");
        if (eds != null) {
            Iterator<String> it = eds.keys();
            while (it.hasNext()) {
                String d = it.next();
                JSONObject e = eds.optJSONObject(d);
                if (e == null || !d.matches("\\d{4}-\\d\\d-\\d\\d")) continue;
                synchronized (Engine.EDLOCK) {
                    JSONObject cur = Store.edition(c, d);
                    boolean keepCur = cur != null && ((cur.has("ai") && !e.has("ai")) || (cur.optLong("deliveredAt") > 0 && e.optLong("deliveredAt") == 0));
                    if (keepCur) continue;
                    e.put("date", d);
                    Store.saveEdition(c, e);
                    edN++;
                }
            }
        }
        SyncJobService.schedule(c, true);
        DeliverReceiver.arm(c);
        if (pwN > 0 && prefsN == 0 && filesN == 0 && edN == 0) return "Explanations added for " + pwN + " PYQs";
        return "Restored " + edN + " editions" + (pwN > 0 ? " and " + pwN + " explanations" : ", " + filesN + " lists and settings");
    }

    static JSONArray mergeBy(JSONArray jSONArray, JSONArray jSONArray2, String str) throws Exception {
        HashSet hashSet = new HashSet();
        JSONArray jSONArray3 = new JSONArray();
        for (JSONArray jSONArray4 : new JSONArray[]{jSONArray, jSONArray2}) {
            for (int i = 0; i < jSONArray4.length(); i++) {
                JSONObject optJSONObject = jSONArray4.optJSONObject(i);
                if (optJSONObject != null) {
                    String jSONObject = str == null ? optJSONObject.toString() : optJSONObject.optString(str);
                    if (jSONObject.length() == 0 || hashSet.add(jSONObject)) {
                        jSONArray3.put(optJSONObject);
                    }
                }
            }
        }
        return jSONArray3;
    }

    static JSONArray mergeFacts(JSONArray jSONArray, JSONArray jSONArray2) throws Exception {
        HashSet hashSet = new HashSet();
        JSONArray jSONArray3 = new JSONArray();
        for (JSONArray jSONArray4 : new JSONArray[]{jSONArray, jSONArray2}) {
            for (int i = 0; i < jSONArray4.length(); i++) {
                JSONObject optJSONObject = jSONArray4.optJSONObject(i);
                if (optJSONObject != null && hashSet.add(Engine.normFact(optJSONObject.optString("t")))) {
                    jSONArray3.put(optJSONObject);
                }
            }
        }
        return jSONArray3;
    }
}
