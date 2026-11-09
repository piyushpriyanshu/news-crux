package app.newsbreak.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Relevance sentinel for the static half of a card.
 *
 * Every knowledge-base recap (body, scheme, species…), concept card, map line, "About" box and PYQ line on a card must be
 * about what the card itself says. A name that only turned up in a neighbouring story, a merged report, a coaching page's
 * next topic or a passing sentence is removed. The test is run on the card's own visible text, weighted by prominence:
 *
 *   headline 3 · "About" title 3 · another outlet's headline of the same story 2 · lead bullet 2 · each other bullet 1 · an "also today" development's headline 2 (its bullets 1) · About points 1 (at most 1 in all) ·
 *   named in the verified notes of a subject in the card's headline 2 (Chilika → Ramsar site; GLOF → Teesta-III dam).
 *
 * A static card stays at 2 or more (3 or more for catch-all bodies such as the Supreme Court or the UN): the card must be
 * about it, not merely mention it in passing (a Telangana El Niño meeting that also asks for MGNREGA works is not an
 * MGNREGA story). With an AI key, the AI also names any static card that is not about the news, and that card is
 * removed too ("un" in the card's AI fields) unless the card names it prominently (score 3+: headline, About title,
 * or the lead bullet plus another mention).
 */
public final class Sentinel {
    private Sentinel() {}

    public static final int KEEP = 2, KEEP_GENERIC = 3;
    /** Catch-all bodies: shown only when the headline itself is about them. */
    static final Set<String> GENERIC = new HashSet<String>(Arrays.asList("e:sc", "e:hc", "e:un", "e:ordinance"));

    /** Words that don't identify a subject on their own (an "About" title must share a real name with the card). */
    static final Set<String> PLAIN = new HashSet<String>(Arrays.asList("india", "indian", "national", "international", "government", "centre", "central",
            "union", "state", "new", "great", "big", "first", "world", "global", "day", "year", "2024", "2025", "2026", "2027", "report", "index", "scheme",
            "mission", "project", "programme", "program", "policy", "act", "bill", "law", "rule", "court", "council", "commission", "committee", "body",
            "organisation", "organization", "ministry", "minister", "department", "development", "prize", "award", "news", "key", "fact", "facts", "about",
            "what", "overview", "feature", "significance", "concept", "initiative", "plan", "system", "framework", "india's", "nobel", "laureate"));
    /** Common nouns inside names ("Chilika Lake", "Ramappa Temple"): they count, but can't make a match on their own. */
    static final Set<String> WEAKN = new HashSet<String>(Arrays.asList("lake", "river", "temple", "park", "reserve", "species", "convention", "treaty",
            "agreement", "summit", "conference", "festival", "bilateral", "relation", "link", "mount", "strait", "island", "sanctuary", "dam", "port",
            "city", "valley", "hill", "forest", "biosphere", "tiger", "bird", "dolphin", "peace", "chemistry", "physic", "medicine", "literature"));

    /** Card text by prominence: [weight, text] groups (see class comment). */
    static final class Seg { final int w; final String s; Seg(int w, String s) { this.w = w; this.s = s == null ? "" : s; } }

    static List<String> list(JSONArray a) {
        List<String> o = new ArrayList<String>();
        for (int i = 0; a != null && i < a.length(); i++) { String s = a.optString(i); if (s.length() > 0) o.add(s); }
        return o;
    }

    /** All visible text of a card (headline, other headlines, bullets incl. AI bullets, also-developments). */
    public static String visible(JSONObject t) {
        StringBuilder b = new StringBuilder(t.optString("head")).append(". ");
        for (String s : list(t.optJSONArray("heads"))) b.append(s).append(". ");
        for (String s : list(t.optJSONArray("bullets"))) b.append(s).append(". ");
        JSONObject ai = t.optJSONObject("ai");
        if (ai != null) for (String s : list(ai.optJSONArray("b"))) b.append(s).append(". ");
        JSONArray al = t.optJSONArray("also");
        for (int i = 0; al != null && i < al.length(); i++) {
            JSONObject x = al.optJSONObject(i);
            if (x == null) continue;
            b.append(x.optString("head")).append(". ");
            for (String s : list(x.optJSONArray("bullets"))) b.append(s).append(". ");
        }
        return b.toString();
    }

    static int bulletScore(List<String> bl, String id, Kb kb, Map<String, Set<String>> cache) {
        int s = 0;
        for (int i = 0; i < bl.size(); i++) if (in(bl.get(i), id, kb, cache)) s += i == 0 ? 2 : 1;
        return s;
    }

    static boolean in(String text, String id, Kb kb, Map<String, Set<String>> cache) {
        Set<String> c = cache.get(text);
        if (c == null) { c = kb.cardsIn(expand(text)); cache.put(text, c); }
        return c.contains(id);
    }

    /** Bullets are written in note style ("Art 200", "Min of"); spell them out again before matching names.
     *  ("SC", "HC" and "UN" are left alone: "SC/ST", "SC students" are not the Supreme Court.) */
    private static final String[][] EXP = {
            {"\\bArt\\.?\\s(?=\\d)", "Article "}, {"\\bSec\\.?\\s(?=\\d)", "Section "}, 
            {"\\bMin of\\b", "Ministry of"}, {"\\bGoI\\b", "Government of India"}, {"\\bgovt\\b", "government"},
            {"\\bEU\\b", "European Union"}, {"\\bUS\\b", "United States"},
            {"\\bUK\\b", "United Kingdom"}, {"\\bDept\\b", "department"}, {"\\bFY\\b", "financial year"}};
    private static final Pattern[] EXP_P = new Pattern[EXP.length];
    static { for (int i = 0; i < EXP.length; i++) EXP_P[i] = Pattern.compile(EXP[i][0]); }
    static String expand(String s) {
        String t = s;
        for (int i = 0; i < EXP_P.length; i++) t = EXP_P[i].matcher(t).replaceAll(EXP[i][1]);
        return t;
    }

    /** Prominence score of one knowledge-base card ("e:…", "m:…", "c:…") on this card. */
    public static int score(JSONObject t, String id, Kb kb) { return score(t, id, kb, new HashMap<String, Set<String>>()); }

    static int score(JSONObject t, String id, Kb kb, Map<String, Set<String>> cache) {
        int s = 0;
        if (in(t.optString("head"), id, kb, cache)) s += 3;
        for (String h : list(t.optJSONArray("heads"))) if (in(h, id, kb, cache)) { s += 2; break; }
        JSONObject ai = t.optJSONObject("ai");
        JSONObject bg = t.optJSONObject("bg"), ab = ai == null ? null : ai.optJSONObject("ab");
        if ((bg != null && in(bg.optString("h"), id, kb, cache)) || (ab != null && in(ab.optString("h"), id, kb, cache))
                || (t.optString("subj").length() > 0 && in(t.optString("subj"), id, kb, cache))) s += 3; // About title (or the subject a verified card replaced)
        int bs = bulletScore(list(t.optJSONArray("bullets")), id, kb, cache);
        if (ai != null) bs = Math.max(bs, bulletScore(list(ai.optJSONArray("b")), id, kb, cache));
        s += bs;
        JSONArray al = t.optJSONArray("also");
        for (int i = 0; al != null && i < al.length(); i++) {
            JSONObject x = al.optJSONObject(i);
            if (x == null) continue;
            if (in(x.optString("head"), id, kb, cache)) s += 2;
            for (String b : list(x.optJSONArray("bullets"))) if (in(b, id, kb, cache)) s += 1;
        }
        boolean pts = false;
        if (bg != null) for (String p : list(bg.optJSONArray("pts"))) if (in(p, id, kb, cache)) pts = true;
        if (ab != null) for (String p : list(ab.optJSONArray("pts"))) if (in(p, id, kb, cache)) pts = true;
        if (pts) s += 1;
        return s;
    }

    /** Another country's politics with no India link: no Indian office or procedure card fits it. */
    static boolean foreignOnly(JSONObject t) { return Foreign.outOfScope(visible(t)); }

    /** An "About" box on a foreign office or election (US Electoral College…) is outside the Prelims scope. */
    static boolean foreignAbout(JSONObject t, JSONObject box) {
        if (box == null) return false;
        String h = box.optString("h");
        if (h.length() == 0) h = box.optString("title");
        return Foreign.politics(h) && !Foreign.india(h) && Foreign.politics(visible(t));
    }

    public static boolean keep(JSONObject t, String id, Kb kb) {
        if (Foreign.INDIA_ONLY.contains(id) && foreignOnly(t)) return false;
        Map<String, Set<String>> cache = new HashMap<String, Set<String>>();
        return keep(score(t, id, kb, cache) + (kin(t, kb, cache).contains(id) ? 2 : 0), id);
    }

    /** Verified notes (line, facts, traps) of a knowledge-base card. */
    static String notes(Kb kb, String id) {
        JSONObject o = id.startsWith("e:") ? kb.entity(id.substring(2)) : id.startsWith("m:") ? kb.map(id.substring(2)) : id.startsWith("c:") ? kb.concept(id.substring(2)) : null;
        if (o == null) return "";
        StringBuilder b = new StringBuilder(o.optString("line")).append(". ");
        for (String f : list(o.optJSONArray("facts"))) b.append(f).append(". ");
        for (String f : list(o.optJSONArray("traps"))) b.append(f).append(". ");
        return b.toString();
    }

    /** Cards named in the verified notes of the card's headline subjects. */
    static Set<String> kin(JSONObject t, Kb kb, Map<String, Set<String>> cache) {
        Set<String> out = new HashSet<String>();
        String[][] kinds = {{"ents", "e:"}, {"cons", "c:"}, {"maps", "m:"}};
        for (String[] k : kinds) {
            JSONArray a = t.optJSONArray(k[0]);
            for (int i = 0; a != null && i < a.length(); i++) {
                String id = k[1] + a.optString(i);
                boolean inHead = in(t.optString("head"), id, kb, cache);
                for (String h : list(t.optJSONArray("heads"))) if (!inHead && in(h, id, kb, cache)) inHead = true;
                if (!inHead) continue; // only the story's own headline subject lends its kin (Chilika → Ramsar), not a background box
                for (String x : kb.cardsIn(notes(kb, id))) if (!x.equals(id)) out.add(x);
            }
        }
        return out;
    }
    static boolean keep(int score, String id) { return score >= (GENERIC.contains(id) ? KEEP_GENERIC : KEEP); }

    private static final Pattern ACR = Pattern.compile("^[A-Z][A-Za-z]{0,2}[A-Z]{1,7}s?$");

    /** Real name words of a subject ("Ramappa Temple" → ramappa; "GLOF" → glacial, outburst, flood if the points spell it out). Numbers don't count. */
    static Set<String> subjectWords(String h, List<String> pts) {
        Set<String> o = new LinkedHashSet<String>();
        String core = h.replaceAll("\\(.*?\\)", " ").trim();
        boolean acrOnly = ACR.matcher(core).matches();
        if (!acrOnly) for (String w : Text.tokens(core)) if (!PLAIN.contains(w) && !w.matches("\\d+")) o.add(w);
        if (acrOnly) { // spelled-out form, e.g. "glacial lake outburst flood (GLOF)"
            String acr = core.replaceAll("s$", "");
            Matcher m = Pattern.compile("((?:[A-Za-z][\\w’'-]*\\s+){1,7}[A-Za-z][\\w’'-]*)\\s*\\(" + Pattern.quote(acr) + "s?\\)").matcher(join(pts));
            if (m.find()) for (String w : Text.tokens(m.group(1))) if (!PLAIN.contains(w) && !w.matches("\\d+")) o.add(w);
        }
        return o;
    }

    /** Acronyms that name the subject on their own: "GLOF", "Nanda Devi Biosphere Reserve (NDBR)" → ndbr. */
    static Set<String> acronyms(String h) {
        Set<String> o = new LinkedHashSet<String>();
        String core = h.replaceAll("\\(.*?\\)", " ").trim();
        if (ACR.matcher(core).matches()) o.add(core.replaceAll("s$", "").toLowerCase(Locale.ROOT));
        Matcher pm = Pattern.compile("\\(([A-Z][A-Za-z-]{1,9})\\)").matcher(h);
        while (pm.find()) o.add(pm.group(1).replaceAll("s$", "").toLowerCase(Locale.ROOT));
        return o;
    }

    static String join(List<String> l) { StringBuilder b = new StringBuilder(); for (String s : l) b.append(s).append(' '); return b.toString(); }

    static boolean has(Set<String> vis, String w) {
        if (vis.contains(w) || vis.contains(w + "e") || (w.length() > 3 && w.endsWith("e") && vis.contains(w.substring(0, w.length() - 1)))) return true; // lake / lakes → "lak"
        if (w.length() >= 7) for (String v : vis) if (v.length() >= 7 && v.substring(0, 6).equals(w.substring(0, 6))) return true; // light stemming
        return false;
    }

    /** Is the "About" box about a subject the card itself names? (Its acronym alone is enough: GLOF, NDBR, ISRO.) */
    public static boolean aboutFits(JSONObject t, JSONObject bg) {
        if (bg == null) return true;
        String h = bg.optString("h");
        Set<String> vis = Text.tokens(expand(visible(t)));
        for (String a : acronyms(h)) if (has(vis, a)) return true;
        Set<String> subj = subjectWords(h, list(bg.optJSONArray("pts")));
        if (subj.isEmpty()) return acronyms(h).isEmpty(); // nothing to judge by — unless only an acronym the card never uses
        int hit = 0; boolean strongHit = false, anyStrong = false;
        for (String w : subj) { boolean st = !WEAKN.contains(w); if (st) anyStrong = true; if (has(vis, w)) { hit++; if (st) strongHit = true; } }
        return hit >= (subj.size() <= 2 ? 1 : 2) && (strongHit || !anyStrong);
    }

    /**
     * Removes static parts that aren't about this card. Safe to run any number of times (only ever removes).
     * @return true if anything was removed.
     */
    public static boolean clean(JSONObject t, Kb kb) {
        if (t == null || kb == null) return false;
        try {
            boolean changed = false;
            if (t.has("bg") && (!aboutFits(t, t.optJSONObject("bg")) || foreignAbout(t, t.optJSONObject("bg")))) { t.remove("bg"); changed = true; }
            Set<String> veto = new HashSet<String>();
            JSONObject ai = t.optJSONObject("ai");
            if (ai != null && ai.optJSONObject("ab") != null && foreignAbout(t, ai.optJSONObject("ab"))) { ai.remove("ab"); changed = true; }
            boolean foreign = foreignOnly(t);
            if (ai != null) for (String n : list(ai.optJSONArray("un"))) veto.addAll(kb.cardsByName(n));
            Map<String, Set<String>> cache = new HashMap<String, Set<String>>();
            Set<String> removed = new HashSet<String>();
            String[][] kinds = {{"ents", "e:"}, {"cons", "c:"}, {"maps", "m:"}};
            Set<String> kin = kin(t, kb, cache);
            for (String[] k : kinds) {
                JSONArray a = t.optJSONArray(k[0]);
                if (a == null) continue;
                JSONArray kept = new JSONArray();
                for (int i = 0; i < a.length(); i++) {
                    String id = k[1] + a.optString(i);
                    int base = score(t, id, kb, cache), sc = base + (kin.contains(id) ? 2 : 0);
                    boolean vetoed = veto.contains(id) && base < 3; // the AI can't overrule a card the card names prominently
                    if (foreign && Foreign.INDIA_ONLY.contains(id)) vetoed = true; // US President ≠ India's President
                    if (!vetoed && keep(sc, id)) kept.put(a.optString(i));
                    else removed.add(id);
                }
                if (kept.length() != a.length()) { t.put(k[0], kept); changed = true; }
            }
            if (removed.isEmpty()) return changed;
            String pent = t.optString("pent", "");
            if (pent.length() > 0 && removed.contains("e:" + pent)) t.remove("pent");
            // PYQ line built on a removed card → fall back to the theme line (never a question about something else)
            String pid = t.optString("pyqId", "");
            boolean pyqGone = pid.length() > 0 && removed.contains(pid);
            String pyq = t.optString("pyq", "");
            if (!pyqGone && pyq.startsWith("PYQ: ") && pyq.indexOf(" — ") > 5) {
                String lab = pyq.substring(5, pyq.indexOf(" — ")).trim();
                for (String lid : kb.cardsByName(lab)) if (removed.contains(lid)) pyqGone = true;
            }
            if (pyqGone) {
                t.put("pyq", "PYQ THEME: closely related to UPSC's repeated questions on " + (t.optString("pyqTheme", "").length() > 0 ? t.optString("pyqTheme") : "contemporary events"));
                t.remove("pyqEv"); t.remove("pyqId");
            }
            return true;
        } catch (Exception e) { return false; }
    }

    /** Runs the sentinel over every card of an edition (main cards and catch-up). */
    public static boolean cleanEdition(JSONObject ed, Kb kb) {
        if (ed == null || kb == null) return false;
        boolean ch = false;
        for (String key : new String[]{"topics", "catchup"}) {
            JSONArray a = ed.optJSONArray(key);
            for (int i = 0; a != null && i < a.length(); i++) if (clean(a.optJSONObject(i), kb)) ch = true;
        }
        return ch;
    }
}
