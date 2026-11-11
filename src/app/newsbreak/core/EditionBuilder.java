package app.newsbreak.core;

import app.newsbreak.core.Pyq;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class EditionBuilder {
    static final double COACH_BOOST = 2.5d;
    private static final String[] JUNK;
    static volatile Kb KBREF = null;
    public static final int MAX_ALL = 15;
    public static final int MAX_AMEND = 4;
    public static final int MAX_CATCHUP = 80;
    public static final int MAX_GAP = 3;
    public static final int MAX_TOPICS = 12;
    private static final Pattern NUM;
    static final Set<String> PLAIN_NAME;
    static final Set<String> WEAK_SIM;
    private final Kb kb;
    private final Pyq pyq;
    public static final List<String> PRIMARY = Arrays.asList("PIB", "PMI", "RBI", "MEA", "PRS", "ISRO", "AIR", "TH", "IE", "HT", "TOI", "ET", "BS", "MC", "DTE", "Reuters", "ANI", "PTI");
    public static final List<String> COACHING = Arrays.asList("Insights", "Drishti", "Vision", "Vajiram");
    private static final String[] PROMO = {"test series", "admission open", "admissions open", "webinar", "mock test", "quiz", "answer writing", "mains answer", "topper", "results declared", "free course", "new batch", "batch starts", "enrol now", "enroll now", "scholarship test", "download pdf", "magazine", "daily current affairs", "current affairs today", "prelims practice", "mcq", "practice question", "interview guidance", "brochure", "foundation course", "syllabus", "upsc strategy", "booklist", "book list", "how to prepare", "exam date", "cut off marks", "admit card", "mains 20", "prelims 20", "csat", "lecture", "podcast", "infographic", "weekly current", "monthly current", "compilation", "pib summary", "news summary", "editorial summary", "important articles", "daily news analysis", "daily updates", "newsletter", "discount", "% off", "join now", "telegram", "upsc cse 20", "ias 20", "free trial"};
    private static final Pattern MAINS_T = Pattern.compile("(?i)\\b(challenges?|need for|issues? (in|with|of|facing|around)|way forward|the case for|rethinking|debate|editorial|ethics|ethical|essay|role of|lessons? (from|for)|why india|how india|should india|crisis (of|in)|future of|state of|concerns? (over|about|around)|governance (of|in)|reimagining|reforming|towards an?|beyond the|the road to|last-mile|balancing|when the)\\b");
    private static final Pattern AWARD = Pattern.compile("(?i)\\b(award(s|ed)?|prize|honoured|honored|felicitat\\w*|conferred)\\b");
    private static final Pattern APPOINT = Pattern.compile("(?i)\\b(appointed|appointment|takes charge|assumes charge|sworn in|named (as )?(new )?(chief|head|chairman|ceo|director)|new (chief|chairman|ceo|director)|elevated as)\\b");
    private static final Pattern CONST_POST = Pattern.compile("(?i)(chief justice|\\bcji\\b|election commission|\\bcec\\b|comptroller|\\bcag\\b|governor\\b|president|attorney general|upsc|information commission|lokpal|finance commission|delimitation)");
    static Comparator<JSONObject> BY_SCORE = new Comparator<JSONObject>() { // from class: app.newsbreak.core.EditionBuilder.1
        @Override // java.util.Comparator
        public int compare(JSONObject jSONObject, JSONObject jSONObject2) {
            int compare = Double.compare(jSONObject2.optDouble("sc", 0.0d), jSONObject.optDouble("sc", 0.0d));
            return compare != 0 ? compare : Integer.compare(EditionBuilder.srcRank(jSONObject.optString("s")), EditionBuilder.srcRank(jSONObject2.optString("s")));
        }
    };
    static final Set<String> WEAK = new HashSet(Arrays.asList("supreme", "court", "high", "india", "indian", "government", "govt", "centre", "central", "union", "state", "national", "minister", "ministry", "new", "bill", "act", "report", "policy", "scheme", "plan", "launch", "launche", "launched", "approve", "approved", "cabinet", "parliament", "lok", "sabha", "rajya", "says", "said", "year", "2025", "2026", "2027", "first", "world", "global", "day", "week", "month", "rule", "law", "case", "panel", "committee", "issue", "development", "growth", "mission", "project", "programme", "data", "rbi", "pib", "upsc", "news", "analysis", "key", "major", "india's"));
    static final Map<String, String> PRIZE = new HashMap();
    public long deliverOffsetMs = 21600000;
    public Map<String, Double> boost = new HashMap();
    public long newsDay = 0;
    public List<JSONObject> history = new ArrayList();
    private List<JSONObject> past = new ArrayList();
    private Map<String, List<JSONObject>> pastIdx = new HashMap();
    private Map<JSONObject, Past> pastInfo = new IdentityHashMap();
    private Map<String, List<JSONObject>> lineIdx = new HashMap();

    static {
        PRIZE.put("physic", "P");
        PRIZE.put("chemistry", "C");
        PRIZE.put("medicine", "M");
        PRIZE.put("physiology", "M");
        PRIZE.put("literature", "L");
        PRIZE.put("peace", "X");
        PRIZE.put("economic", "E");
        PRIZE.put("economy", "E");
        JUNK = new String[]{"click here", "subscribe", "follow us", "also read", "read also", "watch:", "download the", "sign up", "newsletter", "for more", "©", "all rights reserved", "the post ", "appeared first on"};
        NUM = Pattern.compile("(₹\\s?)?(\\d[\\d,]*(?:\\.\\d+)?)\\s?(%|per ?cent|lakh crore|crore|lakh|billion|million|bn|mn|tonnes|km|MW|GW)", 2);
        WEAK_SIM = new HashSet(Arrays.asList("ut", "union", "territory", "territorie", "today", "body", "poll", "1", "2", "3", "4", "5", "inr", "cr", "pct", "start", "launch", "approve", "hold", "raise", "cut"));
        PLAIN_NAME = new HashSet(Arrays.asList("india", "indian", "national", "international", "world", "global", "day", "week", "year", "joint", "military", "exercise", "naval", "army", "navy", "air", "force", "summit", "conference", "meeting", "pradhan", "mantri", "yojana", "abhiyan", "mission", "scheme", "programme", "program", "policy", "report", "index", "survey", "award", "prize", "ministry", "minister", "union", "government", "centre", "central", "state", "court", "supreme", "high", "bill", "act", "amendment", "new", "first", "news", "explained", "editorial", "place", "species", "speci", "exercis", "fact", "prelims", "mains", "upsc", "birth", "death", "anniversary", "festival", "cabinet", "launch", "launches", "celebrated", "observed", "edition", "phase", "session", "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december", "2024", "2025", "2026", "2027", "the", "and", "for", "with", "its", "how", "why", "what"));
    }

    public EditionBuilder(Kb kb, Pyq pyq) {
        this.kb = kb;
        this.pyq = pyq;
        KBREF = kb;
    }

    static Set<String> nameToks(JSONObject jSONObject, JSONObject jSONObject2) {
        HashSet hashSet = new HashSet();
        Kb kb = KBREF;
        if (kb != null) {
            for (JSONObject jSONObject3 : new JSONObject[]{jSONObject, jSONObject2}) {
                JSONArray optJSONArray = jSONObject3.optJSONArray("ents");
                for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
                    JSONObject entity = kb.entity(optJSONArray.optString(i));
                    if (entity != null) {
                        hashSet.addAll(Text.simTokens(entity.optString("name")));
                        JSONArray optJSONArray2 = entity.optJSONArray("match");
                        for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
                            hashSet.addAll(Text.simTokens(optJSONArray2.optString(i2).replace("=", "").replace("*", "")));
                        }
                    }
                }
            }
        }
        return hashSet;
    }

    public static boolean promo(String str) {
        if (ExamMeta.title(str)) return true;
        String lowerCase = str.toLowerCase();
        for (String str2 : PROMO) {
            if (lowerCase.contains(str2)) {
                return true;
            }
        }
        return false;
    }

    static boolean coachRelevant(JSONObject jSONObject) {
        return isCoaching(jSONObject) && jSONObject.optDouble("sc", 0.0d) >= 2.0d && !jSONObject.has("noise") && !promo(jSONObject.optString("t"));
    }

    static boolean coveredBy(JSONObject jSONObject, Set<JSONObject> set, List<JSONObject> list) {
        if (set.contains(jSONObject)) {
            return true;
        }
        Set<String> set2 = Text.tokens(jSONObject.optString("t"));
        JSONArray optJSONArray = jSONObject.optJSONArray("ents");
        for (JSONObject jSONObject2 : list) {
            HashSet hashSet = new HashSet();
            for (Set<String> set3 : allHeads(jSONObject2)) {
                if (Text.jaccard(set2, set3) >= 0.25d || Text.overlap(strong(set2), set3) >= 3) {
                    return true;
                }
                hashSet.addAll(set3);
            }
            JSONArray optJSONArray2 = jSONObject2.optJSONArray("ents");
            for (int i = 0; optJSONArray != null && optJSONArray2 != null && i < optJSONArray.length(); i++) {
                String optString = optJSONArray.optString(i);
                if (!Pyq.GENERIC_IDS.contains("e:" + optString)) {
                    for (int i2 = 0; i2 < optJSONArray2.length(); i2++) {
                        if (optString.equals(optJSONArray2.optString(i2)) && Text.overlap(strong(set2), hashSet) >= 1) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean prelimsTopic(JSONObject jSONObject, Kb kb) {
        JSONObject optJSONObject = jSONObject.optJSONObject("cx");
        String optString = optJSONObject == null ? "" : optJSONObject.optString("label");
        if ("prelims".equals(optString)) {
            return true;
        }
        String optString2 = jSONObject.optString("t");
        int i = "mains".equals(optString) ? -2 : 0;
        if (jSONObject.optString("gs").startsWith("GS-IV")) {
            i -= 5;
        }
        JSONArray optJSONArray = jSONObject.optJSONArray("ents");
        int i2 = 0;
        for (int i3 = 0; optJSONArray != null && i3 < optJSONArray.length(); i3++) {
            if (!Pyq.GENERIC_IDS.contains("e:" + optJSONArray.optString(i3))) {
                i2++;
            }
        }
        if (jSONObject.optJSONArray("maps") != null) {
            i2 += jSONObject.optJSONArray("maps").length();
        }
        if (jSONObject.optJSONArray("cons") != null) {
            i2 += jSONObject.optJSONArray("cons").length();
        }
        if (i2 > 0) {
            i += 2;
        }
        StringBuilder append = new StringBuilder(optString2).append(' ');
        if (optJSONObject != null) {
            try {
                if (Extract.background(optJSONObject, "", 1) != null) {
                    i++;
                }
            } catch (Exception e) {
            }
            JSONArray optJSONArray2 = optJSONObject.optJSONArray("news");
            for (int i4 = 0; optJSONArray2 != null && i4 < optJSONArray2.length() && i4 < 2; i4++) {
                append.append(optJSONArray2.optString(i4)).append(' ');
            }
        } else if (jSONObject.has("th")) {
            i++;
        }
        if (Text.hasFigure(append.toString())) {
            i++;
        }
        if (MAINS_T.matcher(optString2).find()) {
            i -= 3;
        }
        if (optString2.trim().endsWith("?")) {
            i--;
        }
        return i >= 1;
    }

    void rank(Cluster cluster) {
        HashSet hashSet = new HashSet();
        for (JSONObject jSONObject : cluster.items) {
            if (isCoaching(jSONObject) && !jSONObject.has("noise")) {
                hashSet.add(jSONObject.optString("s"));
            }
        }
        if (!hashSet.isEmpty()) {
            cluster.score = (Math.min(hashSet.size() - 1, 2) * 1.0d) + COACH_BOOST + cluster.score;
        }
        JSONObject theme = this.kb.theme(cluster.head().optString("th"));
        if (theme != null && "rising".equals(theme.optString("trend"))) {
            cluster.score += 0.6d;
        }
        String optString = cluster.head().optString("t");
        if (AWARD.matcher(optString).find() && !optString.toLowerCase().contains("nobel")) {
            cluster.score -= 1.5d;
        }
        if (!APPOINT.matcher(optString).find() || CONST_POST.matcher(optString).find()) {
            return;
        }
        cluster.score -= 1.5d;
    }

    static final class Reg {
        Set<String> ids;
        JSONObject it;
        Set<String> names;
        Set<String> ntk;
        Set<String> stk;
        String subj;
        Set<String> tk;

        Reg() {
        }
    }

    static Reg reg(JSONObject jSONObject) {
        JSONObject entity;
        Reg reg = new Reg();
        reg.it = jSONObject;
        reg.tk = Text.tokens(jSONObject.optString("t"));
        reg.stk = strong(reg.tk);
        reg.ids = new HashSet();
        JSONArray optJSONArray = jSONObject.optJSONArray("ents");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            if (!Pyq.GENERIC_IDS.contains("e:" + optJSONArray.optString(i))) {
                reg.ids.add("e:" + optJSONArray.optString(i));
            }
        }
        JSONArray optJSONArray2 = jSONObject.optJSONArray("maps");
        for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
            reg.ids.add("m:" + optJSONArray2.optString(i2));
        }
        JSONArray optJSONArray3 = jSONObject.optJSONArray("cons");
        for (int i3 = 0; optJSONArray3 != null && i3 < optJSONArray3.length(); i3++) {
            if (optJSONArray3.optString(i3).startsWith("w_")) {
                reg.ids.add(optJSONArray3.optString(i3));
            }
        }
        reg.names = new HashSet();
        Kb kb = KBREF;
        for (String str : reg.ids) {
            if (kb == null) {
                entity = null;
            } else {
                entity = str.startsWith("e:") ? kb.entity(str.substring(2)) : str.startsWith("m:") ? kb.map(str.substring(2)) : kb.concept(str);
            }
            if (entity != null) {
                reg.names.addAll(Text.tokens(entity.optString("name")));
                JSONArray optJSONArray4 = entity.optJSONArray("match");
                for (int i4 = 0; optJSONArray4 != null && i4 < optJSONArray4.length(); i4++) {
                    reg.names.addAll(Text.tokens(optJSONArray4.optString(i4).replace("=", "").replace("*", "")));
                }
            }
        }
        reg.subj = "";
        JSONObject optJSONObject = jSONObject.optJSONObject("cx");
        String optString = jSONObject.optString("d");
        if (optJSONObject != null) {
            try {
                JSONObject background = Extract.background(optJSONObject, "", 1);
                if (background != null && background.optString("h").length() >= 3) {
                    ArrayList arrayList = new ArrayList(strong(Text.tokens(background.optString("h"))));
                    Collections.sort(arrayList);
                    reg.subj = join(arrayList).trim();
                }
            } catch (Exception e) {
            }
            JSONArray optJSONArray5 = optJSONObject.optJSONArray("news");
            if (optJSONArray5 != null && optJSONArray5.length() > 0) {
                optString = optJSONArray5.optString(0);
            }
        }
        reg.ntk = Text.tokens(Text.cap(optString, 240));
        return reg;
    }

    static boolean sameTopic(Reg reg, Reg reg2) {
        boolean z;
        if (otherPrize(reg.tk, reg2.tk)) {
            return false;
        }
        if (Text.jaccard(reg.tk, reg2.tk) >= 0.3d || Text.overlap(reg.stk, reg2.stk) >= 3) {
            return true;
        }
        if (reg.subj.length() > 0 && reg.subj.equals(reg2.subj)) {
            return true;
        }
        Iterator<String> it = reg.ids.iterator();
        while (true) {
            if (!it.hasNext()) {
                z = false;
                break;
            }
            if (reg2.ids.contains(it.next())) {
                z = true;
                break;
            }
        }
        if (!z || Text.overlap(minus(reg.stk, reg.names), minus(reg2.stk, reg2.names)) < 2) {
            return reg.ntk.size() >= 5 && reg2.ntk.size() >= 5 && Text.jaccard(reg.ntk, reg2.ntk) >= 0.35d;
        }
        return true;
    }

    static List<List<JSONObject>> register(List<JSONObject> list) {
        int size = list.size();
        Reg[] regArr = new Reg[size];
        for (int i = 0; i < size; i++) {
            regArr[i] = reg(list.get(i));
        }
        int[] iArr = new int[size];
        for (int i2 = 0; i2 < size; i2++) {
            iArr[i2] = i2;
        }
        for (int i3 = 0; i3 < size; i3++) {
            for (int i4 = i3 + 1; i4 < size; i4++) {
                int find = find(iArr, i3);
                int find2 = find(iArr, i4);
                if (find != find2 && sameTopic(regArr[i3], regArr[i4])) {
                    iArr[find2] = find;
                }
            }
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (int i5 = 0; i5 < size; i5++) {
            int find3 = find(iArr, i5);
            List list2 = (List) linkedHashMap.get(Integer.valueOf(find3));
            if (list2 == null) {
                list2 = new ArrayList();
                linkedHashMap.put(Integer.valueOf(find3), list2);
            }
            list2.add(list.get(i5));
        }
        return new ArrayList(linkedHashMap.values());
    }

    static int find(int[] iArr, int i) {
        while (iArr[i] != i) {
            iArr[i] = iArr[iArr[i]];
            i = iArr[i];
        }
        return i;
    }

    static boolean readable(JSONObject jSONObject) {
        return jSONObject.has("cx") || jSONObject.optString("d").length() > 60 || (jSONObject.optJSONArray("x") != null && jSONObject.optJSONArray("x").length() > 0);
    }

    static boolean hasOutlet(Cluster cluster) {
        Iterator<JSONObject> it = cluster.items.iterator();
        while (it.hasNext()) {
            if (!isCoaching(it.next())) {
                return true;
            }
        }
        return false;
    }

    static int sites(JSONObject jSONObject) {
        HashSet hashSet = new HashSet();
        JSONArray optJSONArray = jSONObject.optJSONArray("cu");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            hashSet.add(optJSONArray.optJSONObject(i).optString("s"));
        }
        return hashSet.size();
    }

    static String lastCu(JSONObject jSONObject) {
        String str = "";
        JSONArray optJSONArray = jSONObject.optJSONArray("cu");
        int i = 0;
        while (optJSONArray != null && i < optJSONArray.length()) {
            String optString = optJSONArray.optJSONObject(i).optString("d");
            if (optString.compareTo(str) <= 0) {
                optString = str;
            }
            i++;
            str = optString;
        }
        return str;
    }

    static int tierRank(String str) {
        if ("HIGH".equals(str)) {
            return 0;
        }
        return ("MED".equals(str) || "MEDIUM".equals(str)) ? 1 : 2;
    }

    public static int gsRank(String str) {
        if (str.startsWith("GS-I/")) {
            return 0;
        }
        if (str.startsWith("GS-II/")) {
            return 1;
        }
        if (str.startsWith("GS-III/")) {
            return 2;
        }
        return str.startsWith("GS-IV") ? 3 : 4;
    }

    static double coachMatch(JSONObject jSONObject, Cluster cluster) {
        Set<String> set = Text.tokens(jSONObject.optString("t"));
        if (set.size() < 2 || otherPrize(set, cluster.tok)) {
            return 0.0d;
        }
        double jaccard = Text.jaccard(set, cluster.tok);
        int overlap = Text.overlap(strong(set), cluster.all);
        if (jaccard >= 0.3d || overlap >= 3) {
            return Math.max(jaccard, (overlap * 0.01d) + 0.3d);
        }
        if (overlap < 2 || !sharesEnt(jSONObject, cluster.ents)) {
            return 0.0d;
        }
        return 0.2d + (overlap * 0.01d);
    }

    String coveredEarlier(JSONObject jSONObject, String str) {
        Set<String> set = Text.tokens(jSONObject.optString("t"));
        Set<String> strong = strong(set);
        for (JSONObject jSONObject2 : this.history) {
            String optString = jSONObject2.optString("date");
            if (optString.compareTo(str) < 0) {
                for (Set<String> set2 : allHeads(jSONObject2)) {
                    if (!otherPrize(set, set2) && (Text.jaccard(set, set2) >= 0.3d || Text.overlap(strong, strong(set2)) >= 3)) {
                        return optString;
                    }
                }
                if (jSONObject2.optString("head").equalsIgnoreCase(jSONObject.optString("t"))) {
                    return optString;
                }
            }
        }
        return null;
    }

    static final class Cluster {
        boolean cont;
        double score;
        Set<String> tok;
        final List<JSONObject> items = new ArrayList();
        final Set<String> all = new HashSet();
        final Set<String> ents = new HashSet();

        Cluster() {
        }

        JSONObject head() {
            return this.items.get(0);
        }
    }

    static boolean isCoaching(JSONObject jSONObject) {
        return COACHING.contains(jSONObject.optString("s"));
    }

    static int srcRank(String str) {
        int indexOf = PRIMARY.indexOf(str);
        if (indexOf < 0) {
            int indexOf2 = COACHING.indexOf(str);
            if (indexOf2 >= 0) {
                return indexOf2 + 20;
            }
            return 99;
        }
        return indexOf;
    }

    static Set<String> strong(Set<String> set) {
        HashSet hashSet = new HashSet(set);
        hashSet.removeAll(WEAK);
        return hashSet;
    }

    static boolean otherPrize(Set<String> set, Set<String> set2) {
        if (set == null || set2 == null || !set.contains("nobel") || !set2.contains("nobel")) {
            return false;
        }
        HashSet hashSet = new HashSet();
        HashSet hashSet2 = new HashSet();
        for (String str : set) {
            if (PRIZE.containsKey(str)) {
                hashSet.add(PRIZE.get(str));
            }
        }
        for (String str2 : set2) {
            if (PRIZE.containsKey(str2)) {
                hashSet2.add(PRIZE.get(str2));
            }
        }
        if (hashSet.isEmpty() || hashSet2.isEmpty()) {
            return false;
        }
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            if (hashSet2.contains((String) it.next())) {
                return false;
            }
        }
        return true;
    }

    static boolean sharesEnt(JSONObject jSONObject, Set<String> set) {
        JSONArray optJSONArray = jSONObject.optJSONArray("ents");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            if (set.contains(optJSONArray.optString(i)) && !Pyq.GENERIC_IDS.contains("e:" + optJSONArray.optString(i))) {
                return true;
            }
        }
        return false;
    }

    public static List<Cluster> cluster(List<JSONObject> list) {
        boolean z;
        ArrayList<JSONObject> arrayList = new ArrayList(list);
        Collections.sort(arrayList, BY_SCORE);
        ArrayList<Cluster> arrayList2 = new ArrayList();
        for (JSONObject jSONObject : arrayList) {
            Set<String> set = Text.tokens(jSONObject.optString("t"));
            if (!set.isEmpty()) {
                Cluster cluster = null;
                double d = 0.0d;
                boolean isCoaching = isCoaching(jSONObject);
                for (Cluster cluster2 : arrayList2) {
                    double jaccard = Text.jaccard(set, cluster2.tok);
                    boolean z2 = jaccard >= 0.34d || (Text.overlap(set, cluster2.tok) >= 3 && jaccard >= 0.2d);
                    if (!z2 && isCoaching) {
                        int overlap = Text.overlap(strong(set), cluster2.all);
                        z2 = (overlap >= 2 && Text.jaccard(set, cluster2.all) >= 0.12d) || (overlap >= 1 && sharesEnt(jSONObject, cluster2.ents));
                        if (z2) {
                            jaccard = Math.max(jaccard, 0.12d);
                        }
                    }
                    if (z2 && otherPrize(set, cluster2.tok)) {
                        z2 = false;
                    }
                    if (!z2 || jaccard <= d) {
                        jaccard = d;
                        cluster2 = cluster;
                    }
                    d = jaccard;
                    cluster = cluster2;
                }
                if (cluster == null) {
                    cluster = new Cluster();
                    cluster.tok = set;
                    arrayList2.add(cluster);
                }
                cluster.items.add(jSONObject);
                cluster.all.addAll(set);
                JSONArray optJSONArray = jSONObject.optJSONArray("ents");
                for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
                    if (!Pyq.GENERIC_IDS.contains("e:" + optJSONArray.optString(i))) {
                        cluster.ents.add(optJSONArray.optString(i));
                    }
                }
            }
        }
        for (Cluster cluster3 : arrayList2) {
            HashSet hashSet = new HashSet();
            boolean z3 = false;
            Iterator<JSONObject> it = cluster3.items.iterator();
            while (true) {
                z = z3;
                if (!it.hasNext()) {
                    break;
                }
                JSONObject next = it.next();
                if (!isCoaching(next)) {
                    hashSet.add(next.optString("s"));
                }
                z3 = next.optString("d").length() > 40 ? true : z;
            }
            cluster3.score = (z ? 0.5d : 0.0d) + cluster3.head().optDouble("sc", 0.0d) + (0.5d * Math.min(Math.max(hashSet.size() - 1, 0), 3));
        }
        Collections.sort(arrayList2, new Comparator<Cluster>() { // from class: app.newsbreak.core.EditionBuilder.2
            @Override // java.util.Comparator
            public int compare(Cluster cluster4, Cluster cluster5) {
                return Double.compare(cluster5.score, cluster4.score);
            }
        });
        return arrayList2;
    }

    /** Quiz text, exam tags, bylines, captions and page furniture: never a bullet. */
    private static final Pattern NOISE = Pattern.compile("(?i)consider the following|which (one )?of the following|select the correct (answer|code)|^(statement[- ]?ii?\\b|how many of the|assertion \\(a\\)|q\\.?\\s?\\d+[.)]|question\\s*\\d|ans(wer)?\\s*[:.-]|by\\s+[A-Z][a-z]+\\s+[A-Z][a-z]+\\s*$|written by|edited by|updated\\s*:|published\\s*:|last updated|photo\\s*:|image\\s*:|file photo|representational|credit\\s*:|source\\s*:|also read|read more|tags?\\s*:|gs paper|syllabus|mains (question|practice)|prelims (question|practice)|practice question|daily quiz|test your)|\\(a\\)\\s*[^()]{1,40}\\(b\\)\\s*[^()]{1,40}\\(c\\)|\\[[^\\]]{0,40}(u\\.?p\\.?p\\.?c\\.?s|upsc|cse|pcs|ssc|cds|capf|\\(pre\\)|prelims|mains)|\\(pre\\)\\s*\\d{4}|https?://|www\\.|@\\w+\\.(com|in)|\\b(click|tap) here\\b|\\|\\s*\\w+\\s*\\|");

    static boolean junk(String str) {
        String lowerCase = str.toLowerCase();
        for (String str2 : JUNK) {
            if (lowerCase.contains(str2)) {
                return true;
            }
        }
        if (NOISE.matcher(str.trim()).find() || ExamMeta.line(str)) return true;
        String t = str.trim().replaceAll("[\\s.…]+$", "");
        if (t.contains(" ? ") || t.matches("(?i).*\\b(of|at|to|the|a|an|is|are|was|were|by|in|on|for|with|from|and|or|as)$")) return true; // a cut-off question stem
        return str.endsWith("?") || str.length() < 30;
    }

    /** One fact, short: a long sentence is cut at a clause break (never mid-word). */
    static String concise(String s) {
        String n = Text.note(s, 210);
        if (n.length() <= 170) return n;
        int best = -1;
        for (String br : new String[]{"; ", " — ", " – ", ", while ", ", whereas ", ", which ", ", but ", ", and "}) {
            int k = n.lastIndexOf(br, 168);
            if (k >= 70 && k > best) best = k;
        }
        return best > 0 ? n.substring(0, best).replaceAll("[,;:\\s]+$", "") : n;
    }

    /** A bullet from another item must name something the headline or the lead item names. */
    static boolean onTopic(String line, Set<String> topicWords) {
        if (topicWords.isEmpty()) return true;
        for (String w : strong(Text.tokens(line))) if (topicWords.contains(w)) return true;
        return Text.overlap(strongSim(Text.simTokens(line)), topicWords) > 0;
    }

    JSONObject topic(Cluster cluster, Set<String> set, String str) throws Exception {
        JSONObject jSONObject;
        JSONObject jSONObject2;
        JSONObject jSONObject3;
        String str2;
        Object obj;
        Object obj2;
        List<Pyq.Row> matchTopic;
        String str3;
        Object obj3;
        long j;
        int i;
        JSONObject background;
        int i2;
        boolean z;
        boolean z2;
        JSONObject head = cluster.head();
        Iterator<JSONObject> it = cluster.items.iterator();
        while (true) {
            if (!it.hasNext()) {
                jSONObject = head;
                break;
            }
            jSONObject = it.next();
            if (!isCoaching(jSONObject) && jSONObject.optDouble("sc") >= head.optDouble("sc") - 1.5d && jSONObject.optString("d").length() > 40 && srcRank(jSONObject.optString("s")) < srcRank(head.optString("s"))) {
                break;
            }
        }
        if (isCoaching(jSONObject)) {
            Iterator<JSONObject> it2 = cluster.items.iterator();
            while (true) {
                jSONObject2 = jSONObject;
                if (!it2.hasNext()) {
                    break;
                }
                jSONObject = it2.next();
                if (isCoaching(jSONObject) || (!isCoaching(jSONObject2) && jSONObject.optDouble("sc") <= jSONObject2.optDouble("sc"))) {
                    jSONObject = jSONObject2;
                }
            }
        } else {
            jSONObject2 = jSONObject;
        }
        jSONObject3 = jSONObject2;
        if (isCoaching(jSONObject2) && !jSONObject2.has("cx")) {
            for (JSONObject jSONObject4 : cluster.items) {
                if (jSONObject4.has("cx")) {
                    jSONObject3 = jSONObject4;
                    break;
                }
            }
        }
        String cap = Text.cap(jSONObject3.optString("t"), 120);
        Set<String> set2 = Text.tokens(cap);
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        LinkedHashSet linkedHashSet2 = new LinkedHashSet();
        ArrayList<JSONObject> arrayList = new ArrayList(cluster.items);
        Collections.sort(arrayList, new Comparator<JSONObject>() { // from class: app.newsbreak.core.EditionBuilder.3
            @Override // java.util.Comparator
            public int compare(JSONObject jSONObject5, JSONObject jSONObject6) {
                return Integer.compare(EditionBuilder.srcRank(jSONObject5.optString("s")), EditionBuilder.srcRank(jSONObject6.optString("s")));
            }
        });
        for (JSONObject jSONObject5 : arrayList) {
            (isCoaching(jSONObject5) ? linkedHashSet2 : linkedHashSet).add(jSONObject5.optString("s"));
        }
        int size = linkedHashSet.size();
        Iterator it3 = arrayList.iterator();
        while (it3.hasNext()) {
            JSONObject optJSONObject = ((JSONObject) it3.next()).optJSONObject("cx");
            if (optJSONObject != null && optJSONObject.optString("src").length() > 0) {
                linkedHashSet.add(optJSONObject.optString("src"));
            }
        }
        ArrayList<String> arrayList2 = new ArrayList();
        HashSet<String> headLines = new HashSet<String>();
        HashSet hashSet = new HashSet();
        final HashMap hashMap = new HashMap();
        ArrayList<JSONObject> arrayList3 = new ArrayList(cluster.items);
        Collections.sort(arrayList3, BY_SCORE);
        arrayList3.remove(jSONObject3);
        arrayList3.add(0, jSONObject3);
        boolean z3 = size > 0;
        int i3 = 0;
        for (JSONObject jSONObject6 : arrayList3) {
            if (!isCoaching(jSONObject6)) {
                List<String> sentences = Text.sentences(jSONObject6.optString("d"));
                int i4 = i3;
                for (int i5 = 0; i5 < sentences.size() && i5 < 4; i5++) {
                    String str4 = sentences.get(i5);
                    if (!junk(str4)) {
                        double d = Text.hasFigure(str4) ? 1.0d + 2.0d : 1.0d;
                        if (jSONObject6 == jSONObject3 && i5 == 0) {
                            d += 1.5d;
                        }
                        if (str4.length() > 260) {
                            d -= 1.0d;
                        }
                        if (str4.startsWith("“") || str4.startsWith("\"")) {
                            d -= 0.7d;
                        }
                        double d2 = d - (0.15d * i5);
                        if (!hashMap.containsKey(str4)) {
                            if (jSONObject6 == jSONObject3) headLines.add(str4);
                            arrayList2.add(str4);
                            hashMap.put(str4, Double.valueOf(d2));
                            i4++;
                        }
                    }
                }
                JSONArray optJSONArray = jSONObject6.optJSONArray("x");
                int i6 = i4;
                for (int i7 = 0; optJSONArray != null && i7 < optJSONArray.length(); i7++) {
                    String optString = optJSONArray.optString(i7);
                    if (!junk(optString) && !hashMap.containsKey(optString)) {
                        if (jSONObject6 == jSONObject3) headLines.add(optString);
                        double d3 = 1.3d - (0.1d * i7);
                        if (Text.hasFigure(optString)) {
                            d3 += 1.8d;
                        }
                        if (i7 == 0) {
                            d3 += 0.6d;
                        }
                        if (optString.length() > 260) {
                            d3 -= 0.8d;
                        }
                        arrayList2.add(optString);
                        hashMap.put(optString, Double.valueOf(d3));
                        i6++;
                    }
                }
                i3 = i6;
            }
        }
        for (JSONObject jSONObject7 : arrayList3) {
            if (isCoaching(jSONObject7)) {
                JSONObject optJSONObject2 = jSONObject7.optJSONObject("cx");
                JSONArray optJSONArray2 = optJSONObject2 != null ? optJSONObject2.optJSONArray("news") : null;
                if (optJSONArray2 != null && (this.newsDay <= 0 || Math.abs(jSONObject7.optLong("p") - this.newsDay) <= 172800000 || !z3)) {
                    int i8 = 0;
                    while (i8 < optJSONArray2.length() && i8 < 5) {
                        String optString2 = optJSONArray2.optString(i8);
                        if (optString2.length() >= 30 && !hashMap.containsKey(optString2) && !junk(optString2)) {
                            if (jSONObject7 == jSONObject3) headLines.add(optString2);
                            double d4 = ((1.4d - (0.12d * i8)) + (Text.hasFigure(optString2) ? 1.5d : 0.0d)) - (i3 >= 3 ? 0.9d : 0.0d);
                            double d5 = i8 == 0 ? 0.4d : 0.0d;
                            arrayList2.add(optString2);
                            hashMap.put(optString2, Double.valueOf(d5 + d4));
                        }
                        i8++;
                    }
                }
            }
        }
        for (JSONObject jSONObject8 : arrayList3) {
            if (jSONObject8 != jSONObject3 && !isCoaching(jSONObject8)) {
                String optString3 = jSONObject8.optString("t");
                if (Text.jaccard(Text.tokens(optString3), set2) < 0.6d && !hashMap.containsKey(optString3) && optString3.length() >= 30) {
                    arrayList2.add(optString3);
                    hashMap.put(optString3, Double.valueOf(Text.hasFigure(optString3) ? 1.6d : 0.6d));
                    hashSet.add(optString3);
                }
            }
        }
        Collections.sort(arrayList2, new Comparator<String>() { // from class: app.newsbreak.core.EditionBuilder.4
            @Override // java.util.Comparator
            public int compare(String str5, String str6) {
                return Double.compare(((Double) hashMap.get(str6)).doubleValue(), ((Double) hashMap.get(str5)).doubleValue());
            }
        });
        ArrayList arrayList4 = new ArrayList();
        ArrayList arrayList5 = new ArrayList();
        Set<String> topicWords = new HashSet<String>(strong(Text.tokens(cap)));
        topicWords.addAll(strongSim(Text.simTokens(cap)));
        for (String str5 : arrayList2) {
            if (arrayList4.size() >= 5) {
                break;
            }
            if (!headLines.contains(str5) && !hashSet.contains(str5) && !onTopic(str5, topicWords)) continue; // a neighbouring story's line
            String note = concise(str5);
            Set<String> set3 = Text.tokens(note);
            if (set3.size() >= 3 && Text.jaccard(set3, set2) < 0.7d && addsInfo(note, cap, 3) && (!hashSet.contains(str5) || addsInfo(note, cap + ". " + join(arrayList4), 4))) {
                Iterator it4 = arrayList5.iterator();
                while (true) {
                    if (!it4.hasNext()) {
                        z = false;
                        break;
                    }
                    if (Text.jaccard(set3, (Set) it4.next()) >= 0.5d) {
                        z = true;
                        break;
                    }
                }
                Iterator it5 = arrayList4.iterator();
                while (true) {
                    z2 = z;
                    if (!it5.hasNext()) {
                        break;
                    }
                    String str6 = (String) it5.next();
                    if (!z2 && Text.sameLine(note, str6)) {
                        z2 = true;
                    }
                    z = z2;
                }
                if (!z2) {
                    if (set != null) {
                        for (String str7 : set) {
                            if (Text.jaccard(set3, Text.tokens(str7)) >= 0.6d || Text.sameLine(note, str7)) {
                                z2 = true;
                                break;
                            }
                        }
                        if (z2) {
                            continue;
                        }
                    }
                    arrayList4.add(note);
                    arrayList5.add(set3);
                }
            }
        }
        boolean isEmpty = arrayList4.isEmpty();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (JSONObject jSONObject9 : cluster.items) {
            String optString4 = jSONObject9.optString("th", "");
            if (optString4.length() != 0) {
                Double d6 = (Double) linkedHashMap.get(optString4);
                linkedHashMap.put(optString4, Double.valueOf((d6 == null ? 0.0d : d6.doubleValue()) + Math.max(jSONObject9.optDouble("sc", 0.0d), 0.5d)));
            }
        }
        String optString5 = jSONObject3.optString("th", "");
        double d7 = -1.0d;
        Iterator it6 = linkedHashMap.entrySet().iterator();
        while (true) {
            double d8 = d7;
            str2 = optString5;
            if (!it6.hasNext()) {
                break;
            }
            Map.Entry entry = (Map.Entry) it6.next();
            if (((Double) entry.getValue()).doubleValue() > d8) {
                d7 = ((Double) entry.getValue()).doubleValue();
                optString5 = (String) entry.getKey();
            } else {
                d7 = d8;
                optString5 = str2;
            }
        }
        JSONObject theme = this.kb.theme(str2);
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        LinkedHashMap linkedHashMap3 = new LinkedHashMap();
        for (JSONObject jSONObject10 : cluster.items) {
            JSONArray optJSONArray3 = jSONObject10.optJSONArray("ents");
            if (optJSONArray3 != null) {
                int i9 = 0;
                while (true) {
                    int i10 = i9;
                    if (i10 >= optJSONArray3.length()) {
                        break;
                    }
                    Integer num = (Integer) linkedHashMap2.get(optJSONArray3.optString(i10));
                    linkedHashMap2.put(optJSONArray3.optString(i10), Integer.valueOf(num == null ? 1 : num.intValue() + 1));
                    i9 = i10 + 1;
                }
            }
            JSONArray optJSONArray4 = jSONObject10.optJSONArray("maps");
            for (int i12 = 0; optJSONArray4 != null && i12 < optJSONArray4.length(); i12++) {
                Integer num2 = (Integer) linkedHashMap3.get(optJSONArray4.optString(i12));
                linkedHashMap3.put(optJSONArray4.optString(i12), Integer.valueOf(num2 == null ? 1 : num2.intValue() + 1));
            }
        }
        JSONObject put = new JSONObject().put("head", cap).put("bullets", new JSONArray((Collection) arrayList4));
        JSONObject jSONObject11 = null;
        int i13 = -1;
        for (JSONObject jSONObject12 : cluster.items) {
            JSONObject optJSONObject3 = jSONObject12.optJSONObject("cx");
            if (optJSONObject3 != null && (background = Extract.background(optJSONObject3, Text.cap(jSONObject12.optString("t"), 90), 5)) != null && Sentinel.aboutFits(put, background)) {
                int length = (("Drishti".equals(jSONObject12.optString("s")) || "Insights".equals(jSONObject12.optString("s"))) ? 1 : 0) + background.getJSONArray("pts").length();
                if (length > i13) {
                    JSONObject put2 = background.put("via", jSONObject12.optString("s"));
                    i2 = length;
                    jSONObject11 = put2;
                } else {
                    i2 = i13;
                }
                i13 = i2;
            }
        }
        if (jSONObject11 == null) {
            Iterator it7 = arrayList3.iterator();
            while (true) {
                if (!it7.hasNext()) {
                    break;
                }
                JSONObject jSONObject13 = (JSONObject) it7.next();
                JSONArray optJSONArray5 = jSONObject13.optJSONArray("xd");
                if (optJSONArray5 != null && optJSONArray5.length() != 0 && !isCoaching(jSONObject13)) {
                    JSONArray jSONArray = new JSONArray();
                    String str8 = null;
                    for (int i14 = 0; i14 < optJSONArray5.length() && jSONArray.length() < 3; i14++) {
                        String optString6 = optJSONArray5.optString(i14);
                        if (!dupLine(optString6, arrayList4)) {
                            if (str8 == null) {
                                str8 = Extract.defSubject(optString6);
                            }
                            jSONArray.put(Text.note(optString6, 230));
                        }
                    }
                    if (jSONArray.length() > 0 && str8 != null) {
                        JSONObject put3 = new JSONObject().put("h", str8).put("pts", jSONArray).put("via", jSONObject13.optString("s"));
                        if (Sentinel.aboutFits(put, put3)) {
                            jSONObject11 = put3;
                            break;
                        }
                    }
                }
            }
        }
        StringBuilder append = new StringBuilder(cap).append(". ");
        Iterator it8 = arrayList4.iterator();
        while (it8.hasNext()) {
            append.append((String) it8.next()).append(". ");
        }
        if (jSONObject11 != null) {
            append.append(jSONObject11.optString("h"));
        }
        if (jSONObject11 != null) {
            put.put("bg", jSONObject11);
        }
        List<String> conceptsIn = this.kb.conceptsIn(append.toString());
        put.put("ents", new JSONArray((Collection) new ArrayList(linkedHashMap2.keySet()))).put("maps", new JSONArray((Collection) new ArrayList(linkedHashMap3.keySet()))).put("cons", new JSONArray((Collection) conceptsIn));
        JSONArray jSONArray2 = new JSONArray();
        for (String str9 : conceptsIn) {
            if (jSONArray2.length() >= 2) {
                break;
            }
            if (Sentinel.keep(put, "c:" + str9, this.kb)) {
                jSONArray2.put(str9);
            }
        }
        Iterator it9 = linkedHashMap2.entrySet().iterator();
        while (it9.hasNext()) {
            if (!Sentinel.keep(put, "e:" + ((String) ((Map.Entry) it9.next()).getKey()), this.kb)) {
                it9.remove();
            }
        }
        Iterator it10 = linkedHashMap3.entrySet().iterator();
        while (it10.hasNext()) {
            if (!Sentinel.keep(put, "m:" + ((String) ((Map.Entry) it10.next()).getKey()), this.kb)) {
                it10.remove();
            }
        }
        JSONArray pVar = top(linkedHashMap2, 3);
        JSONArray pVar2 = top(linkedHashMap3, 1);
        String str10 = null;
        JSONArray optJSONArray6 = jSONObject3.optJSONArray("ents");
        // primary body/scheme of the story: named in the lead item, most frequent across the cluster, not a generic body
        for (int i15 = 0; i15 < pVar.length() && str10 == null; i15++) {
            String id = pVar.optString(i15);
            if (Pyq.GENERIC_IDS.contains("e:" + id)) continue;
            for (int k = 0; optJSONArray6 != null && k < optJSONArray6.length(); k++) if (id.equals(optJSONArray6.optString(k))) { str10 = id; break; }
        }
        boolean subjFound = false;
        obj = null;
        obj2 = jSONObject11;
        if (jSONObject11 != null) {
            Set<String> set4 = Text.tokens(jSONObject11.optString("h"));
            ArrayList<String> arrayList6 = new ArrayList();
            for (int i16 = 0; i16 < pVar.length(); i16++) {
                JSONObject entity = this.kb.entity(pVar.optString(i16));
                if (entity != null) {
                    arrayList6.add(entity.optString("name"));
                }
            }
            for (int i17 = 0; i17 < jSONArray2.length(); i17++) {
                JSONObject concept = this.kb.concept(jSONArray2.optString(i17));
                if (concept != null) {
                    arrayList6.add(concept.optString("name"));
                }
            }
            for (String str11 : arrayList6) {
                if (!set4.isEmpty() && Text.overlap(set4, Text.tokens(str11)) >= Math.max(1, (set4.size() + 1) / 2)) {
                    obj = jSONObject11.optString("h");
                    obj2 = null;
                    subjFound = true;
                    break;
                }
            }
        }
        if (!subjFound) {
            obj = null;
            obj2 = jSONObject11;
        }
        LinkedHashSet linkedHashSet3 = new LinkedHashSet();
        for (int i18 = 0; i18 < pVar.length(); i18++) {
            linkedHashSet3.add("e:" + pVar.optString(i18));
        }
        for (int i19 = 0; i19 < pVar2.length(); i19++) {
            linkedHashSet3.add("m:" + pVar2.optString(i19));
        }
        if (this.pyq == null) {
            matchTopic = new ArrayList();
        } else {
            matchTopic = this.pyq.matchTopic(Foreign.mask(cap), Foreign.mask(join(arrayList4)), theme != null ? theme.optString("gs") : "", linkedHashSet3, 4);
        }
        String str12 = null;
        if (matchTopic.isEmpty() || this.pyq == null) {
            str3 = null;
        } else {
            Iterator it11 = linkedHashSet3.iterator();
            str3 = null;
            String str13 = null;
            while (true) {
                if (!it11.hasNext()) {
                    str12 = str13;
                    break;
                }
                String str14 = (String) it11.next();
                if (!Pyq.GENERIC_IDS.contains(str14)) {
                    JSONObject entity2 = str14.startsWith("e:") ? this.kb.entity(str14.substring(2)) : this.kb.map(str14.substring(2));
                    if (entity2 != null) {
                        Iterator<Pyq.Row> it12 = matchTopic.iterator();
                        while (true) {
                            if (!it12.hasNext()) {
                                str14 = str3;
                                str12 = str13;
                                break;
                            }
                            if (it12.next().ids.contains(str14)) {
                                str12 = entity2.optString("name");
                                break;
                            }
                        }
                        if (str12 != null) {
                            str3 = str14;
                            break;
                        }
                        str3 = str14;
                        str13 = str12;
                    } else {
                        continue;
                    }
                }
            }
        }
        JSONObject jSONObject14 = new JSONObject();
        jSONObject14.put("head", cap);
        jSONObject14.put("gs", theme != null ? theme.getString("gs") : "Misc");
        jSONObject14.put("theme", str2);
        jSONObject14.put("themeLabel", theme != null ? theme.getString("label") : "Current events");
        jSONObject14.put("tier", theme != null ? theme.getString("tier") : "LOW");
        String str15 = theme != null ? theme.getString("label") + " — " + theme.getString("pyq") : "contemporary events";
        if (!matchTopic.isEmpty()) {
            Object obj4 = "PYQ: " + (str12 != null ? str12 + " — " : "") + Pyq.compact(matchTopic, str12);
            JSONArray jSONArray3 = new JSONArray();
            for (Pyq.Row row : matchTopic) {
                String aspect = Pyq.aspect(row);
                JSONArray ev = new JSONArray().put(row.year).put((str12 == null || !Pyq.sameAs(aspect, str12)) ? aspect : "asked");
                String qk = this.pyq.key(row);
                if (qk != null) ev.put(qk); // opens the full question in the app
                jSONArray3.put(ev);
            }
            // every other related question from any UPSC paper (each opens in full)
            Set<String> have = new HashSet<String>();
            for (Pyq.Row row : matchTopic) have.add(row.year + "|" + row.qno + "|" + row.exam);
            @SuppressWarnings("unchecked") List<Pyq.Row> all = this.pyq.matchAll(Foreign.mask(cap), Foreign.mask(join(arrayList4)), theme != null ? theme.optString("gs") : "", (Set<String>) linkedHashSet3);
            for (Pyq.Row row : all) {
                if (!have.add(row.year + "|" + row.qno + "|" + row.exam)) continue;
                String qk = this.pyq.key(row);
                if (qk == null) continue; // only questions that open
                jSONArray3.put(new JSONArray().put(row.year).put(Pyq.aspect(row)).put(qk));
            }
            jSONObject14.put("pyqEv", jSONArray3);
            jSONObject14.put("pyqAll", 2);
            if (str3 != null) {
                jSONObject14.put("pyqId", str3);
            }
            obj3 = obj4;
        } else {
            obj3 = "PYQ THEME: closely related to UPSC's repeated questions on " + str15;
        }
        jSONObject14.put("pyq", obj3);
        jSONObject14.put("pyqTheme", str15);
        if (theme != null && "rising".equals(theme.optString("trend"))) {
            jSONObject14.put("rising", true);
        }
        jSONObject14.put("srcs", new JSONArray((Collection) new ArrayList(linkedHashSet)));
        jSONObject14.put("coach", new JSONArray((Collection) new ArrayList(linkedHashSet2)));
        jSONObject14.put("single", linkedHashSet.size() <= 1);
        jSONObject14.put("bullets", new JSONArray((Collection) arrayList4));
        jSONObject14.put("hlOnly", isEmpty);
        jSONObject14.put("ents", pVar);
        jSONObject14.put("maps", pVar2);
        if (str10 != null) {
            jSONObject14.put("pent", str10);
        }
        if (obj2 != null) {
            jSONObject14.put("bg", obj2);
        }
        if (obj != null) {
            jSONObject14.put("subj", obj);
        }
        if (jSONArray2.length() > 0) {
            jSONObject14.put("cons", jSONArray2);
        }
        if (size == 0 && isCoaching(jSONObject3)) {
            jSONObject14.put("via", jSONObject3.optString("s"));
        }
        JSONArray jSONArray4 = new JSONArray();
        for (JSONObject jSONObject15 : arrayList3) {
            if (jSONArray4.length() < 10) {
                jSONArray4.put(jSONObject15.optString("id"));
            }
        }
        jSONObject14.put("iids", jSONArray4);
        jSONObject14.put("score", Math.round(cluster.score * 10.0d) / 10.0d);
        if (str != null) {
            jSONObject14.put("cont", str);
        }
        JSONArray jSONArray5 = new JSONArray();
        HashSet hashSet2 = new HashSet();
        for (JSONObject jSONObject16 : arrayList) {
            if (jSONArray5.length() >= 5) {
                break;
            }
            String optString8 = jSONObject16.optString("u");
            if (optString8.length() != 0 && hashSet2.add(jSONObject16.optString("s") + optString8)) {
                JSONObject jSONObject17 = new JSONObject();
                jSONObject17.put("s", jSONObject16.optString("s"));
                jSONObject17.put("u", optString8);
                jSONObject17.put("t", jSONObject16.optString("t"));
                jSONArray5.put(jSONObject17);
            }
        }
        jSONObject14.put("links", jSONArray5);
        jSONObject14.put("corrob", linkedHashSet.size());
        JSONObject fx = figureSources(cluster.items);
        if (fx.length() > 0) jSONObject14.put("fx", fx); // figures the reader can trust more: carried by 2+ outlets or by an official source
        JSONArray conflicts = conflicts(cluster.items);
        if (conflicts.length() > 0) {
            jSONObject14.put("conflicts", conflicts);
        }
        for (JSONObject jSONObject18 : cluster.items) {
            String lowerCase = jSONObject18.optString("u").toLowerCase();
            String lowerCase2 = jSONObject18.optString("d").toLowerCase();
            if (lowerCase.contains("/premium/") || lowerCase.contains("/premium-") || lowerCase2.startsWith("premium") || lowerCase2.contains("subscribers only") || lowerCase2.contains("subscriber only")) {
                jSONObject14.put("paywalled", jSONObject18.optString("s"));
                break;
            }
        }
        long j2 = 0;
        ArrayList arrayList7 = new ArrayList();
        for (JSONObject jSONObject19 : cluster.items) {
            if (!isCoaching(jSONObject19) || this.newsDay <= 0 || Math.abs(jSONObject19.optLong("p") - this.newsDay) <= 129600000) {
                arrayList7.add(jSONObject19);
            }
        }
        if (arrayList7.isEmpty()) {
            arrayList7.addAll(cluster.items);
        }
        Iterator it13 = arrayList7.iterator();
        while (it13.hasNext()) {
            j2 = Math.max(j2, ((JSONObject) it13.next()).optLong("p"));
        }
        jSONObject14.put("p", j2);
        long j3 = Long.MAX_VALUE;
        long j4 = 0;
        Iterator it14 = arrayList7.iterator();
        while (true) {
            j = j4;
            if (!it14.hasNext()) {
                break;
            }
            JSONObject jSONObject20 = (JSONObject) it14.next();
            j3 = Math.min(j3, jSONObject20.optLong("fs", jSONObject20.optLong("p")));
            j4 = Math.max(j, jSONObject20.optLong("fs", jSONObject20.optLong("p")));
        }
        jSONObject14.put("fs", j3);
        if (j - j3 > 2700000) {
            jSONObject14.put("upd", j);
        }
        return jSONObject14;
    }

    static String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            sb.append(it.next()).append(' ');
        }
        return sb.toString();
    }

    /** Official sources: their figure is the figure of record. */
    public static final List<String> OFFICIAL = Arrays.asList("PIB", "PMI", "RBI", "MEA", "PRS", "ISRO");
    /** Amounts with a unit (₹, $, %, crore, MW, km …): the figures that get quoted and asked. Bare years and counts are not checked. */
    static final Pattern FIGX = Pattern.compile("(?i)((?:₹|rs\\.?\\s?)\\s?\\d[\\d,.]*(?:\\s?(?:lakh crore|crore|lakh|bn|mn|tn|billion|million|trillion))?|\\$\\s?\\d[\\d,.]*(?:\\s?(?:bn|mn|tn|billion|million|trillion))?|\\b\\d[\\d,.]*\\s?(?:%|per ?cent|lakh crore|crore|lakh|bn|mn|tn|billion|million|sq km|km|MW|GW|GWh|ha|hectares?|tonnes?|MT)(?![a-z]))");

    /** Same figure, same key: "Rs 16,300 crore" = "₹16300 crore"; "5.5 per cent" = "5.5%". */
    public static String figKey(String f) {
        String k = f.toLowerCase(Locale.ROOT).replaceAll("rs\\.?\\s?", "₹").replaceAll("per ?cent", "%").replace("billion", "bn").replace("million", "mn").replace("trillion", "tn").replace("hectares", "ha").replace("hectare", "ha").replace("tonnes", "tonne");
        return k.replaceAll("[\\s,]", "").replaceAll("\\.$", "");
    }

    /**
     * Every quotable figure in a story's texts (headline, summary, full article) → the outlets that carry it. Kept when
     * two independent outlets agree or an official source states it. Coaching notes are not independent (they retell
     * the papers), so they don't count.
     */
    static JSONObject figureSources(List<JSONObject> items) {
        Map<String, Set<String>> by = new LinkedHashMap<String, Set<String>>();
        for (JSONObject it : items) {
            if (isCoaching(it)) continue;
            String code = it.optString("s");
            StringBuilder t = new StringBuilder(it.optString("t")).append(". ").append(it.optString("d")).append(". ").append(it.optString("xb"));
            JSONArray x = it.optJSONArray("x");
            for (int i = 0; x != null && i < x.length(); i++) t.append(". ").append(x.optString(i));
            Matcher m = FIGX.matcher(t);
            while (m.find()) {
                String k = figKey(m.group(1));
                if (k.length() < 2 || k.replaceAll("[^0-9]", "").length() == 0) continue;
                Set<String> c = by.get(k);
                if (c == null) { c = new LinkedHashSet<String>(); by.put(k, c); }
                c.add(code);
            }
        }
        JSONObject out = new JSONObject();
        try {
            for (Map.Entry<String, Set<String>> e : by.entrySet()) {
                boolean off = false;
                for (String c : e.getValue()) if (OFFICIAL.contains(c)) off = true;
                if (e.getValue().size() >= 2 || off) {
                    StringBuilder b = new StringBuilder();
                    for (String c : e.getValue()) b.append(b.length() > 0 ? "," : "").append(c);
                    out.put(e.getKey(), b.toString());
                    if (out.length() >= 40) break;
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    static JSONArray conflicts(List<JSONObject> list) {
        HashMap hashMap = new HashMap();
        JSONArray jSONArray = new JSONArray();
        for (JSONObject jSONObject : list) {
            String optString = jSONObject.optString("s");
            if (!isCoaching(jSONObject)) {
                String str = jSONObject.optString("t") + ". " + jSONObject.optString("d");
                Matcher matcher = NUM.matcher(str);
                while (matcher.find()) {
                    String replace = matcher.group(3).toLowerCase().replace("per cent", "%").replace("percent", "%").replace("billion", "bn").replace("million", "mn");
                    ArrayList arrayList = new ArrayList(Text.tokens(str.substring(Math.max(0, matcher.start() - 48), matcher.start())));
                    if (arrayList.size() >= 2) {
                        String str2 = ((String) arrayList.get(arrayList.size() - 2)) + " " + ((String) arrayList.get(arrayList.size() - 1)) + "|" + replace;
                        String replace2 = matcher.group(2).replace(",", "");
                        String[] strArr = (String[]) hashMap.get(str2);
                        if (strArr == null) {
                            hashMap.put(str2, new String[]{replace2, optString, matcher.group().trim()});
                        } else if (!strArr[1].equals(optString)) {
                            try {
                                double parseDouble = Double.parseDouble(strArr[0]);
                                double parseDouble2 = Double.parseDouble(replace2);
                                if (Math.abs(parseDouble - parseDouble2) > Math.max(Math.abs(parseDouble), Math.abs(parseDouble2)) * 0.005d && jSONArray.length() < 2) {
                                    String offc = OFFICIAL.contains(strArr[1]) ? strArr[1] : OFFICIAL.contains(optString) ? optString : null;
                                    jSONArray.put(str2.substring(0, str2.indexOf(124)) + ": " + strArr[2] + " (" + strArr[1] + ") vs " + matcher.group().trim() + " (" + optString + ")" + (offc != null ? "; " + offc + " is the official source" : ""));
                                }
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }
            }
        }
        return jSONArray;
    }

    static JSONArray top(Map<String, Integer> map, int i) {
        ArrayList arrayList = new ArrayList(map.entrySet());
        Collections.sort(arrayList, new Comparator<Map.Entry<String, Integer>>() { // from class: app.newsbreak.core.EditionBuilder.5
            @Override // java.util.Comparator
            public int compare(Map.Entry<String, Integer> entry, Map.Entry<String, Integer> entry2) {
                return entry2.getValue().intValue() - entry.getValue().intValue();
            }
        });
        JSONArray jSONArray = new JSONArray();
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 >= arrayList.size() || i3 >= i) {
                break;
            }
            jSONArray.put(((Map.Entry) arrayList.get(i3)).getKey());
            i2 = i3 + 1;
        }
        return jSONArray;
    }

    static List<JSONObject> inWindow(List<JSONObject> list, long j, long j2) {
        ArrayList arrayList = new ArrayList();
        for (JSONObject jSONObject : list) {
            long optLong = jSONObject.optLong("p");
            if (optLong >= j && optLong < j2) {
                arrayList.add(jSONObject);
            }
        }
        return arrayList;
    }

    static Set<String> topicTok(JSONObject jSONObject) {
        return Text.tokens(jSONObject.optString("head"));
    }

    static List<Set<String>> allHeads(JSONObject jSONObject) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(topicTok(jSONObject));
        JSONArray optJSONArray = jSONObject.optJSONArray("also");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            arrayList.add(Text.tokens(optJSONArray.optJSONObject(i).optString("head")));
        }
        JSONArray optJSONArray2 = jSONObject.optJSONArray("heads");
        for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
            arrayList.add(Text.tokens(optJSONArray2.optString(i2)));
        }
        return arrayList;
    }

    static List<String> headStrs(JSONObject jSONObject) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(jSONObject.optString("head"));
        JSONArray optJSONArray = jSONObject.optJSONArray("also");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            arrayList.add(optJSONArray.optJSONObject(i).optString("head"));
        }
        JSONArray optJSONArray2 = jSONObject.optJSONArray("heads");
        for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
            arrayList.add(optJSONArray2.optString(i2));
        }
        return arrayList;
    }

    static boolean addsInfo(String str, String str2) {
        return addsInfo(str, str2, 3);
    }

    static boolean addsInfo(String str, String str2, int i) {
        Set<String> simTokens = Text.simTokens(str);
        Set<String> simTokens2 = Text.simTokens(str2);
        int i2 = 0;
        for (String str3 : simTokens) {
            if (!simTokens2.contains(str3)) {
                if (str3.matches("\\d+(\\.\\d+)?%?") && str3.length() >= 2) {
                    return true;
                }
                if (!WEAK.contains(str3) && !WEAK_SIM.contains(str3)) {
                    i2++;
                }
                i2 = i2;
            }
        }
        return i2 >= i;
    }

    static Set<String> strongSim(Set<String> set) {
        HashSet hashSet = new HashSet(set);
        hashSet.removeAll(WEAK);
        hashSet.removeAll(WEAK_SIM);
        return hashSet;
    }

    static double headSim(JSONObject jSONObject, Set<String> set) {
        double d = 0.0d;
        Iterator<Set<String>> it = allHeads(jSONObject).iterator();
        while (true) {
            double d2 = d;
            if (!it.hasNext()) {
                return d2;
            }
            d = Math.max(d2, Text.jaccard(it.next(), set));
        }
    }

    static boolean sameStory(JSONObject jSONObject, JSONObject jSONObject2) {
        int overlap;
        String optString = jSONObject2.optString("head");
        if (otherPrize(Text.tokens(jSONObject.optString("head")), Text.tokens(optString))) {
            return false;
        }
        Set<String> nameToks = nameToks(jSONObject, jSONObject2);
        Set<String> minus = minus(topicTok(jSONObject2), nameToks);
        Set<String> minus2 = minus(Text.simTokens(optString), nameToks);
        Set<String> strongSim = strongSim(minus2);
        boolean sameBody = sameBody(jSONObject, jSONObject2);
        for (String str : headStrs(jSONObject)) {
            Set<String> minus3 = minus(Text.tokens(str), nameToks);
            Set<String> minus4 = minus(Text.simTokens(str), nameToks);
            if (Text.jaccard(minus3, minus) >= 0.3d || Text.jaccard(minus4, minus2) >= 0.3d || Text.sameLine(str, optString) || (overlap = Text.overlap(strongSim(minus4), strongSim)) >= 3 || (overlap >= 2 && (sameBody || jSONObject.optString("theme").equals(jSONObject2.optString("theme"))))) {
                return true;
            }
        }
        JSONArray optJSONArray = jSONObject.optJSONArray("bullets");
        JSONArray optJSONArray2 = jSONObject2.optJSONArray("bullets");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
                if (Text.sameLine(optJSONArray.optString(i), optJSONArray2.optString(i2))) {
                    return true;
                }
            }
        }
        return false;
    }

    static Set<String> minus(Set<String> set, Set<String> set2) {
        HashSet hashSet = new HashSet(set);
        hashSet.removeAll(set2);
        return hashSet;
    }

    static boolean sameBody(JSONObject jSONObject, JSONObject jSONObject2) {
        String optString = jSONObject2.optString("pent", "");
        return optString.length() > 0 && optString.equals(jSONObject.optString("pent", ""));
    }

    static JSONObject host(List<JSONObject> list, JSONObject jSONObject) {
        for (JSONObject jSONObject2 : list) {
            if (sameStory(jSONObject2, jSONObject)) {
                return jSONObject2;
            }
        }
        for (JSONObject jSONObject3 : list) {
            if (sameBody(jSONObject3, jSONObject)) {
                return jSONObject3;
            }
        }
        return null;
    }

    static boolean dupLine(String str, List<String> list) {
        Set<String> set = Text.tokens(str);
        for (String str2 : list) {
            if (Text.jaccard(set, Text.tokens(str2)) >= 0.5d || Text.sameLine(str, str2)) {
                return true;
            }
        }
        return false;
    }

    static List<String> cardLines(JSONObject jSONObject) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(jSONObject.optString("head"));
        JSONArray optJSONArray = jSONObject.optJSONArray("bullets");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            arrayList.add(optJSONArray.optString(i));
        }
        JSONArray optJSONArray2 = jSONObject.optJSONArray("also");
        for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
            JSONObject optJSONObject = optJSONArray2.optJSONObject(i2);
            arrayList.add(optJSONObject.optString("head"));
            JSONArray optJSONArray3 = optJSONObject.optJSONArray("bullets");
            for (int i3 = 0; optJSONArray3 != null && i3 < optJSONArray3.length(); i3++) {
                arrayList.add(optJSONArray3.optString(i3));
            }
        }
        return arrayList;
    }

    static void union(JSONObject jSONObject, JSONObject jSONObject2, String str, int i) throws Exception {
        JSONArray optJSONArray = jSONObject.optJSONArray(str);
        JSONArray optJSONArray2 = jSONObject2.optJSONArray(str);
        if (optJSONArray == null) {
            optJSONArray = new JSONArray();
            jSONObject.put(str, optJSONArray);
        }
        HashSet hashSet = new HashSet();
        for (int i2 = 0; i2 < optJSONArray.length(); i2++) {
            hashSet.add(String.valueOf(optJSONArray.opt(i2)));
        }
        for (int i3 = 0; optJSONArray2 != null && i3 < optJSONArray2.length() && optJSONArray.length() < i; i3++) {
            if (hashSet.add(String.valueOf(optJSONArray2.opt(i3)))) {
                optJSONArray.put(optJSONArray2.opt(i3));
            }
        }
    }

    /** Past questions and checked figures of a merged card join the host's, each question once. */
    static void unionEv(JSONObject host, JSONObject other) throws Exception {
        JSONArray a = host.optJSONArray("pyqEv"), b = other.optJSONArray("pyqEv");
        if (a != null && b != null) {
            Set<String> seen = new HashSet<String>();
            for (int i = 0; i < a.length(); i++) { JSONArray e = a.optJSONArray(i); if (e != null) seen.add(e.length() > 2 && e.optString(2).length() > 0 ? e.optString(2) : e.optString(0) + "|" + e.optString(1)); }
            for (int i = 0; i < b.length(); i++) {
                JSONArray e = b.optJSONArray(i);
                if (e == null) continue;
                String k = e.length() > 2 && e.optString(2).length() > 0 ? e.optString(2) : e.optString(0) + "|" + e.optString(1);
                if (seen.add(k)) a.put(e);
            }
        }
        JSONObject fa = host.optJSONObject("fx"), fb = other.optJSONObject("fx");
        if (fb != null) {
            if (fa == null) { host.put("fx", new JSONObject(fb.toString())); }
            else { JSONArray n = fb.names(); for (int i = 0; n != null && i < n.length(); i++) if (!fa.has(n.optString(i))) fa.put(n.optString(i), fb.get(n.optString(i))); }
        }
    }

    /** The body a card is about: its primary entity, else its first entity. Catch-all bodies (courts, UN) never merge cards. */
    static String bodyKey(JSONObject t) {
        String k = t.optString("pent", "");
        if (k.isEmpty()) { JSONArray e = t.optJSONArray("ents"); k = e != null && e.length() > 0 ? e.optString(0) : ""; }
        return k.isEmpty() || Sentinel.GENERIC.contains("e:" + k) ? "" : k;
    }

    /**
     * One card per body per day: several stories about the same body today (three Election Commission stories, say)
     * become one card, with the other stories as "More on … today" and one combined list of past questions. Runs on a
     * single day's cards only, so a body in the news again tomorrow gets its own card then.
     * @return true if any cards were merged.
     */
    public static boolean consolidate(JSONArray topics) {
        if (topics == null || topics.length() < 2) return false;
        boolean any = false;
        try {
            List<JSONObject> keep = new ArrayList<JSONObject>();
            Map<String, JSONObject> byBody = new HashMap<String, JSONObject>();
            for (int i = 0; i < topics.length(); i++) {
                JSONObject t = topics.optJSONObject(i);
                if (t == null) continue;
                String k = bodyKey(t);
                JSONObject host = k.isEmpty() ? null : byBody.get(k);
                if (host != null) {
                    JSONArray before = host.optJSONArray("pyqEv");
                    mergeInto(host, t, 0L);
                    if (before == null && host.optJSONArray("pyqEv") != null) host.put("pyqEv", host.getJSONArray("pyqEv"));
                    any = true;
                    continue;
                }
                if (!k.isEmpty()) byBody.put(k, t);
                keep.add(t);
            }
            if (any) {
                while (topics.length() > 0) topics.remove(topics.length() - 1);
                for (JSONObject t : keep) topics.put(t);
            }
        } catch (Exception ignored) {}
        return any;
    }

    static boolean mergeInto(JSONObject jSONObject, JSONObject jSONObject2, long j) throws Exception {
        boolean z;
        JSONArray jSONArray;
        boolean z2;
        JSONObject jSONObject3 = null;
        JSONArray optJSONArray = jSONObject.optJSONArray("also");
        int i = 0;
        while (optJSONArray != null && i < optJSONArray.length() && jSONObject3 == null) {
            JSONObject optJSONObject = optJSONArray.optJSONObject(i);
            if (!Text.sameLine(optJSONObject.optString("head"), jSONObject2.optString("head")) && Text.jaccard(Text.tokens(optJSONObject.optString("head")), topicTok(jSONObject2)) < 0.3d && Text.overlap(strongSim(Text.simTokens(optJSONObject.optString("head"))), strongSim(Text.simTokens(jSONObject2.optString("head")))) < 2) {
                optJSONObject = jSONObject3;
            }
            i++;
            jSONObject3 = optJSONObject;
        }
        boolean z3 = false;
        Iterator<String> it = headStrs0(jSONObject).iterator();
        while (true) {
            z = z3;
            if (!it.hasNext()) {
                break;
            }
            String next = it.next();
            z3 = (Text.sameLine(next, jSONObject2.optString("head")) || Text.jaccard(Text.tokens(next), topicTok(jSONObject2)) >= 0.3d) ? true : z;
        }
        if (jSONObject3 != null && !z) {
            return foldSub(jSONObject, jSONObject3, jSONObject2, j);
        }
        boolean sameStory = sameStory(jSONObject, jSONObject2);
        List<String> cardLines = cardLines(jSONObject);
        JSONArray optJSONArray2 = jSONObject.optJSONArray("_seen");
        for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
            cardLines.add(optJSONArray2.optString(i2));
        }
        boolean z4 = false;
        union(jSONObject, jSONObject2, "srcs", 8);
        union(jSONObject, jSONObject2, "coach", 4);
        union(jSONObject, jSONObject2, "ents", 3);
        union(jSONObject, jSONObject2, "cons", 2);
        union(jSONObject, jSONObject2, "iids", 14);
        if (!jSONObject.has("bg") && jSONObject2.has("bg")) {
            jSONObject.put("bg", jSONObject2.get("bg"));
        }
        if (sameStory && !jSONObject.has("subj") && jSONObject2.has("subj")) {
            jSONObject.put("subj", jSONObject2.get("subj"));
        }
        if (jSONObject.has("via") && !jSONObject2.has("via")) {
            if (sameStory) {
                JSONArray optJSONArray3 = jSONObject.optJSONArray("heads");
                if (optJSONArray3 == null) {
                    optJSONArray3 = new JSONArray();
                    jSONObject.put("heads", optJSONArray3);
                }
                optJSONArray3.put(jSONObject.optString("head"));
                jSONObject.put("head", jSONObject2.optString("head"));
            }
            jSONObject.remove("via");
        }
        if (jSONObject.optJSONArray("maps") == null || jSONObject.optJSONArray("maps").length() == 0) {
            union(jSONObject, jSONObject2, "maps", 1);
        }
        JSONArray optJSONArray4 = jSONObject.optJSONArray("links");
        HashSet hashSet = new HashSet();
        for (int i3 = 0; optJSONArray4 != null && i3 < optJSONArray4.length(); i3++) {
            hashSet.add(optJSONArray4.optJSONObject(i3).optString("u"));
        }
        JSONArray optJSONArray5 = jSONObject2.optJSONArray("links");
        for (int i4 = 0; optJSONArray4 != null && optJSONArray5 != null && i4 < optJSONArray5.length() && optJSONArray4.length() < 8; i4++) {
            if (hashSet.add(optJSONArray5.optJSONObject(i4).optString("u"))) {
                optJSONArray4.put(optJSONArray5.optJSONObject(i4));
            }
        }
        jSONObject.put("corrob", jSONObject.optJSONArray("srcs") == null ? 0 : jSONObject.getJSONArray("srcs").length());
        jSONObject.put("single", jSONObject.optJSONArray("srcs") == null || jSONObject.getJSONArray("srcs").length() <= 1);
        unionEv(jSONObject, jSONObject2);
        if (!jSONObject.has("pyqEv") && jSONObject2.has("pyqEv")) {
            jSONObject.put("pyq", jSONObject2.optString("pyq"));
            jSONObject.put("pyqEv", jSONObject2.get("pyqEv"));
            if (jSONObject2.has("pyqId")) {
                jSONObject.put("pyqId", jSONObject2.get("pyqId"));
            } else {
                jSONObject.remove("pyqId");
            }
        }
        JSONArray optJSONArray6 = jSONObject2.optJSONArray("conflicts");
        for (int i5 = 0; optJSONArray6 != null && i5 < optJSONArray6.length(); i5++) {
            JSONArray optJSONArray7 = jSONObject.optJSONArray("conflicts");
            if (optJSONArray7 == null) {
                optJSONArray7 = new JSONArray();
                jSONObject.put("conflicts", optJSONArray7);
            }
            if (optJSONArray7.length() < 2 && !optJSONArray7.toString().contains(optJSONArray6.optString(i5))) {
                optJSONArray7.put(optJSONArray6.optString(i5));
            }
        }
        jSONObject.put("p", Math.max(jSONObject.optLong("p"), jSONObject2.optLong("p")));
        if (jSONObject.optString("paywalled").isEmpty() && jSONObject2.has("paywalled")) {
            jSONObject.put("paywalled", jSONObject2.optString("paywalled"));
        }
        JSONArray optJSONArray8 = jSONObject.optJSONArray("bullets");
        if (optJSONArray8 == null) {
            JSONArray jSONArray2 = new JSONArray();
            jSONObject.put("bullets", jSONArray2);
            jSONArray = jSONArray2;
        } else {
            jSONArray = optJSONArray8;
        }
        if (sameStory) {
            JSONArray optJSONArray9 = jSONObject.optJSONArray("heads");
            if (optJSONArray9 == null) {
                optJSONArray9 = new JSONArray();
                jSONObject.put("heads", optJSONArray9);
            }
            if (optJSONArray9.length() < 6 && !jSONObject2.optString("head").equals(jSONObject.optString("head")) && !optJSONArray9.toString().contains(JSONObject.quote(jSONObject2.optString("head")))) {
                optJSONArray9.put(jSONObject2.optString("head"));
            }
            ArrayList arrayList = new ArrayList(toList(jSONObject2.optJSONArray("bullets")));
            arrayList.add(Text.note(jSONObject2.optString("head"), 165));
            int i6 = 0;
            while (true) {
                int i7 = i6;
                if (i7 >= arrayList.size()) {
                    break;
                }
                String str = (String) arrayList.get(i7);
                if (jSONArray.length() >= 5) {
                    break;
                }
                if (str.length() >= 25 && !dupLine(str, cardLines) && ((i7 != arrayList.size() - 1 || addsInfo(str, join(cardLines), 4)) && (optJSONArray2 == null || addsInfo(str, join(cardLines), 3)))) {
                    jSONArray.put(str);
                    cardLines.add(str);
                    z4 = true;
                }
                i6 = i7 + 1;
            }
            if (jSONArray.length() > 0) {
                jSONObject.put("hlOnly", false);
            }
            z2 = z4;
        } else {
            JSONArray optJSONArray10 = jSONObject.optJSONArray("also");
            if (optJSONArray10 == null) {
                optJSONArray10 = new JSONArray();
                jSONObject.put("also", optJSONArray10);
            }
            if (optJSONArray10.length() >= 3 || dupLine(jSONObject2.optString("head"), cardLines)) {
                z2 = false;
            } else {
                JSONObject jSONObject4 = new JSONObject();
                jSONObject4.put("head", jSONObject2.optString("head"));
                JSONArray jSONArray3 = new JSONArray();
                JSONArray optJSONArray11 = jSONObject2.optJSONArray("bullets");
                for (int i8 = 0; optJSONArray11 != null && i8 < optJSONArray11.length() && jSONArray3.length() < 2; i8++) {
                    String optString = optJSONArray11.optString(i8);
                    if (!dupLine(optString, cardLines)) {
                        jSONArray3.put(optString);
                        cardLines.add(optString);
                    }
                }
                jSONObject4.put("bullets", jSONArray3);
                jSONObject4.put("srcs", jSONObject2.optJSONArray("srcs"));
                jSONObject4.put("p", jSONObject2.optLong("p"));
                optJSONArray10.put(jSONObject4);
                z2 = true;
            }
        }
        if (!z2 || j <= 0) {
            return z2;
        }
        jSONObject.put("upd", j);
        return z2;
    }

    static List<String> headStrs0(JSONObject jSONObject) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(jSONObject.optString("head"));
        JSONArray optJSONArray = jSONObject.optJSONArray("heads");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            arrayList.add(optJSONArray.optString(i));
        }
        return arrayList;
    }

    static boolean foldSub(JSONObject jSONObject, JSONObject jSONObject2, JSONObject jSONObject3, long j) throws Exception {
        JSONArray jSONArray;
        boolean z;
        union(jSONObject, jSONObject3, "srcs", 8);
        jSONObject.put("corrob", jSONObject.getJSONArray("srcs").length());
        jSONObject.put("single", jSONObject.getJSONArray("srcs").length() <= 1);
        List<String> cardLines = cardLines(jSONObject);
        JSONArray optJSONArray = jSONObject.optJSONArray("_seen");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            cardLines.add(optJSONArray.optString(i));
        }
        JSONArray optJSONArray2 = jSONObject2.optJSONArray("bullets");
        if (optJSONArray2 == null) {
            JSONArray jSONArray2 = new JSONArray();
            jSONObject2.put("bullets", jSONArray2);
            jSONArray = jSONArray2;
        } else {
            jSONArray = optJSONArray2;
        }
        ArrayList arrayList = new ArrayList(toList(jSONObject3.optJSONArray("bullets")));
        arrayList.add(Text.note(jSONObject3.optString("head"), 165));
        int i2 = 0;
        boolean z2 = false;
        while (i2 < arrayList.size() && jSONArray.length() < 3) {
            String str = (String) arrayList.get(i2);
            if (str.length() < 25) {
                z = z2;
            } else if (dupLine(str, cardLines)) {
                z = z2;
            } else if (i2 != arrayList.size() - 1 || addsInfo(str, join(cardLines), 4)) {
                jSONArray.put(str);
                cardLines.add(str);
                z = true;
            } else {
                z = z2;
            }
            i2++;
            z2 = z;
        }
        union(jSONObject2, jSONObject3, "srcs", 6);
        if (z2 && j > 0) {
            jSONObject.put("upd", j);
        }
        return z2;
    }

    static void dedupeAcross(List<JSONObject> list, boolean z) {
        ArrayList arrayList = new ArrayList();
        for (JSONObject jSONObject : list) {
            if (!z || !jSONObject.optBoolean("_old")) {
                JSONArray optJSONArray = jSONObject.optJSONArray("bullets");
                JSONArray jSONArray = new JSONArray();
                for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
                    if (!dupLine(optJSONArray.optString(i), arrayList)) {
                        jSONArray.put(optJSONArray.optString(i));
                    }
                }
                try {
                    jSONObject.put("bullets", jSONArray);
                    if (jSONArray.length() == 0 && optJSONArray != null && optJSONArray.length() > 0) {
                        jSONObject.put("hlOnly", true);
                    }
                } catch (Exception e) {
                }
            }
            arrayList.addAll(cardLines(jSONObject));
        }
    }

    static Set<String> keyToks(String str) {
        Set<String> strong = strong(Text.tokens(str));
        strong.addAll(strongSim(Text.simTokens(str)));
        return strong;
    }

    Set<String> specific(String str) {
        HashSet hashSet = new HashSet();
        String[] split = str.split("[^A-Za-z0-9\\-]+");
        int i = 0;
        int i2 = 0;
        for (String str2 : split) {
            if (str2.length() >= 2) {
                i++;
                if (Character.isUpperCase(str2.charAt(0))) {
                    i2++;
                }
            }
        }
        boolean z = i > 0 && ((double) i2) >= ((double) i) * 0.7d;
        for (String str3 : split) {
            if (str3.length() >= 2) {
                boolean z2 = str3.length() >= 2 && str3.equals(str3.toUpperCase(Locale.ROOT)) && str3.matches(".*[A-Z].*");
                boolean z3 = str3.matches(".*[A-Za-z].*") && str3.matches(".*\\d.*");
                boolean z4 = !z && Character.isUpperCase(str3.charAt(0));
                if (z2 || z3 || z4 || z) {
                    for (String str4 : Text.tokens(str3)) {
                        if (!PLAIN_NAME.contains(str4) && !WEAK.contains(str4)) {
                            hashSet.add(str4);
                        }
                    }
                }
            }
        }
        if (this.kb != null) {
            for (String str5 : this.kb.cardsIn(str)) {
                if (!Sentinel.GENERIC.contains(str5)) {
                    hashSet.add(str5);
                }
            }
        }
        return hashSet;
    }

    static final class Past {
        List<String> heads;
        String pent;
        List<Set<String>> sim;
        Set<String> spec;
        List<Set<String>> tok;

        Past() {
        }
    }

    Past info(JSONObject jSONObject) {
        Past past = this.pastInfo.get(jSONObject);
        if (past == null) {
            Past past2 = new Past();
            past2.heads = headStrs(jSONObject);
            past2.tok = new ArrayList();
            past2.sim = new ArrayList();
            past2.spec = new HashSet();
            for (String str : past2.heads) {
                past2.tok.add(Text.tokens(str));
                past2.sim.add(Text.simTokens(str));
                past2.spec.addAll(specific(str));
            }
            past2.pent = jSONObject.optString("pent", "");
            this.pastInfo.put(jSONObject, past2);
            return past2;
        }
        return past;
    }

    void indexPast(String str, JSONObject jSONObject) throws Exception {
        this.past = new ArrayList();
        this.pastIdx = new HashMap();
        this.lineIdx = new HashMap();
        this.pastInfo = new IdentityHashMap();
        if (!this.history.isEmpty()) {
            for (JSONObject jSONObject2 : this.history) {
                if (jSONObject2.optString("date").compareTo(str) < 0) {
                    this.past.add(jSONObject2);
                }
            }
        } else if (jSONObject != null && jSONObject.optJSONArray("topics") != null) {
            JSONArray jSONArray = jSONObject.getJSONArray("topics");
            for (int i = 0; i < jSONArray.length(); i++) {
                this.past.add(new JSONObject(jSONArray.getJSONObject(i).toString()).put("date", jSONObject.optString("date")));
            }
        }
        for (JSONObject jSONObject3 : this.past) {
            for (String str2 : info(jSONObject3).spec) {
                List<JSONObject> list = this.pastIdx.get(str2);
                if (list == null) {
                    list = new ArrayList<>();
                    this.pastIdx.put(str2, list);
                }
                list.add(jSONObject3);
            }
            HashSet<String> hashSet = new HashSet();
            Iterator<String> it = headStrs(jSONObject3).iterator();
            while (it.hasNext()) {
                hashSet.addAll(keyToks(it.next()));
            }
            for (String str3 : hashSet) {
                List<JSONObject> list2 = this.lineIdx.get(str3);
                if (list2 == null) {
                    list2 = new ArrayList<>();
                    this.lineIdx.put(str3, list2);
                }
                list2.add(jSONObject3);
            }
        }
    }

    List<JSONObject> earlierOf(JSONObject jSONObject) {
        Past past = new Past();
        past.heads = headStrs(jSONObject);
        past.tok = new ArrayList();
        past.sim = new ArrayList();
        past.spec = new HashSet();
        for (String str : past.heads) {
            past.tok.add(Text.tokens(str));
            past.sim.add(Text.simTokens(str));
            past.spec.addAll(specific(str));
        }
        past.pent = jSONObject.optString("pent", "");
        Set<JSONObject> newSetFromMap = Collections.newSetFromMap(new IdentityHashMap());
        Iterator<String> it = past.spec.iterator();
        while (it.hasNext()) {
            List<JSONObject> list = this.pastIdx.get(it.next());
            if (list != null) {
                newSetFromMap.addAll(list);
            }
        }
        ArrayList arrayList = new ArrayList();
        for (JSONObject jSONObject2 : newSetFromMap) {
            if (sameAcrossDays(info(jSONObject2), past)) {
                arrayList.add(jSONObject2);
            }
        }
        Collections.sort(arrayList, new Comparator<JSONObject>() { // from class: app.newsbreak.core.EditionBuilder.6
            @Override // java.util.Comparator
            public int compare(JSONObject jSONObject3, JSONObject jSONObject4) {
                return jSONObject4.optString("date").compareTo(jSONObject3.optString("date"));
            }
        });
        return arrayList;
    }

    boolean sameAcrossDays(Past past, Past past2) {
        int overlap;
        JSONObject entity;
        HashSet<String> hashSet = new HashSet(past.spec);
        hashSet.retainAll(past2.spec);
        if (hashSet.isEmpty()) {
            return false;
        }
        boolean z = past.pent.length() > 0 && past.pent.equals(past2.pent);
        HashSet hashSet2 = new HashSet();
        for (String str : hashSet) {
            if (str.startsWith("e:") && (entity = this.kb.entity(str.substring(2))) != null) {
                hashSet2.addAll(Text.tokens(entity.optString("name")));
            }
        }
        for (int i = 0; i < past.heads.size(); i++) {
            for (int i2 = 0; i2 < past2.heads.size(); i2++) {
                if (past.heads.get(i).length() != 0 && past2.heads.get(i2).length() != 0 && !otherPrize(past.tok.get(i), past2.tok.get(i2))) {
                    if (Text.sameLine(past.heads.get(i), past2.heads.get(i2))) {
                        return true;
                    }
                    Set<String> minus = minus(minus(past.tok.get(i), hashSet2), PLAIN_NAME);
                    Set<String> minus2 = minus(minus(past2.tok.get(i2), hashSet2), PLAIN_NAME);
                    if ((Text.jaccard(minus, minus2) >= 0.4d && Text.overlap(minus, minus2) >= 2) || (overlap = Text.overlap(minus(strongSim(minus(past.sim.get(i), hashSet2)), PLAIN_NAME), minus(strongSim(minus(past2.sim.get(i2), hashSet2)), PLAIN_NAME))) >= 3) {
                        return true;
                    }
                    if (overlap >= 2 && z) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    List<String> nearbyLines(JSONObject jSONObject) {
        Set newSetFromMap = Collections.newSetFromMap(new IdentityHashMap());
        Iterator<String> it = keyToks(jSONObject.optString("head")).iterator();
        while (it.hasNext()) {
            List<JSONObject> list = this.lineIdx.get(it.next());
            if (list != null) {
                newSetFromMap.addAll(list);
            }
        }
        return saidBefore(new ArrayList(newSetFromMap));
    }

    static List<String> saidBefore(List<JSONObject> list) {
        ArrayList arrayList = new ArrayList();
        for (JSONObject jSONObject : list) {
            arrayList.addAll(cardLines(jSONObject));
            JSONArray optJSONArray = jSONObject.optJSONArray("heads");
            for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
                arrayList.add(optJSONArray.optString(i));
            }
            JSONObject optJSONObject = jSONObject.optJSONObject("ai");
            JSONArray optJSONArray2 = optJSONObject == null ? null : optJSONObject.optJSONArray("b");
            for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
                arrayList.add(optJSONArray2.optString(i2));
            }
        }
        return arrayList;
    }

    static String headline(String str) {
        int i;
        String trim = str.trim();
        if (trim.length() > 120) {
            String[] strArr = {"; ", " — ", " – ", ", which ", ", while ", ", and ", ", "};
            int length = strArr.length;
            int i2 = 0;
            while (true) {
                if (i2 >= length) {
                    i = -1;
                    break;
                }
                i = trim.lastIndexOf(strArr[i2], 118);
                if (i >= 55) {
                    break;
                }
                i2++;
            }
            return i > 0 ? trim.substring(0, i).replaceAll("[,;:\\s]+$", "") : Text.cap(trim, 120);
        }
        return trim;
    }

    static boolean newFigure(String str, String str2) {
        Set<String> simTokens = Text.simTokens(str2);
        for (String str3 : Text.simTokens(str)) {
            if (str3.matches("\\d+(\\.\\d+)?%?") && str3.length() >= 2 && !simTokens.contains(str3)) {
                return true;
            }
        }
        return false;
    }

    static boolean isNew(String str, List<String> list, String str2, int i) {
        return str.trim().length() > 0 && !dupLine(str, list) && addsInfo(str, str2, i);
    }

    boolean continuing(JSONObject jSONObject, List<JSONObject> list) throws Exception {
        boolean z;
        List<String> saidBefore = saidBefore(list);
        String join = join(saidBefore);
        ArrayList arrayList = new ArrayList();
        JSONArray optJSONArray = jSONObject.optJSONArray("bullets");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            if (isNew(optJSONArray.optString(i), saidBefore, join, 3)) {
                arrayList.add(optJSONArray.optString(i));
            }
        }
        String optString = jSONObject.optString("head");
        boolean z2 = optString.trim().length() > 0 && addsInfo(optString, join, 2);
        Iterator<String> it = saidBefore.iterator();
        while (true) {
            z = z2;
            if (!it.hasNext()) {
                break;
            }
            String next = it.next();
            if (z && (Text.jaccard(Text.tokens(optString), Text.tokens(next)) >= 0.5d || optString.equalsIgnoreCase(next))) {
                z = false;
            }
            z2 = z;
        }
        if (arrayList.isEmpty() && (!z || !newFigure(optString, join))) {
            return false;
        }
        if (!z) {
            JSONArray optJSONArray2 = jSONObject.optJSONArray("heads");
            if (optJSONArray2 == null) {
                optJSONArray2 = new JSONArray();
                jSONObject.put("heads", optJSONArray2);
            }
            optJSONArray2.put(optString);
            String replaceAll = ((String) arrayList.remove(0)).replaceAll("[.;:,\\s]+$", "");
            JSONObject entity = jSONObject.has("pent") ? this.kb.entity(jSONObject.optString("pent")) : null;
            if (entity != null && Text.overlap(Text.tokens(replaceAll), Text.tokens(entity.optString("name") + " " + join(toList(entity.optJSONArray("match"))))) == 0 && this.kb.cardsIn(replaceAll).isEmpty()) {
                replaceAll = entity.optString("name") + ": " + replaceAll;
            }
            jSONObject.put("head", headline(replaceAll));
        }
        jSONObject.put("bullets", new JSONArray((Collection) arrayList));
        jSONObject.put("hlOnly", arrayList.isEmpty());
        jSONObject.put("cont", list.get(0).optString("date"));
        jSONObject.put("contMinor", (z ? 1 : 0) + arrayList.size() <= 1);
        JSONArray jSONArray = new JSONArray();
        HashSet hashSet = new HashSet();
        for (JSONObject jSONObject2 : list) {
            if (jSONArray.length() < 5 && hashSet.add(jSONObject2.optString("date"))) {
                JSONObject put = new JSONObject().put("d", jSONObject2.optString("date")).put("h", jSONObject2.optString("head"));
                JSONArray optJSONArray3 = jSONObject2.optJSONArray("bullets");
                if (optJSONArray3 != null && optJSONArray3.length() > 0) {
                    put.put("b", optJSONArray3.optString(0));
                }
                jSONArray.put(put);
            }
        }
        jSONObject.put("branch", jSONArray);
        jSONObject.put("_seen", new JSONArray((Collection) saidBefore));
        return true;
    }

    public JSONObject build(List<JSONObject> list, String str, JSONObject jSONObject, JSONObject jSONObject2, long j) throws Exception {
        int i;
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        JSONObject jSONObject3;
        JSONObject jSONObject4;
        boolean z6;
        JSONObject jSONObject5;
        boolean z7;
        JSONObject jSONObject6;
        long startOf = Ist.startOf(str);
        long j2 = startOf + Ist.DAY;
        List<JSONObject> inWindow = inWindow(list, startOf, j2);
        ArrayList arrayList = new ArrayList();
        for (JSONObject jSONObject7 : inWindow) {
            if (jSONObject7.optDouble("sc", 0.0d) >= 2.0d && !jSONObject7.has("noise")) {
                arrayList.add(jSONObject7);
            }
        }
        List<Cluster> cluster = cluster(arrayList);
        this.newsDay = 43200000 + startOf;
        for (Cluster cluster2 : cluster) {
            Double d = this.boost.get(cluster2.head().optString("th"));
            if (d != null) {
                cluster2.score += Math.min(d.doubleValue(), 1.5d);
            }
        }
        ArrayList<JSONObject> arrayList2 = new ArrayList();
        for (JSONObject jSONObject8 : inWindow(list, startOf - 864000000, 43200000 + j2)) {
            if (coachRelevant(jSONObject8)) {
                arrayList2.add(jSONObject8);
            }
        }
        Set newSetFromMap = Collections.newSetFromMap(new IdentityHashMap());
        Iterator<Cluster> it = cluster.iterator();
        while (it.hasNext()) {
            newSetFromMap.addAll(it.next().items);
        }
        for (JSONObject jSONObject9 : arrayList2) {
            if (!newSetFromMap.contains(jSONObject9) && prelimsTopic(jSONObject9, this.kb)) {
                Cluster cluster3 = null;
                double d2 = 0.0d;
                for (Cluster cluster4 : cluster) {
                    double coachMatch = coachMatch(jSONObject9, cluster4);
                    if (coachMatch <= d2) {
                        coachMatch = d2;
                        cluster4 = cluster3;
                    }
                    d2 = coachMatch;
                    cluster3 = cluster4;
                }
                if (cluster3 != null) {
                    cluster3.items.add(jSONObject9);
                }
            }
        }
        Iterator<Cluster> it2 = cluster.iterator();
        while (it2.hasNext()) {
            rank(it2.next());
        }
        indexPast(str, jSONObject2);
        for (Cluster cluster5 : cluster) {
            if (cluster5.score >= 4.0d && !earlierOf(new JSONObject().put("head", cluster5.head().optString("t")).put("theme", cluster5.head().optString("th"))).isEmpty()) {
                cluster5.cont = true;
                cluster5.score -= 1.5d;
            }
        }
        Collections.sort(cluster, new Comparator<Cluster>() { // from class: app.newsbreak.core.EditionBuilder.7
            @Override // java.util.Comparator
            public int compare(Cluster cluster6, Cluster cluster7) {
                return Double.compare(cluster7.score, cluster6.score);
            }
        });
        ArrayList arrayList3 = new ArrayList();
        if (jSONObject2 != null && jSONObject2.optJSONArray("topics") != null) {
            JSONArray jSONArray = jSONObject2.getJSONArray("topics");
            for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                arrayList3.add(jSONArray.getJSONObject(i2));
            }
        }
        boolean z8 = jSONObject != null && jSONObject.optLong("deliveredAt", 0L) > 0;
        ArrayList<JSONObject> arrayList4 = new ArrayList();
        ArrayList<JSONObject> arrayList5 = new ArrayList();
        if (jSONObject != null && jSONObject.optJSONArray("topics") != null) {
            JSONArray jSONArray2 = jSONObject.getJSONArray("topics");
            for (int i3 = 0; i3 < jSONArray2.length(); i3++) {
                arrayList5.add(jSONArray2.getJSONObject(i3));
            }
        }
        HashMap hashMap = new HashMap();
        HashSet hashSet = new HashSet();
        HashSet hashSet2 = new HashSet();
        int i4 = 0;
        Iterator it3 = arrayList5.iterator();
        while (true) {
            i = i4;
            if (!it3.hasNext()) {
                break;
            }
            i4 = ((JSONObject) it3.next()).optBoolean("amend") ? i + 1 : i;
        }
        if (z8) {
            for (JSONObject jSONObject10 : arrayList5) {
                if (jSONObject10.has("cont")) {
                    List<JSONObject> earlierOf = earlierOf(jSONObject10);
                    if (!earlierOf.isEmpty()) {
                        jSONObject10.put("_seen", new JSONArray((Collection) saidBefore(earlierOf)));
                    }
                }
                topicTok(jSONObject10);
                Iterator<Cluster> it4 = cluster.iterator();
                while (true) {
                    if (!it4.hasNext()) {
                        break;
                    }
                    Cluster next = it4.next();
                    if (!hashSet2.contains(next) && headSim(jSONObject10, next.tok) >= 0.34d) {
                        hashSet2.add(next);
                        mergeInto(jSONObject10, topic(next, null, null), j);
                        break;
                    }
                }
                jSONObject10.put("_old", true);
                arrayList4.add(jSONObject10);
                String optString = jSONObject10.optString("theme");
                hashMap.put(optString, Integer.valueOf((hashMap.containsKey(optString) ? ((Integer) hashMap.get(optString)).intValue() : 0) + 1));
            }
        }
        JSONArray jSONArray3 = new JSONArray();
        HashSet<Cluster> hashSet3 = new HashSet();
        int i5 = 0;
        for (Cluster cluster6 : cluster) {
            if (!hashSet2.contains(cluster6)) {
                if (!z8 && arrayList4.size() >= 12) {
                    break;
                }
                double d3 = z8 ? 8.0d : 6.0d;
                if (cluster6.score >= d3 || (cluster6.cont && cluster6.score + 1.5d >= d3)) {
                    if (z8) {
                        long j3 = 0;
                        for (JSONObject jSONObject11 : cluster6.items) {
                            j3 = Math.max(j3, jSONObject11.optLong("fs", jSONObject11.optLong("p")));
                        }
                        if (j3 <= jSONObject.optLong("deliveredAt")) {
                            continue;
                        }
                    }
                    if (z8 && i >= 4) {
                        JSONObject jSONObject12 = topic(cluster6, null, null);
                        JSONObject host = host(arrayList4, jSONObject12);
                        if (host != null) {
                            mergeInto(host, jSONObject12, j);
                            hashSet2.add(cluster6);
                        }
                    } else {
                        JSONObject jSONObject13 = topic(cluster6, null, null);
                        List<JSONObject> earlierOf2 = earlierOf(jSONObject13);
                        if (!earlierOf2.isEmpty()) {
                            if (z8 && jSONObject13.optBoolean("hlOnly")) {
                                continue;
                            }
                            if (!continuing(jSONObject13, earlierOf2)) {
                                jSONArray3.put(new JSONObject().put("t", cluster6.head().optString("t")).put("d", earlierOf2.get(0).optString("date")));
                                hashSet3.add(cluster6);
                                continue;
                            }
                        } else if (!this.past.isEmpty()) {
                            JSONArray optJSONArray = jSONObject13.optJSONArray("bullets");
                            List<String> nearbyLines = nearbyLines(jSONObject13);
                            if (optJSONArray != null && !nearbyLines.isEmpty()) {
                                JSONArray jSONArray4 = new JSONArray();
                                int i6 = 0;
                                while (true) {
                                    int i7 = i6;
                                    if (i7 >= optJSONArray.length()) {
                                        break;
                                    }
                                    Iterator<String> it5 = nearbyLines.iterator();
                                    while (true) {
                                        if (!it5.hasNext()) {
                                            z7 = false;
                                            break;
                                        }
                                        if (Text.sameLine(optJSONArray.optString(i7), it5.next())) {
                                            z7 = true;
                                            break;
                                        }
                                    }
                                    if (!z7) {
                                        jSONArray4.put(optJSONArray.optString(i7));
                                    }
                                    i6 = i7 + 1;
                                }
                                if (jSONArray4.length() != optJSONArray.length()) {
                                    jSONObject13.put("bullets", jSONArray4);
                                    if (jSONArray4.length() == 0) {
                                        jSONObject13.put("hlOnly", true);
                                    }
                                }
                            }
                        }
                        JSONObject host2 = host(arrayList4, jSONObject13);
                        if (host2 != null) {
                            mergeInto(host2, jSONObject13, z8 ? j : 0L);
                            hashSet2.add(cluster6);
                            i5++;
                        } else {
                            String optString2 = jSONObject13.optString("theme");
                            int intValue = hashMap.containsKey(optString2) ? ((Integer) hashMap.get(optString2)).intValue() : 0;
                            if (intValue < 3) {
                                hashMap.put(optString2, Integer.valueOf(intValue + 1));
                                Iterator it6 = arrayList5.iterator();
                                while (true) {
                                    if (!it6.hasNext()) {
                                        jSONObject6 = null;
                                        break;
                                    }
                                    jSONObject6 = (JSONObject) it6.next();
                                    if (!hashSet.contains(jSONObject6.optString("id")) && headSim(jSONObject6, cluster6.tok) >= 0.4d) {
                                        break;
                                    }
                                }
                                if (jSONObject6 != null) {
                                    hashSet.add(jSONObject6.optString("id"));
                                    jSONObject13.put("id", jSONObject6.optString("id"));
                                    jSONObject13.put("addedAt", jSONObject6.optLong("addedAt", j));
                                    if (jSONObject6.has("ai")) {
                                        jSONObject13.put("ai", jSONObject6.get("ai"));
                                    }
                                } else {
                                    jSONObject13.put("id", Text.hash(str + jSONObject13.optString("head")));
                                    jSONObject13.put("addedAt", j);
                                }
                                if (z8) {
                                    jSONObject13.put("amend", true);
                                    i++;
                                }
                                hashSet2.add(cluster6);
                                arrayList4.add(jSONObject13);
                            }
                        }
                    }
                }
            }
        }
        for (Cluster cluster7 : cluster) {
            if (cluster7.cont && !hashSet2.contains(cluster7) && !hashSet3.contains(cluster7) && cluster7.score + 1.5d >= 6.0d && jSONArray3.length() < 12) {
                JSONObject jSONObject14 = topic(cluster7, null, null);
                List<JSONObject> earlierOf3 = earlierOf(jSONObject14);
                if (!earlierOf3.isEmpty() && !continuing(jSONObject14, earlierOf3)) {
                    hashSet3.add(cluster7);
                }
                if (hashSet3.contains(cluster7)) {
                    jSONArray3.put(new JSONObject().put("t", cluster7.head().optString("t")).put("d", earlierOf3.get(0).optString("date")));
                }
            }
        }
        for (Cluster cluster8 : cluster) {
            if (!hashSet2.contains(cluster8) && !hashSet3.contains(cluster8) && cluster8.score >= 3.0d) {
                if (z8) {
                    long j4 = 0;
                    for (JSONObject jSONObject15 : cluster8.items) {
                        j4 = Math.max(j4, jSONObject15.optLong("fs", jSONObject15.optLong("p")));
                    }
                    if (j4 <= jSONObject.optLong("deliveredAt")) {
                        continue;
                    }
                }
                boolean z9 = false;
                Iterator<JSONObject> it7 = cluster8.items.iterator();
                while (true) {
                    z6 = z9;
                    if (!it7.hasNext()) {
                        break;
                    }
                    z9 = it7.next().has("noise") ? true : z6;
                }
                if (!z6) {
                    JSONObject jSONObject16 = topic(cluster8, null, null);
                    if (jSONObject16.has("pent")) {
                        Iterator it8 = arrayList4.iterator();
                        while (true) {
                            if (!it8.hasNext()) {
                                jSONObject5 = null;
                                break;
                            }
                            jSONObject5 = (JSONObject) it8.next();
                            if (sameBody(jSONObject5, jSONObject16)) {
                                break;
                            }
                        }
                        if (jSONObject5 != null) {
                            mergeInto(jSONObject5, jSONObject16, z8 ? j : 0L);
                            hashSet2.add(cluster8);
                        }
                    }
                }
            }
        }
        Set newSetFromMap2 = Collections.newSetFromMap(new IdentityHashMap());
        Iterator it9 = hashSet2.iterator();
        while (it9.hasNext()) {
            newSetFromMap2.addAll(((Cluster) it9.next()).items);
        }
        ArrayList arrayList6 = new ArrayList();
        HashSet hashSet4 = new HashSet();
        ArrayList<JSONObject> arrayList7 = new ArrayList();
        for (JSONObject jSONObject17 : inWindow(list, startOf - 864000000, j2)) {
            if (coachRelevant(jSONObject17)) {
                arrayList7.add(jSONObject17);
            }
        }
        Collections.sort(arrayList7, new Comparator<JSONObject>() { // from class: app.newsbreak.core.EditionBuilder.8
            @Override // java.util.Comparator
            public int compare(JSONObject jSONObject18, JSONObject jSONObject19) {
                return Boolean.compare(jSONObject19.has("cx"), jSONObject18.has("cx"));
            }
        });
        for (JSONObject jSONObject18 : arrayList7) {
            if (hashSet4.add(jSONObject18.optString("s") + "|" + jSONObject18.optString("t").toLowerCase().replaceAll("[^a-z0-9]+", " ").trim())) {
                arrayList6.add(jSONObject18);
            }
        }
        List<List<JSONObject>> register = register(arrayList6);
        JSONArray jSONArray5 = new JSONArray();
        JSONArray jSONArray6 = new JSONArray();
        ArrayList<JSONObject> arrayList8 = new ArrayList();
        HashMap hashMap2 = new HashMap();
        JSONArray optJSONArray2 = jSONObject != null ? jSONObject.optJSONArray("catchup") : null;
        for (int i8 = 0; optJSONArray2 != null && i8 < optJSONArray2.length(); i8++) {
            hashMap2.put(optJSONArray2.getJSONObject(i8).optString("id"), optJSONArray2.getJSONObject(i8));
        }
        int i9 = 0;
        int i10 = 0;
        int i11 = 0;
        int i12 = 0;
        int i13 = 0;
        int i14 = 0;
        for (List<JSONObject> list2 : register) {
            i10++;
            Iterator<JSONObject> it10 = list2.iterator();
            while (true) {
                if (!it10.hasNext()) {
                    z3 = false;
                    break;
                }
                if (prelimsTopic(it10.next(), this.kb)) {
                    z3 = true;
                    break;
                }
            }
            if (z3) {
                i11++;
                Iterator<JSONObject> it11 = list2.iterator();
                while (true) {
                    if (!it11.hasNext()) {
                        z4 = false;
                        break;
                    }
                    if (coveredBy(it11.next(), newSetFromMap2, arrayList4)) {
                        z4 = true;
                        break;
                    }
                }
                if (z4) {
                    i13++;
                } else {
                    String str2 = null;
                    Iterator<JSONObject> it12 = list2.iterator();
                    while (it12.hasNext() && (str2 = coveredEarlier(it12.next(), str)) == null) {
                    }
                    if (str2 != null) {
                        i12++;
                    } else {
                        boolean z10 = false;
                        for (Cluster cluster9 : hashSet3) {
                            Iterator<JSONObject> it13 = list2.iterator();
                            boolean z11 = z10;
                            while (it13.hasNext()) {
                                if (cluster9.items.contains(it13.next())) {
                                    z11 = true;
                                }
                            }
                            z10 = z11;
                        }
                        if (z10) {
                            i12++;
                        } else {
                            Iterator<JSONObject> it14 = list2.iterator();
                            while (true) {
                                if (!it14.hasNext()) {
                                    z5 = false;
                                    break;
                                }
                                if (readable(it14.next())) {
                                    z5 = true;
                                    break;
                                }
                            }
                            Collections.sort(list2, new Comparator<JSONObject>() { // from class: app.newsbreak.core.EditionBuilder.9
                                @Override // java.util.Comparator
                                public int compare(JSONObject jSONObject19, JSONObject jSONObject20) {
                                    return Long.compare(jSONObject19.optLong("p"), jSONObject20.optLong("p"));
                                }
                            });
                            JSONObject jSONObject19 = list2.get(0);
                            Iterator<JSONObject> it15 = list2.iterator();
                            while (true) {
                                if (!it15.hasNext()) {
                                    jSONObject3 = jSONObject19;
                                    break;
                                }
                                JSONObject next2 = it15.next();
                                if (next2.has("cx")) {
                                    jSONObject3 = next2;
                                    break;
                                }
                            }
                            if (!z5) {
                                jSONArray6.put(new JSONObject().put("s", jSONObject3.optString("s")).put("t", jSONObject3.optString("t")).put("d", Ist.dateKey(jSONObject3.optLong("p"))).put("id", jSONObject3.optString("id")).put("why", "only a headline was available; retried next sync"));
                            } else {
                                if (jSONObject3.optString("th").length() > 0) {
                                    jSONArray5.put(jSONObject3.optString("th"));
                                }
                                if (!z8) {
                                    Cluster cluster10 = null;
                                    for (Cluster cluster11 : cluster) {
                                        if (!hashSet2.contains(cluster11) && !hashSet3.contains(cluster11)) {
                                            for (JSONObject x16 : list2) {
                                                if (cluster11.items.contains(x16)) {
                                                    cluster10 = cluster11;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (cluster10 == null) {
                                        cluster10 = new Cluster();
                                        cluster10.tok = Text.tokens(jSONObject3.optString("t"));
                                        cluster10.score = jSONObject3.optDouble("sc");
                                    }
                                    for (JSONObject jSONObject20 : list2) {
                                        if (!cluster10.items.contains(jSONObject20)) {
                                            cluster10.items.add(jSONObject20);
                                            cluster10.all.addAll(Text.tokens(jSONObject20.optString("t")));
                                        }
                                    }
                                    JSONObject jSONObject21 = topic(cluster10, null, null);
                                    JSONObject host3 = host(arrayList4, jSONObject21);
                                    if (host3 != null) {
                                        mergeInto(host3, jSONObject21, 0L);
                                        hashSet2.add(cluster10);
                                        newSetFromMap2.addAll(cluster10.items);
                                        i13++;
                                    } else if (Ist.dateKey(list2.get(list2.size() - 1).optLong("p")).equals(str) && i9 < 3 && arrayList4.size() < 15) {
                                        jSONObject21.put("gap", jSONObject3.optString("s"));
                                        Iterator it17 = arrayList5.iterator();
                                        while (true) {
                                            if (!it17.hasNext()) {
                                                jSONObject4 = null;
                                                break;
                                            }
                                            JSONObject jSONObject22 = (JSONObject) it17.next();
                                            if (!hashSet.contains(jSONObject22.optString("id")) && headSim(jSONObject22, cluster10.tok) >= 0.4d) {
                                                jSONObject4 = jSONObject22;
                                                break;
                                            }
                                        }
                                        if (jSONObject4 != null) {
                                            hashSet.add(jSONObject4.optString("id"));
                                        }
                                        jSONObject21.put("id", jSONObject4 != null ? jSONObject4.optString("id") : Text.hash(str + jSONObject21.optString("head")));
                                        jSONObject21.put("addedAt", jSONObject4 != null ? jSONObject4.optLong("addedAt", j) : j);
                                        if (jSONObject4 != null && jSONObject4.has("ai")) {
                                            jSONObject21.put("ai", jSONObject4.get("ai"));
                                        }
                                        hashSet2.add(cluster10);
                                        newSetFromMap2.addAll(cluster10.items);
                                        arrayList4.add(jSONObject21);
                                        i13++;
                                        i9++;
                                    } else {
                                        JSONArray jSONArray7 = new JSONArray();
                                        HashSet hashSet5 = new HashSet();
                                        for (JSONObject jSONObject23 : list2) {
                                            if (hashSet5.add(jSONObject23.optString("s") + "|" + Ist.dateKey(jSONObject23.optLong("p")))) {
                                                jSONArray7.put(new JSONObject().put("s", jSONObject23.optString("s")).put("d", Ist.dateKey(jSONObject23.optLong("p"))));
                                            }
                                        }
                                        jSONObject21.put("cu", jSONArray7);
                                        jSONObject21.put("id", Text.hash("cu|" + list2.get(0).optString("id")));
                                        JSONObject jSONObject24 = (JSONObject) hashMap2.get(jSONObject21.optString("id"));
                                        if (jSONObject24 != null && jSONObject24.has("ai")) {
                                            jSONObject21.put("ai", jSONObject24.get("ai"));
                                        }
                                        JSONObject host4 = host(arrayList8, jSONObject21);
                                        if (host4 != null) {
                                            mergeInto(host4, jSONObject21, 0L);
                                            JSONArray optJSONArray3 = host4.optJSONArray("cu");
                                            for (int i15 = 0; i15 < jSONArray7.length(); i15++) {
                                                optJSONArray3.put(jSONArray7.get(i15));
                                            }
                                        } else {
                                            newSetFromMap2.addAll(cluster10.items);
                                            arrayList8.add(jSONObject21);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                i14++;
            }
        }
        Collections.sort(arrayList8, new Comparator<JSONObject>() { // from class: app.newsbreak.core.EditionBuilder.10
            @Override // java.util.Comparator
            public int compare(JSONObject jSONObject25, JSONObject jSONObject26) {
                int gsRank = EditionBuilder.gsRank(jSONObject25.optString("gs")) - EditionBuilder.gsRank(jSONObject26.optString("gs"));
                if (gsRank == 0) {
                    int tierRank = EditionBuilder.tierRank(jSONObject25.optString("tier")) - EditionBuilder.tierRank(jSONObject26.optString("tier"));
                    if (tierRank == 0) {
                        int compare = Boolean.compare(jSONObject26.optBoolean("rising"), jSONObject25.optBoolean("rising"));
                        if (compare == 0) {
                            int sites = EditionBuilder.sites(jSONObject26) - EditionBuilder.sites(jSONObject25);
                            return sites == 0 ? EditionBuilder.lastCu(jSONObject26).compareTo(EditionBuilder.lastCu(jSONObject25)) : sites;
                        }
                        return compare;
                    }
                    return tierRank;
                }
                return gsRank;
            }
        });
        while (arrayList8.size() > 80) {
            JSONObject jSONObject25 = (JSONObject) arrayList8.remove(arrayList8.size() - 1);
            jSONArray6.put(new JSONObject().put("s", jSONObject25.getJSONArray("cu").getJSONObject(0).optString("s")).put("t", jSONObject25.optString("head")).put("d", lastCu(jSONObject25)).put("why", "catch-up list full"));
        }
        for (JSONObject jSONObject26 : arrayList8) {
            JSONObject optJSONObject = jSONObject26.optJSONObject("ai");
            if (optJSONObject != null) {
                if (Ai.sig(jSONObject26).equals(optJSONObject.optString("sig"))) {
                    optJSONObject.remove("stale");
                } else {
                    optJSONObject.put("stale", true);
                }
            }
        }
        dedupeAcross(arrayList4, true);
        if (!z8) {
            final IdentityHashMap identityHashMap = new IdentityHashMap();
            for (JSONObject jSONObject27 : arrayList4) {
                identityHashMap.put(jSONObject27, Double.valueOf((jSONObject27.optDouble("score", 0.0d) - (jSONObject27.has("cont") ? jSONObject27.optBoolean("contMinor") ? COACH_BOOST : 1.0d : 0.0d)) - (jSONObject27.has("gap") ? 0.01d : 0.0d)));
            }
            ArrayList arrayList9 = new ArrayList();
            ArrayList arrayList10 = new ArrayList();
            for (JSONObject jSONObject28 : arrayList4) {
                (jSONObject28.has("gap") ? arrayList10 : arrayList9).add(jSONObject28);
            }
            Collections.sort(arrayList9, new Comparator<JSONObject>() { // from class: app.newsbreak.core.EditionBuilder.11
                @Override // java.util.Comparator
                public int compare(JSONObject jSONObject29, JSONObject jSONObject30) {
                    return Double.compare(((Double) identityHashMap.get(jSONObject30)).doubleValue(), ((Double) identityHashMap.get(jSONObject29)).doubleValue());
                }
            });
            arrayList4.clear();
            arrayList4.addAll(arrayList9);
            arrayList4.addAll(arrayList10);
        }
        for (JSONObject jSONObject29 : arrayList4) {
            jSONObject29.remove("_old");
            jSONObject29.remove("_seen");
            JSONObject optJSONObject2 = jSONObject29.optJSONObject("ai");
            if (optJSONObject2 != null) {
                if (Ai.sig(jSONObject29).equals(optJSONObject2.optString("sig"))) {
                    optJSONObject2.remove("stale");
                } else {
                    optJSONObject2.put("stale", true);
                }
            }
        }
        JSONArray jSONArray8 = new JSONArray();
        ArrayList<JSONObject> arrayList11 = new ArrayList();
        for (JSONObject jSONObject30 : inWindow) {
            if ("PIB".equals(jSONObject30.optString("s")) && jSONObject30.optDouble("sc", 0.0d) >= 3.0d && !jSONObject30.has("noise")) {
                arrayList11.add(jSONObject30);
            }
        }
        Collections.sort(arrayList11, BY_SCORE);
        ArrayList arrayList12 = new ArrayList();
        for (JSONObject jSONObject31 : arrayList11) {
            if (jSONArray8.length() >= 15) {
                break;
            }
            Set<String> set = Text.tokens(jSONObject31.optString("t"));
            Iterator it18 = arrayList12.iterator();
            while (true) {
                if (!it18.hasNext()) {
                    z2 = false;
                    break;
                }
                if (Text.jaccard(set, (Set) it18.next()) >= 0.6d) {
                    z2 = true;
                    break;
                }
            }
            if (!z2) {
                arrayList12.add(set);
                JSONObject jSONObject32 = new JSONObject();
                jSONObject32.put("t", Text.note(jSONObject31.optString("t"), 150));
                jSONObject32.put("u", jSONObject31.optString("u"));
                jSONObject32.put("gs", jSONObject31.optString("gs"));
                jSONArray8.put(jSONObject32);
            }
        }
        JSONArray jSONArray9 = new JSONArray();
        ArrayList<JSONObject> arrayList13 = new ArrayList();
        for (JSONObject jSONObject33 : inWindow) {
            if (isCoaching(jSONObject33)) {
                arrayList13.add(jSONObject33);
            }
        }
        if (!arrayList13.isEmpty()) {
            z = false;
        } else {
            z = true;
            for (JSONObject jSONObject34 : inWindow(list, startOf - 259200000, startOf)) {
                if (isCoaching(jSONObject34) && jSONObject34.optDouble("sc", 0.0d) >= 6.0d && !jSONObject34.has("noise")) {
                    arrayList13.add(jSONObject34);
                }
            }
        }
        Collections.sort(arrayList13, BY_SCORE);
        for (JSONObject jSONObject35 : arrayList13) {
            if (!promo(jSONObject35.optString("t"))) {
                Text.tokens(jSONObject35.optString("t"));
                boolean coveredBy = coveredBy(jSONObject35, newSetFromMap2, arrayList4);
                boolean coachRelevant = coachRelevant(jSONObject35);
                if (jSONArray9.length() < 12) {
                    JSONObject jSONObject36 = new JSONObject();
                    jSONObject36.put("s", jSONObject35.optString("s"));
                    jSONObject36.put("t", jSONObject35.optString("t"));
                    jSONObject36.put("u", jSONObject35.optString("u"));
                    jSONObject36.put("covered", coveredBy);
                    jSONObject36.put("date", Ist.dateKey(jSONObject35.optLong("p")));
                    jSONObject36.put("relevant", coachRelevant);
                    jSONArray9.put(jSONObject36);
                }
            }
        }
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        Iterator<JSONObject> it19 = inWindow.iterator();
        while (it19.hasNext()) {
            linkedHashSet.add(it19.next().optString("s"));
        }
        JSONObject jSONObject37 = new JSONObject();
        jSONObject37.put("date", str);
        jSONObject37.put("heading", Ist.heading(str));
        jSONObject37.put("window", Ist.shortDay(str) + " news (00:00–23:59 IST). Continuing stories carry only new developments; single-source items flagged.");
        JSONArray jSONArray10 = new JSONArray();
        Iterator it20 = arrayList4.iterator();
        while (it20.hasNext()) {
            jSONArray10.put((JSONObject) it20.next());
        }
        jSONObject37.put("topics", jSONArray10);
        jSONObject37.put("pib", jSONArray8);
        jSONObject37.put("coach", jSONArray9);
        jSONObject37.put("coachBackfill", z);
        if (i5 > 0) {
            jSONObject37.put("merged", i5);
        }
        if (z8 && jSONObject.has("coverage")) {
            jSONObject37.put("coverage", jSONObject.get("coverage"));
            if (jSONObject.has("catchup")) {
                jSONObject37.put("catchup", jSONObject.get("catchup"));
            }
            if (jSONObject.has("missed")) {
                jSONObject37.put("missed", jSONObject.get("missed"));
            }
        } else {
            if (i10 > 0) {
                jSONObject37.put("coverage", new JSONObject().put("pages", arrayList6.size()).put("topics", i10).put("prelims", i11).put("mains", i14).put("today", i13).put("earlier", i12).put("catchup", arrayList8.size()).put("missed", jSONArray6.length()).put("gaps", i9).put("covered", i13 + i12 + arrayList8.size()).put("of", i11).put("days", 10));
            }
            JSONArray jSONArray11 = new JSONArray();
            Iterator it21 = arrayList8.iterator();
            while (it21.hasNext()) {
                jSONArray11.put((JSONObject) it21.next());
            }
            if (jSONArray11.length() > 0) {
                jSONObject37.put("catchup", jSONArray11);
            }
            if (jSONArray6.length() > 0) {
                jSONObject37.put("missed", jSONArray6);
            }
        }
        if (jSONArray5.length() > 0) {
            jSONObject37.put("gapThemes", jSONArray5);
        }
        if (jSONArray3.length() > 0) {
            jSONObject37.put("held", jSONArray3);
        }
        jSONObject37.put("itemsInWindow", inWindow.size());
        jSONObject37.put("sources", new JSONArray((Collection) new ArrayList(linkedHashSet)));
        jSONObject37.put("builtAt", j);
        jSONObject37.put("firstBuiltAt", jSONObject != null ? jSONObject.optLong("firstBuiltAt", j) : j);
        long optLong = jSONObject != null ? jSONObject.optLong("deliveredAt", 0L) : 0L;
        if (optLong == 0 && j >= this.deliverOffsetMs + j2 && arrayList4.size() > 0) {
            optLong = j;
        }
        jSONObject37.put("deliveredAt", optLong);
        jSONObject37.put("status", optLong > 0 ? "final" : j < j2 ? "live" : "compiling");
        if (jSONObject != null) {
            for (String str3 : new String[]{"ai", "aiTries", "aiError", "patternNotes", "notifyPending", "aiRuns"}) {
                if (jSONObject.has(str3)) {
                    jSONObject37.put(str3, jSONObject.get(str3));
                }
            }
        }
        Sentinel.cleanEdition(jSONObject37, this.kb);
        for (String str4 : new String[]{"topics", "catchup"}) {
            JSONArray optJSONArray4 = jSONObject37.optJSONArray(str4);
            for (int i16 = 0; optJSONArray4 != null && i16 < optJSONArray4.length(); i16++) {
                JSONObject optJSONObject3 = optJSONArray4.optJSONObject(i16);
                if (optJSONObject3 != null) {
                    optJSONObject3.put("needAi", !selfSufficient(optJSONObject3));
                }
            }
        }
        jSONObject37.put("check", checklist(jSONObject37));
        return jSONObject37;
    }

    public JSONArray checklist(JSONObject jSONObject) throws Exception {
        JSONArray jSONArray = jSONObject.getJSONArray("topics");
        JSONArray jSONArray2 = new JSONArray();
        int length = jSONArray.length();
        add(jSONArray2, 1, true, "Date heading first; no preamble");
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            if (jSONArray.getJSONObject(i2).has("gap")) {
                i++;
            }
        }
        add(jSONArray2, 2, length - i <= 12, (length - i >= 8 ? (length - i) + " topics" : (length - i) + " topics — slow news day, no forcing") + (i > 0 ? " + " + i + " coverage gap-fill" : "") + (jSONObject.optInt("merged") > 0 ? " · " + jSONObject.optInt("merged") + " repeat report(s) folded into existing cards" : ""));
        boolean z = true;
        boolean z2 = true;
        boolean z3 = false;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        while (i7 < length) {
            JSONObject jSONObject2 = jSONArray.getJSONObject(i7);
            if (jSONObject2.optString("gs").length() == 0 || len(jSONObject2, "srcs") + len(jSONObject2, "coach") == 0) {
                z = false;
            }
            if (!jSONObject2.optString("pyq").startsWith("PYQ")) {
                z2 = false;
            }
            JSONArray optJSONArray = jSONObject2.optJSONArray("ents");
            if (optJSONArray == null) {
                optJSONArray = new JSONArray();
            }
            for (int i9 = 0; i9 < optJSONArray.length(); i9++) {
                JSONObject entity = this.kb.entity(optJSONArray.optString(i9));
                if (entity != null) {
                    String optString = entity.optString("kind");
                    if ("International".equals(optString)) {
                        i3++;
                    } else if ("Scheme".equals(optString)) {
                        i5++;
                    } else if ("Protected area".equals(optString) || "Species".equals(optString)) {
                        i6++;
                    } else {
                        i4++;
                    }
                }
            }
            int len = i8 + len(jSONObject2, "maps");
            String lowerCase = (jSONObject2.optString("head") + " " + jSONObject2.optJSONArray("bullets")).toLowerCase();
            boolean z4 = false;
            for (int i10 = 0; i10 < optJSONArray.length(); i10++) {
                JSONObject entity2 = this.kb.entity(optJSONArray.optString(i10));
                if (entity2 != null && "Scheme".equals(entity2.optString("kind"))) {
                    z4 = true;
                }
            }
            boolean z5 = ((lowerCase.contains("scheme") || lowerCase.contains("yojana")) && !z4) ? true : z3;
            i7++;
            i8 = len;
            z3 = z5;
        }
        add(jSONArray2, 3, z, "GS tag + ultra-short source on every topic");
        int i11 = 0;
        for (int i12 = 0; i12 < length; i12++) {
            if (jSONArray.getJSONObject(i12).has("pyqEv")) {
                i11++;
            }
        }
        add(jSONArray2, 4, z2, "PYQ line on every topic — " + i11 + " backed by matching CSE PYQs, rest as PYQ THEME");
        add(jSONArray2, 5, true, i3 + " international-organisation profile(s) attached from verified KB");
        add(jSONArray2, 6, true, i4 + " Indian-body profile(s)" + (i6 > 0 ? " + " + i6 + " protected-area/species card(s)" : "") + " attached from verified KB");
        add(jSONArray2, 7, !z3, z3 ? "Scheme in news not in the verified scheme list — use AI edition for its components (PM-AJAY pattern)" : i5 > 0 ? i5 + " scheme profile(s) in PM-AJAY pattern from verified KB" : "No scheme topics");
        JSONArray jSONArray3 = jSONObject.getJSONArray("coach");
        JSONObject optJSONObject = jSONObject.optJSONObject("coverage");
        int length2 = jSONObject.optJSONArray("missed") == null ? 0 : jSONObject.getJSONArray("missed").length();
        add(jSONArray2, 8, length2 == 0, optJSONObject == null ? jSONArray3.length() > 0 ? "Coaching cross-check: " + jSONArray3.length() + " item(s) scanned" : "No coaching pages in the last 7 days yet" : "Coaching (7-day look-back): " + optJSONObject.optInt("covered") + " of " + optJSONObject.optInt("of") + " topics covered" + (optJSONObject.optInt("earlier") > 0 ? " (" + optJSONObject.optInt("earlier") + " in earlier editions)" : "") + (length2 > 0 ? "; " + length2 + " missed" : ""));
        add(jSONArray2, 9, true, i8 + " map line(s), PYQ map buckets only");
        int i13 = 0;
        int i14 = 0;
        for (int i15 = 0; i15 < length; i15++) {
            if (jSONArray.getJSONObject(i15).has("conflicts")) {
                i13++;
            }
            if (jSONArray.getJSONObject(i15).optInt("corrob") >= 2) {
                i14++;
            }
        }
        add(jSONArray2, 10, true, i14 + "/" + length + " topics corroborated by ≥2 sources; single-source flagged" + (i13 > 0 ? "; " + i13 + " figure conflict(s) flagged" : ""));
        add(jSONArray2, 11, jSONObject.getJSONArray("pib").length() > 0, jSONObject.getJSONArray("pib").length() > 0 ? "PIB filter present; fact bank appended; archive saved" : "No exam-relevant PIB release in window; fact bank + archive saved");
        return jSONArray2;
    }

    static int len(JSONObject jSONObject, String str) {
        JSONArray optJSONArray = jSONObject.optJSONArray(str);
        if (optJSONArray == null) {
            return 0;
        }
        return optJSONArray.length();
    }

    static void add(JSONArray jSONArray, int i, boolean z, String str) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("n", i);
        jSONObject.put("ok", z);
        jSONObject.put("msg", str);
        jSONArray.put(jSONObject);
    }

    public JSONObject live(List<JSONObject> list, String str, long j) throws Exception {
        long startOf = Ist.startOf(str);
        List<JSONObject> inWindow = inWindow(list, startOf, Ist.DAY + startOf);
        ArrayList arrayList = new ArrayList();
        for (JSONObject jSONObject : inWindow) {
            if (jSONObject.optDouble("sc", 0.0d) >= 2.0d && !jSONObject.has("noise")) {
                arrayList.add(jSONObject);
            }
        }
        List<Cluster> cluster = cluster(arrayList);
        ArrayList arrayList2 = new ArrayList();
        for (Cluster cluster2 : cluster) {
            if (arrayList2.size() >= 40 || cluster2.score < 4.0d) {
                break;
            }
            JSONObject jSONObject2 = topic(cluster2, null, null);
            JSONObject host = host(arrayList2, jSONObject2);
            if (host != null) {
                mergeInto(host, jSONObject2, 0L);
            } else {
                jSONObject2.put("id", Text.hash("live" + jSONObject2.optString("head")));
                arrayList2.add(jSONObject2);
            }
        }
        dedupeAcross(arrayList2, false);
        JSONArray jSONArray = new JSONArray();
        Iterator it = arrayList2.iterator();
        while (it.hasNext()) {
            jSONArray.put((JSONObject) it.next());
        }
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("date", str);
        jSONObject3.put("heading", Ist.heading(str));
        jSONObject3.put("topics", jSONArray);
        jSONObject3.put("itemsInWindow", inWindow.size());
        jSONObject3.put("builtAt", j);
        Sentinel.cleanEdition(jSONObject3, this.kb);
        return jSONObject3;
    }

    public List<JSONObject> candidates(List<JSONObject> list, String str) throws Exception {
        long startOf = Ist.startOf(str);
        ArrayList<JSONObject> arrayList = new ArrayList<JSONObject>();
        for (JSONObject jSONObject : inWindow(list, startOf, Ist.DAY + startOf)) {
            if (jSONObject.optDouble("sc", 0.0d) >= 2.0d && !jSONObject.has("noise")) {
                arrayList.add(jSONObject);
            }
        }
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        for (Cluster cluster : cluster(arrayList)) {
            if (arrayList2.size() >= 45 || cluster.score < 3.0d) {
                break;
            }
            JSONObject jSONObject2 = topic(cluster, null, null);
            JSONObject host = host(arrayList3, jSONObject2);
            if (host != null) {
                mergeInto(host, jSONObject2, 0L);
                JSONObject jSONObject3 = (JSONObject) arrayList2.get(arrayList3.indexOf(host));
                JSONArray jSONArray = jSONObject3.getJSONArray("items");
                ArrayList<JSONObject> arrayList4 = new ArrayList(cluster.items);
                Collections.sort(arrayList4, BY_SCORE);
                for (JSONObject jSONObject4 : arrayList4) {
                    if (jSONArray.length() < 8) {
                        jSONArray.put(new JSONObject().put("s", jSONObject4.optString("s")).put("t", jSONObject4.optString("t")).put("d", jSONObject4.optString("d")).put("u", jSONObject4.optString("u")));
                    }
                }
                jSONObject3.put("head", jSONObject3.optString("head").contains(" ＋ ") ? jSONObject3.optString("head") : jSONObject3.optString("head") + " ＋ related: " + jSONObject2.optString("head"));
            } else {
                arrayList3.add(jSONObject2);
                JSONObject jSONObject5 = new JSONObject();
                jSONObject5.put("head", jSONObject2.optString("head"));
                StringBuilder sb = new StringBuilder();
                JSONArray jSONArray2 = jSONObject2.getJSONArray("srcs");
                JSONArray jSONArray3 = jSONObject2.getJSONArray("coach");
                int i = 0;
                while (i < jSONArray2.length()) {
                    sb.append(i > 0 ? "/" : "").append(jSONArray2.optString(i));
                    i++;
                }
                for (int i2 = 0; i2 < jSONArray3.length(); i2++) {
                    sb.append(sb.length() > 0 ? " " : "").append("(").append(jSONArray3.optString(i2)).append(")");
                }
                jSONObject5.put("srcs", sb.toString());
                jSONObject5.put("theme", jSONObject2.optString("themeLabel") + " (" + jSONObject2.optString("tier") + ")");
                JSONArray jSONArray4 = new JSONArray();
                ArrayList<JSONObject> arrayList5 = new ArrayList(cluster.items);
                Collections.sort(arrayList5, BY_SCORE);
                for (JSONObject jSONObject6 : arrayList5) {
                    jSONArray4.put(new JSONObject().put("s", jSONObject6.optString("s")).put("t", jSONObject6.optString("t")).put("d", jSONObject6.optString("d")).put("u", jSONObject6.optString("u")));
                }
                jSONObject5.put("items", jSONArray4);
                StringBuilder sb2 = new StringBuilder();
                if (jSONObject2.optJSONArray("pyqEv") != null && this.pyq != null) {
                    LinkedHashSet linkedHashSet = new LinkedHashSet();
                    JSONArray jSONArray5 = jSONObject2.getJSONArray("ents");
                    JSONArray jSONArray6 = jSONObject2.getJSONArray("maps");
                    for (int i3 = 0; i3 < jSONArray5.length(); i3++) {
                        linkedHashSet.add("e:" + jSONArray5.optString(i3));
                    }
                    for (int i4 = 0; i4 < jSONArray6.length(); i4++) {
                        linkedHashSet.add("m:" + jSONArray6.optString(i4));
                    }
                    sb2.append(Pyq.evidence(this.pyq.matchTopic(Foreign.mask(jSONObject2.optString("head")), Foreign.mask(join(toList(jSONObject2.getJSONArray("bullets")))), jSONObject2.optString("gs"), linkedHashSet, 4)));
                }
                jSONObject5.put("pyqEvidence", sb2.toString());
                arrayList2.add(jSONObject5);
            }
        }
        return arrayList2;
    }

    public List<String> coaching(List<JSONObject> list, String str) {
        boolean z;
        long startOf = Ist.startOf(str);
        ArrayList<JSONObject> arrayList = new ArrayList();
        for (JSONObject jSONObject : inWindow(list, startOf, Ist.DAY + startOf)) {
            if (isCoaching(jSONObject) && !jSONObject.has("noise") && !promo(jSONObject.optString("t")) && jSONObject.optDouble("sc", 0.0d) >= 3.0d) {
                arrayList.add(jSONObject);
            }
        }
        Collections.sort(arrayList, BY_SCORE);
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        for (JSONObject jSONObject2 : arrayList) {
            Set<String> set = Text.tokens(jSONObject2.optString("t"));
            Iterator it = arrayList3.iterator();
            while (true) {
                if (!it.hasNext()) {
                    z = false;
                    break;
                }
                if (Text.jaccard((Set) it.next(), set) >= 0.5d) {
                    z = true;
                    break;
                }
            }
            if (!z) {
                arrayList3.add(set);
                arrayList2.add("[" + jSONObject2.optString("s") + "] " + jSONObject2.optString("t"));
            }
        }
        return arrayList2;
    }

    static List<String> toList(JSONArray jSONArray) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; jSONArray != null && i < jSONArray.length(); i++) {
            arrayList.add(jSONArray.optString(i));
        }
        return arrayList;
    }

    public static boolean selfSufficient(JSONObject jSONObject) {
        if (jSONObject == null) {
            return false;
        }
        if (jSONObject.has("cont")) {
            return true;
        }
        if (jSONObject.optBoolean("hlOnly")) {
            return false;
        }
        JSONArray optJSONArray = jSONObject.optJSONArray("bullets");
        int i = 0;
        for (int i2 = 0; optJSONArray != null && i2 < optJSONArray.length(); i2++) {
            if (optJSONArray.optString(i2).length() >= 35) {
                i++;
            }
        }
        JSONArray optJSONArray2 = jSONObject.optJSONArray("also");
        if (optJSONArray2 != null && optJSONArray2.length() > 0 && i == 1) {
            i = 2;
        }
        if (i < 2) {
            return false;
        }
        JSONObject optJSONObject = jSONObject.optJSONObject("bg");
        return !(optJSONObject == null || optJSONObject.optJSONArray("pts") == null || optJSONObject.optJSONArray("pts").length() <= 0) || (len(jSONObject, "ents") + len(jSONObject, "cons")) + len(jSONObject, "maps") > 0 || jSONObject.has("cont");
    }

    public static List<String> facts(JSONObject jSONObject) {
        JSONArray jSONArray;
        ArrayList arrayList = new ArrayList();
        JSONArray optJSONArray = jSONObject.optJSONArray("topics");
        if (optJSONArray == null) {
            return arrayList;
        }
        String str = "[" + Ist.tag(jSONObject.optString("date")) + "] ";
        JSONArray optJSONArray2 = jSONObject.optJSONArray("catchup");
        if (optJSONArray2 != null) {
            JSONArray jSONArray2 = new JSONArray();
            for (int i = 0; i < optJSONArray.length(); i++) {
                jSONArray2.put(optJSONArray.opt(i));
            }
            for (int i2 = 0; i2 < optJSONArray2.length(); i2++) {
                jSONArray2.put(optJSONArray2.opt(i2));
            }
            jSONArray = jSONArray2;
        } else {
            jSONArray = optJSONArray;
        }
        for (int i3 = 0; i3 < jSONArray.length(); i3++) {
            JSONObject optJSONObject = jSONArray.optJSONObject(i3);
            JSONArray optJSONArray3 = optJSONObject.optJSONArray("bullets");
            int i4 = 0;
            for (int i5 = 0; optJSONArray3 != null && i5 < optJSONArray3.length() && i4 < 2; i5++) {
                String optString = optJSONArray3.optString(i5);
                if (Text.hasFigure(optString)) {
                    arrayList.add(str + Text.cap(optJSONObject.optString("head"), 70) + " — " + optString);
                    i4++;
                }
            }
            JSONArray optJSONArray4 = optJSONObject.optJSONArray("also");
            for (int i6 = 0; optJSONArray4 != null && i6 < optJSONArray4.length(); i6++) {
                JSONObject optJSONObject2 = optJSONArray4.optJSONObject(i6);
                JSONArray optJSONArray5 = optJSONObject2.optJSONArray("bullets");
                int i7 = 0;
                while (true) {
                    if (optJSONArray5 != null && i7 < optJSONArray5.length()) {
                        if (Text.hasFigure(optJSONArray5.optString(i7))) {
                            arrayList.add(str + Text.cap(optJSONObject2.optString("head"), 70) + " — " + optJSONArray5.optString(i7));
                            break;
                        }
                        i7++;
                    } else {
                    break;
                }
                }
            }
        }
        return arrayList;
    }
}
