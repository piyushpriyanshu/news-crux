package app.newsbreak.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
public final class Pyq {
    private static final Pattern ACRO;
    private static final Pattern ASPECT;
    private static final Set<String> GENERIC;
    public static final Set<String> GENERIC_IDS;
    static final String[][] GS2SUBJ;
    private static final Pattern QUOTED;
    private static final Pattern TRAIL;
    private static final Pattern WORD;
    final Kb kb;
    static final Set<String> STOP = new HashSet();
    static final Set<String> NEWS = new HashSet();
    final List<Row> rows = new ArrayList();
    final Map<String, Integer> df = new HashMap();
    /** CDS / CAPF questions: used only when CSE evidence is thin. */
    final List<Row> rows2 = new ArrayList();
    final Map<String, Integer> df2 = new HashMap();

    public static final class Row {
        String asp;
        public Set<String> ids = new HashSet();
        /** Cards named only in the question's statements (weaker evidence than the lead or the answer). */
        Set<String> ids2 = new HashSet();
        /** Whole question text (null when not available). */
        String full;
        public String qno;
        public String stem;
        public String subj;
        public String theme;
        Set<String> tok;
        public String topic;
        public int year;
        /** null = UPSC CSE Prelims; "CDS" / "CAPF" = other UPSC papers (secondary evidence). */
        public String exam;
    }

    static {
        for (String str : "consider following statements statement correct which with reference context india indian among given below code select answer using only both neither none pairs pair matched about regarding respect government national state states world these those that this have been from were what where when more than most other also into under over their there they them include includes including known called recently news often mentioned refers refer true false wrong incorrect correctly following: following, assertion reason explanation".split(" ")) {
            STOP.add(str);
        }
        for (String str2 : "talk talks sign signs signed resume resumes record records rise rises approve approves approved extension complete completes completed test tests issue issues official officials week month held meet meets meeting seek seeks oppose opposes opposed cuts said told country countries cooperation keeps keep retains retain unchanged discovered named after first second third launch launches launched announce announces announced plan plans says report reports data year years today amid deal deals visit visits hold holds union cabinet minister ministry scheme schemes policy government court bench refers lowers raises mark marks list lists rank ranks project projects review reviews boost boosts push pushes major towards toward tamil nadu kerala karnataka maharashtra gujarat rajasthan punjab haryana bihar odisha bengal assam pradesh uttar andhra madhya telangana jharkhand chhattisgarh uttarakhand himachal manipur meghalaya mizoram nagaland tripura sikkim arunachal delhi jammu kashmir ladakh drop drops friday saturday sunday monday tuesday wednesday thursday crore lakh percent article articles constitution constitutional supreme court courts rate rates flag flags item items india indian amendment bill bills bench probe complaint order orders notified notify success programme program".split(" ")) {
            NEWS.add(str2);
        }
        WORD = Pattern.compile("[A-Za-z][A-Za-z0-9]+");
        ACRO = Pattern.compile("^[A-Z]{2,5}s?$");
        GENERIC_IDS = new HashSet(Arrays.asList("e:sc", "e:hc", "e:un", "e:ordinance"));
        GS2SUBJ = new String[][]{new String[]{"Polity", "Polity"}, new String[]{"Governance", "Polity|Economy"}, new String[]{"Social", "Economy|Polity"}, new String[]{"Economy", "Economy"}, new String[]{"Agriculture", "Economy|Indian Geography"}, new String[]{"Infrastructure", "Economy|Indian Geography"}, new String[]{"Energy", "Economy|Indian Geography|Science & Technology"}, new String[]{"Environment", "Environment"}, new String[]{"S&T", "Science & Technology"}, new String[]{"Security", "Science & Technology"}, new String[]{"IR", "International Relations|World Geography"}, new String[]{"Geography", "Indian Geography|World Geography|Physical Geography"}, new String[]{"Art & Culture", "Art & Culture|Ancient History|Medieval History"}, new String[]{"History", "Modern History|Ancient History|Medieval History"}};
        ASPECT = Pattern.compile("(?i)(?:with reference to|in the context of|with regard to|regarding|in respect of|statements? (?:about|regarding|on)|which (?:of the following|one of the following) (?:is|are|was|were) (?:true|correct|not correct|incorrect) (?:of|about|regarding)|the term|the concept of|the expression)\\s+(?:the\\s+|an?\\s+)?[‘'\"“]?([^,:;?.‘’'\"“”]{3,60})");
        QUOTED = Pattern.compile("[‘'\"“]([^‘’'\"“”]{3,48})[’'\"”]");
        GENERIC = new HashSet();
        for (String str3 : "miscellaneous general-awareness-india general-awareness-world contemporary-events-india contemporary-events-world important-concepts-in-economy location-map-based-indian-geography location-map-based-world-geography human-and-economic-geography biology physics chemistry basic-science everyday-biology international-relations".split(" ")) {
            GENERIC.add(str3);
        }
        TRAIL = Pattern.compile("(?i)(\\s+(the|of|in|on|for|a|an|and|or|to|with|by|at|from|was|is|are|were|which|that|its|their|as|under|between))+\\s*$");
    }

    static String stem(String str) {
        return (str.length() <= 5 || !str.endsWith("ies")) ? (str.length() <= 4 || !str.endsWith("es") || str.endsWith("ses")) ? (str.length() <= 3 || !str.endsWith("s") || str.endsWith("ss")) ? str : str.substring(0, str.length() - 1) : str.substring(0, str.length() - 2) : str.substring(0, str.length() - 3) + "y";
    }

    static List<String> tok(String str) {
        ArrayList arrayList = new ArrayList();
        Pattern pattern = WORD;
        if (str == null) {
            str = "";
        }
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String group = matcher.group();
            if (ACRO.matcher(group).matches()) {
                arrayList.add(group.toLowerCase(Locale.ROOT).replaceAll("s$", ""));
            } else {
                String lowerCase = group.toLowerCase(Locale.ROOT);
                if (lowerCase.length() >= 4 && !STOP.contains(lowerCase)) {
                    arrayList.add(stem(lowerCase));
                }
            }
        }
        return arrayList;
    }

    public Pyq(String str, Kb kb) throws Exception {
        this(str, null, kb);
    }

    /** Full question texts by "year|q": [stem, [a, b, c, d], answer index] (pyq_text.js). */
    JSONObject text;

    /** @param textJs full questions with options and answers; matching then uses the whole stem plus the correct option. */
    public Pyq(String str, String textJs, Kb kb) throws Exception {
        this.kb = kb;
        if (textJs != null) {
            String t = textJs.trim();
            int a = t.indexOf('{'), b = t.lastIndexOf('}');
            if (a >= 0 && b > a) this.text = new JSONObject(t.substring(a, b + 1));
        }
        load(str, this.rows, this.df, null);
    }

    /** The correct option's text when it names something ("Sundarbans"), else "". */
    String right(Row row) {
        if (text == null) return "";
        JSONArray q = text.optJSONArray(row.year + "|" + row.qno);
        if (q == null) return "";
        row.full = q.optString(0);
        JSONArray o = q.optJSONArray(1);
        int ans = q.optInt(2, -1);
        String right = (o != null && ans >= 0 && ans < o.length()) ? o.optString(ans) : "";
        if (right.matches("(?i)^(?:[1-4IV]+(?:\\s*(?:,|and|&)\\s*[1-4IV]+)*\\s*only|both.*|neither.*|none.*|all.*|only (one|two|three|four)|[1-4IV, and]+)$")) right = "";
        return right;
    }

    /**
     * Cards saved before full questions existed carry [year, aspect] without the question key.
     * Finds the question again (same year and same "what was asked" line) so the year opens it.
     * @return true if any key was added.
     */
    public boolean addKeys(JSONObject t, Kb kb) {
        JSONArray ev = t == null ? null : t.optJSONArray("pyqEv");
        if (ev == null || text == null) return false;
        boolean missing = false;
        for (int i = 0; i < ev.length(); i++) { JSONArray e = ev.optJSONArray(i); if (e != null && e.length() < 3) missing = true; }
        if (!missing) return false;
        Set<String> ids = new LinkedHashSet<String>();
        String[][] kinds = {{"ents", "e:"}, {"maps", "m:"}};
        for (String[] k : kinds) { JSONArray a = t.optJSONArray(k[0]); for (int i = 0; a != null && i < a.length(); i++) ids.add(k[1] + a.optString(i)); }
        if (t.optString("pyqId").length() > 0) ids.add(t.optString("pyqId"));
        StringBuilder b = new StringBuilder();
        JSONArray bl = t.optJSONArray("bullets");
        for (int i = 0; bl != null && i < bl.length(); i++) b.append(bl.optString(i)).append(' ');
        List<Row> cand = new ArrayList<Row>(matchTopic(this.rows, this.df, Foreign.mask(t.optString("head")), Foreign.mask(b.toString()), t.optString("gs"), ids, 12));
        for (Row r : this.rows) for (String id : ids) if (r.ids.contains(id) && !cand.contains(r)) { cand.add(r); break; }
        boolean added = false;
        Set<String> used = new HashSet<String>();
        for (int i = 0; i < ev.length(); i++) {
            JSONArray e = ev.optJSONArray(i);
            if (e == null || e.length() >= 3) continue;
            int year = e.optInt(0);
            String asp = e.optString(1);
            Row hit = null;
            for (Row r : cand) {
                if (r.year != year || r.exam != null || used.contains(r.year + "|" + r.qno)) continue;
                String a = aspect(r);
                if (a.equalsIgnoreCase(asp) || sameAs(a, asp) || "asked".equals(asp)) { hit = r; break; }
            }
            try {
                if (hit != null && key(hit) != null) { e.put(key(hit)); used.add(key(hit)); }
                else e.put(""); // no full text (CDS/CAPF row or not found): the year shows, the tap explains
                added = true;
            } catch (Exception ignored) {}
        }
        return added;
    }

    /** Full question for "year|q": [stem, [a, b, c, d], answer index], or null. */
    public JSONArray question(String key) {
        return text == null || key == null ? null : text.optJSONArray(key);
    }

    /** "year|q" key of a CSE question with full text, or null. */
    public String key(Row row) {
        String k = row.year + "|" + row.qno;
        return row.exam == null && text != null && text.has(k) ? k : null;
    }

    /** Adds CDS / CAPF rows ([year, q, subject, topic, theme, stem, exam]). */
    public void addMore(String str) throws Exception {
        load(str, this.rows2, this.df2, "?");
    }

    private void load(String str, List<Row> rowsOut, Map<String, Integer> dfOut, String examDefault) throws Exception {
        String trim = str.trim();
        JSONArray jSONArray = new JSONArray(trim.substring(trim.indexOf(91), trim.lastIndexOf(93) + 1));
        JSONObject optJSONObject = kb != null ? kb.raw.optJSONObject("pyqFix") : null;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < jSONArray.length()) {
                JSONArray jSONArray2 = jSONArray.getJSONArray(i2);
                Row row = new Row();
                row.year = jSONArray2.getInt(0);
                row.qno = jSONArray2.optString(1);
                row.subj = jSONArray2.getString(2);
                row.topic = jSONArray2.getString(3);
                row.theme = jSONArray2.getString(4);
                row.stem = jSONArray2.getString(5);
                if (examDefault != null) row.exam = jSONArray2.optString(6, examDefault);
                if (!row.stem.startsWith("Directions")) {
                    // lead of the question + the correct option = what was asked; the statements are weaker evidence
                    String core = examDefault == null ? (row.stem + " " + right(row)).trim() : row.stem;
                    row.tok = new HashSet(tok(core + " " + row.theme.replace('-', ' ')));
                    if (kb != null) {
                        row.ids.addAll(kb.idsIn(core));
                        if (row.full != null) for (String id : kb.idsIn(row.full)) if (!row.ids.contains(id)) row.ids2.add(id);
                        String replace = row.theme.replace('-', ' ');
                        if (!row.theme.startsWith("x-") && !GENERIC.contains(row.theme) && row.full != null) row.ids.addAll(kb.idsIn(replace)); // researched theme names the subject
                        if (core.split("\\s+").length < 7) {
                            row.ids.addAll(kb.idsIn(replace));
                        } else if (row.theme.length() <= 6) {
                            row.ids.addAll(kb.idsIn(row.theme.toUpperCase(Locale.ROOT)));
                        }
                    }
                    if (optJSONObject != null) {
                        String str2 = row.year + "|" + jSONArray2.optString(1) + "|" + row.theme;
                        JSONArray optJSONArray = optJSONObject.optJSONObject("drop") != null ? optJSONObject.optJSONObject("drop").optJSONArray(str2) : null;
                        for (int i3 = 0; optJSONArray != null && i3 < optJSONArray.length(); i3++) {
                            row.ids.remove(optJSONArray.optString(i3));
                        }
                        JSONObject optJSONObject2 = optJSONObject.optJSONObject("asp");
                        if (optJSONObject2 != null && optJSONObject2.has(str2)) {
                            row.asp = optJSONObject2.optString(str2);
                        }
                    }
                    for (String str3 : row.tok) {
                        Integer num = dfOut.get(str3);
                        dfOut.put(str3, Integer.valueOf(num == null ? 1 : num.intValue() + 1));
                    }
                    rowsOut.add(row);
                }
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    public int size() {
        return this.rows.size();
    }

    public List<Row> rows() {
        return this.rows;
    }

    static Set<String> subjFor(String str) {
        HashSet hashSet = new HashSet();
        if (str != null) {
            for (String[] strArr : GS2SUBJ) {
                if (str.contains(strArr[0])) {
                    String[] split = strArr[1].split("\\|");
                    for (String str2 : split) {
                        hashSet.add(str2);
                    }
                }
            }
        }
        return hashSet;
    }

    public List<Row> matchTopic(String str, String str2, String str3, Set<String> set, int i) {
        List<Row> out = matchTopic(this.rows, this.df, str, str2, str3, set, i);
        if (out.size() < i && !this.rows2.isEmpty()) {
            out = new ArrayList<Row>(out);
            for (Row r : matchTopic(this.rows2, this.df2, str, str2, str3, set, i - out.size())) out.add(r);
        }
        return out;
    }

    List<Row> matchTopic(List<Row> rs, Map<String, Integer> dfm, String str, String str2, String str3, Set<String> set, int i) {
        if (i <= 0) return new ArrayList<Row>();
        if (set != null && !set.isEmpty()) {
            HashSet hashSet = new HashSet();
            for (String str4 : set) {
                if (!GENERIC_IDS.contains(str4)) {
                    hashSet.add(str4);
                }
            }
            if (!hashSet.isEmpty()) {
                set = hashSet;
            }
            ArrayList<Row> arrayList = new ArrayList();
            for (Row row : rs) {
                for (String id : set) if (row.ids.contains(id)) { arrayList.add(row); break; }
            }
            final Set<Row> weak = new HashSet<Row>();
            if (arrayList.size() < 2) { // few direct questions: also those that name it in a statement
                for (Row row : rs) {
                    if (arrayList.contains(row)) continue;
                    for (String id : set) if (row.ids2.contains(id)) { arrayList.add(row); weak.add(row); break; }
                }
            }
            if (!arrayList.isEmpty()) {
                final HashMap hashMap = new HashMap();
                HashSet<String> hashSet2 = new HashSet();
                for (String str5 : tok(str + " " + str2)) {
                    if (!NEWS.contains(str5)) {
                        hashSet2.add(str5);
                    }
                }
                double size = rs.size();
                for (Row row2 : arrayList) {
                    double d = 0.0d;
                    HashSet hashSet3 = new HashSet(row2.tok);
                    if (row2.asp != null) {
                        hashSet3.addAll(tok(row2.asp));
                    }
                    for (String str6 : hashSet2) {
                        if (hashSet3.contains(str6) && dfm.containsKey(str6)) {
                            d += Math.log(size / dfm.get(str6).intValue());
                        }
                    }
                    hashMap.put(row2, Double.valueOf(d));
                }
                Collections.sort(arrayList, new Comparator<Row>() { // from class: app.newsbreak.core.Pyq.1
                    @Override // java.util.Comparator
                    public int compare(Row row3, Row row4) {
                        int w = Boolean.compare(weak.contains(row3), weak.contains(row4));
                        if (w != 0) return w;
                        int compare = Double.compare(((Double) hashMap.get(row4)).doubleValue(), ((Double) hashMap.get(row3)).doubleValue());
                        return compare != 0 ? compare : row4.year - row3.year;
                    }
                });
                // a weak row must share at least one real word with the story
                for (java.util.Iterator<Row> it = arrayList.iterator(); it.hasNext(); ) { Row r = it.next(); if (weak.contains(r) && ((Double) hashMap.get(r)).doubleValue() <= 0) it.remove(); }
                if (arrayList.isEmpty()) return match(rs, dfm, str, str2, str3, i);
                return arrayList.size() > i ? new ArrayList(arrayList.subList(0, i)) : arrayList;
            }
        }
        return match(rs, dfm, str, str2, str3, i);
    }

    public List<Row> match(String str, String str2, String str3, int i) {
        return match(this.rows, this.df, str, str2, str3, i);
    }

    List<Row> match(List<Row> rs, Map<String, Integer> dfm, String str, String str2, String str3, int i) {
        int i2;
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet();
        HashSet hashSet = new HashSet();
        for (String str4 : tok(str + " " + str2)) {
            if (!NEWS.contains(str4)) {
                linkedHashSet.add(str4);
            }
        }
        for (String str5 : tok(str)) {
            if (!NEWS.contains(str5)) {
                hashSet.add(str5);
            }
        }
        Set<String> subjFor = subjFor(str3);
        final HashMap hashMap = new HashMap();
        ArrayList arrayList = new ArrayList();
        if (!linkedHashSet.isEmpty()) {
            double size = rs.size();
            for (Row row : rs) {
                double d = 0.0d;
                double d2 = 0.0d;
                int i3 = 0;
                int i4 = 0;
                for (String str6 : linkedHashSet) {
                    if (row.tok.contains(str6)) {
                        double log = Math.log(size / dfm.get(str6).intValue());
                        d += log;
                        int i5 = i4 + 1;
                        if (log > d2) {
                            d2 = log;
                        }
                        if (hashSet.contains(str6) && log >= 3.0d) {
                            i3++;
                        }
                        i4 = i5;
                    }
                }
                if (i4 != 0 && i3 != 0 && subjFor.contains(row.subj)) {
                    i2 = 0; // headline words that are rare across all PYQs (strong evidence)
                    for (Object o : hashSet) {
                        String w = (String) o;
                        if (row.tok.contains(w) && Math.log(size / dfm.get(w).intValue()) >= 4.0d) i2++;
                    }
                    if ((i2 >= 2 && d >= 9.0d) || (i2 >= 1 && d2 >= 6.5d && i4 >= 2)) {
                        hashMap.put(row, Double.valueOf(d));
                        arrayList.add(row);
                    }
                }
            }
            Collections.sort(arrayList, new Comparator<Row>() { // from class: app.newsbreak.core.Pyq.2
                @Override // java.util.Comparator
                public int compare(Row row2, Row row3) {
                    int compare = Double.compare(((Double) hashMap.get(row3)).doubleValue(), ((Double) hashMap.get(row2)).doubleValue());
                    return compare != 0 ? compare : row3.year - row2.year;
                }
            });
            return arrayList.size() > i ? new ArrayList(arrayList.subList(0, i)) : arrayList;
        }
        return arrayList;
    }

    static String norm(String str) {
        String replaceAll = str.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return replaceAll.length() > 60 ? replaceAll.substring(0, 60) : replaceAll;
    }

    public static String aspect(Row row) {
        String a = aspect1(row);
        return row.exam == null ? a : a + " (" + row.exam + ")";
    }

    static String aspect1(Row row) {
        if (row.asp != null && row.asp.length() > 0) {
            return row.asp;
        }
        String tidy = tidy(aspect0(row));
        if (bad(tidy)) {
            tidy = row.theme.startsWith("x-") ? "asked" : tidy(row.theme.replace('-', ' '));
            if (GENERIC.contains(row.theme) || bad(tidy)) {
                tidy = "asked";
            }
        }
        if (tidy.matches("[a-z]{2,4}") || (tidy.matches("[a-z]{5,6}") && Pattern.compile("\\b" + tidy.toUpperCase(Locale.ROOT) + "\\b").matcher(row.stem).find())) {
            tidy = tidy.toUpperCase(Locale.ROOT);
        }
        return tidy.replaceAll("(system|location|rivers)(brahmaputra|map|drainage)", "$1 / $2").replaceAll("\\bst recognition\\b", "ST recognition").replaceAll("\\bsc and hc\\b", "SC & HC").replaceAll("\\bpresidents rule\\b", "President's rule");
    }

    static String tidy(String str) {
        String trim = str.replace("…", "").replaceAll("[`‘’'\"“”]", "").trim();
        while (true) {
            String trim2 = TRAIL.matcher(trim).replaceAll("").trim();
            if (trim2.equals(trim)) {
                return trim2;
            }
            trim = trim2;
        }
    }

    static boolean bad(String str) {
        if (str == null || str.length() < 3 || str.contains("?")) {
            return true;
        }
        String lowerCase = str.toLowerCase(Locale.ROOT);
        if (lowerCase.matches("^(recently|according|a present|which|who|whom|what|consider|not |never|economist|pairs|bilateral|with |in |the following|it is|and |or |as per|how |why |where |when ).*")) {
            return true;
        }
        if (lowerCase.matches("^(statements?|the statements?|match list|: ).*") || lowerCase.matches(".*\\b\\d$") || lowerCase.matches(".*\\b(is|are) (not )?(correct|incorrect|true|false)\\b.*")) return true;
        return lowerCase.matches("^(india|indian|the world|world|indian economy|the indian economy|india's|the country|countries|statements?|the context|following)$");
    }

    static String aspect0(Row row) {
        String a = aspectFull(row);
        if (a != null) return a;
        Matcher matcher = ASPECT.matcher(row.stem);
        if (matcher.find()) {
            String replaceAll = matcher.group(1).trim().replaceAll("\\s+(consider|which|the following).*$", "").replaceAll("\\s+", " ");
            if (replaceAll.length() >= 3) {
                return Text.cap(replaceAll, 36);
            }
        }
        Matcher matcher2 = QUOTED.matcher(row.stem);
        if (matcher2.find()) {
            return Text.cap(matcher2.group(1).trim(), 36);
        }
        if (!GENERIC.contains(row.theme) && !row.theme.startsWith("x-")) {
            return row.theme.replace('-', ' ');
        }
        String[] split = row.stem.replaceAll("(?i)^(consider the following( statements)?:?|which (one )?of the following|who (among|amongst) the following|what is|what are|in india,?)\\s*", "").replaceAll("(?i)^(is|are|was|were|has|have)\\s+", "").replaceAll("[:?….]+$", "").trim().split("\\s+");
        if (split.length >= 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < split.length && i < 7; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(split[i]);
            }
            return sb.toString().replaceAll("^(?i)the ", "");
        }
        return row.topic.toLowerCase(Locale.ROOT);
    }

    private static final Pattern STMT1 = Pattern.compile("(?:^|\\s)(?:1\\.|I\\.)\\s*([^.;:?]{8,80})");
    private static final Pattern WHICH = Pattern.compile("(?i)^(?:which|who|what|where)\\b(?: one)?(?: (?:of|among|amongst) the following)?\\s+(?:is|are|was|were|has|have|can|does)?\\s*(?:the |an? )?(.{6,60}?)[?:.]?$");

    /** "What was asked" from the whole question when the short stem says too little. */
    static String aspectFull(Row row) {
        if (row.full == null || row.full.length() < 20) return null;
        if (!row.theme.startsWith("x-")) return null; // researched/specific themes keep the old wording
        String a = aspectFull0(row);
        return a == null || a.toLowerCase(Locale.ROOT).matches(".*\\b(statements?|correct|following|above|given below|code)\\b.*") ? null : a;
    }

    static String aspectFull0(Row row) {
        Matcher m = ASPECT.matcher(row.full);
        if (m.find()) {
            String t = m.group(1).trim().replaceAll("\\s+(consider|which|the following).*$", "").replaceAll("\\s+", " ");
            if (t.length() >= 3 && !bad(tidy(t))) return Text.cap(t, 36);
        }
        Matcher q = QUOTED.matcher(row.full);
        if (q.find()) return Text.cap(q.group(1).trim(), 36);
        Matcher s1 = STMT1.matcher(row.full);
        if (s1.find() && row.full.matches("(?is)^\\s*consider the following.*")) {
            String t = s1.group(1).replaceAll("\\s+(?:\\d|I{1,3}|IV|V)\\.?\\s*.*$", "").replaceAll("[\\s:,;–—-]+$", "").trim();
            if (t.length() < 8 || t.matches("(?i)^(according|the following|which|in the|it is).*")) return null;
            if (!bad(tidy(t))) return Text.cap(t, 40);
        }
        String first = row.full.split("(?<=[?:])\\s")[0].trim();
        Matcher w = WHICH.matcher(first);
        if (w.find() && !bad(tidy(w.group(1)))) return Text.cap(w.group(1).trim(), 40);
        return null;
    }

    public static String compact(List<Row> list, String str) {
        if (str == null) {
            return compact(list);
        }
        String compact = compact(list);
        StringBuilder sb = new StringBuilder();
        String[] split = compact.split(" · ");
        int length = split.length;
        for (int i = 0; i < length; i++) {
            String str2 = split[i];
            int lastIndexOf = str2.lastIndexOf(" ’");
            if (sameAs(lastIndexOf > 0 ? str2.substring(0, lastIndexOf) : str2, str)) {
                str2 = "asked" + (lastIndexOf > 0 ? str2.substring(lastIndexOf) : "");
            }
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(str2);
        }
        return sb.toString();
    }

    public static boolean sameAs(String str, String str2) {
        String replaceAll = str.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String replaceAll2 = str2.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return replaceAll.length() > 0 && (replaceAll.equals(replaceAll2) || ((replaceAll2.startsWith(replaceAll) && replaceAll.length() >= 3) || (replaceAll.startsWith(replaceAll2) && replaceAll.length() <= replaceAll2.length() + 4)));
    }

    public static String compact(List<Row> list) {
        ArrayList<Row> arrayList = new ArrayList(list);
        Collections.sort(arrayList, new Comparator<Row>() { // from class: app.newsbreak.core.Pyq.3
            @Override // java.util.Comparator
            public int compare(Row row, Row row2) {
                return row2.year - row.year;
            }
        });
        LinkedHashMap<String, List<Integer>> linkedHashMap = new LinkedHashMap<String, List<Integer>>();
        for (Row row : arrayList) {
            String aspect = aspect(row);
            String lowerCase = aspect.toLowerCase(Locale.ROOT);
            String str = null;
            for (String str2 : linkedHashMap.keySet()) {
                String lowerCase2 = str2.toLowerCase(Locale.ROOT);
                if (!lowerCase2.equals(lowerCase) && !lowerCase2.contains(lowerCase) && !lowerCase.contains(lowerCase2)) {
                    str2 = str;
                }
                str = str2;
            }
            if (str == null) {
                linkedHashMap.put(aspect, new ArrayList());
                str = aspect;
            }
            if (!((List) linkedHashMap.get(str)).contains(Integer.valueOf(row.year))) {
                ((List) linkedHashMap.get(str)).add(Integer.valueOf(row.year));
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append((String) entry.getKey()).append(' ');
            List list2 = (List) entry.getValue();
            for (int i = 0; i < list2.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append((char) 8217).append(String.format(Locale.ROOT, "%02d", Integer.valueOf(((Integer) list2.get(i)).intValue() % 100)));
            }
        }
        return sb.toString();
    }

    public static String evidence(List<Row> list) {
        StringBuilder sb = new StringBuilder();
        for (Row row : list) {
            sb.append("    · ").append(row.year).append(row.exam == null ? "" : " " + row.exam).append(" [").append(row.subj).append(" / ").append(row.theme).append("] ").append(row.stem).append("… → asked: ").append(aspect(row)).append('\n');
        }
        return sb.toString();
    }
}
