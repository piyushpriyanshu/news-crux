package app.newsbreak.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Knowledge base: PYQ-derived themes, entity profiles, map buckets, noise filters — and the scorer. */
public final class Kb {

    /** One keyword phrase. Lowercase unless cs; last word may be a prefix. */
    static final class Phrase {
        String[] w; boolean prefix, cs; int group, owner; String key; String[] notPrev;
    }
    static final int G_THEME = 0, G_ENT = 1, G_MAP = 2, G_NOISE = 3, G_CON = 4;

    /** Single-pass tokenised lexicon across all themes, entities, maps and noise filters. */
    static final class Lexicon {
        final Map<String, List<Phrase>> first = new HashMap<String, List<Phrase>>();
        final Map<String, List<Phrase>> pre3 = new HashMap<String, List<Phrase>>();

        void add(String raw, int group, int owner) {
            String k = raw;
            String[] notPrev = null; // "president of india~vice" = not when preceded by "vice"
            int tl = k.indexOf('~');
            if (tl > 0) { notPrev = k.substring(tl + 1).toLowerCase(Locale.ROOT).split(","); k = k.substring(0, tl); }
            boolean cs = k.startsWith("=");
            if (cs) k = k.substring(1);
            k = k.trim();
            boolean prefix = k.endsWith("*");
            if (prefix) k = k.substring(0, k.length() - 1);
            List<String> ws = split(cs ? k : k.toLowerCase(Locale.ROOT));
            if (ws.isEmpty()) return;
            Phrase p = new Phrase();
            p.w = ws.toArray(new String[0]); p.prefix = prefix; p.cs = cs; p.group = group; p.owner = owner; p.notPrev = notPrev;
            p.key = (cs ? "=" : "") + k.toLowerCase(Locale.ROOT) + (prefix ? "*" : "");
            String mark = cs ? "\u00a7" : "";
            if (prefix && p.w.length == 1) {
                String f = p.w[0];
                if (f.length() < 3) return;
                put(pre3, mark + f.substring(0, 3), p);
            } else put(first, mark + p.w[0], p);
        }
        static void put(Map<String, List<Phrase>> m, String k, Phrase p) {
            List<Phrase> l = m.get(k);
            if (l == null) { l = new ArrayList<Phrase>(); m.put(k, l); }
            l.add(p);
        }
        /** hits[group][owner] -> set of phrase keys; titleHits likewise for tokens inside the title. */
        void scan(String title, String body, Map<Long, Set<String>> hits, Map<Long, Set<String>> titleHits) {
            // "US President", "Pakistan's Supreme Court": not India's offices (see Foreign)
            title = Foreign.mask(title);
            body = Foreign.mask(body);
            List<String> orig = split(title);
            int tLen = orig.size();
            orig.add("\u0000");
            orig.addAll(split(body));
            int n = orig.size();
            String[] o = orig.toArray(new String[0]);
            String[] lo = new String[n];
            for (int i = 0; i < n; i++) lo[i] = o[i].toLowerCase(Locale.ROOT);
            for (int i = 0; i < n; i++) {
                check(first.get(lo[i]), o, lo, i, tLen, hits, titleHits);
                check(first.get("\u00a7" + o[i]), o, lo, i, tLen, hits, titleHits);
                if (lo[i].length() >= 3) check(pre3.get(lo[i].substring(0, 3)), o, lo, i, tLen, hits, titleHits);
                if (o[i].length() >= 3) check(pre3.get("\u00a7" + o[i].substring(0, 3)), o, lo, i, tLen, hits, titleHits);
            }
        }
        static void check(List<Phrase> ps, String[] o, String[] lo, int i, int tLen, Map<Long, Set<String>> hits, Map<Long, Set<String>> th) {
            if (ps == null) return;
            for (Phrase p : ps) {
                if (i + p.w.length > o.length) continue;
                boolean ok = true;
                for (int k = 0; k < p.w.length && ok; k++) {
                    String t = p.cs ? o[i + k] : lo[i + k];
                    if (k == p.w.length - 1 && p.prefix) ok = t.startsWith(p.w[k]);
                    else ok = t.equals(p.w[k]);
                }
                if (ok && p.notPrev != null && i > 0) for (String np : p.notPrev) if (lo[i - 1].equals(np)) ok = false;
                if (!ok) continue;
                long id = ((long) p.group << 32) | p.owner;
                Set<String> s = hits.get(id);
                if (s == null) { s = new HashSet<String>(); hits.put(id, s); }
                s.add(p.key);
                if (i < tLen) {
                    Set<String> s2 = th.get(id);
                    if (s2 == null) { s2 = new HashSet<String>(); th.put(id, s2); }
                    s2.add(p.key);
                }
            }
        }
    }

    static List<String> split(String s) {
        List<String> out = new ArrayList<String>();
        int n = s.length(), st = -1;
        for (int i = 0; i <= n; i++) {
            boolean w = i < n && Character.isLetterOrDigit(s.charAt(i));
            if (w && st < 0) st = i;
            else if (!w && st >= 0) { out.add(s.substring(st, i)); st = -1; }
        }
        return out;
    }

    final Lexicon lex = new Lexicon();

    public final JSONObject raw;
    final List<JSONObject> themes = new ArrayList<JSONObject>();
    final List<JSONObject> ents = new ArrayList<JSONObject>();
    final List<JSONObject> maps = new ArrayList<JSONObject>();
    final List<JSONObject> cons = new ArrayList<JSONObject>();
    final Map<String, JSONObject> conById = new HashMap<String, JSONObject>();
    final List<Integer> noiseW = new ArrayList<Integer>();
    final Map<String, JSONObject> themeById = new HashMap<String, JSONObject>();
    final Map<String, JSONObject> entById = new HashMap<String, JSONObject>();
    final Map<String, JSONObject> mapById = new HashMap<String, JSONObject>();

    public Kb(String json) throws Exception {
        raw = new JSONObject(json);
        JSONArray t = raw.getJSONArray("themes");
        for (int i = 0; i < t.length(); i++) {
            JSONObject o = t.getJSONObject(i);
            themes.add(o); themeById.put(o.getString("id"), o);
            JSONArray kw = o.getJSONArray("kw");
            for (int k = 0; k < kw.length(); k++) lex.add(kw.getString(k), G_THEME, i);
        }
        JSONArray e = raw.getJSONArray("entities");
        for (int i = 0; i < e.length(); i++) {
            JSONObject o = e.getJSONObject(i);
            ents.add(o); entById.put(o.getString("id"), o);
            JSONArray kw = o.getJSONArray("match");
            for (int k = 0; k < kw.length(); k++) lex.add(kw.getString(k), G_ENT, i);
        }
        JSONArray m = raw.getJSONArray("maps");
        for (int i = 0; i < m.length(); i++) {
            JSONObject o = m.getJSONObject(i);
            maps.add(o); mapById.put(o.getString("id"), o);
            JSONArray kw = o.getJSONArray("match");
            for (int k = 0; k < kw.length(); k++) lex.add(kw.getString(k), G_MAP, i);
        }
        JSONArray cn = raw.optJSONArray("concepts");
        for (int i = 0; cn != null && i < cn.length(); i++) {
            JSONObject o = cn.getJSONObject(i);
            cons.add(o); conById.put(o.getString("id"), o);
            JSONArray kw = o.getJSONArray("match");
            for (int k = 0; k < kw.length(); k++) lex.add(kw.getString(k), G_CON, i);
        }
        JSONArray n = raw.getJSONArray("noise");
        for (int i = 0; i < n.length(); i++) {
            JSONObject o = n.getJSONObject(i);
            noiseW.add(o.getInt("w"));
            JSONArray kw = o.getJSONArray("kw");
            for (int k = 0; k < kw.length(); k++) lex.add(kw.getString(k), G_NOISE, i);
        }
    }

    /** Entity ("e:id") and map ("m:id") ids mentioned in a text. */
    public Set<String> idsIn(String text) {
        Map<Long, Set<String>> hits = new HashMap<Long, Set<String>>(), th = new HashMap<Long, Set<String>>();
        lex.scan(text == null ? "" : text, "", hits, th);
        Set<String> out = new HashSet<String>();
        for (Long k : hits.keySet()) {
            int g = (int) (k >> 32), o = (int) (k & 0xffffffffL);
            if (g == G_ENT) out.add("e:" + ents.get(o).optString("id"));
            else if (g == G_MAP) out.add("m:" + maps.get(o).optString("id"));
        }
        return out;
    }

    /** Every knowledge-base card named in a text: "e:id" (body/scheme/species…), "m:id" (map), "c:id" (concept). */
    public Set<String> cardsIn(String text) {
        Map<Long, Set<String>> hits = new HashMap<Long, Set<String>>(), th = new HashMap<Long, Set<String>>();
        lex.scan(text == null ? "" : text, "", hits, th);
        Set<String> out = new HashSet<String>();
        for (Long k : hits.keySet()) {
            int g = (int) (k >> 32), o = (int) (k & 0xffffffffL);
            if (g == G_ENT) out.add("e:" + ents.get(o).optString("id"));
            else if (g == G_MAP) out.add("m:" + maps.get(o).optString("id"));
            else if (g == G_CON) out.add("c:" + cons.get(o).optString("id"));
        }
        return out;
    }

    /** "e:id" / "m:id" / "c:id" of every card with this display name (case-insensitive; "Money Bill" is both a body and a concept). */
    public List<String> cardsByName(String name) {
        List<String> out = new ArrayList<String>();
        if (name == null) return out;
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (n.isEmpty()) return out;
        for (JSONObject o : ents) if (o.optString("name").toLowerCase(Locale.ROOT).equals(n)) out.add("e:" + o.optString("id"));
        for (JSONObject o : cons) if (o.optString("name").toLowerCase(Locale.ROOT).equals(n)) out.add("c:" + o.optString("id"));
        for (JSONObject o : maps) if (o.optString("name").toLowerCase(Locale.ROOT).equals(n)) out.add("m:" + o.optString("id"));
        return out;
    }

    /** First card with this display name, or null. */
    public String cardByName(String name) { List<String> l = cardsByName(name); return l.isEmpty() ? null : l.get(0); }

    /** Theme ids whose keywords appear in a text. */
    public Set<String> themesIn(String text) {
        Map<Long, Set<String>> hits = new HashMap<Long, Set<String>>(), th = new HashMap<Long, Set<String>>();
        lex.scan(text == null ? "" : text, "", hits, th);
        Set<String> out = new HashSet<String>();
        for (Long k : hits.keySet()) if ((int) (k >> 32) == G_THEME) out.add(themes.get((int) (k & 0xffffffffL)).optString("id"));
        return out;
    }

    /** Concept ids named in a text, most specific (longest keyword) first. */
    public List<String> conceptsIn(String text) {
        Map<Long, Set<String>> hits = new HashMap<Long, Set<String>>(), th = new HashMap<Long, Set<String>>();
        lex.scan(text == null ? "" : text, "", hits, th);
        final Map<String, Integer> best = new HashMap<String, Integer>();
        for (Map.Entry<Long, Set<String>> e : hits.entrySet()) {
            if ((int) (e.getKey() >> 32) != G_CON) continue;
            int len = 0;
            for (String k : e.getValue()) len = Math.max(len, k.length());
            best.put(cons.get((int) (e.getKey() & 0xffffffffL)).optString("id"), len);
        }
        List<String> out = new ArrayList<String>(best.keySet());
        java.util.Collections.sort(out, new java.util.Comparator<String>() {
            public int compare(String a, String b) { return best.get(b) - best.get(a); }
        });
        return out;
    }

    public JSONObject theme(String id) { return id == null ? null : themeById.get(id); }
    public JSONObject entity(String id) { return entById.get(id); }
    public JSONObject map(String id) { return mapById.get(id); }
    public JSONObject concept(String id) { return conById.get(id); }

    static int tierW(String tier) {
        if ("HIGH".equals(tier)) return 3;
        if ("MED".equals(tier)) return 2;
        return 1;
    }

    public static final double DIGEST_MIN = 6.0;
    public static final double ALERT_MIN = 11.0;

    /** Scores an item in place: sc, th, th2, gs, tier, ents, maps, noise. */
    public void score(JSONObject it) throws Exception {
        String title = it.optString("t");
        String desc = it.optString("d");
        String orig = title + ". " + desc;
        Map<Long, Set<String>> hits = new HashMap<Long, Set<String>>(), thits = new HashMap<Long, Set<String>>();
        lex.scan(title, desc, hits, thits);
        double best = 0, second = 0; int bi = -1, si = -1;
        for (int i = 0; i < themes.size(); i++) {
            Set<String> h = hits.get(((long) G_THEME << 32) | i);
            if (h == null) continue;
            Set<String> t2 = thits.get(((long) G_THEME << 32) | i);
            double raw = Math.min(h.size(), 4) + Math.min(t2 == null ? 0 : t2.size(), 2);
            double lift = themes.get(i).optDouble("lift", 1.0);
            double m = Math.max(0.9, Math.min(1.3, 0.75 + 0.15 * lift)); // recency weighting (2013–2025 vs earlier PYQs)
            double s = tierW(themes.get(i).optString("tier")) * raw * m;
            if (s > best) { second = best; si = bi; best = s; bi = i; }
            else if (s > second) { second = s; si = i; }
        }
        double score = best + 0.35 * second;
        JSONArray ea = new JSONArray();
        for (int i = 0; i < ents.size(); i++) if (hits.containsKey(((long) G_ENT << 32) | i)) ea.put(ents.get(i).getString("id"));
        JSONArray ma = new JSONArray();
        for (int i = 0; i < maps.size(); i++) if (hits.containsKey(((long) G_MAP << 32) | i)) ma.put(maps.get(i).getString("id"));
        score += Math.min(ea.length(), 2) * 1.5 + Math.min(ma.length(), 1) * 1.5;
        if (Text.hasFigure(orig)) score += 1;
        String s = it.optString("s");
        if (s.equals("PIB") || s.equals("PMI") || s.equals("RBI") || s.equals("MEA") || s.equals("PRS") || s.equals("ISRO") || s.equals("AIR")) score += 1;
        else if (s.equals("TH") || s.equals("IE")) score += 0.5;
        double pen = 0;
        JSONArray noise = new JSONArray();
        for (int i = 0; i < noiseW.size(); i++) {
            if (hits.containsKey(((long) G_NOISE << 32) | i)) { pen += noiseW.get(i); noise.put(i); }
        }
        score += pen;
        if (desc.length() == 0 && bi >= 0) score -= 0.5; // headline-only items are weaker evidence
        // another country's domestic politics with no India link is outside the Prelims scope
        boolean foreign = Foreign.outOfScope(orig) && !Foreign.neighbour(orig);
        if (foreign) score -= 2.5;
        it.put("sc", Math.round(score * 10) / 10.0);
        if (bi >= 0 && best >= 2) {
            JSONObject th = themes.get(bi);
            it.put("th", th.getString("id"));
            it.put("gs", th.getString("gs"));
            it.put("tier", th.getString("tier"));
        } else {
            it.remove("th"); it.put("gs", "Misc"); it.put("tier", "LOW");
        }
        if (si >= 0 && second >= 2) it.put("th2", themes.get(si).getString("id")); else it.remove("th2");
        it.put("ents", ea);
        it.put("maps", ma);
        if (noise.length() > 0) it.put("noise", noise); else it.remove("noise");
        if (foreign) it.put("fx", true); else it.remove("fx");
        // concept cards named by the story itself (no effect on the score)
        JSONArray ca = new JSONArray();
        for (int i = 0; i < cons.size(); i++) {
            Set<String> h = hits.get(((long) G_CON << 32) | i);
            if (h != null) ca.put(cons.get(i).getString("id"));
        }
        if (ca.length() > 0) it.put("cons", ca); else it.remove("cons");
    }
}
