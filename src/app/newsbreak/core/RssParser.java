package app.newsbreak.core;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

/* loaded from: classes.dex */
public final class RssParser {
    private static final Pattern ITEM = Pattern.compile("<(item|entry)[\\s>].*?</\\1>", 34);
    private static final Pattern URL_DATE = Pattern.compile("/(20\\d\\d)[/-](0?[1-9]|1[0-2])[/-](0?[1-9]|[12]\\d|3[01])(?=[/-]|$)");

    public interface ParserFactory {
        XmlPullParser create() throws Exception;
    }

    public static final class Raw {
        public String title = "";
        public String link = "";
        public String desc = "";
        public String date = "";
        public String publisher = "";
    }

    public static List<Raw> parse(String str, ParserFactory parserFactory) {
        List<Raw> arrayList;
        ArrayList arrayList2 = new ArrayList();
        if (str != null) {
            String trim = str.trim();
            int indexOf = trim.indexOf(60);
            String substring = indexOf > 0 ? trim.substring(indexOf) : trim;
            try {
                arrayList = pull(substring, parserFactory);
            } catch (Throwable th) {
                arrayList = new ArrayList<>();
            }
            return arrayList.isEmpty() ? regex(substring) : arrayList;
        }
        return arrayList2;
    }

    private static List<Raw> pull(String str, ParserFactory parserFactory) throws Exception {
        ArrayList arrayList = new ArrayList();
        XmlPullParser create = parserFactory.create();
        try {
            create.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", false);
        } catch (Exception e) {
        }
        create.setInput(new StringReader(str));
        Raw raw = null;
        for (int eventType = create.getEventType(); eventType != 1; eventType = create.next()) {
            if (eventType == 2) {
                String local = local(create.getName());
                if (local.equals("item") || local.equals("entry")) {
                    raw = new Raw();
                } else if (raw != null) {
                    if (local.equals("link")) {
                        String attributeValue = create.getAttributeValue(null, "href");
                        String attributeValue2 = create.getAttributeValue(null, "rel");
                        if (attributeValue == null || !(attributeValue2 == null || attributeValue2.equals("alternate"))) {
                            String safeText = safeText(create);
                            if (raw.link.length() == 0) {
                                raw.link = safeText.trim();
                            }
                        } else if (raw.link.length() == 0) {
                            raw.link = attributeValue;
                        }
                    } else if (local.equals("title")) {
                        String safeText2 = safeText(create);
                        if (raw.title.trim().length() == 0) {
                            raw.title = safeText2;
                        }
                    } else if (local.equals("description") || local.equals("summary")) {
                        String safeText3 = safeText(create);
                        if (raw.desc.length() == 0 || safeText3.length() > raw.desc.length()) {
                            raw.desc = safeText3;
                        }
                    } else if (local.equals("encoded") || local.equals("content")) {
                        String safeText4 = safeText(create);
                        if (raw.desc.length() < 40 && safeText4.length() > raw.desc.length()) {
                            raw.desc = safeText4;
                        }
                    } else if (local.equals("pubDate") || local.equals("published") || local.equals("updated") || local.equals("date")) {
                        String safeText5 = safeText(create);
                        if (raw.date.length() == 0) {
                            raw.date = safeText5;
                        }
                    } else if (local.equals("source")) {
                        raw.publisher = safeText(create);
                    } else if (local.equals("guid") && raw.link.length() == 0) {
                        String safeText6 = safeText(create);
                        if (safeText6.startsWith("http")) {
                            raw.link = safeText6;
                        }
                    }
                }
            } else if (eventType == 3) {
                String local2 = local(create.getName());
                if ((local2.equals("item") || local2.equals("entry")) && raw != null) {
                    if (raw.title.trim().length() > 0) {
                        arrayList.add(raw);
                    }
                    raw = null;
                }
            }
        }
        return arrayList;
    }

    private static String local(String str) {
        if (str == null) {
            return "";
        }
        int indexOf = str.indexOf(58);
        return indexOf >= 0 ? str.substring(indexOf + 1) : str;
    }

    private static String safeText(XmlPullParser xmlPullParser) {
        try {
            StringBuilder sb = new StringBuilder();
            int i = 1;
            while (i > 0) {
                int next = xmlPullParser.next();
                if (next == 4 || next == 5 || next == 6) {
                    String text = xmlPullParser.getText();
                    if (text != null) {
                        sb.append(text);
                    }
                } else if (next == 2) {
                    i++;
                } else if (next == 3) {
                    i--;
                } else if (next == 1) {
                    break;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String tag(String str, String str2) {
        Matcher matcher = Pattern.compile("<(?:[a-z]+:)?" + str2 + "(?:\\s[^>]*)?>(.*?)</(?:[a-z]+:)?" + str2 + ">", 34).matcher(str);
        if (!matcher.find()) {
            return "";
        }
        String trim = matcher.group(1).trim();
        if (trim.startsWith("<![CDATA[")) {
            return trim.substring(9, trim.endsWith("]]>") ? trim.length() - 3 : trim.length());
        }
        return trim;
    }

    static List<Raw> regex(String str) {
        ArrayList arrayList = new ArrayList();
        Matcher matcher = ITEM.matcher(str);
        while (matcher.find()) {
            String group = matcher.group();
            Raw raw = new Raw();
            raw.title = tag(group, "title");
            raw.link = tag(group, "link");
            if (raw.link.length() == 0) {
                Matcher matcher2 = Pattern.compile("<link[^>]*href=\"([^\"]+)\"", 2).matcher(group);
                if (matcher2.find()) {
                    raw.link = matcher2.group(1);
                }
            }
            raw.desc = tag(group, "description");
            if (raw.desc.length() == 0) {
                raw.desc = tag(group, "summary");
            }
            raw.date = tag(group, "pubDate");
            if (raw.date.length() == 0) {
                raw.date = tag(group, "published");
            }
            if (raw.date.length() == 0) {
                raw.date = tag(group, "updated");
            }
            if (raw.date.length() == 0) {
                raw.date = tag(group, "date");
            }
            raw.publisher = tag(group, "source");
            if (raw.title.trim().length() > 0) {
                arrayList.add(raw);
            }
        }
        return arrayList;
    }

    public static long urlDate(String str) {
        if (str == null) {
            return 0L;
        }
        Matcher matcher = URL_DATE.matcher(str);
        if (!matcher.find()) {
            return 0L;
        }
        try {
            return Ist.startOf(String.format(Locale.ROOT, "%04d-%02d-%02d", Integer.valueOf(Integer.parseInt(matcher.group(1))), Integer.valueOf(Integer.parseInt(matcher.group(2))), Integer.valueOf(Integer.parseInt(matcher.group(3)))));
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Raw feed entries → items. Google News descriptions are only kept when they say more than the title.
     * Date: the feed's pubDate, unless the article URL carries a date and the pubDate is more than half a day past that
     * day (republished/updated pages) — then the URL's date wins ("ud").
     */
    public static List<JSONObject> normalise(List<Raw> raws, String code, String label, boolean gn, long now) throws Exception {
        List<JSONObject> out = new ArrayList<JSONObject>();
        for (Raw r : raws) {
            String pub = Text.clean(r.publisher);
            String t = Text.cleanTitle(r.title, pub);
            if (t.length() < 12) continue;
            String d = Text.clean(r.desc);
            if (gn && (d.length() < 220 || Text.jaccard(Text.tokens(d), Text.tokens(t)) > 0.5)) d = "";
            if (d.equalsIgnoreCase(t)) d = "";
            if (d.length() > 900) d = Text.cap(d, 900);
            String link = Text.clean(r.link);
            long pubT = Ist.parse(Text.clean(r.date));
            boolean ud = false;
            long p;
            long urlD = urlDate(link);
            if (urlD > 0) {
                p = urlD + 12 * 3600000L;
                if (pubT <= 0) { p = Math.min(p, now); ud = true; }
                else if (pubT - (urlD + Ist.DAY) > 12 * 3600000L) ud = true;
                else p = pubT;
            } else p = pubT;
            JSONObject o = new JSONObject();
            o.put("id", Text.hash(code + "|" + (link.length() > 0 ? Text.normUrl(link) : t.toLowerCase())));
            o.put("t", t);
            o.put("d", d);
            o.put("u", link);
            o.put("s", code);
            o.put("f", label);
            o.put("p", p > 0 ? p : now);
            o.put("pd", p > 0);
            if (ud) o.put("ud", true);
            o.put("fs", now);
            if (pub.length() > 0) o.put("pub", pub);
            out.add(o);
        }
        return out;
    }
}
