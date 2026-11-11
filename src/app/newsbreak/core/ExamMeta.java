package app.newsbreak.core;

import java.util.regex.Pattern;

/**
 * News about the exam itself is not current affairs: UPSC notifications and calendars, results, toppers, strategy,
 * "how Mains questions are designed", answer-writing practice, quizzes, daily roundup posts and "latest news" link lists.
 * None of it is asked in Prelims, so it never becomes a card, an "also" line or a bullet.
 *
 * News about bodies that happen to run exams stays (a new UPSC chairperson, the Public Examinations Act).
 */
public final class ExamMeta {
    private ExamMeta() {}

    /** An exam or recruiting body, or its exam. */
    private static final String EXAM = "(upsc( cse)?|uppsc|bpsc|mpsc|tnpsc|rpsc|hpsc|appsc|tspsc|kpsc|opsc|wbpsc|state psc|ias|ssc|"
            + "civil services?|civil service exam\\w*)";
    /** What the exam news is about (not a policy or an appointment). */
    private static final String ABOUT = "(mains|prelims|preliminary|notification|calendar|exams?|examination|results?|interview|personality test|answers?|"
            + "answer key|quiz|questions?|question papers?|syllabus|cut-?offs?|cut-?off marks|toppers?|ranks?|rankers?|strategy|prepar\\w*|aspirants?|"
            + "admit cards?|registration|essentials|mock|attempts?|age limit|coaching|mentors?|booklists?|"
            + "optional subjects?|gs ?paper ?[1-4]|csat|success stor\\w*|cleared|cracked|crack)";

    private static final Pattern EXAM_ABOUT = Pattern.compile("(?i)\\b" + EXAM + "\\b.{0,50}\\b" + ABOUT + "\\b|\\b" + ABOUT + "\\b.{0,30}\\b" + EXAM + "\\b");

    /** Roundups, practice and quiz posts, link lists: never a news story whatever the exam word. */
    private static final Pattern FORMAT = Pattern.compile("(?i)answer[- ]?(writing|practice)|\\bquiz(zes)?\\b|\\bmcqs?\\b|practice (questions?|sets?|tests?)|"
            + "daily current affairs|current affairs (today|quiz|analysis|roundup|\\+)|\\+ ?pib summary|pib summary|editorial (analysis|summary|simplified)|"
            + "(daily )?news analysis|\\(week \\d+\\)|winners? list|\\blist here\\b|\\bcheck\\b.{0,60}\\bhere\\b|^insights?\\b.{0,20}(daily|current|ias)|"
            + "\\bupsc (key|essentials|issues at a glance|weekly)\\b|\\bsecure synopsis\\b|\\bmains (answer|practice|question)|\\bgs ?paper ?[1-4]\\b|"
            + "\\b(prelims|mains) 20\\d\\d\\b|\\b20\\d\\d (prelims|mains)\\b|\\bfor (upsc|ias|civil services)\\b(?! (chair|member))|"
            + ",? (latest|breaking) news$|^latest news\\b|\\bthis week in\\b.{0,30}(upsc|ias)");

    /** Explainers that carry real news ("Knowledge Nugget: Begonia siangensis …") are kept, renamed (see {@link #head}). */
    private static final Pattern EXPLAINER = Pattern.compile("(?i)^(upsc (essentials|key)\\s*[|:–—-]\\s*)?knowledge nugget\\b");
    private static final Pattern PRACTICE = Pattern.compile("(?i)answer|practice|quiz|mcq|question");

    /** True when a headline is about the exam or is a roundup/practice post, not a news story. */
    public static boolean title(String t) {
        if (t == null) return false;
        String s = t.trim();
        if (s.isEmpty()) return false;
        if (EXPLAINER.matcher(s).find() && !PRACTICE.matcher(s).find()) return false;
        return EXAM_ABOUT.matcher(s).find() || FORMAT.matcher(s).find();
    }

    /** Bullet lines that describe a post, list links or promote the exam ("INSIGHTS Daily Current Affairs article covers …"). */
    private static final Pattern LINE = Pattern.compile("(?i)daily current affairs|\\barticle covers\\b|\\b(post|page|edition) covers\\b|"
            + "notification (will be|is|to be) (released|out)|winners? list|\\bcheck\\b.{0,60}\\bhere\\b|upsconline|\\baspirants?\\b|"
            + "\\bfor (upsc|ias|civil services)\\b(?! (chair|member))|\\b(upsc|ias) (prelims|mains|exam\\w*|notification|calendar|cse|20\\d\\d)\\b|"
            + "\\b(prelims|mains) (20\\d\\d|exam|answer|practice|quiz|questions?)\\b|\\b(quiz|mcqs?|answer writing)\\b|^[\\s\\-–—•·*]*(→|->|»|›)");

    public static boolean line(String l) {
        return l != null && LINE.matcher(l).find();
    }

    private static final Pattern HEAD_PREFIX = Pattern.compile("(?i)^(upsc (essentials|key)\\s*[|:–—-]\\s*)?(knowledge nugget\\s*[:|–—-]\\s*)");
    private static final Pattern HEAD_SUFFIX = Pattern.compile("(?i)(\\s*[|:–—-]\\s*[^|:–—-]{0,60}\\b(upsc|ias|cse|civil services|prelims|mains)\\b[^|:–—-]{0,30}$|,?\\s+(latest|breaking) news$)");

    /** "Knowledge Nugget: X — what you should know for UPSC" → "X"; "X, Latest News" → "X". */
    public static String head(String h) {
        if (h == null) return null;
        String s = HEAD_PREFIX.matcher(h.trim()).replaceFirst("");
        s = HEAD_SUFFIX.matcher(s).replaceFirst("").trim();
        return s.length() >= 8 ? s : h.trim();
    }
}
