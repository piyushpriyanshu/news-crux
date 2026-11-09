package app.newsbreak.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Foreign offices and foreign domestic politics.
 *
 * "US President", "Vice President Vance", "Pakistan's Supreme Court" and "Bangladesh election" are not India's
 * President, Vice-President, Supreme Court or Election Commission. Such phrases are masked before knowledge-base and
 * PYQ matching, so no Indian polity card (Art 54 electoral college, CEC removal…) is pinned to a foreign story.
 * Foreign domestic politics with no India link is outside the Prelims scope and is scored down.
 */
public final class Foreign {
    private Foreign() {}

    /** Countries and demonyms (no India). Longest first inside the regex. */
    static final String[] PLACES = {
            "United States", "United Kingdom", "US", "U.S.", "USA", "UK", "America", "American", "British", "Britain", "England", "English",
            "Pakistan", "Pakistani", "Bangladesh", "Bangladeshi", "Sri Lanka", "Sri Lankan", "Nepal", "Nepali", "Nepalese", "Bhutan", "Bhutanese",
            "Maldives", "Maldivian", "Myanmar", "Afghanistan", "Afghan", "China", "Chinese", "Taiwan", "Taiwanese", "Japan", "Japanese",
            "South Korea", "South Korean", "North Korea", "North Korean", "Korea", "Korean", "Russia", "Russian", "Ukraine", "Ukrainian",
            "France", "French", "Germany", "German", "Italy", "Italian", "Spain", "Spanish", "Portugal", "Portuguese", "Netherlands", "Dutch",
            "Belgium", "Belgian", "Poland", "Polish", "Hungary", "Hungarian", "Romania", "Romanian", "Greece", "Greek", "Turkey", "Türkiye",
            "Turkish", "Iran", "Iranian", "Iraq", "Iraqi", "Israel", "Israeli", "Palestinian", "Syria", "Syrian", "Lebanon", "Lebanese",
            "Saudi Arabia", "Saudi", "UAE", "Emirati", "Qatar", "Qatari", "Egypt", "Egyptian", "Brazil", "Brazilian", "Argentina", "Argentine",
            "Argentinian", "Mexico", "Mexican", "Canada", "Canadian", "Australia", "Australian", "New Zealand", "Indonesia", "Indonesian",
            "Malaysia", "Malaysian", "Philippines", "Philippine", "Filipino", "Thailand", "Thai", "Vietnam", "Vietnamese", "Singapore",
            "Singaporean", "South Africa", "South African", "Nigeria", "Nigerian", "Kenya", "Kenyan", "Ethiopia", "Ethiopian", "Venezuela",
            "Venezuelan", "Colombia", "Colombian", "Chile", "Chilean", "Peru", "Peruvian", "Belarus", "Belarusian", "Georgia", "Georgian",
            "Armenia", "Armenian", "Azerbaijan", "Azerbaijani", "Kazakhstan", "Uzbekistan", "Mongolia", "Mongolian", "Ireland", "Irish",
            "Scotland", "Scottish", "Sweden", "Swedish", "Norway", "Norwegian", "Finland", "Finnish", "Denmark", "Danish", "Austria", "Austrian",
            "Switzerland", "Swiss", "Czech", "Serbia", "Serbian", "Moldova", "Moldovan", "Mauritius", "Mauritian", "Seychelles", "Fiji", "Fijian",
            "Tanzania", "Tanzanian", "Uganda", "Ugandan", "Ghana", "Ghanaian", "Senegal", "Sudan", "Sudanese", "Libya", "Libyan", "Tunisia",
            "Algeria", "Morocco", "Moroccan", "Yemen", "Yemeni", "Oman", "Omani", "Kuwait", "Kuwaiti", "Bahrain", "Jordan", "Jordanian",
            "Cuba", "Cuban", "Bolivia", "Bolivian", "Ecuador", "Haiti", "Cambodia", "Cambodian", "Laos", "European", "EU"};
    /** India's neighbourhood: its politics can matter for India, so it is not scored down. */
    static final Set<String> NEIGHBOURS = new HashSet<String>(Arrays.asList("pakistan", "pakistani", "bangladesh", "bangladeshi", "sri lanka",
            "sri lankan", "nepal", "nepali", "nepalese", "bhutan", "bhutanese", "maldives", "maldivian", "myanmar", "afghanistan", "afghan", "china", "chinese"));

    /** Sitting or recent foreign leaders whose names mark a foreign political story. */
    static final String LEADERS = "Trump|Biden|Vance|Kamala Harris|Rubio|Hegseth|Putin|Zelensky|Zelenskyy|Xi Jinping|Macron|Merz|Scholz|Starmer|Sunak|Meloni|"
            + "Erdogan|Erdoğan|Netanyahu|Pezeshkian|Khamenei|Albanese|Carney|Trudeau|Lula|Milei|Sheinbaum|Ramaphosa|Prabowo|Anwar Ibrahim|Marcos|"
            + "Lee Jae-myung|Ishiba|Takaichi|Shehbaz|Zardari|Asim Munir|Yunus|Dissanayake|Muizzu|Tobgay|Lukashenko|Maduro|Orbán|Orban";
    /** "Prime Minister Internship Scheme", "President's Colour": Indian schemes and honours, not foreign leaders. */
    static final Pattern SCHEME_WORD = Pattern.compile("(?i)\\b(yojana|scheme|programme|program|mission|fund|award|medal|colour|color|internship|employment|kisan|awas|rashtriya|abhiyan|nidhi|package|relief|cares)\\b");
    /** India's own office-holders (never masked). */
    static final Pattern INDIAN_NAME = Pattern.compile("^(Narendra|Modi|Murmu|Droupadi|Radhakrishnan|C\\.?\\s?P\\.?|CP|Dhankhar|Jagdeep|Ram Nath|Kovind|Venkaiah|Naidu|Pranab|Manmohan|Vajpayee|Venkataramani)\\b");

    static final String OFFICE = "(?:(?:former|new|then|acting|interim|outgoing|incoming|caretaker|top)\\s+)?"
            + "(?:Vice[- ]President|President|Presidency|Supreme Court|Chief Justice|Constitutional Court|High Court|Federal Court|"
            + "Chief Election Commissioner|Election Commission|Electoral Commission|Parliament|Congress|Senate|House of Representatives|"
            + "Speaker|Prime Minister|Premier|Attorney General|Governor|Cabinet|Constitution|"
            + "(?:presidential|general|parliamentary|snap|midterm|legislative|federal|provincial)\\s+(?:election|elections|polls?|vote|race|campaign|primary|primaries)|"
            + "elections?|polls?|referendum|impeachment|coalition)";

    private static final Pattern PLACE_OFFICE, OFFICE_OF, TITLE_NAME, LEADER, POLITICS_ONLY, BARE;
    static {
        String[] ps = PLACES.clone();
        Arrays.sort(ps, new java.util.Comparator<String>() { public int compare(String a, String b) { return b.length() - a.length(); } });
        StringBuilder alt = new StringBuilder();
        for (String p : ps) { if (alt.length() > 0) alt.append('|'); alt.append(Pattern.quote(p)); }
        String place = "(?:" + alt + ")";
        PLACE_OFFICE = Pattern.compile("(?<![\\w.])(?:the\\s+)?(" + place + ")(?:'s|’s)?\\s+" + OFFICE + "\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        OFFICE_OF = Pattern.compile("\\b" + OFFICE + "\\s+(?:of|in)\\s+(?:the\\s+)?(" + place + ")(?![\\w])(?!\\s+(?:Indian|Association|Society|Council|Academy|Institute|Federation|Board|Company|Bank|League|Club|Chamber|Museum|University|College)\\b)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        TITLE_NAME = Pattern.compile("\\b(?:Vice[- ]President|President|Prime Minister|Chancellor|Premier)\\s+((?:[A-Z][\\w’'.-]+\\s?){1,3})");
        BARE = Pattern.compile("\\b" + OFFICE + "\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        LEADER = Pattern.compile("\\b(?:" + LEADERS + ")\\b");
        POLITICS_ONLY = Pattern.compile("(?i)\\b(white house|oval office|capitol hill|electoral college|midterms|republicans?|democrats?|gop|tories|labour party|kremlin|downing street)\\b");
    }

    /** India marker: the story is about India, Indians or an Indian office. */
    static final Pattern INDIA = Pattern.compile("(?i)\\b(india|india's|indian|indians|new delhi|bharat|modi|murmu|jaishankar|rajnath|doval|lok sabha|rajya sabha|"
            + "union cabinet|union government|centre's|chief justice of india|cji|election commission of india|eci|rbi|isro|"
            + "andhra|arunachal|assam|bihar|chhattisgarh|goa|gujarat|haryana|himachal|jharkhand|karnataka|kerala|madhya pradesh|maharashtra|manipur|"
            + "meghalaya|mizoram|nagaland|odisha|punjab|rajasthan|sikkim|tamil nadu|telangana|tripura|uttar pradesh|uttarakhand|west bengal|"
            + "jammu|kashmir|ladakh|puducherry|delhi|mumbai|kolkata|chennai|bengaluru|hyderabad)\\b");

    /** Cheap pre-check: no office word, nothing to mask. */
    private static final Pattern HINT = Pattern.compile("(?i)(president|court|justice|elect|parliament|congress|senate|house of|speaker|minister|premier|attorney|governor|cabinet|constitution|poll|vote|referendum|impeach|coalition|chancellor|presidency)");

    /** Text with foreign offices replaced by a neutral marker (for matching only; shown text is never changed). */
    public static String mask(String s) {
        if (s == null || s.length() == 0 || !HINT.matcher(s).find()) return s;
        String t = s;
        t = replace(PLACE_OFFICE, t, true);
        t = replace(OFFICE_OF, t, true);
        Matcher m = TITLE_NAME.matcher(t);
        StringBuilder b = null; int last = 0;
        while (m.find()) {
            String name = m.group(1).trim();
            if (INDIAN_NAME.matcher(name).find() || SCHEME_WORD.matcher(name).find() || name.matches("(?i)(Rule|Rules|Election|Elections|Medal|Award)\\b.*")) continue;
            if (b == null) b = new StringBuilder();
            b.append(t, last, m.start()).append("foreignleader ").append(name).append(' ');
            last = m.end();
        }
        if (b != null) { b.append(t.substring(last)); t = b.toString(); }
        // a foreign political story with no India link: its bare "Parliament", "Vice President", "snap election" are foreign too
        if (!t.equals(s) || LEADER.matcher(s).find() || POLITICS_ONLY.matcher(s).find()) {
            if (!INDIA.matcher(s).find()) t = BARE.matcher(t).replaceAll("foreignoffice");
        }
        return t;
    }

    private static String replace(Pattern p, String t, boolean keepPlace) {
        Matcher m = p.matcher(t);
        StringBuilder b = null; int last = 0;
        while (m.find()) {
            String place = m.group(1);
            if (b == null) b = new StringBuilder();
            b.append(t, last, m.start()).append(keepPlace ? place + " foreignoffice" : "foreignoffice");
            last = m.end();
        }
        if (b == null) return t;
        b.append(t.substring(last));
        return b.toString();
    }

    /** Does the text name a foreign office, a foreign leader or foreign party politics? */
    public static boolean politics(String s) {
        if (s == null || s.length() == 0) return false;
        return PLACE_OFFICE.matcher(s).find() || OFFICE_OF.matcher(s).find() || LEADER.matcher(s).find() || POLITICS_ONLY.matcher(s).find()
                || !mask(s).equals(s);
    }

    /** Is India (or an Indian body or place) named? */
    public static boolean india(String s) { return s != null && INDIA.matcher(s).find(); }

    /** Foreign politics with no India link at all (outside the Prelims scope). */
    public static boolean outOfScope(String s) { return politics(s) && !india(s); }

    /** Foreign politics of a neighbour (Pakistan, Bangladesh, Nepal…), which can matter for India. */
    public static boolean neighbour(String s) {
        if (s == null) return false;
        Matcher m = PLACE_OFFICE.matcher(s);
        while (m.find()) if (NEIGHBOURS.contains(m.group(1).toLowerCase(java.util.Locale.ROOT))) return true;
        m = OFFICE_OF.matcher(s);
        while (m.find()) if (NEIGHBOURS.contains(m.group(1).toLowerCase(java.util.Locale.ROOT))) return true;
        return false;
    }

    /** India-only offices and procedures: never shown on a foreign story that has no India link. */
    public static final Set<String> INDIA_ONLY = new HashSet<String>(Arrays.asList(
            "e:pres", "e:vp", "e:eci", "e:sc", "e:hc", "e:ag", "e:gov", "e:speaker", "e:cag", "e:upsc", "e:fc", "e:a356", "e:a352",
            "e:ordinance", "e:moneybill", "e:tenth", "e:delim", "e:basic", "e:collegium", "e:isc", "e:zonal", "e:pri",
            "c:c_jointsitting", "c:c_quorum", "c:c_zerohour", "c:c_moneybill", "c:c_nota", "c:c_mcc", "c:c_rpa", "c:c_funds", "c:c_budget"));
}
