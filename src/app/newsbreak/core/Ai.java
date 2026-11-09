package app.newsbreak.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/* loaded from: classes.dex */
public final class Ai {
    public static final String ADDENDUM = "\n\n=== APP ADDENDUM (the reader's standing preferences — apply on top of the rules above) ===\nA. Language: plain, easy English that sticks in memory; ultra-short self-note bullets; expand an abbreviation the first time it appears in a topic.\nB. Each topic = what happened (exact figures/dates) + a STATIC RECAP for quick revision: 3–6 shorthand bullets starting 'Static:' covering what UPSC actually asks about that body/concept (article/schedule, act + year, composition, appointment, tenure, removal, eligibility for re-appointment/further office, powers, HQ, founding year, members, India in/out, reports) — prioritise the aspects in the PYQ evidence; then 1–3 'Trap:' bullets with the commonly confused point or exception (e.g. 'Trap: CEC/ECs not eligible for re-appointment'). Shorthand like self-notes (Art, CAA, yrs, →, ↔, vs); no full sentences.\nC. PYQ line: build it ONLY from the 'PYQ evidence' printed under that candidate. Format: 'PYQ: <entity> — <what was asked> ’YY · <what was asked> ’YY', copying the '→ asked:' keywords exactly (they were researched from the full question papers). Never write just a year or a bare name — the reader must see WHAT was asked (appointment, removal, tenure, powers, location…). If no evidence is printed, write 'PYQ THEME: closely related to UPSC's repeated questions on <theme>'. Never invent a year or a question.\nD. Static facts must be correct. If not fully certain, write 'verification required' instead of guessing.\nE. Cross-check every topic against at least two authentic sources (PIB / ministry / official body site + a national daily) where possible; otherwise mark '(single-source)'. If figures differ, write '(conflict: A vs B)'.\nF. Do NOT write questions, quizzes or MCQs. Do NOT add a preamble.\nG. ONE card per story and ONE card per body/scheme per day: when several reports carry the same news, or several developments concern the same body (e.g. Election Commission, RBI, a scheme), write ONE topic and put each extra development as a short '- Also:' bullet inside it. A candidate marked '＋ related' is already grouped — keep it as one topic.\nH. Never repeat within the same edition: a Static/Trap line, profile or map line given in one topic must not appear again in another topic of the same day (write 'Static: see <topic>' instead). Every bullet must add new information.\nI. Foreign offices (US President/Vice President, another country's court, parliament or election) are not India's: never attach Indian polity static to them; give only the India angle, or skip the topic if there is none.\n";
    public static final String ENRICH_SYSTEM = "You write exam-ready news cards for a UPSC Civil Services aspirant (Prelims first). For each TOPIC you get its headline, the current bullets, text from several outlets (The Hindu, Indian Express, PIB, …) and coaching notes (Drishti, Insights, Vision IAS, Vajiram).\nRules:\n1. bullets: 3–5 crisp self-note bullets on WHAT HAPPENED, each leading with what Prelims asks — which body/ministry/organisation, where (State/country), the exact number, the year/date, the law/Article/Schedule, and any 'first/largest/only' claim. Plain easy English, one fact per bullet, max ~20 words; expand an abbreviation the first time; write dates as '4 Oct 2026' and keep the source's units (₹ crore, lakh, %, km). No commentary, quotes or opinions. Use ONLY facts found in the supplied texts. Cross-check the outlets against each other: if they disagree on a figure, do not pick one — add it to 'conflicts' as '<what>: <A> (<outlet>) vs <B> (<outlet>)'.\n2. about: explain the SPECIFIC subject in the news — the temple, species, technique, disease, scheme, law, place or treaty itself, not the parent body (e.g. for 'Ramappa Temple gets UNESCO grant' explain Ramappa Temple: where, built by whom, when, style; not UNESCO). 2–5 static points a Prelims question could test (what it is, who built/created it, when, where, key features/provisions, composition). For an international body: full form, year founded, HQ, members, India in/out. For an Indian body: Article or Act, who appoints, tenure, removal. Use settled textbook facts and the coaching notes; if you are not fully certain of a static fact, leave it out — never guess. Omit 'about' when the subject is already covered by the KNOWN CARDS listed under that topic.\n3. traps: 0–2 common confusions about the subject ('X is not Y', 'only X, not Y'), only when certain.\n4. No questions, no opinions, no 'way forward', no filler. Keep every topic id exactly as given.\n5. unrelated: the KNOWN CARDS were attached by keyword matching and can be wrong. List the exact name of every known card that is NOT about this topic's news — a body, scheme, place or concept that the sources only mention in passing or that belongs to a different story (e.g. MGNREGA on a card about an El Niño review meeting; the Election Commission on a card about child marriage). Leave it empty when every known card fits.\n6. A topic that lists ALREADY COVERED lines is an update on an earlier story: write bullets only about what is new since then and never restate those lines; omit 'about'.\n7. PYQ ASKED lines show what UPSC actually asked about this subject: let 'about' and 'traps' cover those aspects first.\n8. Foreign offices and foreign politics (a US/other country's President, Vice President, Prime Minister, court, parliament, election or party): never add static facts about India's own offices (no Article 54/63/66, no Electoral College of India, no Election Commission of India) and no 'about' on the foreign election system. If the topic is marked SCOPE: India link, write only the India angle (trade, tariffs, visas, defence, treaty, diaspora, a visit). If it is marked SCOPE: no India link, keep 2–3 bullets of plain fact and omit 'about' and 'traps'.\nOutput ONLY JSON: {\"topics\":[{\"id\":\"…\",\"bullets\":[\"…\"],\"about\":{\"title\":\"…\",\"points\":[\"…\"]},\"traps\":[\"…\"],\"conflicts\":[\"…\"],\"unrelated\":[\"…\"]}]}";
    public static final String SOURCECHECK_JSON_SYSTEM = "You are a meticulous fact-checker for a UPSC Civil Services current-affairs app; the reader cannot afford a single wrong fact. You have NO web access. Check every bullet against the SOURCE TEXTS of its topic: fix any figure/date/name that a source states differently, delete a bullet no source supports, and put outlet disagreements into 'conflicts'. For 'about' and 'traps' keep only settled textbook facts you are certain of (or that the coaching notes state); delete anything doubtful, an 'about' on a different subject, and any India-office static added to a story about another country. Keep the same structure and ids; do not add topics; no questions. Output ONLY the corrected JSON object, with a top-level \"corrections\": <number of changes>.";
    public static final String SOURCECHECK_SYSTEM = "You are a meticulous fact-checker for a UPSC Civil Services current-affairs digest read by a 4-year aspirant who cannot afford a single wrong fact.\nYou have NO web access. Cross-check the digest against the FETCHED SOURCE ITEMS supplied (articles from PIB, ministries, RBI, The Hindu, Indian Express and other authentic outlets).\nRules: every current figure/date/name must match the fetched items — correct it if a source states it differently, mark '(conflict: A vs B)' if two sources differ, and replace it with 'verification required' if no fetched item supports it. Static facts (articles, acts, years, HQ, members) may stay only if they are standard, settled textbook facts you are certain of; otherwise write 'verification required'. Do not add or remove topics, do not add questions, keep the exact locked format and order. Keep PYQ lines unchanged.\nOutput ONLY: the corrected digest, then a line ---FACTBANK--- with the fact-bank lines (corrected), then a line ---CHECKLOG--- with the check-log lines plus one line 'CROSS-CHECK: <number> correction(s) — <very short list>'.";
    public static final String VERIFY_JSON_SYSTEM = "You are a meticulous fact-checker for a UPSC Civil Services current-affairs app; the reader cannot afford a single wrong fact. Using web search on authentic sources (PIB, official ministry/body sites, RBI, UN/agency sites, UNESCO, The Hindu, Indian Express), check EVERY figure, date, name, article/section number, year, location and static fact in the cards JSON. Correct anything wrong; delete any point you cannot confirm; if authentic sources disagree, move it to 'conflicts'. Delete an 'about' that explains a different subject from the card's news, and any India-office static (President's election, ECI, Articles) added to a story about another country. Keep the same structure and ids; do not add topics; no questions. Output ONLY the corrected JSON object, with a top-level \"corrections\": <number of changes>.";
    public static final String VERIFY_SYSTEM = "You are a meticulous fact-checker for a UPSC Civil Services current-affairs digest read by a 4-year aspirant who cannot afford a single wrong fact.\nUsing web search on authentic sources (PIB, official ministry/body websites, RBI, PRS, Parliament, UN/agency sites, The Hindu, Indian Express), check EVERY figure, date, name, article/section/schedule number, act and year, membership count, HQ, founding year and static fact in the digest you are given.\nRules: correct anything wrong; if a fact cannot be confirmed write 'verification required'; if authentic sources disagree write '(conflict: A vs B)'; do not add or remove topics; do not add questions; keep the exact same locked format, order and line types (bold headline lines, '- ' bullets, 'PYQ'/'Map:' lines, PIB filter). Keep PYQ lines unchanged.\nOutput ONLY: the corrected digest, then a line ---FACTBANK--- with the fact-bank lines (corrected), then a line ---CHECKLOG--- with the check-log lines plus one line 'VERIFY: <number> correction(s) — <very short list>'.";
    public static final String[] PROVIDERS = {"gemini", "groq", "openrouter", "openai", "xai", "claude"};
    private static final Pattern DATE_HEAD = Pattern.compile("^[#*\\s]*\\d{1,2} [A-Z][a-z]+ \\d{4}");
    private static final Pattern HEAD = Pattern.compile("^(\\d+[.)]\\s*)?\\*\\*[^*]+\\*\\*");
    public static volatile String testBase = null;

    public static final class Parsed {
        public String digest = "";
        public String facts = "";
        public String checklog = "";
    }

    public static final class Result {
        public int code;
        public boolean searchDropped;
        public boolean tooLarge;
        public boolean usedSearch;
        public String text = "";
        public String error = null;
        public String model = "";
        public String provider = "";
        public final Map<String, String> citations = new LinkedHashMap();
        public String body = "";
        public double retryAfter = 0.0d;
    }

    private Ai() {
    }

    public static String detectProvider(String str) {
        if (str == null) {
            return null;
        }
        String trim = str.trim();
        if (trim.startsWith("sk-ant-")) {
            return "claude";
        }
        if (trim.startsWith("sk-or-")) {
            return "openrouter";
        }
        if (trim.startsWith("gsk_")) {
            return "groq";
        }
        if (trim.startsWith("xai-")) {
            return "xai";
        }
        if (trim.startsWith("AIza") || trim.startsWith("AQ.")) {
            return "gemini";
        }
        if (trim.startsWith("sk-")) {
            return "openai";
        }
        return null;
    }

    static String[] fallbacks(String str) {
        return "gemini".equals(str) ? new String[]{"gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-2.5-flash-lite"} : "groq".equals(str) ? new String[]{"openai/gpt-oss-120b", "openai/gpt-oss-20b", "llama-3.3-70b-versatile"} : "openrouter".equals(str) ? new String[]{"openrouter/free", "google/gemini-2.5-flash-lite"} : "openai".equals(str) ? new String[]{"gpt-6-luna", "gpt-6.1-sol"} : "xai".equals(str) ? new String[]{"grok-4.7"} : new String[]{"claude-haiku-4-5-20251001", "claude-sonnet-5-5"};
    }

    public static String defaultModel(String str) {
        return "gemini".equals(str) ? "gemini-3.5-flash-lite" : "groq".equals(str) ? "openai/gpt-oss-120b" : "openrouter".equals(str) ? "openrouter/free" : "openai".equals(str) ? "gpt-6-luna" : "xai".equals(str) ? "grok-4.7" : "claude-haiku-4-5-20251001";
    }

    public static String providerName(String str) {
        return "gemini".equals(str) ? "Gemini" : "groq".equals(str) ? "Groq" : "openrouter".equals(str) ? "OpenRouter" : "openai".equals(str) ? "OpenAI" : "xai".equals(str) ? "Grok (xAI)" : "Claude";
    }

    public static boolean canSearch(String str, String str2) {
        if ("groq".equals(str)) {
            return str2 != null && str2.startsWith("openai/gpt-oss");
        }
        if ("openrouter".equals(str)) {
            return (str2 == null || str2.equals("openrouter/free") || str2.endsWith(":free")) ? false : true;
        }
        return true;
    }

    public static String sig(JSONObject jSONObject) {
        StringBuilder sb = new StringBuilder(jSONObject.optString("head"));
        JSONArray optJSONArray = jSONObject.optJSONArray("bullets");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            sb.append('|').append(optJSONArray.optString(i));
        }
        JSONArray optJSONArray2 = jSONObject.optJSONArray("also");
        for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
            sb.append('|').append(optJSONArray2.optJSONObject(i2).optString("head"));
        }
        return Text.hash(sb.toString());
    }

    static void sourceTexts(StringBuilder sb, JSONObject jSONObject, Map<String, JSONObject> map, boolean z) {
        String optString;
        JSONArray optJSONArray;
        int i;
        JSONArray optJSONArray2 = jSONObject.optJSONArray("iids");
        int i2 = 0;
        int i3 = z ? 520 : 1300;
        int i4 = 0;
        while (optJSONArray2 != null && i4 < optJSONArray2.length()) {
            if (i2 < (z ? 3 : 5)) {
                JSONObject jSONObject2 = map.get(optJSONArray2.optString(i4));
                if (jSONObject2 == null) {
                    i = i2;
                } else {
                    JSONObject optJSONObject = jSONObject2.optJSONObject("cx");
                    if (optJSONObject != null) {
                        StringBuilder sb2 = new StringBuilder();
                        JSONArray optJSONArray3 = optJSONObject.optJSONArray("sec");
                        for (int i5 = 0; optJSONArray3 != null && i5 < optJSONArray3.length() && sb2.length() < i3; i5++) {
                            JSONObject optJSONObject2 = optJSONArray3.optJSONObject(i5);
                            if (!"skip".equals(optJSONObject2.optString("k"))) {
                                sb2.append(optJSONObject2.optString("h")).append(": ");
                                JSONArray optJSONArray4 = optJSONObject2.optJSONArray("p");
                                for (int i6 = 0; optJSONArray4 != null && i6 < optJSONArray4.length(); i6++) {
                                    sb2.append(optJSONArray4.optString(i6)).append(" / ");
                                }
                                sb2.append('\n');
                            }
                        }
                        optString = sb2.toString();
                    } else {
                        optString = jSONObject2.optString("xb").length() > 0 ? jSONObject2.optString("xb") : jSONObject2.optString("d");
                        if (optString.length() == 0 && (optJSONArray = jSONObject2.optJSONArray("x")) != null) {
                            String str = optString;
                            for (int i7 = 0; i7 < optJSONArray.length(); i7++) {
                                str = str + optJSONArray.optString(i7) + " ";
                            }
                            optString = str;
                        }
                    }
                    if (optString.trim().length() < 20) {
                        i = i2;
                    } else {
                        sb.append("   [").append(jSONObject2.optString("s")).append(optJSONObject != null ? " coaching notes" : "").append("] ").append(jSONObject2.optString("t")).append(" — ").append(Text.cap(optString.replace("\n", " ").trim(), i3)).append('\n');
                        i = i2 + 1;
                    }
                }
                i4++;
                i2 = i;
            } else {
                return;
            }
        }
    }

    static void knownCards(StringBuilder sb, JSONObject jSONObject, Kb kb) {
        ArrayList arrayList = new ArrayList();
        JSONArray optJSONArray = jSONObject.optJSONArray("ents");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            JSONObject entity = kb.entity(optJSONArray.optString(i));
            if (entity != null) {
                arrayList.add(entity.optString("name"));
            }
        }
        JSONArray optJSONArray2 = jSONObject.optJSONArray("cons");
        for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
            JSONObject concept = kb.concept(optJSONArray2.optString(i2));
            if (concept != null) {
                arrayList.add(concept.optString("name"));
            }
        }
        JSONArray optJSONArray3 = jSONObject.optJSONArray("maps");
        for (int i3 = 0; optJSONArray3 != null && i3 < optJSONArray3.length(); i3++) {
            JSONObject map = kb.map(optJSONArray3.optString(i3));
            if (map != null) {
                arrayList.add(map.optString("name"));
            }
        }
        if (!arrayList.isEmpty()) {
            sb.append("   KNOWN CARDS (already shown from the verified knowledge base — don't repeat): ").append(arrayList).append('\n');
        }
    }

    public static String enrichMessage(String str, List<JSONObject> list, Map<String, JSONObject> map, Kb kb, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("NEWS DATE: ").append(Ist.heading(str)).append(" (IST)\n\n");
        for (JSONObject jSONObject : list) {
            sb.append("TOPIC id=").append(jSONObject.optString("id")).append(" | ").append(jSONObject.optString("gs")).append(" | ").append(jSONObject.optString("head")).append('\n');
            JSONArray optJSONArray = jSONObject.optJSONArray("bullets");
            for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
                sb.append("   current bullet: ").append(optJSONArray.optString(i)).append('\n');
            }
            JSONArray optJSONArray2 = jSONObject.optJSONArray("also");
            for (int i2 = 0; optJSONArray2 != null && i2 < optJSONArray2.length(); i2++) {
                sb.append("   also today (same body, keep as an extra bullet): ").append(optJSONArray2.optJSONObject(i2).optString("head")).append('\n');
            }
            JSONArray optJSONArray3 = jSONObject.optJSONArray("branch");
            for (int i3 = 0; optJSONArray3 != null && i3 < optJSONArray3.length(); i3++) {
                JSONObject optJSONObject = optJSONArray3.optJSONObject(i3);
                sb.append("   ALREADY COVERED on ").append(optJSONObject.optString("d")).append(": ").append(optJSONObject.optString("h")).append(optJSONObject.optString("b").length() > 0 ? " — " + optJSONObject.optString("b") : "").append('\n');
            }
            JSONArray ev = jSONObject.optJSONArray("pyqEv");
            if (ev != null && ev.length() > 0) {
                sb.append("   PYQ ASKED:");
                for (int i4 = 0; i4 < ev.length() && i4 < 4; i4++) {
                    JSONArray e = ev.optJSONArray(i4);
                    if (e != null) sb.append(i4 == 0 ? " " : " · ").append(e.optInt(0)).append(' ').append(e.optString(1));
                }
                sb.append('\n');
            }
            String vis = Sentinel.visible(jSONObject);
            if (Foreign.politics(vis)) sb.append(Foreign.india(vis) ? "   SCOPE: India link (foreign office in the news: give only the India angle)\n" : "   SCOPE: no India link (foreign politics: plain facts only, no India-office static)\n");
            sourceTexts(sb, jSONObject, map, z);
            if (kb != null) {
                knownCards(sb, jSONObject, kb);
            }
            sb.append('\n');
        }
        sb.append("Write the cards now. Output ONLY the JSON object.");
        return sb.toString();
    }

    public static String verifyMessage(String str, JSONObject jSONObject, List<JSONObject> list, Map<String, JSONObject> map, Kb kb, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("NEWS DATE: ").append(Ist.heading(str)).append(" (IST)\n\nCARDS TO CHECK:\n").append(jSONObject.toString()).append("\n\nSOURCE TEXTS:\n");
        for (JSONObject jSONObject2 : list) {
            sb.append("TOPIC id=").append(jSONObject2.optString("id")).append(" | ").append(jSONObject2.optString("head")).append('\n');
            sourceTexts(sb, jSONObject2, map, z);
        }
        sb.append("\nOutput ONLY the corrected JSON object with \"corrections\".");
        return sb.toString();
    }

    public static JSONObject parseJson(String str) {
        if (str == null) {
            return null;
        }
        String replaceAll = str.trim().replaceAll("(?s)^```(?:json)?\\s*", "").replaceAll("(?s)```\\s*$", "");
        int indexOf = replaceAll.indexOf(123);
        int lastIndexOf = replaceAll.lastIndexOf(125);
        if (indexOf < 0 || lastIndexOf <= indexOf) {
            return null;
        }
        try {
            JSONObject jSONObject = new JSONObject(replaceAll.substring(indexOf, lastIndexOf + 1));
            if (jSONObject.optJSONArray("topics") == null) {
                jSONObject = null;
            }
            return jSONObject;
        } catch (Exception e) {
            return null;
        }
    }

    static JSONArray lines(JSONArray jSONArray, int i, int i2) {
        JSONArray jSONArray2 = new JSONArray();
        for (int i3 = 0; jSONArray != null && i3 < jSONArray.length() && jSONArray2.length() < i; i3++) {
            String replaceAll = jSONArray.optString(i3).trim().replaceAll("^(?:[-•*]+|\\d{1,2}[.)])\\s+", "").replaceAll("\\s+", " ");
            if (replaceAll.length() >= 8 && replaceAll.length() <= i2 && !replaceAll.endsWith("?") && !replaceAll.toLowerCase().contains("verification required")) {
                jSONArray2.put(replaceAll);
            }
        }
        return jSONArray2;
    }

    public static JSONArray unrelated(JSONObject jSONObject) {
        JSONArray jSONArray = new JSONArray();
        JSONArray optJSONArray = jSONObject == null ? null : jSONObject.optJSONArray("unrelated");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length() && jSONArray.length() < 6; i++) {
            String trim = optJSONArray.optString(i).trim();
            if (trim.length() >= 2 && trim.length() <= 90) {
                jSONArray.put(trim);
            }
        }
        return jSONArray;
    }

    public static JSONArray unrelated(JSONObject jSONObject, String str) {
        JSONArray optJSONArray = jSONObject == null ? null : jSONObject.optJSONArray("topics");
        for (int i = 0; optJSONArray != null && i < optJSONArray.length(); i++) {
            JSONObject optJSONObject = optJSONArray.optJSONObject(i);
            if (optJSONObject != null && str.equals(optJSONObject.optString("id"))) {
                return unrelated(optJSONObject);
            }
        }
        return new JSONArray();
    }

    public static JSONObject cleanCard(JSONObject jSONObject) {
        if (jSONObject == null) {
            return null;
        }
        try {
            if (jSONObject.optString("id").length() == 0) {
                return null;
            }
            JSONObject put = new JSONObject().put("id", jSONObject.optString("id"));
            JSONArray lines = lines(jSONObject.optJSONArray("bullets"), 6, 320);
            if (lines.length() == 0) {
                return null;
            }
            put.put("b", lines);
            JSONObject optJSONObject = jSONObject.optJSONObject("about");
            if (optJSONObject != null) {
                JSONArray lines2 = lines(optJSONObject.optJSONArray("points"), 6, 300);
                String trim = optJSONObject.optString("title").trim();
                if (lines2.length() > 0 && trim.length() >= 2 && trim.length() <= 90) {
                    put.put("ab", new JSONObject().put("h", trim).put("pts", lines2));
                }
            }
            JSONArray lines3 = lines(jSONObject.optJSONArray("traps"), 3, 240);
            if (lines3.length() > 0) {
                put.put("tr", lines3);
            }
            JSONArray lines4 = lines(jSONObject.optJSONArray("conflicts"), 3, 240);
            if (lines4.length() > 0) {
                put.put("cf", lines4);
            }
            JSONArray unrelated = unrelated(jSONObject);
            if (unrelated.length() > 0) {
                put.put("un", unrelated);
            }
            return put;
        } catch (Exception e) {
            return null;
        }
    }

    public static String userMessage(String str, List<JSONObject> list, String str2, JSONObject jSONObject, List<String> list2, boolean z) {
        return userMessage(str, list, str2, jSONObject, list2, z ? 1 : 0);
    }

    public static String userMessage(String str, List<JSONObject> list, String str2, JSONObject jSONObject, List<String> list2, int i) {
        return userMessage(str, list, str2, jSONObject, list2, i, null);
    }

    public static String userMessage(String str, List<JSONObject> list, String str2, JSONObject jSONObject, List<String> list2, int i, List<String> list3) {
        int i2;
        int i3;
        boolean z = i >= 1;
        boolean z2 = i >= 2;
        StringBuilder sb = new StringBuilder();
        sb.append("NEWS DATE: ").append(Ist.heading(str)).append(" — window 00:00–23:59 IST (Asia/Calcutta).\n");
        sb.append("Produce the edition for this date now, following the system prompt exactly.\n\n");
        if (z2) {
            sb.append("=== PYQ TIERS (UPSC CSE 1995–2025) ===\nHIGH: Parliament, fundamental rights, national movement, climatology, protected areas, basic science, economic geography, map locations, industry, economic concepts, external sector, public finance, inflation. MED: judiciary, panchayats, amendments/bodies, agriculture, taxation, securities, species, climate change, pollution, biotech, space, defence tech, ICT, Buddhism, temple architecture, UN, places in news.\n");
        } else {
            sb.append("=== USER'S PYQ THEME-FREQUENCY TIERS (UPSC CSE, 3,489 Qs, 1995–2025) ===\n").append(str2).append("\n");
        }
        sb.append("Recent-paper shift (2013–2025 vs earlier, from the same database): rising — species, protected areas, pollution, climate change, ecology, money & banking, emerging tech, international organisations, schemes/governance, agriculture, parliament, art & culture; falling — basic science GK, modern history, space-mission trivia, awards.\n\n");
        sb.append("=== CANDIDATE STORIES (clustered across sources, ranked by PYQ alignment; [codes] = outlets reporting it) ===\n");
        int i4 = 0;
        for (JSONObject jSONObject2 : list) {
            if (z2) {
                i2 = 12;
            } else {
                i2 = z ? 24 : 45;
            }
            if (i4 >= i2) {
                break;
            }
            i4++;
            sb.append(i4).append(". ").append(jSONObject2.optString("head")).append("  [").append(jSONObject2.optString("srcs")).append("]  theme: ").append(jSONObject2.optString("theme")).append('\n');
            JSONArray optJSONArray = jSONObject2.optJSONArray("items");
            for (int i5 = 0; optJSONArray != null && i5 < optJSONArray.length(); i5++) {
                if (i5 >= (z ? 1 : 3) || (z2 && i5 > 0)) {
                    break;
                }
                JSONObject optJSONObject = optJSONArray.optJSONObject(i5);
                String optString = optJSONObject.optString("d");
                sb.append("   - [").append(optJSONObject.optString("s")).append("] ").append(optJSONObject.optString("t"));
                if (optString.length() > 0) {
                    StringBuilder append = sb.append(" — ");
                    if (z2) {
                        i3 = 110;
                    } else {
                        i3 = z ? 160 : 300;
                    }
                    append.append(Text.cap(optString, i3));
                }
                sb.append(" <").append(optJSONObject.optString("u")).append(">\n");
            }
            String optString2 = jSONObject2.optString("pyqEvidence");
            if (z2 && optString2.length() > 0) {
                String[] split = optString2.split("\n");
                StringBuilder sb2 = new StringBuilder();
                for (int i6 = 0; i6 < split.length && i6 < 2; i6++) {
                    sb2.append(Text.cap(split[i6], 110)).append('\n');
                }
                optString2 = sb2.toString();
            }
            sb.append("   PYQ evidence:").append(optString2.length() == 0 ? " none\n" : "\n" + optString2);
        }
        if (i4 == 0) {
            sb.append("(no items fetched — rely on web search)\n");
        }
        if (list3 != null && !list3.isEmpty()) {
            sb.append("\n=== COACHING ITEMS FOR THIS DATE (Drishti / Vision / Insights / Vajiram — cross-check only) ===\n");
            int i7 = 0;
            for (String str3 : list3) {
                int i8 = i7 + 1;
                if (i7 >= (z2 ? 8 : 15)) {
                    break;
                }
                sb.append("- ").append(Text.cap(str3, 120)).append('\n');
                i7 = i8;
            }
            sb.append("GAP CHECK: if an exam-relevant topic above is missing from your selection, include it (within the 12-topic cap) ONLY when a primary source (PIB/ministry/national daily) confirms it; build its facts from that primary source, never from the coaching page.\n");
        }
        if (jSONObject != null && jSONObject.optJSONArray("topics") != null) {
            sb.append("\n=== PREVIOUS EDITION (").append(jSONObject.optString("date")).append(") HEADLINES — apply the continuing-stories rule ===\n");
            JSONArray optJSONArray2 = jSONObject.optJSONArray("topics");
            for (int i9 = 0; i9 < optJSONArray2.length(); i9++) {
                sb.append("- ").append(optJSONArray2.optJSONObject(i9).optString("head")).append('\n');
            }
        }
        if (list2 != null && !list2.isEmpty() && !z2) {
            sb.append("\n=== EXISTING FACT BANK (recent lines; deduplicate against these) ===\n");
            Iterator<String> it = list2.iterator();
            while (it.hasNext()) {
                sb.append(it.next()).append('\n');
            }
        }
        sb.append("\n=== OUTPUT CONTRACT (the app parses this) ===\n").append("1) The digest in the locked format (markdown): first line = date heading; optional italic window line; each topic starts with a bold headline line '**Headline** — GS tag — sources'; '- ' bullets; one 'PYQ:' or 'PYQ THEME:' line per topic; optional 'Map:' line; then a '## PIB filter' heading with '- ' lines.\n").append("2) A line ---FACTBANK--- then new fact-bank lines, each starting with [").append(Ist.tag(str)).append("].\n").append("3) A line ---CHECKLOG--- then the 11 pre-send checklist results, one per line: '<n>. OK' or '<n>. FIXED — <what>'.\n");
        return sb.toString();
    }

    public static List<String> validate(String str) {
        ArrayList arrayList = new ArrayList();
        String[] split = str.replace("\r", "").split("\n");
        String str2 = "";
        int length = split.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String str3 = split[i];
            if (str3.trim().length() > 0) {
                str2 = str3.trim();
                break;
            }
            i++;
        }
        if (!DATE_HEAD.matcher(str2).find()) {
            arrayList.add("Rule 1: first line must be the date heading, no preamble");
        }
        boolean z = false;
        boolean z2 = true;
        boolean z3 = false;
        int i2 = 0;
        int i3 = 0;
        for (String str4 : split) {
            String trim = str4.trim();
            if (trim.toLowerCase().matches("^#+\\s*pib.*") || trim.toLowerCase().startsWith("**pib filter")) {
                if (z3 && !z2) {
                    i2++;
                }
                z = true;
                z3 = false;
            } else {
                if (!z && HEAD.matcher(trim).find()) {
                    if (z3 && !z2) {
                        i2++;
                    }
                    i3++;
                    z2 = false;
                    z3 = true;
                } else if (z3 && trim.matches("(?i)^[-•*]?\\s*PYQ.*")) {
                    z2 = true;
                }
                if (trim.matches("(?i)^(q\\.?\\s*\\d|question\\s*\\d|\\(a\\)|a\\)\\s).*")) {
                    arrayList.add("No questions/MCQs allowed — remove: " + Text.cap(trim, 60));
                }
            }
        }
        if (z3 && !z2) {
            i2++;
        }
        if (i3 == 0) {
            arrayList.add("Rule 2: no topic headlines found ('**Headline** — GS tag — source')");
        }
        if (i3 > 12) {
            arrayList.add("Rule 2: " + i3 + " topics — maximum is 12");
        }
        if (i2 > 0) {
            arrayList.add("Rule 4: " + i2 + " topic(s) without a PYQ / PYQ THEME line");
        }
        return arrayList;
    }

    public static Result call(String str, String str2, String str3, boolean z, boolean z2, String str4, String str5, int i) {
        Result call0;
        Result call02 = call0(str, str2, str3, z, z2, str4, str5, i);
        if (call02.code == 404 || (call02.code == 400 && call02.body.toLowerCase().contains("model") && (call02.body.contains("not found") || call02.body.contains("does not exist") || call02.body.contains("not supported") || call02.body.contains("decommissioned")))) {
            for (String str6 : fallbacks(str)) {
                if (!str6.equals(str3)) {
                    call0 = call0(str, str2, str6, z, z2, str4, str5, i);
                    if (call0.code != 404) {
                        break;
                    }
                }
            }
        }
        call0 = call02;
        if (call0.error != null && z && searchRefused(call0)) {
            call0 = call0(str, str2, call0.model, false, false, str4, str5, i);
            call0.searchDropped = true;
        }
        if (call0.error != null && call0.code == 429 && call0.retryAfter > 0.0d && call0.retryAfter <= 75.0d && !quotaGone(call0)) {
            try {
                Thread.sleep(((long) (call0.retryAfter * 1000.0d)) + 1500);
            } catch (InterruptedException e) {
            }
            boolean z3 = call0.searchDropped;
            Result call03 = call0(str, str2, call0.model, z && !z3, z2 && !z3, str4, str5, i);
            call03.searchDropped = call03.searchDropped || z3;
            return call03;
        }
        return call0;
    }

    static boolean searchRefused(Result result) {
        if (result.code == 401) {
            return false;
        }
        String lowerCase = result.body.toLowerCase();
        if ((lowerCase.contains("api key") && (lowerCase.contains("invalid") || lowerCase.contains("not valid"))) || lowerCase.contains("insufficient_quota") || lowerCase.contains("credit")) {
            return false;
        }
        if ((lowerCase.contains("ground") || lowerCase.contains("search") || lowerCase.contains("tool") || lowerCase.contains("plugin") || lowerCase.contains("browser")) && (result.code == 400 || result.code == 403 || result.code == 429 || result.code == 422)) {
            return true;
        }
        return "gemini".equals(result.provider) && !(!(result.code == 429 || result.code == 403) || lowerCase.contains("per day") || lowerCase.contains("perday"));
    }

    static boolean quotaGone(Result result) {
        String lowerCase = result.body.toLowerCase();
        return lowerCase.contains("insufficient_quota") || lowerCase.contains("per day") || lowerCase.contains("perday") || lowerCase.contains("tokens per day") || lowerCase.contains("requests per day") || lowerCase.contains("billing");
    }

    static Result call0(String str, String str2, String str3, boolean z, boolean z2, String str4, String str5, int i) {
        Result result = new Result();
        result.provider = str;
        result.model = str3;
        try {
            if ("gemini".equals(str)) {
                gemini(result, str2, str3, z, str4, str5, i);
            } else if ("groq".equals(str)) {
                openaiChat(result, "https://api.groq.com/openai/v1/chat/completions", str2, str3, z && canSearch(str, str3), z2, str4, str5, i, "groq");
            } else if ("openrouter".equals(str)) {
                openaiChat(result, "https://openrouter.ai/api/v1/chat/completions", str2, str3, z && canSearch(str, str3), z2, str4, str5, i, "openrouter");
            } else if ("openai".equals(str)) {
                openaiResponses(result, "https://api.openai.com/v1/responses", str2, str3, z, str4, str5, i, "openai");
            } else if ("xai".equals(str)) {
                openaiResponses(result, "https://api.x.ai/v1/responses", str2, str3, z, str4, str5, i, "xai");
            } else {
                claude(result, str2, str3, z, str4, str5, i);
            }
        } catch (Exception e) {
            result.error = "Network error: " + e.getClass().getSimpleName() + (e.getMessage() != null ? " — " + Text.cap(e.getMessage(), 140) : "");
        }
        if (result.error == null && result.text.trim().length() < 2) {
            result.error = providerName(str) + " returned an empty response.";
        }
        if (result.error != null) {
            String lowerCase = result.body.toLowerCase();
            result.tooLarge = result.code == 413 || lowerCase.contains("too large") || lowerCase.contains("tokens per minute") || lowerCase.contains("tpm") || lowerCase.contains("context length") || lowerCase.contains("context_length") || lowerCase.contains("maximum context");
            if (result.retryAfter == 0.0d) {
                Matcher matcher = Pattern.compile("(?i)try again in ([0-9.]+)\\s*(ms|s)").matcher(result.body);
                if (matcher.find()) {
                    double parseDouble = Double.parseDouble(matcher.group(1));
                    if ("ms".equalsIgnoreCase(matcher.group(2))) {
                        parseDouble /= 1000.0d;
                    }
                    result.retryAfter = parseDouble;
                }
                Matcher matcher2 = Pattern.compile("\"retryDelay\"\\s*:\\s*\"([0-9.]+)s\"").matcher(result.body);
                if (result.retryAfter == 0.0d && matcher2.find()) {
                    result.retryAfter = Double.parseDouble(matcher2.group(1));
                }
            }
        }
        return result;
    }

    static String errMsg(String str, int i, String str2, String str3) {
        String str4;
        try {
            Object nextValue = new JSONTokener(str2).nextValue();
            JSONObject optJSONObject = nextValue instanceof JSONArray ? ((JSONArray) nextValue).optJSONObject(0) : (JSONObject) nextValue;
            Object opt = optJSONObject.opt("error");
            if (opt instanceof JSONObject) {
                str4 = ((JSONObject) opt).optString("message", str2);
            } else if (opt instanceof String) {
                str4 = (String) opt;
            } else {
                str4 = optJSONObject.has("message") ? optJSONObject.optString("message") : str2;
            }
        } catch (Exception e) {
            str4 = str2;
        }
        String providerName = providerName(str);
        String lowerCase = str2.toLowerCase();
        String str5 = " [" + i + ": " + Text.cap(str4.replaceAll("\\s+", " "), 260) + "]";
        if (lowerCase.contains("api key not valid") || lowerCase.contains("api_key_invalid") || lowerCase.contains("invalid api key") || lowerCase.contains("invalid_api_key") || lowerCase.contains("incorrect api key") || i == 401) {
            return providerName + " rejected the key. Copy the full API key again from " + console(str) + " (no spaces), and make sure it is a " + providerName + " key." + str5;
        }
        if (lowerCase.contains("insufficient_quota") || (lowerCase.contains("exceeded your current quota") && "openai".equals(str))) {
            return "Your OpenAI API account has no credit. The OpenAI API is prepaid and separate from a ChatGPT subscription — add credit at platform.openai.com → Billing, or use Gemini/Groq/OpenRouter instead." + str5;
        }
        if (i == 402 || lowerCase.contains("insufficient credits") || lowerCase.contains("requires more credits")) {
            return providerName + ": not enough credits on this account. Add credit at " + console(str) + "." + str5;
        }
        if (lowerCase.contains("has not been used in project") || lowerCase.contains("service_disabled") || lowerCase.contains("permission_denied")) {
            return "Gemini API is not enabled for this key's Google project. Create the key at aistudio.google.com → Get API key (it enables the API automatically)." + str5;
        }
        if (lowerCase.contains("location is not supported") || lowerCase.contains("unsupported_country")) {
            return providerName + " is not available from your current region/network (VPN?)." + str5;
        }
        if (i == 404) {
            return providerName + ": model '" + str3 + "' not found for this key. Pick another model in Settings." + str5;
        }
        if (i == 413 || lowerCase.contains("too large") || lowerCase.contains("tokens per minute")) {
            return providerName + ": request too large for this key's free-tier limit." + str5;
        }
        return i == 429 ? providerName + ": rate limit / free-tier quota reached. News Crux retries automatically on the next sync." + str5 : i >= 500 ? providerName + " is busy right now (" + i + "). It will retry on the next sync." + str5 : i != 0 ? providerName + " error." + str5 : str4;
    }

    static String console(String str) {
        return "gemini".equals(str) ? "aistudio.google.com" : "groq".equals(str) ? "console.groq.com" : "openrouter".equals(str) ? "openrouter.ai/keys" : "openai".equals(str) ? "platform.openai.com" : "xai".equals(str) ? "console.x.ai" : "console.anthropic.com";
    }

    static boolean toolRejected(int i, String str) {
        String lowerCase = str.toLowerCase();
        return i == 400 && (lowerCase.contains("tool") || lowerCase.contains("google_search") || lowerCase.contains("plugin") || lowerCase.contains("web_search") || lowerCase.contains("browser_search"));
    }

    static void gemini(Result result, String str, String str2, boolean z, String str3, String str4, int i) throws Exception {
        for (int i2 = 0; i2 < 2; i2++) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", str3))));
            jSONObject.put("contents", new JSONArray().put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(new JSONObject().put("text", str4)))));
            jSONObject.put("generationConfig", new JSONObject().put("temperature", 0.2d).put("maxOutputTokens", 16384));
            if (z) {
                jSONObject.put("tools", new JSONArray().put(new JSONObject().put("google_search", new JSONObject())));
            }
            String[] post = post("https://generativelanguage.googleapis.com/v1beta/models/" + str2 + ":generateContent", jSONObject.toString(), i, "x-goog-api-key", str);
            result.code = Integer.parseInt(post[0]);
            if (result.code != 200) {
                if (z && toolRejected(result.code, post[1])) {
                    z = false;
                } else {
                    result.body = post[1];
                    result.retryAfter = ra(post);
                    result.error = errMsg("gemini", result.code, post[1], str2);
                    return;
                }
            } else {
                JSONObject jSONObject2 = new JSONObject(post[1]);
                JSONArray optJSONArray = jSONObject2.optJSONArray("candidates");
                if (optJSONArray == null || optJSONArray.length() == 0) {
                    JSONObject optJSONObject = jSONObject2.optJSONObject("promptFeedback");
                    result.error = "Gemini returned no candidates" + (optJSONObject != null ? " (" + optJSONObject.optString("blockReason") + ")" : "");
                    return;
                }
                JSONObject jSONObject3 = optJSONArray.getJSONObject(0);
                JSONObject optJSONObject2 = jSONObject3.optJSONObject("content");
                StringBuilder sb = new StringBuilder();
                JSONArray optJSONArray2 = optJSONObject2 != null ? optJSONObject2.optJSONArray("parts") : null;
                for (int i3 = 0; optJSONArray2 != null && i3 < optJSONArray2.length(); i3++) {
                    JSONObject jSONObject4 = optJSONArray2.getJSONObject(i3);
                    if (!jSONObject4.optBoolean("thought")) {
                        sb.append(jSONObject4.optString("text"));
                    }
                }
                result.text = sb.toString();
                JSONObject optJSONObject3 = jSONObject3.optJSONObject("groundingMetadata");
                if (optJSONObject3 != null) {
                    JSONArray optJSONArray3 = optJSONObject3.optJSONArray("webSearchQueries");
                    result.usedSearch = optJSONArray3 != null && optJSONArray3.length() > 0;
                    JSONArray optJSONArray4 = optJSONObject3.optJSONArray("groundingChunks");
                    for (int i4 = 0; optJSONArray4 != null && i4 < optJSONArray4.length(); i4++) {
                        JSONObject optJSONObject4 = optJSONArray4.getJSONObject(i4).optJSONObject("web");
                        if (optJSONObject4 != null && optJSONObject4.optString("uri").length() > 0) {
                            result.citations.put(optJSONObject4.optString("uri"), optJSONObject4.optString("title"));
                        }
                    }
                }
                String optString = jSONObject3.optString("finishReason");
                if (result.text.length() != 0 || optString.length() <= 0) {
                    return;
                }
                result.error = "Gemini stopped: " + optString;
                return;
            }
        }
    }

    static void openaiChat(Result result, String str, String str2, String str3, boolean z, boolean z2, String str4, String str5, int i, String str6) throws Exception {
        String[] post;
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < 2) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("model", str3);
                JSONArray jSONArray = new JSONArray();
                jSONArray.put(new JSONObject().put("role", "system").put("content", str4));
                jSONArray.put(new JSONObject().put("role", "user").put("content", str5));
                jSONObject.put("messages", jSONArray);
                jSONObject.put("temperature", 0.2d);
                if ("groq".equals(str6)) {
                    jSONObject.put("max_completion_tokens", 3000);
                    if (str3.startsWith("openai/gpt-oss")) {
                        jSONObject.put("reasoning_effort", "low");
                    }
                    if (z) {
                        jSONObject.put("tools", new JSONArray().put(new JSONObject().put("type", "browser_search")));
                        jSONObject.put("tool_choice", z2 ? "required" : "auto");
                    }
                } else {
                    jSONObject.put("max_tokens", 12000);
                    if (z) {
                        jSONObject.put("plugins", new JSONArray().put(new JSONObject().put("id", "web").put("max_results", 8)));
                    }
                }
                if ("openrouter".equals(str6)) {
                    post = post(str, jSONObject.toString(), i, "Authorization", "Bearer " + str2, "HTTP-Referer", "https://newsbreak.app", "X-Title", "News Crux");
                } else {
                    post = post(str, jSONObject.toString(), i, "Authorization", "Bearer " + str2);
                }
                result.code = Integer.parseInt(post[0]);
                if (result.code != 200) {
                    if (!z || !toolRejected(result.code, post[1])) {
                        break;
                    }
                    z = false;
                    i2 = i3 + 1;
                } else {
                    JSONArray optJSONArray = new JSONObject(post[1]).optJSONArray("choices");
                    if (optJSONArray != null && optJSONArray.length() != 0) {
                        JSONObject optJSONObject = optJSONArray.getJSONObject(0).optJSONObject("message");
                        result.text = optJSONObject == null ? "" : optJSONObject.optString("content");
                        if (optJSONObject != null) {
                            JSONArray optJSONArray2 = optJSONObject.optJSONArray("annotations");
                            for (int i4 = 0; optJSONArray2 != null && i4 < optJSONArray2.length(); i4++) {
                                JSONObject optJSONObject2 = optJSONArray2.getJSONObject(i4).optJSONObject("url_citation");
                                if (optJSONObject2 != null) {
                                    result.citations.put(optJSONObject2.optString("url"), optJSONObject2.optString("title"));
                                }
                            }
                            Object opt = optJSONObject.opt("executed_tools");
                            if (opt != null) {
                                result.usedSearch = true;
                                collectUrls(opt, result.citations, 0);
                            }
                        }
                        if (!z || !"openrouter".equals(str6)) {
                            return;
                        }
                        result.usedSearch = result.usedSearch || !result.citations.isEmpty();
                        return;
                    }
                    result.error = providerName(str6) + " returned no choices";
                    return;
                }
            } else {
                return;
            }
        }
        result.body = post[1];
        result.retryAfter = ra(post);
        result.error = errMsg(str6, result.code, post[1], str3);
    }

    static void collectUrls(Object obj, Map<String, String> map, int i) {
        int i2 = 0;
        if (i <= 8 && map.size() <= 40) {
            if (obj instanceof JSONObject) {
                JSONObject jSONObject = (JSONObject) obj;
                String optString = jSONObject.optString("url", "");
                if (optString.startsWith("http")) {
                    map.put(optString, jSONObject.optString("title", optString));
                }
                JSONArray names = jSONObject.names();
                while (names != null && i2 < names.length()) {
                    collectUrls(jSONObject.opt(names.optString(i2)), map, i + 1);
                    i2++;
                }
                return;
            }
            if (obj instanceof JSONArray) {
                JSONArray jSONArray = (JSONArray) obj;
                while (i2 < jSONArray.length()) {
                    collectUrls(jSONArray.opt(i2), map, i + 1);
                    i2++;
                }
            }
        }
    }

    static void openaiResponses(Result result, String str, String str2, String str3, boolean z, String str4, String str5, int i, String str6) throws Exception {
        for (int i2 = 0; i2 < 2; i2++) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("model", str3);
            jSONObject.put("instructions", str4);
            jSONObject.put("input", str5);
            jSONObject.put("max_output_tokens", 16000);
            if (z) {
                jSONObject.put("tools", new JSONArray().put(new JSONObject().put("type", "web_search")));
            }
            String[] post = post(str, jSONObject.toString(), i, "Authorization", "Bearer " + str2);
            result.code = Integer.parseInt(post[0]);
            if (result.code != 200) {
                if (z && toolRejected(result.code, post[1])) {
                    z = false;
                } else {
                    result.body = post[1];
                    result.retryAfter = ra(post);
                    result.error = errMsg(str6, result.code, post[1], str3);
                    return;
                }
            } else {
                JSONObject jSONObject2 = new JSONObject(post[1]);
                StringBuilder sb = new StringBuilder();
                JSONArray optJSONArray = jSONObject2.optJSONArray("output");
                for (int i3 = 0; optJSONArray != null && i3 < optJSONArray.length(); i3++) {
                    JSONObject jSONObject3 = optJSONArray.getJSONObject(i3);
                    String optString = jSONObject3.optString("type");
                    if ("web_search_call".equals(optString)) {
                        result.usedSearch = true;
                    }
                    if ("message".equals(optString)) {
                        JSONArray optJSONArray2 = jSONObject3.optJSONArray("content");
                        for (int i4 = 0; optJSONArray2 != null && i4 < optJSONArray2.length(); i4++) {
                            JSONObject jSONObject4 = optJSONArray2.getJSONObject(i4);
                            if ("output_text".equals(jSONObject4.optString("type"))) {
                                sb.append(jSONObject4.optString("text"));
                                JSONArray optJSONArray3 = jSONObject4.optJSONArray("annotations");
                                for (int i5 = 0; optJSONArray3 != null && i5 < optJSONArray3.length(); i5++) {
                                    JSONObject jSONObject5 = optJSONArray3.getJSONObject(i5);
                                    if (jSONObject5.optString("url").length() > 0) {
                                        result.citations.put(jSONObject5.optString("url"), jSONObject5.optString("title"));
                                    }
                                }
                            }
                        }
                    }
                }
                if (sb.length() == 0) {
                    sb.append(jSONObject2.optString("output_text"));
                }
                result.text = sb.toString();
                if (!"incomplete".equals(jSONObject2.optString("status")) || result.text.length() >= 200) {
                    return;
                }
                result.error = "OpenAI response incomplete (" + jSONObject2.optJSONObject("incomplete_details") + ")";
                return;
            }
        }
    }

    static void claude(Result result, String str, String str2, boolean z, String str3, String str4, int i) throws Exception {
        int i2;
        JSONArray put = new JSONArray().put(new JSONObject().put("role", "user").put("content", str4));
        StringBuilder sb = new StringBuilder();
        int i3 = 0;
        boolean z2 = z;
        while (i3 < 6) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("model", str2);
            jSONObject.put("max_tokens", 12000);
            jSONObject.put("system", str3);
            jSONObject.put("messages", put);
            if (z2) {
                jSONObject.put("tools", new JSONArray().put(new JSONObject().put("type", "web_search_20250305").put("name", "web_search").put("max_uses", 10)));
            }
            String[] post = post("https://api.anthropic.com/v1/messages", jSONObject.toString(), i, "x-api-key", str, "anthropic-version", "2023-06-01");
            result.code = Integer.parseInt(post[0]);
            if (result.code != 200) {
                if (!z2 || !toolRejected(result.code, post[1])) {
                    result.body = post[1];
                    result.retryAfter = ra(post);
                    result.error = errMsg("claude", result.code, post[1], str2);
                    return;
                }
                z2 = false;
                i2 = i3 - 1;
            } else {
                JSONObject jSONObject2 = new JSONObject(post[1]);
                JSONArray jSONArray = jSONObject2.getJSONArray("content");
                for (int i4 = 0; i4 < jSONArray.length(); i4++) {
                    JSONObject jSONObject3 = jSONArray.getJSONObject(i4);
                    String optString = jSONObject3.optString("type");
                    if ("text".equals(optString)) {
                        sb.append(jSONObject3.optString("text"));
                        JSONArray optJSONArray = jSONObject3.optJSONArray("citations");
                        for (int i5 = 0; optJSONArray != null && i5 < optJSONArray.length(); i5++) {
                            JSONObject jSONObject4 = optJSONArray.getJSONObject(i5);
                            if (jSONObject4.optString("url").length() > 0) {
                                result.citations.put(jSONObject4.optString("url"), jSONObject4.optString("title"));
                            }
                        }
                    } else if ("server_tool_use".equals(optString)) {
                        result.usedSearch = true;
                    }
                }
                if (!"pause_turn".equals(jSONObject2.optString("stop_reason"))) {
                    break;
                }
                put.put(new JSONObject().put("role", "assistant").put("content", jSONArray));
                i2 = i3;
            }
            i3 = i2 + 1;
        }
        result.text = sb.toString();
    }

    public static Parsed split(String str) {
        String substring;
        Parsed parsed = new Parsed();
        String replaceAll = str.trim().replaceAll("(?s)^```(?:markdown|md)?\\s*", "").replaceAll("(?s)```\\s*$", "");
        int indexOf = replaceAll.indexOf("---FACTBANK---");
        int indexOf2 = replaceAll.indexOf("---CHECKLOG---");
        if (indexOf >= 0) {
            substring = replaceAll.substring(0, indexOf);
        } else {
            substring = indexOf2 >= 0 ? replaceAll.substring(0, indexOf2) : replaceAll;
        }
        if (indexOf >= 0) {
            parsed.facts = replaceAll.substring(indexOf + 14, indexOf2 > indexOf ? indexOf2 : replaceAll.length()).trim();
        }
        if (indexOf2 >= 0) {
            parsed.checklog = replaceAll.substring(indexOf2 + 14).trim();
        }
        Matcher matcher = Pattern.compile("(?m)^[#*\\s]*\\d{1,2} [A-Z][a-z]+ \\d{4}").matcher(substring);
        parsed.digest = ((!matcher.find() || matcher.start() <= 0 || matcher.start() >= 800) ? substring : substring.substring(matcher.start())).trim();
        return parsed;
    }

    static String[] post(String str, String str2, int i, String... strArr) throws Exception {
        if (testBase != null) {
            str = testBase + str.replaceFirst("^https://[^/]+", "");
        }
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setConnectTimeout(20000);
        httpURLConnection.setReadTimeout(i * 1000);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestProperty("content-type", "application/json");
        for (int i2 = 0; i2 + 1 < strArr.length; i2 += 2) {
            httpURLConnection.setRequestProperty(strArr[i2], strArr[i2 + 1]);
        }
        OutputStream outputStream = httpURLConnection.getOutputStream();
        outputStream.write(str2.getBytes("UTF-8"));
        outputStream.close();
        int responseCode = httpURLConnection.getResponseCode();
        InputStream errorStream = responseCode >= 400 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
        String read = errorStream == null ? "" : read(errorStream);
        String headerField = httpURLConnection.getHeaderField("Retry-After");
        httpURLConnection.disconnect();
        String[] strArr2 = new String[3];
        strArr2[0] = String.valueOf(responseCode);
        strArr2[1] = read;
        strArr2[2] = headerField == null ? "" : headerField;
        return strArr2;
    }

    static double ra(String[] strArr) {
        try {
            if (strArr.length <= 2 || strArr[2].length() <= 0) {
                return 0.0d;
            }
            return Double.parseDouble(strArr[2].trim());
        } catch (Exception e) {
            return 0.0d;
        }
    }

    public static String read(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[16384];
        while (true) {
            int read = inputStream.read(bArr);
            if (read <= 0) {
                inputStream.close();
                return byteArrayOutputStream.toString("UTF-8");
            }
            byteArrayOutputStream.write(bArr, 0, read);
        }
    }

    static List<String> unused() {
        return Collections.emptyList();
    }
}
