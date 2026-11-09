import app.newsbreak.core.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

/** Foreign offices must never pick up India's polity cards or PYQs (run by tests/run.sh). */
public class ForeignTest {
    static final String D = System.getProperty("data", "assets/www/data/");
    static int pass = 0, fail = 0;
    static void ok(boolean c, String what) { if (c) pass++; else { fail++; System.out.println("FAIL: " + what); } }
    public static void main(String[] a) throws Exception {
        Kb kb = new Kb(new String(Files.readAllBytes(Paths.get(D + "kb.json")), "UTF-8"));
        Pyq pyq = new Pyq(new String(Files.readAllBytes(Paths.get(D + "pyq.js")), "UTF-8"), kb);
        pyq.addMore(new String(Files.readAllBytes(Paths.get(D + "pyq_more.js")), "UTF-8"));
        String[][] none = {
            {"US President Donald Trump signs executive order on tariffs", "e:pres,e:vp"},
            {"Vice President JD Vance meets European leaders in Munich", "e:vp"},
            {"US Vice-President says Senate will vote on the bill", "e:vp"},
            {"Pakistan's Supreme Court disqualifies former prime minister", "e:sc"},
            {"Supreme Court of Bangladesh stays caretaker government order", "e:sc"},
            {"Bangladesh Election Commission announces February polls", "e:eci"},
            {"Election Commission of Pakistan delays provincial vote", "e:eci"},
            {"US Attorney General Pam Bondi orders review", "e:ag"},
            {"Sri Lankan President Dissanayake dissolves Parliament", "e:pres"},
            {"French Prime Minister resigns after no-confidence vote", "e:pres"},
            {"Governor of California signs climate bill", "e:gov"},
        };
        for (String[] t : none) {
            Set<String> c = kb.cardsIn(t[0]);
            for (String id : t[1].split(",")) ok(!c.contains(id), "no " + id + " in: " + t[0] + " -> " + c);
        }
        String[][] keep = {
            {"Vice-President C.P. Radhakrishnan addresses Rajya Sabha", "e:vp"},
            {"President Droupadi Murmu gives assent to the Bill", "e:pres"},
            {"President of India summons Parliament for winter session", "e:pres"},
            {"Supreme Court asks Centre to respond on Governor's assent delays", "e:sc"},
            {"Election Commission announces bypolls to 8 Assembly seats", "e:eci"},
            {"Attorney General R. Venkataramani appears before the Supreme Court", "e:ag"},
            {"Lok Sabha Speaker Om Birla adjourns the House", "e:speaker"},
        };
        for (String[] t : keep) {
            Set<String> c = kb.cardsIn(t[0]);
            for (String id : t[1].split(",")) ok(c.contains(id), "keeps " + id + " in: " + t[0] + " -> " + c);
        }
        ok(Foreign.mask("Prime Minister Narendra Modi meets farmers").equals("Prime Minister Narendra Modi meets farmers"), "Modi not masked");
        ok(Foreign.mask("President Murmu confers awards").equals("President Murmu confers awards"), "Murmu not masked");
        ok(Foreign.mask("President's Rule imposed in Manipur").contains("President's Rule"), "President's Rule not masked");
        ok(kb.cardsIn("President's Rule imposed in Manipur under Article 356").contains("e:a356"), "a356 kept");
        // scope
        ok(Foreign.outOfScope("Trump wins Republican primary in Iowa"), "US primary out of scope");
        ok(!Foreign.outOfScope("Trump announces 50% tariffs on Indian goods"), "India link kept");
        ok(!Foreign.outOfScope("RBI keeps repo rate unchanged at 5.5%"), "domestic not foreign");
        ok(Foreign.neighbour("Nepal's Prime Minister wins trust vote"), "neighbour");
        ok(!Foreign.politics("UN General Assembly adopts resolution on AI"), "UN not foreign politics");
        ok(!Foreign.politics("COP31 in Antalya ends with deal on adaptation finance"), "COP not foreign politics");
        // scoring
        JSONObject us = new JSONObject().put("t", "US President Trump signs order ending Senate filibuster").put("d", "The White House said the President would sign the order on Monday. Republicans in Congress welcomed it.").put("s", "Reuters");
        JSONObject usIn = new JSONObject().put("t", "US President Trump signs order ending Senate filibuster").put("d", "The White House said the President would sign the order on Monday. Republicans in Congress welcomed it.").put("s", "Reuters");
        kb.score(us);
        ok(us.optBoolean("fx"), "foreign item flagged");
        JSONArray ents = us.optJSONArray("ents");
        ok(ents == null || !ents.toString().contains("\"pres\"") && !ents.toString().contains("\"vp\""), "no pres/vp on US item: " + ents);
        ok(!"parliament".equals(us.optString("th")) && !"elections".equals(us.optString("th")), "no polity theme: " + us.optString("th"));
        JSONObject ind = new JSONObject().put("t", "Trump says US will cut tariffs on Indian goods to 15%").put("d", "India and the US are close to a trade deal, the President said.").put("s", "Reuters");
        kb.score(ind);
        ok(!ind.optBoolean("fx"), "India-linked not flagged");
        // sentinel
        JSONObject card = new JSONObject().put("head", "Vance sworn in as acting head after US President falls ill").put("bullets", new JSONArray().put("The vice-president took charge at the White House").put("Senate leaders met the President's team"))
                .put("ents", new JSONArray().put("vp").put("pres")).put("bg", new JSONObject().put("h", "US Electoral College").put("pts", new JSONArray().put("538 electors choose the President")));
        Sentinel.clean(card, kb);
        ok(card.optJSONArray("ents").length() == 0, "sentinel drops vp/pres: " + card.optJSONArray("ents"));
        ok(!card.has("bg"), "sentinel drops foreign About");
        JSONObject cardIn = new JSONObject().put("head", "Vice-President C.P. Radhakrishnan to chair Rajya Sabha session").put("bullets", new JSONArray().put("The Vice-President is ex-officio Chairman of the Rajya Sabha"))
                .put("ents", new JSONArray().put("vp"));
        Sentinel.clean(cardIn, kb);
        ok(cardIn.optJSONArray("ents").length() == 1, "sentinel keeps India VP");
        // PYQ
        List<Pyq.Row> rows = pyq.matchTopic(Foreign.mask("US presidential election: Vice President wins Democratic nomination"), Foreign.mask("The President will be chosen by the Electoral College"), "GS-II/IR", new HashSet<String>(), 4);
        boolean polity = false; for (Pyq.Row r : rows) if ("Polity".equals(r.subj)) polity = true;
        ok(!polity, "no Indian polity PYQ on US election: " + rows.size());
        List<Pyq.Row> r2 = pyq.matchTopic("Exercise Yudh Abhyas between India and US army", "", "GS-III/Security", new HashSet<String>(kb.idsIn("Exercise Yudh Abhyas")), 4);
        System.out.println("yudh abhyas rows: " + r2.size() + (r2.isEmpty() ? "" : " e.g. " + r2.get(0).year + " " + r2.get(0).exam + " " + Pyq.aspect(r2.get(0))));
        List<Pyq.Row> r3 = pyq.matchTopic("Election Commission revises electoral rolls", "", "GS-II/Polity", new HashSet<String>(Arrays.asList("e:eci")), 4);
        boolean cse = false; for (Pyq.Row r : r3) if (r.exam == null) cse = true;
        ok(cse && r3.get(0).exam == null, "CSE rows first for ECI");
        System.out.println("PASS " + pass + "  FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
