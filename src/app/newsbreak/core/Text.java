package app.newsbreak.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class Text {
    private static final Pattern FIG;
    private static final Pattern LEAD;
    private static final Pattern SAID;
    static final Set<String> STOP;
    private static final Map<String, String> SYN;
    private static final Pattern TAGS = Pattern.compile("<[^>]+>", 32);
    private static final Pattern SCRIPT = Pattern.compile("<(script|style)[^>]*>.*?</\\1>", 34);
    private static final Pattern WS = Pattern.compile("\\s+");
    private static final Pattern ENT_NUM = Pattern.compile("&#(x?)([0-9a-fA-F]+);");
    private static final Pattern SENT = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z0-9“\"‘'(₹])");
    private static final String[][] ABBR = {new String[]{"(?i)\\bGovernment of India\\b", "GoI"}, new String[]{"(?i)\\bgovernment\\b", "govt"}, new String[]{"(?i)\\bthe Centre\\b", "Centre"}, new String[]{"(?i)\\bSupreme Court\\b", "SC"}, new String[]{"(?i)\\bHigh Court\\b", "HC"}, new String[]{"(?i)\\bReserve Bank of India\\b", "RBI"}, new String[]{"(?i)\\bPrime Minister\\b", "PM"}, new String[]{"(?i)\\bChief Minister\\b", "CM"}, new String[]{"(?i)\\bUnion Minister\\b", "Union Min"}, new String[]{"(?i)\\bMinistry of\\b", "Min of"}, new String[]{"(?i)\\bper cent\\b", "%"}, new String[]{"(?i)\\bpercent\\b", "%"}, new String[]{"(?i)\\bbillion\\b", "bn"}, new String[]{"(?i)\\bmillion\\b", "mn"}, new String[]{"(?i)\\btrillion\\b", "tn"}, new String[]{"(?i)\\bthousand\\b", "k"}, new String[]{"(?i)\\bapproximately\\b", "~"}, new String[]{"(?i)\\baround\\b(?=\\s*[₹$0-9])", "~"}, new String[]{"(?i)\\babout\\b(?=\\s*[₹$0-9])", "~"}, new String[]{"(?i)\\bRs\\s?", "₹"}, new String[]{"(?i)\\bINR\\s?", "₹"}, new String[]{"(?i)\\bArticle\\s(?=\\d)", "Art "}, new String[]{"(?i)\\bSection\\s(?=\\d)", "Sec "}, new String[]{"(?i)\\bUnited Nations\\b", "UN"}, new String[]{"(?i)\\bUnited States\\b", "US"}, new String[]{"(?i)\\bUnited Kingdom\\b", "UK"}, new String[]{"(?i)\\bEuropean Union\\b", "EU"}, new String[]{"(?i)\\bkilometres?\\b", "km"}, new String[]{"(?i)\\bkilometers?\\b", "km"}, new String[]{"(?i)\\bhectares?\\b", "ha"}, new String[]{"(?i)\\bfinancial year\\b", "FY"}, new String[]{"(?i)\\bfiscal year\\b", "FY"}, new String[]{"(?i)\\byear-on-year\\b", "YoY"}, new String[]{"(?i)\\bcompared to\\b", "vs"}, new String[]{"(?i)\\bcompared with\\b", "vs"}, new String[]{"(?i)\\bin order to\\b", "to"}, new String[]{"(?i)\\bLegislative Assembly\\b", "Assembly"}, new String[]{"(?i)\\bConstitution of India\\b", "Constitution"}, new String[]{"(?i)\\bwith respect to\\b", "re"}, new String[]{"(?i)\\bnumber of\\b", "no. of"}, new String[]{"(?i)\\bdepartment\\b", "Dept"}};
    private static final Pattern[] ABBR_P = new Pattern[ABBR.length];

    private Text() {
    }

    static {
        for (int i = 0; i < ABBR.length; i++) {
            ABBR_P[i] = Pattern.compile(ABBR[i][0]);
        }
        LEAD = Pattern.compile("^(?i)(the|a|an|meanwhile|however|also|further|additionally|notably|in a statement|according to (the )?(officials?|sources|reports?)),?\\s+");
        SAID = Pattern.compile("(?i),?\\s+(an? )?(official|officials|sources|spokesperson|he|she|they) (said|added|noted|told [A-Za-z ]+)\\.?$");
        STOP = new HashSet();
        for (String str : "a an the and or but of to in on at for from by with as is are was were be been being this that these those it its into over under after before about above below than then there their they them he she his her we our you your i me my not no yes will would shall should can could may might must has have had do does did done also just more most less least very said says say new news today yesterday week year years day days amid via per up down out off all any each other some such only own same so too s t now how what when where which who whom why while during between against again further once here both few nor upon within without across toward towards since until till among including include includes included like report reports reported says sources official officials government govt india indian centre center state states minister ministry".split(" ")) {
            STOP.add(str);
        }
        SYN = new HashMap();
        for (String[] strArr : new String[][]{new String[]{"first", "1"}, new String[]{"one", "1"}, new String[]{"second", "2"}, new String[]{"two", "2"}, new String[]{"third", "3"}, new String[]{"three", "3"}, new String[]{"fourth", "4"}, new String[]{"four", "4"}, new String[]{"fifth", "5"}, new String[]{"five", "5"}, new String[]{"uts", "ut"}, new String[]{"begins", "start"}, new String[]{"began", "start"}, new String[]{"begin", "start"}, new String[]{"starts", "start"}, new String[]{"started", "start"}, new String[]{"commences", "start"}, new String[]{"launched", "launch"}, new String[]{"launches", "launch"}, new String[]{"rs", "inr"}, new String[]{"rupees", "inr"}, new String[]{"crore", "cr"}, new String[]{"crores", "cr"}, new String[]{"percent", "pct"}, new String[]{"cent", "pct"}, new String[]{"pc", "pct"}, new String[]{"bypolls", "bypoll"}, new String[]{"byelections", "bypoll"}, new String[]{"by", ""}, new String[]{"elections", "election"}, new String[]{"approves", "approve"}, new String[]{"approved", "approve"}, new String[]{"nod", "approve"}, new String[]{"clears", "approve"}, new String[]{"cleared", "approve"}, new String[]{"hikes", "raise"}, new String[]{"hiked", "raise"}, new String[]{"raises", "raise"}, new String[]{"raised", "raise"}, new String[]{"cuts", "cut"}, new String[]{"slashes", "cut"}, new String[]{"unchanged", "hold"}, new String[]{"holds", "hold"}, new String[]{"keeps", "hold"}, new String[]{"retains", "hold"}, new String[]{"pauses", "hold"}}) {
            SYN.put(strArr[0], strArr[1]);
        }
        FIG = Pattern.compile("(\\d[\\d,.]*\\s?(%|per ?cent|crore|lakh|bn|mn|billion|million|trillion|tonnes?|km|MW|GW|GWh|ha)|₹\\s?\\d|\\$\\s?\\d|\\b(19|20)\\d\\d\\b|\\bArt(icle)?\\s?\\d+|\\b\\d+(st|nd|rd|th)\\b)", 2);
    }

    public static String decodeEntities(String str) {
        String str2;
        if (str == null) {
            return "";
        }
        if (str.indexOf(38) >= 0) {
            Matcher matcher = ENT_NUM.matcher(str);
            StringBuffer stringBuffer = new StringBuffer();
            while (matcher.find()) {
                try {
                    str2 = new String(Character.toChars(Integer.parseInt(matcher.group(2), matcher.group(1).length() > 0 ? 16 : 10)));
                } catch (Exception e) {
                    str2 = " ";
                }
                matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(str2));
            }
            matcher.appendTail(stringBuffer);
            String stringBuffer2 = stringBuffer.toString();
            for (String[] strArr : new String[][]{new String[]{"&nbsp;", " "}, new String[]{"&quot;", "\""}, new String[]{"&apos;", "'"}, new String[]{"&lsquo;", "‘"}, new String[]{"&rsquo;", "’"}, new String[]{"&ldquo;", "“"}, new String[]{"&rdquo;", "”"}, new String[]{"&ndash;", "–"}, new String[]{"&mdash;", "—"}, new String[]{"&hellip;", "…"}, new String[]{"&rupee;", "₹"}, new String[]{"&#8377;", "₹"}, new String[]{"&lt;", "<"}, new String[]{"&gt;", ">"}, new String[]{"&amp;", "&"}}) {
                stringBuffer2 = stringBuffer2.replace(strArr[0], strArr[1]);
            }
            return stringBuffer2;
        }
        return str;
    }

    public static String clean(String str) {
        if (str == null) {
            return "";
        }
        return WS.matcher(TAGS.matcher(decodeEntities(TAGS.matcher(SCRIPT.matcher(decodeEntities(str)).replaceAll(" ")).replaceAll(" "))).replaceAll(" ").replace((char) 160, ' ').replace("\u200b", "")).replaceAll(" ").trim().replaceAll("(?i)\\s*(The post .*? appeared first on .*)$", "").replaceAll("(?i)\\s*(Continue reading.*|Read more.*|Click here.*)$", "").replaceAll("(?i)^\\s*\\[?(Premium|Subscriber only)\\]?\\s*", "").trim();
    }

    public static String cleanTitle(String str, String str2) {
        if (str == null) {
            return "";
        }
        String clean = clean(str);
        if (str2 != null && str2.length() > 0) {
            String str3 = " - " + str2;
            if (clean.endsWith(str3)) {
                clean = clean.substring(0, clean.length() - str3.length());
            }
        }
        return clean.replaceAll("\\s+[-|–]\\s+(The Hindu|Hindustan Times|The Indian Express|Indian Express|Times of India|The Times of India|Economic Times|The Economic Times|Business Standard|Moneycontrol|Moneycontrol\\.com|Reuters|ANI News|ANI|PTI|Press Information Bureau|PIB|Drishti IAS|Vision IAS|INSIGHTSIAS|Insights IAS|Vajiram & Ravi|Vajiram and Ravi|ETGovernment|ET Government|BusinessLine|Mint|livemint|PM India)\\s*$", "").replaceAll("^(?i)((upsc|daily|today'?s)\\s+)?(current affairs|ca|news analysis|editorial analysis)\\s*[-–—:|]?\\s*(\\d{1,2}(st|nd|rd|th)?\\s+[A-Za-z]+,?\\s+\\d{4}|\\d{1,2}[-./]\\d{1,2}[-./]\\d{2,4})?\\s*[-–—:|]\\s*", "").replaceAll("^(?i)(explained|explainer|upsc key|opinion|editorial|analysis|watch|video|photos|live updates?|breaking)\\s*[:|]\\s*", "").trim();
    }

    public static List<String> sentences(String str) {
        ArrayList arrayList = new ArrayList();
        if (str != null && str.length() != 0) {
            for (String str2 : SENT.split(str.replace("Rs.", "Rs").replace("No.", "No").replace("Dr.", "Dr").replace("Mr.", "Mr").replace("Mrs.", "Mrs").replace("Ms.", "Ms").replace("St.", "St").replace("vs.", "vs").replace("Govt.", "Govt").replace("U.S.", "US").replace("U.K.", "UK").replace("U.N.", "UN").replace("i.e.", "ie").replace("e.g.", "eg").replace("Jan.", "Jan").replace("Feb.", "Feb").replace("Aug.", "Aug").replace("Sept.", "Sept").replace("Oct.", "Oct").replace("Nov.", "Nov").replace("Dec.", "Dec").replace("Art.", "Art").replace("approx.", "approx"))) {
                String trim = str2.trim();
                if (trim.length() > 0) {
                    arrayList.add(trim);
                }
            }
        }
        return arrayList;
    }

    public static String note(String str, int i) {
        if (str == null) {
            return "";
        }
        String trim = str.trim();
        for (int i2 = 0; i2 < ABBR_P.length; i2++) {
            trim = ABBR_P[i2].matcher(trim).replaceAll(Matcher.quoteReplacement(ABBR[i2][1]));
        }
        String trim2 = SAID.matcher(LEAD.matcher(trim).replaceFirst("")).replaceFirst("").replaceAll("\\s+%", "%").replaceAll("₹\\s+", "₹").replaceAll("~\\s+(?=[₹$0-9])", "~").replaceAll("\\s{2,}", " ").trim();
        if (trim2.endsWith(".")) {
            trim2 = trim2.substring(0, trim2.length() - 1);
        }
        if (trim2.length() > 0) {
            trim2 = Character.toUpperCase(trim2.charAt(0)) + trim2.substring(1);
        }
        return cap(trim2, i);
    }

    public static String cap(String str, int i) {
        if (str == null) {
            return "";
        }
        if (str.length() > i) {
            int lastIndexOf = str.lastIndexOf(32, i - 1);
            if (lastIndexOf < i * 0.6d) {
                lastIndexOf = i - 1;
            }
            return str.substring(0, lastIndexOf).replaceAll("[,;:\\-–—\\s]+$", "") + "…";
        }
        return str;
    }

    public static Set<String> tokens(String str) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        if (str == null) {
            return linkedHashSet;
        }
        for (String str2 : str.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\u0900-\\u097f]+", " ").trim().split(" ")) {
            if (str2.length() >= 3 && !STOP.contains(str2)) {
                if (str2.length() > 4 && str2.endsWith("ies")) {
                    str2 = str2.substring(0, str2.length() - 3) + "y";
                } else if (str2.length() > 4 && str2.endsWith("es") && !str2.endsWith("ses")) {
                    str2 = str2.substring(0, str2.length() - 2);
                } else if (str2.length() > 3 && str2.endsWith("s") && !str2.endsWith("ss")) {
                    str2 = str2.substring(0, str2.length() - 1);
                }
                linkedHashSet.add(str2);
            }
        }
        return linkedHashSet;
    }

    public static Set<String> simTokens(String str) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        if (str == null) {
            return linkedHashSet;
        }
        String[] split = str.replaceAll("[^A-Za-z0-9ऀ-ॿ%₹.]+", " ").trim().split(" ");
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String str2 : split) {
            String replaceAll = str2.replaceAll("^\\.+|\\.+$", "");
            if (replaceAll.length() != 0) {
                boolean z = Character.isUpperCase(replaceAll.charAt(0)) && (replaceAll.length() <= 1 || !replaceAll.equals(replaceAll.toUpperCase(Locale.ROOT)));
                String lowerCase = replaceAll.toLowerCase(Locale.ROOT);
                if (z) {
                    sb.append(lowerCase.charAt(0));
                    i++;
                } else if (i <= 0 || (!lowerCase.equals("of") && !lowerCase.equals("and") && !lowerCase.equals("for"))) {
                    if (i >= 2) {
                        linkedHashSet.add(sb.toString());
                    }
                    sb.setLength(0);
                    i = 0;
                }
                if (!STOP.contains(lowerCase) || SYN.containsKey(lowerCase)) {
                    String str3 = SYN.containsKey(lowerCase) ? SYN.get(lowerCase) : lowerCase;
                    if (str3.length() != 0) {
                        if (str3.matches("\\d+(\\.\\d+)?%")) {
                            linkedHashSet.add("pct");
                            str3 = str3.substring(0, str3.length() - 1);
                        }
                        if (!str3.matches("\\d+(\\.\\d+)?%?")) {
                            if (str3.length() < 3) continue; // short words (and codes like "3d", "%6") say nothing
                            {
                                if (str3.length() > 5 && str3.endsWith("ing")) {
                                    str3 = str3.substring(0, str3.length() - 3);
                                } else if (str3.length() > 4 && str3.endsWith("ed")) {
                                    str3 = str3.substring(0, str3.length() - 2);
                                } else if (str3.length() > 4 && str3.endsWith("ies")) {
                                    str3 = str3.substring(0, str3.length() - 3) + "y";
                                } else if (str3.length() > 4 && str3.endsWith("es") && !str3.endsWith("ses")) {
                                    str3 = str3.substring(0, str3.length() - 2);
                                } else if (str3.length() > 3 && str3.endsWith("s") && !str3.endsWith("ss")) {
                                    str3 = str3.substring(0, str3.length() - 1);
                                }
                            }
                        }
                        linkedHashSet.add(str3);
                    }
                }
            }
        }
        if (i >= 2) {
            linkedHashSet.add(sb.toString());
        }
        return linkedHashSet;
    }

    static boolean isFig(String str) {
        return str.matches("\\d+(\\.\\d+)?%?") && str.length() >= 2;
    }

    public static boolean sameLine(String str, String str2) {
        Set<String> simTokens = simTokens(str);
        Set<String> simTokens2 = simTokens(str2);
        if (simTokens.isEmpty() || simTokens2.isEmpty()) {
            return false;
        }
        for (String str3 : simTokens) {
            if (isFig(str3) && !simTokens2.contains(str3)) {
                return false;
            }
        }
        for (String str4 : simTokens2) {
            if (isFig(str4) && !simTokens.contains(str4)) {
                return false;
            }
        }
        int overlap = overlap(simTokens, simTokens2);
        double size = overlap / (double) ((simTokens.size() + simTokens2.size()) - overlap);
        int min = Math.min(simTokens.size(), simTokens2.size());
        if (min < 4) {
            return size >= 0.6d;
        }
        return size >= 0.45d || ((double) overlap) / ((double) min) >= 0.6d;
    }

    public static double jaccard(Set<String> set, Set<String> set2) {
        if (set.isEmpty() || set2.isEmpty()) {
            return 0.0d;
        }
        int i = 0;
        Iterator<String> it = set.iterator();
        while (true) {
            int i2 = i;
            if (!it.hasNext()) {
                return i2 / (double) ((set.size() + set2.size()) - i2);
            }
            i = set2.contains(it.next()) ? i2 + 1 : i2;
        }
    }

    public static int overlap(Set<String> set, Set<String> set2) {
        int i = 0;
        Iterator<String> it = set.iterator();
        while (true) {
            int i2 = i;
            if (!it.hasNext()) {
                return i2;
            }
            i = set2.contains(it.next()) ? i2 + 1 : i2;
        }
    }

    public static boolean hasFigure(String str) {
        return str != null && FIG.matcher(str).find();
    }

    public static String hash(String str) {
        long j = 1125899906842597L;
        for (int i = 0; i < str.length(); i++) {
            j = (j * 31) + str.charAt(i);
        }
        return Long.toHexString(j);
    }

    public static String normUrl(String str) {
        if (str == null) {
            return "";
        }
        String trim = str.trim();
        int indexOf = trim.indexOf("?utm_");
        if (indexOf > 0) {
            trim = trim.substring(0, indexOf);
        }
        if (trim.endsWith("/")) {
            trim = trim.substring(0, trim.length() - 1);
        }
        return trim.replaceFirst("^https?://(www\\.)?", "");
    }
}
