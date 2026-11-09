package app.newsbreak;

import android.content.Context;
import android.content.SharedPreferences;
import app.newsbreak.core.Ai;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class Store {
    static final Object LOCK = new Object();

    private Store() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("newsbreak", 0);
    }

    static File dir(Context context, String str) {
        File filesDir = str == null ? context.getFilesDir() : new File(context.getFilesDir(), str);
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }
        return filesDir;
    }

    public static String read(Context context, String str) {
        String str2 = null;
        synchronized (LOCK) {
            File file = new File(context.getFilesDir(), str);
            if (file.exists()) {
                try {
                    str2 = Ai.read(new FileInputStream(file));
                } catch (Exception e) {
                }
            }
        }
        return str2;
    }

    public static void write(Context context, String str, String str2) {
        synchronized (LOCK) {
            try {
                File file = new File(context.getFilesDir(), str);
                File parentFile = file.getParentFile();
                if (parentFile != null && !parentFile.exists()) {
                    parentFile.mkdirs();
                }
                File file2 = new File(file.getPath() + ".tmp");
                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                fileOutputStream.write(str2.getBytes("UTF-8"));
                fileOutputStream.getFD().sync();
                fileOutputStream.close();
                if (!file2.renameTo(file)) {
                    file.delete();
                    file2.renameTo(file);
                }
            } catch (Exception e) {
            }
        }
    }

    public static String asset(Context context, String str) {
        try {
            return Ai.read(context.getAssets().open(str));
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject readObj(Context context, String str) {
        String read = read(context, str);
        if (read == null) {
            return null;
        }
        try {
            return new JSONObject(read);
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONArray readArr(Context context, String str) {
        String read = read(context, str);
        if (read == null) {
            return new JSONArray();
        }
        try {
            return new JSONArray(read);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static String editionPath(String str) {
        return "editions/" + str + ".json";
    }

    public static JSONObject edition(Context context, String str) {
        return readObj(context, editionPath(str));
    }

    public static void saveEdition(Context context, JSONObject jSONObject) {
        synchronized (LOCK) {
            write(context, editionPath(jSONObject.optString("date")), jSONObject.toString());
            try {
                JSONObject readObj = readObj(context, "editions/index.json");
                if (readObj == null) {
                    readObj = new JSONObject();
                }
                readObj.put(jSONObject.optString("date"), summary(jSONObject));
                write(context, "editions/index.json", readObj.toString());
            } catch (Exception e) {
            }
        }
    }

    static JSONObject summary(JSONObject jSONObject) throws Exception {
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("date", jSONObject.optString("date"));
        jSONObject2.put("heading", jSONObject.optString("heading"));
        JSONArray optJSONArray = jSONObject.optJSONArray("topics");
        jSONObject2.put("topics", optJSONArray == null ? 0 : optJSONArray.length());
        jSONObject2.put("ai", jSONObject.has("ai"));
        jSONObject2.put("status", jSONObject.optString("status"));
        if (optJSONArray != null && optJSONArray.length() > 0) {
            jSONObject2.put("lead", optJSONArray.optJSONObject(0).optString("head"));
        }
        return jSONObject2;
    }

    public static JSONArray editionIndex(Context context, int i) {
        JSONArray jSONArray = new JSONArray();
        synchronized (LOCK) {
            JSONObject readObj = readObj(context, "editions/index.json");
            List<String> editionDates = editionDates(context);
            boolean z = false;
            if (readObj == null) { readObj = new JSONObject(); z = true; }
            for (int k = 0; k < editionDates.size() && jSONArray.length() < i; k++) {
                String str = editionDates.get(k);
                JSONObject sum = readObj.optJSONObject(str);
                try {
                    if (sum == null) {
                        JSONObject edition = edition(context, str);
                        if (edition == null) continue;
                        sum = summary(edition);
                        readObj.put(str, sum);
                        z = true;
                    }
                    jSONArray.put(sum);
                } catch (Exception e) {
                }
            }
            if (z) write(context, "editions/index.json", readObj.toString());
        }
        return jSONArray;
    }

    public static List<String> editionDates(Context context) {
        String[] list = dir(context, "editions").list();
        ArrayList arrayList = new ArrayList();
        if (list != null) {
            Arrays.sort(list);
            for (int length = list.length - 1; length >= 0; length--) {
                if (list[length].endsWith(".json") && list[length].matches("\\d{4}-\\d\\d-\\d\\d\\.json")) {
                    arrayList.add(list[length].substring(0, 10));
                }
            }
        }
        return arrayList;
    }
}
