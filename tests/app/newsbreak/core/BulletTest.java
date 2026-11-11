package app.newsbreak.core;

import java.util.*;

/** Card bullets: quiz text, exam tags, bylines and a neighbouring story's lines never get in (run by tests/run.sh). */
public class BulletTest {
    static int pass = 0, fail = 0;
    static void ok(boolean c, String w) { if (c) pass++; else { fail++; System.out.println("FAIL: " + w); } }
    public static void main(String[] a) {
        String[] junk = {
            "Consider the following statements regarding the Ramsar Convention: 1. It was signed in 1971",
            "Which of the following is correct about the Montreal Protocol and its amendments",
            "Followers of Ramanuja are known as [U.P.P.C.S. (Pre) 1999 Methane gas producing field is",
            "Q3. Which one of the following countries is a member of the Arctic Council",
            "Answer: (b) Only two statements are correct as per the latest notification",
            "(a) 1 only (b) 2 and 3 only (c) 1 and 3 only (d) 1, 2 and 3 are the options given",
            "Photo: The Prime Minister at the inauguration of the new terminal building",
            "Also read: How the new rules change the game for small farmers in Punjab",
            "Updated: November 9, 2026 10:42 IST, the report was first published here",
            "For more such content visit www.example.com/upsc-daily-current-affairs-today",
            "Mains Practice Question: Discuss the role of cooperative federalism in India",
            "Select the correct answer using the code given below the lists of options",
            "Indian Institute of Sugarcane Research is located at ? Cetane number is used as a quality",
            "Aim of education as stated by the Wood's Despatch of 1854 was to spread western knowledge among the",
        };
        for (String j : junk) ok(EditionBuilder.junk(j), "junk: " + j);
        String[] good = {
            "MPC cut the repo rate by 25 basis points to 5.00% citing CPI inflation of 1.9% in September",
            "The Cabinet approved ₹12,000 crore for the scheme over five years from 2026-27",
            "In the context of rising prices, the RBI kept the stance neutral and the SDF at 4.75%",
            "With reference to the new rules, States must notify the authorities within 90 days",
            "The Lok Sabha passed the Bill after a seven-hour debate; the Rajya Sabha will take it up next week",
        };
        for (String g : good) ok(!EditionBuilder.junk(g), "good: " + g);
        String longOne = "The Union Cabinet on Wednesday approved the National Critical Mineral Mission with an outlay of ₹16,300 crore, which will fund exploration of lithium and rare earths in India and abroad, while PSUs will invest a further ₹18,000 crore over seven years";
        String c = EditionBuilder.concise(longOne);
        ok(c.length() <= 172 && !c.endsWith("…") && c.contains("National Critical Mineral Mission"), "concise: " + c);
        Set<String> tw = new HashSet<String>(EditionBuilder.strong(Text.tokens("RBI cuts repo rate by 25 bps to 5.00% in surprise move")));
        tw.addAll(EditionBuilder.strongSim(Text.simTokens("RBI cuts repo rate by 25 bps to 5.00% in surprise move")));
        ok(EditionBuilder.onTopic("The repo rate now stands at 5.00% after the cut", tw), "on-topic repo");
        ok(!EditionBuilder.onTopic("Harijan Sewak Sangh was established after the Poona Pact of 1932", tw), "off-topic history line");
        ok(!EditionBuilder.onTopic("The Archaeological Survey of India's first Director General was Alexander Cunningham", tw), "off-topic ASI line");
        System.out.println("PASS " + pass + "  FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
