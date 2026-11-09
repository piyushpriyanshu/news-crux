package app.newsbreak.core;

import app.newsbreak.core.Html;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class Extract {
    private static final Pattern DEF = Pattern.compile("^(?:The |An? )?([A-Z][^,;:]{2,70}?)\\s*(?:\\([^)]{1,40}\\)\\s*)?,?\\s+(?:is|are|was|were)\\s+(?:an?|the|one of|India's|India’s|located|situated|built|founded|established|set up|known|a\\s)\\b.{15,}");
    private static final Pattern DEF2 = Pattern.compile("(?i)\\b(refers to|is located (in|at|on)|is situated|was built (by|in|during)|was constructed|was established (in|by|under)|was set up (in|by|under)|is headquartered|was founded|is named after|was inscribed|is a (tributary|species|disease|condition|technique|phenomenon|process|statutory|constitutional|non-statutory|treaty|grouping|bloc|programme|scheme|mission))\\b");
    private static final Pattern QUOTEY = Pattern.compile("[“”\"]|\\b(I|we|our|my)\\b");
    private static final Pattern PRE_L = Pattern.compile("(?i)(prelims|facts? for prelims|quick facts|\\bffp\\b|places in news|species in news|schemes? in news)");
    private static final Pattern MAINS_L = Pattern.compile("(?i)(editorial|mains practice|\\bmains\\b|gs paper\\s*-?\\s*4|ethics|essay)");
    private static final Pattern SOURCE = Pattern.compile("(?i)^\\s*(source|sources|source\\s*:)\\s*[:\\-–]?\\s*(.{0,80})$");
    private static final Pattern NEWS_H = Pattern.compile("(?i)(why in (the )?news|^in news|\\bin the news|^context|^news\\b|^summary|recent development|(key )?(outcomes?|highlights|takeaways|announcements|decisions)\\b|what (has|have) (happened|been)|^highlights|^background of the news|^about the news)");
    private static final Pattern BG_H = Pattern.compile("(?i)^(what (is|are|was|were)\\b|what it is|about\\b|know about|key (facts|features|provisions|points|components|aspects|characteristics|details)|features|objectives|components|provisions|location|geograph|histor|origin|composition|structure|mandate|functions|powers|who (is|are|was|were)|where is|how (does|do|is)|mechanism|types|classification|significance of (the )?[A-Z]|architectur|facts about|important facts|salient)");
    private static final Pattern SKIP_H = Pattern.compile("(?i)(faq|frequently asked|previous year|\\bpyqs?\\b|mains\\b|practice question|answer writing|way forward|conclusion|challenges|concerns|issues|criticism|steps taken|measures|suggestions|quiz|test series|reference|read more|also read|share|tags|related|image|^subject\\b|^gs paper|^prelims$|^ans\\b|^q\\.)");

    public static final class CoachArticle {
        public String title = "";
        public String url = "";
        public String src = "";
        public final List<Section> sections = new ArrayList();
        public String label = "";
    }

    public static final class Section {
        public String h = "";
        public final List<String> pts = new ArrayList();
        public String kind = "misc";
    }

    private Extract() {
    }

    public static List<String> keySentences(Html.Article article, int i) {
        boolean z;
        ArrayList<String> arrayList = new ArrayList();
        final LinkedHashMap linkedHashMap = new LinkedHashMap();
        int i2 = 0;
        Iterator<String> it = article.paras.iterator();
        while (true) {
            int i3 = i2;
            if (!it.hasNext()) {
                break;
            }
            String next = it.next();
            if (i3 >= 14) {
                break;
            }
            int i4 = 0;
            Iterator<String> it2 = Text.sentences(next).iterator();
            while (it2.hasNext()) {
                String trim = it2.next().trim();
                if (trim.length() < 45 || trim.length() > 330 || trim.endsWith("?") || Html.boiler(trim)) {
                    i4++;
                } else {
                    double d = 1.0d;
                    if (i3 == 0) {
                        d = 1.0d + 2.2d;
                    } else if (i3 == 1) {
                        d = 1.0d + 1.2d;
                    } else if (i3 < 4) {
                        d = 1.0d + 0.5d;
                    }
                    if (i4 == 0) {
                        d += 0.4d;
                    }
                    if (Text.hasFigure(trim)) {
                        d += 1.6d;
                    }
                    if (QUOTEY.matcher(trim).find()) {
                        d -= 1.2d;
                    }
                    if (trim.matches("(?i).*\\b(said|told|added|claimed|alleged)\\b.*")) {
                        d -= 0.5d;
                    }
                    if (trim.matches("(?i).*\\b(approved|launched|signed|announced|notified|passed|ruled|inaugurated|released|unveiled|declared|inscribed|awarded|ranked|tested|commissioned)\\b.*")) {
                        d += 0.8d;
                    }
                    double d2 = d - (0.06d * i3);
                    if (!linkedHashMap.containsKey(trim)) {
                        arrayList.add(trim);
                        linkedHashMap.put(trim, Double.valueOf(d2));
                    }
                    i4++;
                }
            }
            i2 = i3 + 1;
        }
        ArrayList<String> arrayList2 = new ArrayList(arrayList);
        Collections.sort(arrayList2, new Comparator<String>() { // from class: app.newsbreak.core.Extract.1
            @Override // java.util.Comparator
            public int compare(String str, String str2) {
                return Double.compare(((Double) linkedHashMap.get(str2)).doubleValue(), ((Double) linkedHashMap.get(str)).doubleValue());
            }
        });
        ArrayList arrayList3 = new ArrayList();
        for (String str : arrayList2) {
            if (arrayList3.size() >= i) {
                break;
            }
            Iterator it3 = arrayList3.iterator();
            while (true) {
                if (!it3.hasNext()) {
                    z = false;
                    break;
                }
                if (Text.jaccard(Text.tokens((String) it3.next()), Text.tokens(str)) >= 0.5d) {
                    z = true;
                    break;
                }
            }
            if (!z) {
                arrayList3.add(str);
            }
        }
        ArrayList arrayList4 = new ArrayList();
        for (String str2 : arrayList) {
            if (arrayList3.contains(str2)) {
                arrayList4.add(str2);
            }
        }
        return arrayList4;
    }

    public static List<String> definitions(Html.Article article, int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 0;
        for (String str : article.paras) {
            int i3 = i2 + 1;
            if (i2 >= 20) {
                break;
            }
            Iterator<String> it = Text.sentences(str).iterator();
            while (it.hasNext()) {
                String trim = it.next().trim();
                if (trim.length() >= 40 && trim.length() <= 300 && !QUOTEY.matcher(trim).find() && !Html.boiler(trim) && (DEF.matcher(trim).matches() || DEF2.matcher(trim).find())) {
                    Iterator it2 = arrayList.iterator();
                    boolean z = false;
                    while (it2.hasNext()) {
                        z = Text.jaccard(Text.tokens((String) it2.next()), Text.tokens(trim)) >= 0.5d ? true : z;
                    }
                    if (!z) {
                        arrayList.add(trim);
                    }
                    if (arrayList.size() >= i) {
                        return arrayList;
                    }
                }
            }
            i2 = i3;
        }
        return arrayList;
    }

    public static String defSubject(String str) {
        Matcher matcher = DEF.matcher(str.trim());
        if (matcher.matches()) {
            String replaceAll = matcher.group(1).trim().replaceAll("^(The|An?)\\s+", "");
            if (replaceAll.split("\\s+").length <= 8 && !replaceAll.matches("(?i)^(it|this|he|she|they|these|there|that)\\b.*")) {
                return replaceAll;
            }
        }
        return null;
    }

    static String label(List<Html.Block> list) {
        boolean z;
        Iterator<Html.Block> it = list.iterator();
        boolean z2 = false;
        boolean z3 = false;
        while (it.hasNext()) {
            String trim = it.next().text.trim();
            if (trim.length() <= 80) {
                if (PRE_L.matcher(trim).find()) {
                    z = z2;
                    z3 = true;
                } else {
                    z = MAINS_L.matcher(trim).find() ? true : z2;
                }
                z2 = z;
            }
        }
        return z3 ? "prelims" : z2 ? "mains" : "";
    }

    static String kind(String str) {
        return SKIP_H.matcher(str).find() ? "skip" : NEWS_H.matcher(str).find() ? "news" : BG_H.matcher(str.replaceAll("^[^A-Za-z]+", "")).find() ? "bg" : "misc";
    }

    static boolean skipTitle(String str) {
        if ("skip".equals(kind(str))) {
            return str.trim().split("\\s+").length <= 6 || str.matches("(?i)^\\W*(faq|frequently asked|previous year|pyq|practice question|mains|quiz|test series|tags|related|share|read more|also read).*");
        }
        return false;
    }

    static boolean isHeader(Html.Block block) {
        if ("h".equals(block.tag)) {
            return true;
        }
        String trim = block.text.trim();
        return "p".equals(block.tag) && block.bold && trim.length() <= 100 && (trim.endsWith(":") || trim.endsWith("?") || trim.length() <= 60);
    }

    static String[] labelSplit(String str) {
        int indexOf = str.indexOf(58);
        if (indexOf > 2 && indexOf <= 42) {
            String trim = str.substring(0, indexOf).trim();
            if (trim.split("\\s+").length <= 6 && Character.isUpperCase(trim.charAt(0))) {
                return new String[]{trim, str.substring(indexOf + 1).trim()};
            }
        }
        return null;
    }

    static String code(String str) {
        String trim = str.replaceAll("(?i)^source\\s*[:\\-–]?\\s*", "").trim();
        String lowerCase = trim.toLowerCase(Locale.ROOT);
        for (String[] strArr : new String[][]{new String[]{"the hindu", "TH"}, new String[]{"hindu", "TH"}, new String[]{"indian express", "IE"}, new String[]{"hindustan times", "HT"}, new String[]{"times of india", "TOI"}, new String[]{"economic times", "ET"}, new String[]{"business standard", "BS"}, new String[]{"press information bureau", "PIB"}, new String[]{"pib", "PIB"}, new String[]{"down to earth", "DTE"}, new String[]{"mint", "Mint"}, new String[]{"livemint", "Mint"}, new String[]{"all india radio", "AIR"}, new String[]{"newsonair", "AIR"}, new String[]{"reuters", "Reuters"}, new String[]{"pti", "PTI"}, new String[]{"ani", "ANI"}, new String[]{"rbi", "RBI"}, new String[]{"isro", "ISRO"}, new String[]{"mea", "MEA"}, new String[]{"prs", "PRS"}, new String[]{"moneycontrol", "MC"}}) {
            if (lowerCase.startsWith(strArr[0]) || lowerCase.equals(strArr[0])) {
                return strArr[1];
            }
        }
        String str2 = trim.split("[\\s,;|/]+")[0];
        return !str2.matches("[A-Z]{2,5}") ? "" : str2;
    }

    static String cleanPoint(String str) {
        return str.trim().replaceAll("\\s+", " ").replaceAll("^[•·▪◦\\-–*]+\\s*", "").replaceAll("(?i)^about\\s*:\\s*", "");
    }

    public static String subject(String str, String str2) {
        String trim = str.trim().replaceAll("[:?.]+$", "").trim();
        for (int i = 0; i < 2; i++) {
            trim = trim.replaceAll("(?i)^(what (is|are|was|were) (the |an |a )?|about (the )?|know about (the )?|key (facts|features|details|points|provisions|aspects|characteristics) (about|of) (the )?|important facts about (the )?|facts about (the )?|who (is|are|was|were) (the )?|where is (the )?|salient features of (the )?|features of (the )?)", "").trim();
        }
        return (trim.length() < 3 || trim.matches("(?i)(it|this|they|these|key facts|key features|features|about|what it is|overview|details|objectives|components)")) ? str2 : trim;
    }

    static void sections(CoachArticle a, List<Html.Block> list) {
        int start = 0;
        for (int k = 0; k < list.size() && k < 14; k++) {
            String t = list.get(k).text.trim();
            if (!SOURCE.matcher(t).find() || t.length() >= 90) continue;
            boolean allShort = true;
            for (int q = 0; q < k; q++) if (list.get(q).text.trim().length() >= 60) allShort = false;
            if (allShort) { a.src = code(t); start = k + 1; }
            break;
        }
        int skipLevel = 0;
        boolean skipping = false;
        Section cur = null;
        for (int k = start; k < list.size(); k++) {
            Html.Block b = list.get(k);
            String t = b.text.trim();
            if (t.length() < 2) continue;
            if (SOURCE.matcher(t).find() && t.length() < 90) {
                if (a.src.length() == 0) a.src = code(t);
                continue;
            }
            if (t.matches("(?i)^(tags?|subject|gs paper)\\b.*") && t.length() < 160) continue;
            if (isHeader(b)) {
                int lvl = "h".equals(b.tag) ? b.level : 9;
                if (!skipping || lvl <= skipLevel) {
                    cur = new Section();
                    cur.h = t.replaceAll("\\s*[:]+$", ":").trim();
                    cur.kind = kind(cur.h);
                    skipping = "skip".equals(cur.kind);
                    if (skipping) skipLevel = lvl;
                    a.sections.add(cur);
                }
                continue;
            }
            if (skipping) continue;
            if (t.matches("(?i)^(e\\.?g\\.?,?|for example|ans\\s*:|\\(?[a-d]\\)\\s).*") || t.length() < 12 || Html.boiler(t)) continue;
            if (t.matches("(?i).*(answer writing|practice question|secure (synopsis|answer)|mains question).*")) continue;
            String[] ls = ("p".equals(b.tag) || ("li".equals(b.tag) && b.level <= 1)) ? labelSplit(t) : null;
            if (ls != null && "p".equals(b.tag) && (cur == null || !"li".equals(b.tag))) {
                String kd = kind(ls[0]);
                if ("news".equals(kd) || "bg".equals(kd) || "skip".equals(kd)) {
                    cur = new Section();
                    cur.h = ls[0];
                    cur.kind = kd;
                    skipping = "skip".equals(kd);
                    if (skipping) skipLevel = 9;
                    a.sections.add(cur);
                    if (!skipping && ls[1].length() > 12) cur.pts.add(cleanPoint(ls[1]));
                    continue;
                }
            }
            if (cur == null) {
                cur = new Section();
                cur.h = "";
                cur.kind = "news";
                a.sections.add(cur);
            }
            if ("li".equals(b.tag) && b.level >= 2 && cur.pts.size() >= 4) continue;
            if (cur.pts.size() < 12) cur.pts.add(cleanPoint(t));
        }
        for (int k = a.sections.size() - 1; k >= 0; k--) if (a.sections.get(k).pts.isEmpty()) a.sections.remove(k);
    }

    public static List<CoachArticle> daily(String str, String str2) {
        int i;
        int i2;
        List<Html.Block> list = Html.parse(str).blocks;
        ArrayList arrayList = new ArrayList();
        for (int i4 = 0; i4 < list.size(); i4++) {
            Html.Block block = list.get(i4);
            if ("h".equals(block.tag) && block.level <= 3 && block.text.length() >= 6 && block.text.length() <= 160 && !skipTitle(block.text) && !NEWS_H.matcher(block.text).find()) {
                for (int i6 = i4 + 1; i6 < list.size() && i6 <= i4 + 14; i6++) {
                    String trim = list.get(i6).text.trim();
                    if ("h".equals(list.get(i6).tag) && list.get(i6).level <= block.level) break;
                    if (SOURCE.matcher(trim).find() && trim.length() < 90) {
                        arrayList.add(Integer.valueOf(i4));
                        break;
                    }
                }
            }
        }
        if (arrayList.size() < 2) {
            int i7 = 1;
            while (i7 <= 3 && arrayList.size() < 2) {
                ArrayList arrayList2 = new ArrayList();
                int i8 = 0;
                while (true) {
                    int i9 = i8;
                    if (i9 >= list.size()) {
                        break;
                    }
                    if ("h".equals(list.get(i9).tag) && list.get(i9).level == i7 && !skipTitle(list.get(i9).text) && !NEWS_H.matcher(list.get(i9).text).find()) {
                        arrayList2.add(Integer.valueOf(i9));
                    }
                    i8 = i9 + 1;
                }
                int i10 = 0;
                int i11 = 0;
                while (true) {
                    int i12 = i11;
                    if (i12 >= arrayList2.size()) {
                        break;
                    }
                    if ((i12 + 1 < arrayList2.size() ? ((Integer) arrayList2.get(i12 + 1)).intValue() : list.size()) - ((Integer) arrayList2.get(i12)).intValue() >= 4) {
                        i10++;
                    }
                    i11 = i12 + 1;
                }
                i7++;
                arrayList = (arrayList2.size() < 2 || i10 < 2) ? arrayList : arrayList2;
            }
        }
        HashSet hashSet = new HashSet();
        if (!arrayList.isEmpty()) {
            HashMap hashMap = new HashMap();
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                int intValue = ((Integer) it.next()).intValue();
                Integer num = (Integer) hashMap.get(Integer.valueOf(list.get(intValue).level));
                hashMap.put(Integer.valueOf(list.get(intValue).level), Integer.valueOf(num == null ? 1 : num.intValue() + 1));
            }
            int i13 = list.get(((Integer) arrayList.get(0)).intValue()).level;
            int i14 = 0;
            Iterator it2 = hashMap.entrySet().iterator();
            while (true) {
                int i15 = i14;
                i = i13;
                if (!it2.hasNext()) {
                    break;
                }
                Map.Entry entry = (Map.Entry) it2.next();
                if (((Integer) entry.getValue()).intValue() > i15) {
                    int intValue2 = ((Integer) entry.getValue()).intValue();
                    i13 = ((Integer) entry.getKey()).intValue();
                    i14 = intValue2;
                } else {
                    i14 = i15;
                    i13 = i;
                }
            }
            HashSet hashSet2 = new HashSet(arrayList);
            int intValue3 = ((Integer) arrayList.get(0)).intValue();
            int intValue4 = ((Integer) arrayList.get(arrayList.size() - 1)).intValue();
            ArrayList arrayList3 = new ArrayList();
            while (true) {
                intValue3++;
                if (intValue3 >= list.size()) {
                    break;
                }
                Html.Block block2 = list.get(intValue3);
                if ("h".equals(block2.tag) && block2.level <= i && !hashSet2.contains(Integer.valueOf(intValue3))) {
                    String kind = skipTitle(block2.text) ? "skip" : kind(block2.text);
                    if ("skip".equals(kind) && !skipTitle(block2.text)) {
                        kind = "misc";
                    }
                    if ("skip".equals(kind) || "misc".equals(kind) || block2.level != i) {
                        if (block2.level < i || "skip".equals(kind)) {
                            hashSet.add(Integer.valueOf(intValue3));
                        } else if (block2.text.length() >= 6 && block2.text.length() <= 160) {
                            int size = list.size();
                            int i16 = 0;
                            int i17 = intValue3 + 1;
                            while (true) {
                                i2 = i17;
                                if (i2 >= list.size()) {
                                    i2 = size;
                                    break;
                                }
                                if ("h".equals(list.get(i2).tag) && list.get(i2).level <= i) {
                                    break;
                                }
                                i17 = i2 + 1;
                            }
                            int i18 = intValue3 + 1;
                            boolean z = false;
                            while (i18 < i2 && i18 <= intValue3 + 40) {
                                String trim2 = list.get(i18).text.trim();
                                if (trim2.length() >= 60) {
                                    i16++;
                                }
                                boolean z2 = (i18 > intValue3 + 6 || !trim2.matches("(?i)^subject\\s*:.*")) ? z : true;
                                String[] labelSplit = labelSplit(trim2);
                                if (labelSplit != null && i18 <= intValue3 + 6 && ("news".equals(kind(labelSplit[0])) || "bg".equals(kind(labelSplit[0])))) {
                                    z2 = true;
                                }
                                i18++;
                                z = z2;
                            }
                            if (z && (intValue3 < intValue4 ? i16 >= 1 : i16 >= 2)) {
                                hashSet.add(Integer.valueOf(intValue3));
                                arrayList3.add(Integer.valueOf(intValue3));
                            }
                        }
                    }
                }
            }
            if (!arrayList3.isEmpty()) {
                arrayList.addAll(arrayList3);
                Collections.sort(arrayList);
            }
        }
        ArrayList arrayList4 = new ArrayList();
        int i19 = 0;
        while (true) {
            int i20 = i19;
            if (i20 < arrayList.size()) {
                int intValue5 = ((Integer) arrayList.get(i20)).intValue();
                int intValue6 = i20 + 1 < arrayList.size() ? ((Integer) arrayList.get(i20 + 1)).intValue() : list.size();
                int i21 = intValue5 + 1;
                while (true) {
                    if (i21 >= intValue6) {
                        i21 = intValue6;
                        break;
                    }
                    if (hashSet.contains(Integer.valueOf(i21))) {
                        break;
                    }
                    i21++;
                }
                CoachArticle coachArticle = new CoachArticle();
                Html.Block block3 = list.get(intValue5);
                coachArticle.title = Text.clean(block3.text);
                coachArticle.url = absolute(str2, block3.href);
                sections(coachArticle, list.subList(intValue5 + 1, i21));
                ArrayList arrayList5 = new ArrayList(list.subList(Math.max(0, intValue5 - 4), intValue5));
                int i22 = intValue5 + 1;
                while (true) {
                    int i23 = i22;
                    if (i23 >= Math.min(i21, intValue5 + 14) || "h".equals(list.get(i23).tag)) {
                        break;
                    }
                    arrayList5.add(list.get(i23));
                    i22 = i23 + 1;
                }
                coachArticle.label = label(arrayList5);
                if (!coachArticle.sections.isEmpty() && coachArticle.title.length() >= 6) {
                    arrayList4.add(coachArticle);
                }
                i19 = i20 + 1;
            } else {
                return arrayList4;
            }
        }
    }

    public static CoachArticle single(String str, String str2) {
        Html.Doc parse = Html.parse(str);
        CoachArticle coachArticle = new CoachArticle();
        coachArticle.url = str2;
        coachArticle.title = Text.clean(Html.first(parse.ogTitle, parse.ldHeadline, parse.title).replaceAll("\\s*[|\\-–—]\\s*(Vision IAS|VisionIAS|Vajiram.*|Drishti IAS|Insights.*)$", ""));
        List<Html.Block> mainBlocks = Html.mainBlocks(parse);
        ArrayList arrayList = new ArrayList();
        for (Html.Block block : mainBlocks) {
            if (!"h".equals(block.tag) || Text.jaccard(Text.tokens(block.text), Text.tokens(coachArticle.title)) < 0.7d) {
                arrayList.add(block);
            }
        }
        sections(coachArticle, arrayList);
        return coachArticle;
    }

    static String absolute(String str, String str2) {
        if (str2 == null || str2.length() == 0 || str2.startsWith("#") || str2.startsWith("javascript")) {
            return str;
        }
        try {
            return new URL(new URL(str), str2).toString();
        } catch (Exception e) {
            return str;
        }
    }

    public static JSONObject coachJson(CoachArticle coachArticle) throws Exception {
        JSONObject jSONObject = new JSONObject();
        if (coachArticle.src.length() > 0) {
            jSONObject.put("src", coachArticle.src);
        }
        JSONArray jSONArray = new JSONArray();
        JSONArray jSONArray2 = new JSONArray();
        int i = 0;
        for (Section section : coachArticle.sections) {
            if ("news".equals(section.kind)) {
                for (String str : section.pts) {
                    if (jSONArray.length() < 6) {
                        jSONArray.put(Text.cap(str, 420));
                    }
                }
            }
            if (i <= 5000) {
                JSONArray jSONArray3 = new JSONArray();
                for (String str2 : section.pts) {
                    if (jSONArray3.length() >= 8) {
                        break;
                    }
                    String cap = Text.cap(str2, 420);
                    jSONArray3.put(cap);
                    i += cap.length();
                }
                jSONArray2.put(new JSONObject().put("h", Text.cap(section.h, 120)).put("k", section.kind).put("p", jSONArray3));
            }
        }
        jSONObject.put("news", jSONArray);
        if (coachArticle.label.length() > 0) {
            jSONObject.put("label", coachArticle.label);
        }
        jSONObject.put("sec", jSONArray2);
        return jSONObject;
    }

    public static JSONObject background(JSONObject jSONObject, String str, int i) throws Exception {
        JSONObject jSONObject2 = null;
        JSONArray optJSONArray = jSONObject.optJSONArray("sec");
        if (optJSONArray != null) {
            int i2 = -1;
            JSONObject jSONObject3 = null;
            for (int i3 = 0; i3 < optJSONArray.length(); i3++) {
                JSONObject jSONObject4 = optJSONArray.getJSONObject(i3);
                if ("bg".equals(jSONObject4.optString("k"))) {
                    int min = ((jSONObject4.optString("h").matches("(?i)^(what (is|are)|about|know about).*") ? 10 : 5) + Math.min(jSONObject4.getJSONArray("p").length(), 5)) - i3;
                    if (min > i2) {
                        i2 = min;
                        jSONObject3 = jSONObject4;
                    }
                }
            }
            if (jSONObject3 != null) {
                jSONObject2 = new JSONObject();
                jSONObject2.put("h", subject(jSONObject3.optString("h"), str));
                JSONArray jSONArray = new JSONArray();
                JSONArray jSONArray2 = jSONObject3.getJSONArray("p");
                for (int i4 = 0; i4 < jSONArray2.length() && jSONArray.length() < i; i4++) {
                    jSONArray.put(Text.note(jSONArray2.optString(i4), 230));
                }
                jSONObject2.put("pts", jSONArray);
            }
        }
        return jSONObject2;
    }
}
