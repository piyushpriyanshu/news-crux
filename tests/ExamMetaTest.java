import app.newsbreak.core.*;
import org.json.*;
import java.nio.file.*;

/** Exam news, roundups and quiz posts never reach a card; real news does (run by tests/run.sh). */
public class ExamMetaTest {
    static final String D = System.getProperty("data", "assets/www/data/");
    static int pass = 0, fail = 0;
    static void ok(boolean c, String what) { if (c) pass++; else { fail++; System.out.println("FAIL: " + what); } }
    public static void main(String[] a) throws Exception {
        String[] drop = {
            "UPSC: Questions in Mains are designed to assess not just knowledge but also critical thinking",
            "UPSC Mains Answer Practice — GS 3: Food security and India's technological self-reliance (Week 180)",
            "UPSC Prelims 2027 Polity Quiz (Week 188): MCQs on Model Code of Conduct, Section 58 of the BNSS, and more",
            "UPSC 2027 Notification will be released on 13 January 2027 at upsconline.nic.in",
            "Check Nobel Prize 2026 Winners List here",
            "Begonia siangensis, Latest News",
            "INSIGHTS Daily Current Affairs + PIB Summary: 10 November 2026",
            "UPSC Key — 11 November 2026: Monsoon, Article 142 and Ramsar sites",
            "UPSC CSE Result 2026 declared: Check toppers list",
            "UPSC Prelims 2026 answer key released",
            "How to prepare for UPSC Prelims in six months: strategy by AIR 3",
            "IAS topper shares her preparation strategy",
            "Civil Services Exam 2026: UPSC releases interview schedule",
            "UPSC Essentials | Daily subject-wise quiz: Environment and Ecology MCQs",
            "Daily Current Affairs Quiz – 9 November 2026",
            "Editorial Analysis: The Hindu, 10 November 2026",
            "SSC CGL 2026 admit card out",
            "BPSC 71st prelims result announced",
            "UPSC aspirants protest outside the commission over CSAT",
        };
        for (String t : drop) ok(ExamMeta.title(t), "should drop: " + t);
        String[] keep = {
            "President appoints new UPSC Chairperson",
            "UPSC withdraws lateral entry advertisement after Centre's request",
            "CDS General Anil Chauhan outlines theatre command strategy",
            "NDA wins Bihar Assembly election results",
            "CSE report finds PM2.5 levels rising in Indo-Gangetic plain",
            "CAPF personnel to get new housing scheme",
            "Supreme Court questions Centre on Governor's assent delays",
            "RBI keeps repo rate unchanged at 5.5%",
            "Knowledge Nugget: Begonia siangensis — new species found in Arunachal Pradesh",
            "Begonia siangensis: new species of begonia discovered in Arunachal Pradesh",
            "Ramsar Convention COP15 adopts Victoria Falls declaration",
            "India, Japan hold 2+2 dialogue; key outcomes",
            "Public Examinations (Prevention of Unfair Means) Act rules notified",
            "IAS officers to undergo Mission Karmayogi training",
            "Nobel Prize in Economics awarded for work on innovation-driven growth",
            "Cabinet approves National Critical Mineral Mission",
        };
        for (String t : keep) ok(!ExamMeta.title(t), "should keep: " + t);

        String[] badLines = {
            "INSIGHTS Daily Current Affairs article covers education, institutional transparency, sports, defence technology.",
            "→ UPSC 2027 Notification will be released on 13 January 2027 at upsconline.nic.in",
            "– → Check Nobel Prize 2026 Winners List here",
            "Useful for UPSC Prelims 2027: questions on the Model Code of Conduct",
        };
        for (String l : badLines) ok(ExamMeta.line(l), "bad line: " + l);
        String[] goodLines = {
            "Begonia siangensis was found in the Siang valley of Arunachal Pradesh.",
            "UPSC is a constitutional body under Article 315.",
            "The Model Code of Conduct is not backed by a statute; ECI enforces it under Article 324.",
            "Vizhinjam is India's first deep-water transshipment port.",
        };
        for (String l : goodLines) ok(!ExamMeta.line(l), "good line: " + l);

        ok(ExamMeta.head("Knowledge Nugget: Begonia siangensis — what you should know for UPSC").equals("Begonia siangensis"), "nugget head: " + ExamMeta.head("Knowledge Nugget: Begonia siangensis — what you should know for UPSC"));

        // the card in the screenshot of 11 Nov goes, its "also" junk goes, a real card stays
        JSONObject ed = new JSONObject("{\"topics\":["
            + "{\"id\":\"a\",\"head\":\"UPSC: Questions in Mains are designed to assess not just knowledge but also critical thinking\",\"bullets\":[\"INSIGHTS Daily Current Affairs article covers education, institutional transparency.\"],"
            + "\"also\":[{\"head\":\"Begonia siangensis, Latest News\",\"bullets\":[\"→ UPSC 2027 Notification will be released on 13 January 2027 at upsconline.nic.in\"]}]},"
            + "{\"id\":\"b\",\"head\":\"Cabinet approves National Critical Mineral Mission\",\"bullets\":[\"Outlay Rs 16,300 crore over 7 years.\",\"→ Check Nobel Prize 2026 Winners List here\"],"
            + "\"also\":[{\"head\":\"UPSC Prelims 2027 Polity Quiz (Week 188): MCQs on Model Code of Conduct\"},{\"head\":\"KABIL to acquire lithium blocks in Argentina\",\"bullets\":[\"Five blocks in Catamarca.\"]}]},"
            + "{\"id\":\"c\",\"head\":\"Ramsar adds two Indian wetlands\",\"bullets\":[\"INSIGHTS Daily Current Affairs article covers urbanisation and exports.\"]}"
            + "],\"catchup\":[{\"id\":\"k\",\"head\":\"UPSC Mains Answer Practice — GS 2: Federalism (Week 180)\"}]}");
        ok(Sentinel.dropMeta(ed), "dropMeta changed");
        JSONArray t = ed.getJSONArray("topics");
        ok(t.length() == 1 && t.getJSONObject(0).getString("id").equals("b"), "only the real card left: " + t);
        JSONObject b = t.getJSONObject(0);
        ok(b.getJSONArray("bullets").length() == 1, "link bullet removed");
        ok(b.getJSONArray("also").length() == 1 && b.getJSONArray("also").getJSONObject(0).getString("head").startsWith("KABIL"), "quiz also removed");
        ok(ed.getJSONArray("catchup").length() == 0, "catch-up practice post removed");
        ok(!Sentinel.dropMeta(ed), "second run changes nothing");

        // stored items: exam news scores as noise
        Kb kb = new Kb(new String(Files.readAllBytes(Paths.get(D + "kb.json")), "UTF-8"));
        JSONObject it = new JSONObject().put("s", "IE").put("t", "UPSC Prelims 2027 Polity Quiz (Week 188): MCQs on Model Code of Conduct").put("d", "Election Commission, Article 324");
        kb.score(it);
        ok(it.has("noise") && it.optDouble("sc") <= 0, "quiz item is noise: " + it);
        JSONObject it2 = new JSONObject().put("s", "PIB").put("t", "Cabinet approves National Critical Mineral Mission").put("d", "Outlay of Rs 16,300 crore");
        kb.score(it2);
        ok(!it2.has("noise") || !it2.getJSONArray("noise").toString().contains("exam"), "real item not exam noise");

        System.out.println("ExamMetaTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
