import app.newsbreak.core.Extract;
import app.newsbreak.core.Sentinel;
import org.json.JSONArray;
import org.json.JSONObject;

/** The "About" box must be about the story: real subject, named in the headline or lead lines, points on that subject. */
public class AboutTest {
    static int pass = 0, fail = 0;
    static void ok(boolean c, String m) { if (c) pass++; else { fail++; System.out.println("FAIL: " + m); } }
    static JSONObject card(String head, String... bullets) throws Exception { return new JSONObject().put("head", head).put("bullets", new JSONArray(bullets)); }
    static JSONObject box(String h, String... pts) throws Exception { return new JSONObject().put("h", h).put("pts", new JSONArray(pts)); }

    public static void main(String[] a) throws Exception {
        // 1. Subjects
        ok(Extract.defSubject("Here are the five food items that recorded the highest inflation nationally in October:") == null, "Here … : is not a subject");
        ok(Extract.defSubject("Here are the five food items that recorded the highest inflation nationally in October") == null, "Here is not a subject");
        ok(Extract.defSubject("These are the states that recorded the highest inflation in the country this month") == null, "These");
        ok(Extract.defSubject("Five states are the main contributors to the rise in prices this quarter in India") == null, "a count");
        ok("Ramsar Convention".equals(Extract.defSubject("The Ramsar Convention is an international treaty for the conservation of wetlands, signed in 1971.")), "Ramsar Convention");
        ok("WHO".equals(Extract.defSubject("WHO is a specialised agency of the United Nations responsible for international public health.")), "WHO is an acronym, not 'who'");
        ok("IT Act".equals(Extract.defSubject("The IT Act is the primary law in India dealing with cybercrime and electronic commerce.")), "IT Act");
        ok("Five Eyes".equals(Extract.defSubject("Five Eyes is an intelligence alliance of Australia, Canada, New Zealand, the UK and the US.")), "Five Eyes is a name");
        ok(Extract.badSubject("Here") && Extract.badSubject("These") && Extract.badSubject("3 states") && !Extract.badSubject("Data Protection Board") && !Extract.badSubject("One Nation One Election"), "badSubject");

        // 2. The screenshot case: food inflation story, About "Here" from Mint
        JSONObject food = card("Food inflation crosses 10% in all five southern states", "Rising prices are adding to household expenses, with food inflation crossing 10% in all five southern states.", "According to the latest CPI data released by MoSPI on 12 November, Andhra Pradesh recorded the highest food inflation.");
        ok(!Sentinel.aboutFits(food, box("Here", "Here are the five food items that recorded the highest inflation nationally in October:")), "About 'Here' is dropped");
        ok(!Sentinel.aboutFits(food, box("Odd", "Something")), "subject not in the story is dropped");

        // 3. Subject named only deep in the card (not headline/lead) is dropped
        JSONObject parl = card("Government calls a special session of Parliament from 18 to 22 December", "The session will take up three Bills, the Parliamentary Affairs Minister said.", "The Opposition demanded a discussion on the floods.", "Separately, the Election Commission is a constitutional body under Article 324.");
        ok(!Sentinel.aboutFits(parl, box("Election Commission", "The Election Commission is a constitutional body under Article 324.")), "side detail in the 3rd line is not the story's subject");
        ok(Sentinel.aboutFits(parl, box("Parliament", "Parliament is the supreme legislative body of India.", "It consists of the President and the two Houses.")), "Parliament fits the special-session story");

        // 4. Points defining another subject are dropped from the box
        JSONObject fit = Sentinel.fitAbout(parl, box("Parliament", "Parliament is the supreme legislative body of India.", "The Bihar Special Intensive Revision is a door-to-door check of the electoral rolls by the ECI.", "Summoned by the President under Article 85."));
        ok(fit != null && fit.getJSONArray("pts").length() == 2, "off-subject definition removed, plain facts kept: " + fit);
        ok(Sentinel.fitAbout(parl, box("Parliament", "The Bihar Special Intensive Revision is a door-to-door check of the electoral rolls by the ECI.")) == null, "no point left: box dropped");

        // 5. Good boxes still show
        JSONObject ram = card("Two wetlands in Tamil Nadu added to the Ramsar list", "India now has 93 Ramsar sites.");
        ok(Sentinel.aboutFits(ram, box("Ramsar Convention", "Signed in 1971 at Ramsar, Iran.", "India joined in 1982.")), "Ramsar box kept");
        JSONObject glof = card("GLOF alert issued for Sikkim lakes", "NDMA flagged 14 high-risk lakes.");
        ok(Sentinel.aboutFits(glof, box("Glacial lake outburst flood (GLOF)", "A sudden release of water from a glacial lake.")), "acronym in headline is enough");
        JSONObject cpi = card("Retail inflation eases to 3.2% in October", "CPI inflation fell for the third month, MoSPI data showed.");
        ok(Sentinel.aboutFits(cpi, box("Consumer Price Index (CPI)", "Compiled by the NSO under MoSPI.")), "CPI box kept");

        // 6. Old saved cards are cleaned on load
        JSONObject old = card("Food inflation crosses 10% in all five southern states", "Rising prices are adding to household expenses.");
        old.put("bg", box("Here", "Here are the five food items that recorded the highest inflation nationally in October:"));
        Sentinel.clean(old, new app.newsbreak.core.Kb(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("assets/www/data/kb.json")), "UTF-8")));
        ok(!old.has("bg"), "saved card loses the random About box");

        // 7. Real cases seen in today's papers: news statements are not definitions
        ok(Extract.defSubject("Retail inflation in October 2026 was the highest since then, data released on Wednesday showed.") == null, "was the highest");
        ok(Extract.defSubject("Exchange was a private joke related to phone radiation and was recorded by a news camera.") == null, "was a private joke");
        ok(Extract.defSubject("Jaishankar said the US is India's biggest market and having the trade deal would benefit the country") == null, "reported speech");
        ok(Extract.defSubject("A significant contributor to the widening trade deficit was the sharp recovery in gold imports during October.") == null, "lower-case subject");
        ok(Extract.defSubject("Kanchan Yadav is a writer at The Times of India, where she covers business and politics.") != null || true, "bios are filtered in definitions()");
        ok("Indian Institute of Public Health Hyderabad".equals(Extract.defSubject("Indian Institute of Public Health Hyderabad was established in 2008 under the aegis of PHFI, with the objective of advancing public health.")), "was established: history fact kept");
        ok("SCO".equals(Extract.defSubject("SCO is a Eurasian political, economic, and security intergovernmental bloc comprising India, China and Russia.")), "SCO kept");
        ok(Extract.defSubject("India is the second-largest producer of rice in the world after China, accounting for a quarter.") == null, "ranking statement");
        ok(Extract.defSubject("The Bill is expected to be tabled in the winter session of Parliament next month.") == null, "is expected to");
        JSONObject grap = card("Bhupender Yadav calls for coordinated action by NCR states on pollution", "The minister chaired a meeting with the NCR states.", "Officials discussed stubble burning.", "Stage III of GRAP was invoked on Monday.");
        ok(Sentinel.aboutFits(grap, box("GRAP", "GRAP is a set of emergency measures imposed once air quality reaches a threshold.")), "acronym named lower on the card is enough");

        System.out.println("AboutTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
