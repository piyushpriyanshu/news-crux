import app.newsbreak.core.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

/** PYQ data (CSE 1995–2026 with answers) and the new knowledge-base cards (run by tests/run.sh). */
public class PyqTest {
    static final String D = System.getProperty("data", "assets/www/data/");
    static int pass = 0, fail = 0;
    static void ok(boolean c, String w) { if (c) pass++; else { fail++; System.out.println("FAIL: " + w); } }
    static String r(String f) throws Exception { return new String(Files.readAllBytes(Paths.get(D + f)), "UTF-8"); }
    public static void main(String[] a) throws Exception {
        Kb kb = new Kb(r("kb.json"));
        String pj = r("pyq.js"), tj = r("pyq_text.js");
        Pyq p = new Pyq(pj, tj, kb);
        JSONObject text = new JSONObject(tj.substring(tj.indexOf('{'), tj.lastIndexOf('}') + 1));
        int y26 = 0, keyed = 0;
        for (Pyq.Row x : p.rows()) { if (x.year == 2026) y26++; if (p.key(x) != null) keyed++; }
        ok(y26 == 100, "2026 paper: " + y26);
        ok(keyed == p.size(), "every CSE row has its full question: " + keyed + "/" + p.size());
        boolean shapes = true;
        for (Iterator<String> it = text.keys(); it.hasNext(); ) {
            JSONArray q = text.getJSONArray(it.next());
            if (q.getString(0).length() < 5 || q.getJSONArray(1).length() != 4 || q.getInt(2) < 0 || q.getInt(2) > 3) shapes = false;
        }
        ok(shapes, "every question has a stem, 4 options and an answer");
        // every researched "what was asked" line still points at a question
        JSONObject asp = kb.raw.getJSONObject("pyqFix").getJSONObject("asp");
        Set<String> keys = new HashSet<String>();
        for (Pyq.Row x : p.rows()) keys.add(x.year + "|" + x.qno + "|" + x.theme);
        int lost = 0;
        for (Iterator<String> it = asp.keys(); it.hasNext(); ) { String k = it.next(); if (!keys.contains(k)) { lost++; System.out.println("  lost aspect " + k); } }
        ok(lost == 0, "researched aspects all mapped (" + asp.length() + ")");
        // matching keeps the researched lines first
        String eci = Pyq.compact(p.matchTopic("Election Commission revises electoral rolls in Bihar", "", "GS-II/Polity", new LinkedHashSet<String>(kb.idsIn("Election Commission revises electoral rolls")), 4));
        ok(eci.startsWith("ECI size (3 not 5)"), "ECI line: " + eci);
        String vp = Pyq.compact(p.matchTopic("Vice-President election: candidate files nomination", "", "GS-II/Polity", new LinkedHashSet<String>(kb.idsIn("Vice-President election")), 4));
        ok(vp.contains("VP election: nominated MPs can vote"), "VP line: " + vp);
        for (String s : new String[]{"Supreme Court strikes down Governor's delay on Bills", "Coral bleaching in Lakshadweep", "RBI keeps repo rate unchanged"}) {
            String c = Pyq.compact(p.matchTopic(s, "", "GS-II/Polity", new LinkedHashSet<String>(kb.idsIn(s)), 4)).toLowerCase();
            ok(!c.matches(".*\\b(statements? is|is not correct|gandhi peace prize|union state executive)\\b.*"), "no junk PYQ aspect for '" + s + "': " + c);
        }
        // new cards attach to their own news only
        String[][] hit = {{"Parties to the Montreal Protocol meet in Nairobi", "e:montreal"}, {"Lok Sabha to take up no-confidence motion", "c:c_motions"},
                {"Gross NPAs of banks fall to 2.3%", "c:c_npa"}, {"Cabinet extends PMGKAY free foodgrain scheme", "c:c_nfsa"}, {"Coral bleaching hits Gulf of Mannar reefs", "c:c_coral"},
                {"E-waste generation rises 70% in five years", "c:c_ewaste"}, {"Gram Sabhas file claims under Forest Rights Act", "c:c_fra"}, {"Centre notifies Citizenship Amendment Rules", "c:c_caa"},
                {"Masala bonds: NHAI plans issue in London", "c:c_masala"}, {"Critical minerals auction: lithium block in Reasi", "c:c_critmin"}};
        for (String[] h : hit) ok(kb.cardsIn(h[0]).contains(h[1]), h[1] + " in: " + h[0]);
        String[][] miss = {{"Article 6 of the Constitution: migrants from Pakistan", "c:c_carbonmkt"}, {"Results declared at 10 am IST", "c:c_ist"},
                {"Bihar cooperative bank chairman arrested", "c:c_parlcomm"}, {"PM POSHAN outlay raised to ₹12,467 crore", "c:c_msme"}};
        for (String[] h : miss) ok(!kb.cardsIn(h[0]).contains(h[1]), "no " + h[1] + " in: " + h[0]);
        // cards saved before 1.7.0 have [year, aspect] only: the year must still open the question
        for (String head : new String[]{"Election Commission revises electoral rolls in Bihar", "Vice-President election: candidate files nomination", "Ramsar Convention adds two Indian wetlands"}) {
            Set<String> ids = new LinkedHashSet<String>(kb.idsIn(head));
            List<Pyq.Row> rows = p.matchTopic(head, "", "GS-II/Polity", ids, 8);
            JSONObject t = new JSONObject().put("head", head).put("gs", "GS-II/Polity");
            JSONArray ents = new JSONArray(), maps = new JSONArray(), ev = new JSONArray();
            for (String id : ids) { if (id.startsWith("e:")) ents.put(id.substring(2)); if (id.startsWith("m:")) maps.put(id.substring(2)); }
            t.put("ents", ents).put("maps", maps);
            List<String> want = new ArrayList<String>();
            for (Pyq.Row x : rows) { ev.put(new JSONArray().put(x.year).put(Pyq.aspect(x))); want.add(p.key(x)); }
            t.put("pyqEv", ev);
            ok(rows.isEmpty() || p.addKeys(t, kb), "keys added: " + head);
            int got = 0;
            for (int i = 0; i < ev.length(); i++) if (ev.getJSONArray(i).length() == 3 && want.contains(ev.getJSONArray(i).getString(2))) got++;
            ok(got == rows.size(), "every year opens its own question (" + got + "/" + rows.size() + "): " + head);
            ok(!p.addKeys(t, kb), "second run adds nothing");
        }
        // CDS / CAPF: full questions with exam and paper (since 1.9.1)
        String mt = r("pyq_more_text.js");
        p.addMoreText(mt); // full questions first: their answers feed the matching
        p.addMore(r("pyq_more.js"));
        JSONObject more = new JSONObject(mt.substring(mt.indexOf('{'), mt.lastIndexOf('}') + 1));
        boolean mshape = more.length() > 1500;
        for (Iterator<String> it = more.keys(); it.hasNext(); ) {
            String k = it.next(); JSONArray q = more.getJSONArray(k);
            if (!k.matches("20\\d\\d\\|(CDS(I|II)?|CAPF|NDA(I|II)?|CGS|EPFO(APFC|EOAO)?|ESE)-\\d+-\\d+") || q.getJSONArray(1).length() != 4 || q.getInt(2) < 0 || q.getInt(2) > 3 || q.optInt(3) != 0 || !q.getString(4).matches("(CDS|CAPF|NDA|Geo-Scientist|EPFO|ESE)( \\([A-Z/I]+\\))? 20\\d\\d")) { mshape = false; System.out.println("  bad entry " + k + " " + q.optString(4)); break; }
        }
        ok(mshape, "other-paper questions: " + more.length() + ", each complete with 4 options, answer and exam label");
        // every listed question opens
        String mr = r("pyq_more.js");
        JSONArray mrows = new JSONArray(mr.substring(mr.indexOf('['), mr.lastIndexOf(']') + 1));
        int open = 0;
        for (int i = 0; i < mrows.length(); i++) { JSONArray x = mrows.getJSONArray(i); if (more.has(x.getInt(0) + "|" + x.getString(1))) open++; }
        ok(open == mrows.length(), "every CDS/CAPF/NDA/Geo-Scientist/EPFO/ESE row opens: " + open + "/" + mrows.length());
        // an Election Commission story lists questions from several papers, each once, each opening
        Set<String> eciIds = new LinkedHashSet<String>(kb.idsIn("Election Commission of India announces the schedule"));
        List<Pyq.Row> all = p.matchAll("Election Commission announces bypolls", "Election Commission of India; model code of conduct comes into force", "GS-II/Polity", eciIds);
        Map<String, Integer> byExam = new TreeMap<String, Integer>();
        Set<String> seenK = new HashSet<String>(); boolean allOpen = true, dup = false;
        for (Pyq.Row x : all) { String ex = x.exam == null ? "CSE" : x.exam; byExam.put(ex, byExam.containsKey(ex) ? byExam.get(ex) + 1 : 1); String k = p.key(x); if (k == null || p.question(k) == null) allOpen = false; else if (!seenK.add(k)) dup = true; }
        System.out.println("  Election Commission story: " + byExam);
        ok(byExam.containsKey("CSE") && byExam.size() >= 3, "questions from CSE and other papers: " + byExam);
        ok(allOpen && !dup, "each listed once and each opens");
        // same-day cards about one body become one card, with one list of questions
        JSONArray tp = new JSONArray();
        tp.put(new JSONObject("{\"id\":\"a\",\"head\":\"ECI announces bypolls to 8 seats\",\"pent\":\"eci\",\"ents\":[\"eci\"],\"bullets\":[\"Polling on 4 December.\"],\"srcs\":[\"TH\"],\"pyqEv\":[[2017,\"composition\",\"2017|7\"]]}"));
        tp.put(new JSONObject("{\"id\":\"b\",\"head\":\"Supreme Court on Governor assent\",\"pent\":\"sc\",\"ents\":[\"sc\"],\"bullets\":[\"Bench of five judges.\"],\"srcs\":[\"IE\"]}"));
        tp.put(new JSONObject("{\"id\":\"c\",\"head\":\"Election Commission revises rolls in Bihar\",\"pent\":\"eci\",\"ents\":[\"eci\"],\"bullets\":[\"Special Intensive Revision ends.\"],\"srcs\":[\"HT\"],\"pyqEv\":[[2017,\"composition\",\"2017|7\"],[2015,\"powers\",\"2015|12\"]]}"));
        tp.put(new JSONObject("{\"id\":\"d\",\"head\":\"Supreme Court stays order on mining lease\",\"pent\":\"sc\",\"ents\":[\"sc\"],\"bullets\":[\"Interim stay.\"],\"srcs\":[\"TH\"]}"));
        tp.put(new JSONObject("{\"id\":\"e\",\"head\":\"Election Commission launches voter app\",\"ents\":[\"eci\"],\"bullets\":[\"App in 12 languages.\"],\"srcs\":[\"PIB\"]}"));
        ok(EditionBuilder.consolidate(tp), "same-body cards merged");
        int eciN = 0, sc = 0; JSONObject host = null;
        for (int i = 0; i < tp.length(); i++) { String k = tp.getJSONObject(i).optString("pent", tp.getJSONObject(i).optJSONArray("ents").optString(0)); if (k.equals("eci")) { eciN++; host = tp.getJSONObject(i); } if (k.equals("sc")) sc++; }
        ok(eciN == 1, "one Election Commission card: " + eciN);
        ok(sc == 2, "Supreme Court stories stay separate (a catch-all body): " + sc);
        ok(host != null && host.getJSONArray("pyqEv").length() == 2, "questions joined, none repeated: " + (host == null ? "" : host.getJSONArray("pyqEv")));
        ok(host != null && host.optJSONArray("also") != null && host.getJSONArray("also").length() >= 1, "the other stories sit inside the card");
        List<Pyq.Row> vk = p.matchTopic("Vikramkhol Cave rock shelter in Odisha", "prehistoric rock art", "GS-I/History", new LinkedHashSet<String>(), 4);
        boolean cds = false;
        for (Pyq.Row x : vk) if (x.exam != null && p.key(x) != null && p.question(p.key(x)).length() == 5) cds = true;
        ok(cds, "a CDS question opens in full: " + Pyq.compact(vk, null));
        System.out.println("PASS " + pass + "  FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
