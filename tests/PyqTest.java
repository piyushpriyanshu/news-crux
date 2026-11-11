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
        System.out.println("PASS " + pass + "  FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
