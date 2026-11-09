package app.newsbreak.core;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class Ist {
    public static final long DAY = 86400000;
    public static final TimeZone TZ = TimeZone.getTimeZone("Asia/Kolkata");
    private static final String[] PATTERNS = {"EEE, dd MMM yyyy HH:mm:ss Z", "EEE, d MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, d MMM yyyy HH:mm:ss zzz", "EEE, dd MMM yyyy HH:mm Z", "dd MMM yyyy HH:mm:ss Z", "d MMM yyyy HH:mm:ss Z", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "EEE, dd MMM yyyy HH:mm:ss", "EEE, d MMM yyyy HH:mm:ss", "dd MMM yyyy HH:mm:ss", "MMM dd, yyyy HH:mm", "dd-MM-yyyy HH:mm:ss", "dd MMM yyyy", "yyyy-MM-dd"};
    private static final Pattern IST_SUFFIX = Pattern.compile("\\s(IST|\\+05:?30)$");
    private static final Pattern Z_FIX = Pattern.compile("Z$");

    private Ist() {
    }

    public static String dateKey(long j) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        simpleDateFormat.setTimeZone(TZ);
        return simpleDateFormat.format(new Date(j));
    }

    public static long startOf(String str) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        simpleDateFormat.setTimeZone(TZ);
        try {
            return simpleDateFormat.parse(str).getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    public static String addDays(String str, int i) {
        return dateKey(startOf(str) + (i * DAY) + 3600000);
    }

    public static String heading(String str) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMMM yyyy (EEEE)", Locale.ENGLISH);
        simpleDateFormat.setTimeZone(TZ);
        return simpleDateFormat.format(new Date(startOf(str) + 3600000));
    }

    public static String shortDay(String str) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d-MMM", Locale.ENGLISH);
        simpleDateFormat.setTimeZone(TZ);
        return simpleDateFormat.format(new Date(startOf(str) + 3600000));
    }

    public static String tag(String str) {
        return str.substring(8, 10) + "-" + str.substring(5, 7) + "-" + str.substring(0, 4);
    }

    public static int hourNow(long j) {
        Calendar calendar = Calendar.getInstance(TZ, Locale.US);
        calendar.setTimeInMillis(j);
        return calendar.get(11);
    }

    public static int minuteOfDay(long j) {
        Calendar calendar = Calendar.getInstance(TZ, Locale.US);
        calendar.setTimeInMillis(j);
        return calendar.get(12) + (calendar.get(11) * 60);
    }

    public static long parse(String str) {
        if (str == null) {
            return 0L;
        }
        String replaceAll = str.trim().replaceAll("\\s+", " ");
        if (replaceAll.length() == 0) {
            return 0L;
        }
        Matcher matcher = IST_SUFFIX.matcher(replaceAll);
        if (matcher.find()) {
            replaceAll = replaceAll.substring(0, matcher.start()) + " +0530";
        }
        if (replaceAll.matches(".*\\d\\dZ$")) {
            replaceAll = Z_FIX.matcher(replaceAll).replaceFirst("+00:00");
        }
        String replace = replaceAll.replace(" GMT", " +0000").replace(" UTC", " +0000").replace(" UT", " +0000").replace(" EDT", " -0400").replace(" EST", " -0500");
        for (String str2 : PATTERNS) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(str2, Locale.ENGLISH);
            simpleDateFormat.setTimeZone(TZ);
            simpleDateFormat.setLenient(true);
            ParsePosition parsePosition = new ParsePosition(0);
            Date parse = simpleDateFormat.parse(replace, parsePosition);
            if (parse != null && parsePosition.getIndex() >= Math.min(replace.length(), str2.length() - 4)) {
                long time = parse.getTime();
                if (time > 946684800000L && time < System.currentTimeMillis() + 172800000) {
                    return time;
                }
            }
        }
        return 0L;
    }
}
