package app.newsbreak.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/* loaded from: classes.dex */
public final class Html {
    static final Set<String> VOID = new HashSet(Arrays.asList("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"));
    static final Set<String> SKIP = new HashSet(Arrays.asList("nav", "footer", "aside", "form", "button", "select", "svg", "noscript", "template", "iframe", "figure", "picture", "video", "audio", "canvas", "object", "math", "dialog", "label", "textarea"));
    static final Set<String> RAW = new HashSet(Arrays.asList("script", "style"));
    static final Set<String> BLOCKISH = new HashSet(Arrays.asList("p", "div", "li", "h1", "h2", "h3", "h4", "h5", "h6", "td", "th", "dd", "dt", "blockquote", "section", "article", "main", "pre", "figcaption", "caption", "tr", "ul", "ol", "table", "header", "hr", "br"));
    static final Set<String> CONTAINER = new HashSet(Arrays.asList("div", "article", "section", "main", "td", "body", "header"));
    private static final Pattern SKIP_TOKEN = Pattern.compile("(?i)^(?:[a-z0-9]+[-_])*(nav|navbar|navigation|menu|menubar|sidebar|footer|breadcrumbs?|social|share|sharing|addtoany|a2a_kit|comments?|disqus|related|advert|advertisement|ads|adsbygoogle|adslot|subscribe|newsletter|popup|modal|cookie|consent|login|signup|trending|pagination)(?:[-_][a-z0-9]+)*$");
    private static final Pattern SKIP_EXACT = Pattern.compile("(?i)^(also-read|read-also|more-stories|top-stories|latest-news|most-read|most-popular|author-box|tags-list|taglist|sr-only|visually-hidden|skip-link|ad-slot|ad-container)$");
    static final Set<String> NEVER_SKIP = new HashSet(Arrays.asList("html", "body", "main", "article", "h1", "h2", "h3", "h4", "h5", "h6"));
    static final Set<String> OPTIONAL_END = new HashSet(Arrays.asList("li", "p", "td", "th", "dd", "dt", "tr", "option"));
    private static final Pattern ATTR = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*(?:=\\s*(\"[^\"]*\"|'[^']*'|[^\\s\"'>]+))?");
    private static final Pattern BOILER = Pattern.compile("(?i)^(also read|read also|read more|click here|follow us|subscribe|sign up|download the|published\\s*[-:–]|updated\\s*[-:–]|copyright|©|all rights reserved|advertisement|share this|share on|tags?\\s*:|first published|last updated|this article is closed|support quality journalism|to read this article|you have reached your|already have an account|for all the latest|catch all the|get latest|stay updated|join our|whatsapp|telegram|recommended|trending|listen to this article|view comments|image\\s*(source|credit)|photo\\s*:|file photo)");

    public static final class Article {
        public long published;
        public String title = "";
        public String desc = "";
        public String url = "";
        public final List<String> paras = new ArrayList();
    }

    public static final class Doc {
        public final List<Block> blocks = new ArrayList();
        public String title = "";
        public String ogTitle = "";
        public String desc = "";
        public String published = "";
        public String canonical = "";
        public String ldBody = "";
        public String ldHeadline = "";
        public String ldDate = "";
        public String ldDesc = "";
    }

    private Html() {
    }

    public static final class Block {
        public boolean bold;
        public String href;
        public int level;
        int[] path;
        public String tag;
        public String text;

        public String toString() {
            return this.tag + this.level + (this.bold ? "*" : "") + ": " + this.text;
        }
    }

    static boolean furniture(String str) {
        if (str == null) {
            return false;
        }
        for (String str2 : str.trim().split("\\s+")) {
            if (str2.length() > 1 && (SKIP_TOKEN.matcher(str2).matches() || SKIP_EXACT.matcher(str2).matches())) {
                return true;
            }
        }
        return false;
    }

    static String attr(String str, String str2) {
        Matcher matcher = ATTR.matcher(str);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase(str2)) {
                String group = matcher.group(2);
                if (group == null) {
                    return "";
                }
                if ((group.startsWith("\"") && group.endsWith("\"")) || (group.startsWith("'") && group.endsWith("'"))) {
                    group = group.substring(1, group.length() - 1);
                }
                return Text.decodeEntities(group);
            }
        }
        return null;
    }

    public static Doc parse(String str) {
        String attr;
        int i;
        String str2;
        int i2;
        String attr2;
        Doc doc = new Doc();
        if (str != null) {
            int length = str.length();
            int i3 = 0;
            StringBuilder sb = new StringBuilder();
            int i4 = 0;
            String str3 = null;
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            ArrayList arrayList3 = new ArrayList();
            int i5 = 0;
            int i6 = 1;
            String str4 = null;
            StringBuilder sb2 = new StringBuilder();
            String str5 = null;
            boolean z = false;
            boolean z2 = false;
            int i7 = 0;
            int i8 = 0;
            while (i3 < length) {
                if (str.charAt(i3) != '<') {
                    int indexOf = str.indexOf(60, i3);
                    if (indexOf < 0) {
                        indexOf = length;
                    }
                    if (str4 == null) {
                        String substring = str.substring(i3, indexOf);
                        if (z2) {
                            sb2.append(substring);
                        } else {
                            String replace = Text.decodeEntities(substring).replace((char) 160, ' ');
                            if (replace.trim().length() > 0) {
                                if (i8 > 0) {
                                    i4 += replace.trim().length();
                                }
                                if (z && str3 == null && str5 != null) {
                                    str3 = str5;
                                }
                            }
                            sb.append(replace);
                        }
                    }
                    i3 = indexOf;
                } else {
                    char charAt = i3 + 1 < length ? str.charAt(i3 + 1) : ' ';
                    if (!Character.isLetter(charAt) && charAt != '/' && charAt != '!' && charAt != '?') {
                        if (str4 == null && !z2) {
                            sb.append('<');
                        }
                        i3++;
                    } else if (str.startsWith("<!--", i3)) {
                        int indexOf2 = str.indexOf("-->", i3 + 4);
                        i3 = indexOf2 < 0 ? length : indexOf2 + 3;
                    } else if (i3 + 1 >= length || !(str.charAt(i3 + 1) == '!' || str.charAt(i3 + 1) == '?')) {
                        int tagEnd = tagEnd(str, i3);
                        if (tagEnd < 0 && (tagEnd = str.indexOf(62, i3 + 1)) < 0) {
                            break;
                        }
                        String substring2 = str.substring(i3 + 1, tagEnd);
                        i3 = tagEnd + 1;
                        boolean startsWith = substring2.startsWith("/");
                        String substring3 = startsWith ? substring2.substring(1) : substring2;
                        int i9 = 0;
                        while (i9 < substring3.length() && !Character.isWhitespace(substring3.charAt(i9)) && substring3.charAt(i9) != '/' && substring3.charAt(i9) != '>') {
                            i9++;
                        }
                        String lowerCase = substring3.substring(0, i9).toLowerCase(Locale.ROOT);
                        if (lowerCase.length() != 0) {
                            boolean endsWith = substring2.endsWith("/");
                            if (!startsWith && RAW.contains(lowerCase)) {
                                int indexOfIgnoreCase = indexOfIgnoreCase(str, "</" + lowerCase, i3);
                                String substring4 = str.substring(i3, indexOfIgnoreCase < 0 ? length : indexOfIgnoreCase);
                                if ("script".equals(lowerCase) && (attr2 = attr(substring3, "type")) != null && attr2.toLowerCase(Locale.ROOT).contains("ld+json")) {
                                    ldJson(doc, substring4);
                                }
                                if (indexOfIgnoreCase < 0) {
                                    i3 = length;
                                } else {
                                    int indexOf3 = str.indexOf(62, indexOfIgnoreCase);
                                    i3 = indexOf3 < 0 ? length : indexOf3 + 1;
                                }
                            } else if (str4 != null) {
                                if (lowerCase.equals(str4) && !VOID.contains(lowerCase)) {
                                    if (startsWith) {
                                        int i10 = i7 - 1;
                                        if (i10 == 0) {
                                            i7 = i10;
                                            str4 = null;
                                        } else {
                                            i7 = i10;
                                        }
                                    } else if (!endsWith) {
                                        i7++;
                                    }
                                }
                            } else if (!startsWith) {
                                if ("meta".equals(lowerCase)) {
                                    meta(doc, substring3);
                                } else if ("link".equals(lowerCase)) {
                                    String attr3 = attr(substring3, "rel");
                                    if (attr3 != null && attr3.equalsIgnoreCase("canonical") && (attr = attr(substring3, "href")) != null) {
                                        doc.canonical = attr;
                                    }
                                } else if ("title".equals(lowerCase) && doc.title.length() == 0) {
                                    z2 = true;
                                } else {
                                    boolean contains = SKIP.contains(lowerCase);
                                    if (!contains && !VOID.contains(lowerCase) && !NEVER_SKIP.contains(lowerCase) && !OPTIONAL_END.contains(lowerCase)) {
                                        String attr4 = attr(substring3, "class");
                                        String attr5 = attr(substring3, "id");
                                        String attr6 = attr(substring3, "role");
                                        String attr7 = attr(substring3, "hidden");
                                        String attr8 = attr(substring3, "aria-hidden");
                                        String attr9 = attr(substring3, "style");
                                        if (furniture(attr4) || furniture(attr5)) {
                                            contains = true;
                                        }
                                        if (attr6 != null && (attr6.equals("navigation") || attr6.equals("banner") || attr6.equals("contentinfo") || attr6.equals("complementary") || attr6.equals("dialog"))) {
                                            contains = true;
                                        }
                                        if (attr7 != null || "true".equals(attr8)) {
                                            contains = true;
                                        }
                                        if (attr9 != null && attr9.replace(" ", "").toLowerCase(Locale.ROOT).contains("display:none")) {
                                            contains = true;
                                        }
                                        if (!"header".equals(lowerCase) || attr4 != null || attr5 != null || attr6 == null) {
                                        }
                                    }
                                    if (contains && !VOID.contains(lowerCase) && !endsWith) {
                                        flush(doc, sb, arrayList, arrayList2, i5, str3, i4);
                                        sb.setLength(0);
                                        str3 = null;
                                        i4 = 0;
                                        i7 = 1;
                                        str4 = lowerCase;
                                    } else if ("a".equals(lowerCase)) {
                                        str5 = attr(substring3, "href");
                                        z = true;
                                    } else if ("b".equals(lowerCase) || "strong".equals(lowerCase)) {
                                        i8++;
                                    } else if ("br".equals(lowerCase)) {
                                        sb.append(' ');
                                    } else if (!"img".equals(lowerCase)) {
                                        if (BLOCKISH.contains(lowerCase)) {
                                            flush(doc, sb, arrayList, arrayList2, i5, str3, i4);
                                            sb.setLength(0);
                                            i2 = 0;
                                            if ("ul".equals(lowerCase) || "ol".equals(lowerCase)) {
                                                i5++;
                                            }
                                            if (!VOID.contains(lowerCase) && !endsWith && !"hr".equals(lowerCase)) {
                                                arrayList.add(lowerCase);
                                            }
                                            if (!CONTAINER.contains(lowerCase) || endsWith) {
                                                i = i6;
                                                str2 = null;
                                            } else {
                                                arrayList2.add(Integer.valueOf(i6));
                                                arrayList3.add(lowerCase);
                                                i = i6 + 1;
                                                str2 = null;
                                            }
                                        } else {
                                            i = i6;
                                            str2 = str3;
                                            i2 = i4;
                                        }
                                        i6 = i;
                                        str3 = str2;
                                        i4 = i2;
                                    }
                                }
                            } else if ("title".equals(lowerCase)) {
                                if (z2) {
                                    doc.title = Text.clean(sb2.toString());
                                    z2 = false;
                                }
                            } else if ("a".equals(lowerCase)) {
                                str5 = null;
                                z = false;
                            } else if (!"b".equals(lowerCase) && !"strong".equals(lowerCase)) {
                                if (BLOCKISH.contains(lowerCase)) {
                                    flush(doc, sb, arrayList, arrayList2, i5, str3, i4);
                                    sb.setLength(0);
                                    str2 = null;
                                    i2 = 0;
                                    int size = arrayList.size() - 1;
                                    while (true) {
                                        if (size < 0) {
                                            break;
                                        }
                                        if (((String) arrayList.get(size)).equals(lowerCase)) {
                                            while (arrayList.size() > size) {
                                                arrayList.remove(arrayList.size() - 1);
                                            }
                                            break;
                                        } else {
                                            size--;
                                        }
                                    }
                                    int i11 = (("ul".equals(lowerCase) || "ol".equals(lowerCase)) && i5 > 0) ? i5 - 1 : i5;
                                    if (CONTAINER.contains(lowerCase)) {
                                        int size2 = arrayList3.size() - 1;
                                        while (true) {
                                            if (size2 < 0) {
                                                break;
                                            }
                                            if (((String) arrayList3.get(size2)).equals(lowerCase)) {
                                                while (arrayList3.size() > size2) {
                                                    arrayList3.remove(arrayList3.size() - 1);
                                                    arrayList2.remove(arrayList2.size() - 1);
                                                }
                                                break;
                                            } else {
                                                size2--;
                                            }
                                        }
                                    }
                                    i5 = i11;
                                } else {
                                    str2 = str3;
                                    i2 = i4;
                                }
                                str3 = str2;
                                i4 = i2;
                            } else if (i8 > 0) {
                                i8--;
                            }
                        }
                    } else {
                        int indexOf4 = str.indexOf(62, i3);
                        i3 = indexOf4 < 0 ? length : indexOf4 + 1;
                    }
                }
            }
            flush(doc, sb, arrayList, arrayList2, i5, str3, i4);
            if (doc.title.length() == 0 && sb2.length() > 0) {
                doc.title = Text.clean(sb2.toString());
            }
        }
        return doc;
    }

    static int tagEnd(String str, int i) {
        boolean z = false;
        boolean z2 = false;
        for (int i2 = i + 1; i2 < str.length(); i2++) {
            char charAt = str.charAt(i2);
            if (charAt == '\"' && !z2) {
                z = !z;
            } else if (charAt == '\'' && !z) {
                z2 = !z2;
            } else if (charAt != '>' || z2 || z) {
                if (charAt == '<' && !z2 && !z && i2 > i + 1) {
                    return i2 - 1;
                }
            } else {
                return i2;
            }
        }
        return -1;
    }

    static int indexOfIgnoreCase(String str, String str2, int i) {
        int length = str2.length();
        for (int i2 = i; i2 + length <= str.length(); i2++) {
            if (str.regionMatches(true, i2, str2, 0, length)) {
                return i2;
            }
        }
        return -1;
    }

    static void flush(Doc doc, StringBuilder sb, List<String> list, List<Integer> list2, int i, String str, int i2) {
        String trim = sb.toString().replaceAll("\\s+", " ").trim();
        if (trim.length() < 2) return;
        Block block = new Block();
        String tag = "p";
        int level = i;
        found:
        {
            for (int k = list.size() - 1; k >= 0; k--) {
                String t = list.get(k);
                if (t.length() == 2 && t.charAt(0) == 'h' && Character.isDigit(t.charAt(1))) { level = t.charAt(1) - '0'; tag = "h"; break found; }
                if ("li".equals(t)) { level = Math.max(1, level); tag = "li"; break found; }
                if ("p".equals(t) || "dd".equals(t) || "dt".equals(t) || "blockquote".equals(t) || "td".equals(t) || "th".equals(t) || "pre".equals(t)) break;
                if ("figcaption".equals(t)) { level = 0; break found; }
            }
            level = 0;
        }
        block.tag = tag;
        block.level = level;
        block.text = trim;
        block.href = str;
        block.bold = ((double) i2) >= Math.max(3.0d, ((double) trim.replaceAll("[\\s:?.\\-–—]", "").length()) * 0.85d);
        block.path = new int[list2.size()];
        for (int k = 0; k < list2.size(); k++) block.path[k] = list2.get(k).intValue();
        doc.blocks.add(block);
    }

    static void meta(Doc doc, String str) {
        String attr = attr(str, "property");
        String attr2 = attr(str, "name");
        String attr3 = attr(str, "content");
        if (attr3 != null) {
            if (attr == null) {
                attr = attr2 != null ? attr2 : "";
            }
            String lowerCase = attr.toLowerCase(Locale.ROOT);
            String clean = Text.clean(attr3);
            if (!lowerCase.equals("og:title") || doc.ogTitle.length() != 0) {
                if ((!lowerCase.equals("og:description") && !lowerCase.equals("description") && !lowerCase.equals("twitter:description")) || clean.length() <= doc.desc.length()) {
                    if ((!lowerCase.equals("article:published_time") && !lowerCase.equals("datepublished") && !lowerCase.equals("publish-date") && !lowerCase.equals("pubdate")) || doc.published.length() != 0) {
                        if (!lowerCase.equals("og:url") || doc.canonical.length() != 0) {
                            return;
                        }
                        doc.canonical = clean;
                        return;
                    }
                    doc.published = clean;
                    return;
                }
                doc.desc = clean;
                return;
            }
            doc.ogTitle = clean;
        }
    }

    static void ldJson(Doc doc, String str) {
        try {
            ldWalk(doc, new JSONTokener(str.trim()).nextValue(), 0);
        } catch (Exception e) {
        }
    }

    static void ldWalk(Doc doc, Object obj, int i) {
        if (i <= 6 && obj != null) {
            if (!(obj instanceof JSONArray)) {
                if (obj instanceof JSONObject) {
                    JSONObject jSONObject = (JSONObject) obj;
                    String optString = jSONObject.optString("articleBody", "");
                    if (optString.length() > doc.ldBody.length()) {
                        doc.ldBody = optString;
                        doc.ldHeadline = Text.clean(jSONObject.optString("headline", doc.ldHeadline));
                        doc.ldDate = jSONObject.optString("datePublished", doc.ldDate);
                        doc.ldDesc = Text.clean(jSONObject.optString("description", doc.ldDesc));
                    } else if (doc.ldDate.length() == 0 && jSONObject.has("datePublished") && String.valueOf(jSONObject.opt("@type")).contains("Article")) {
                        doc.ldDate = jSONObject.optString("datePublished");
                        if (doc.ldHeadline.length() == 0) {
                            doc.ldHeadline = Text.clean(jSONObject.optString("headline"));
                        }
                    }
                    if (jSONObject.has("@graph")) {
                        ldWalk(doc, jSONObject.opt("@graph"), i + 1);
                    }
                    if (jSONObject.has("mainEntity")) {
                        ldWalk(doc, jSONObject.opt("mainEntity"), i + 1);
                        return;
                    }
                    return;
                }
                return;
            }
            JSONArray jSONArray = (JSONArray) obj;
            for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                ldWalk(doc, jSONArray.opt(i2), i + 1);
            }
        }
    }

    public static boolean boiler(String str) {
        return BOILER.matcher(str.trim()).find();
    }

    public static List<Block> mainBlocks(Doc doc) {
        int i;
        int intValue;
        ArrayList arrayList = new ArrayList();
        HashMap hashMap = new HashMap();
        HashMap hashMap2 = new HashMap();
        long j = 0;
        for (Block block : doc.blocks) {
            if ("p".equals(block.tag) || "li".equals(block.tag)) {
                int length = block.text.length();
                if (length >= 70 && !boiler(block.text)) {
                    j += length;
                    int i2 = 0;
                    while (true) {
                        int i3 = i2;
                        if (i3 < block.path.length) {
                            Long l = (Long) hashMap.get(Integer.valueOf(block.path[i3]));
                            hashMap.put(Integer.valueOf(block.path[i3]), Long.valueOf((l == null ? 0L : l.longValue()) + length));
                            hashMap2.put(Integer.valueOf(block.path[i3]), Integer.valueOf(i3));
                            i2 = i3 + 1;
                        } else {
                    break;
                }
                    }
                }
            }
        }
        if (j == 0) {
            return arrayList;
        }
        int i4 = -1;
        int i5 = -1;
        Iterator it = hashMap.entrySet().iterator();
        while (true) {
            int i6 = i5;
            i = i4;
            if (!it.hasNext()) {
                break;
            }
            Map.Entry entry = (Map.Entry) it.next();
            if (((Long) entry.getValue()).longValue() < j * 0.6d || (intValue = ((Integer) hashMap2.get(entry.getKey())).intValue()) <= i6) {
                i5 = i6;
            } else {
                i = ((Integer) entry.getKey()).intValue();
                i5 = intValue;
            }
            i4 = i;
        }
        for (Block block2 : doc.blocks) {
            boolean z = i < 0;
            for (int i7 = 0; !z && i7 < block2.path.length; i7++) {
                if (block2.path[i7] == i) {
                    z = true;
                }
            }
            if (z) {
                arrayList.add(block2);
            }
        }
        return arrayList;
    }

    public static Article article(String str, String str2) {
        Doc parse = parse(str);
        Article article = new Article();
        article.url = str2;
        article.title = first(parse.ldHeadline, parse.ogTitle, parse.title);
        article.desc = first(parse.desc, parse.ldDesc);
        article.published = Ist.parse(first(parse.ldDate, parse.published));
        String clean = Text.clean(parse.ldBody.replace("\\n", "\n"));
        List<Block> mainBlocks = mainBlocks(parse);
        int i = 0;
        for (Block block : mainBlocks) {
            i = !"h".equals(block.tag) ? block.text.length() + i : i;
        }
        if (clean.length() > 400 && clean.length() >= i * 0.6d) {
            List<String> sentences = Text.sentences(clean);
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = sentences.iterator();
            while (it.hasNext()) {
                sb.append(it.next()).append(' ');
                if (sb.length() > 320) {
                    article.paras.add(sb.toString().trim());
                    sb.setLength(0);
                }
            }
            if (sb.length() > 0) {
                article.paras.add(sb.toString().trim());
            }
        } else {
            for (Block block2 : mainBlocks) {
                if (!"h".equals(block2.tag)) {
                    String str3 = block2.text;
                    if (str3.length() >= 40 && !boiler(str3)) {
                        article.paras.add(str3);
                    }
                }
            }
        }
        return article;
    }

    static String first(String... strArr) {
        for (String str : strArr) {
            if (str != null && str.trim().length() > 0) {
                return str.trim();
            }
        }
        return "";
    }
}
