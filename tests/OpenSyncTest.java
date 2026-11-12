import app.newsbreak.core.OpenSync;

public class OpenSyncTest {
    static int pass = 0, fail = 0;
    static void ok(boolean c, String m) { if (c) pass++; else { fail++; System.out.println("FAIL: " + m); } }
    static long ist(int y, int mo, int d, int h, int mi) {
        return java.time.LocalDateTime.of(y, mo, d, h, mi).toInstant(java.time.ZoneOffset.ofHoursMinutes(5, 30)).toEpochMilli();
    }
    public static void main(String[] a) {
        long noon = ist(2026, 11, 12, 12, 0), min = 60000L;
        ok(OpenSync.due(noon, 0, 0, 360), "never synced");
        ok(!OpenSync.due(noon, noon - 5 * min, 40, 360), "5 min old: no sync");
        ok(!OpenSync.due(noon, noon - 30 * min, 40, 360), "30 min old: no sync");
        ok(!OpenSync.due(noon, noon - 59 * min, 40, 360), "59 min old: no sync");
        ok(OpenSync.due(noon, noon - 61 * min, 40, 360), "61 min old: sync");
        ok(!OpenSync.due(noon, noon - 5 * min, 0, 360), "failed sync 5 min ago: wait");
        ok(OpenSync.due(noon, noon - 11 * min, 0, 360), "failed sync 11 min ago: retry");
        ok(OpenSync.due(noon, noon + 5 * min, 40, 360), "clock moved back: sync");
        long six10 = ist(2026, 11, 12, 6, 10);
        ok(OpenSync.due(six10, ist(2026, 11, 12, 5, 40), 40, 360), "first open after 6:00 edition, last sync 5:40: sync");
        ok(!OpenSync.due(six10, ist(2026, 11, 12, 6, 2), 40, 360), "synced at 6:02: no sync at 6:10");
        ok(!OpenSync.due(ist(2026, 11, 12, 5, 50), ist(2026, 11, 12, 5, 20), 40, 360), "before edition time, 30 min old: no sync");
        ok(!OpenSync.due(ist(2026, 11, 12, 0, 20), ist(2026, 11, 11, 23, 50), 40, 360), "just after midnight, 30 min old: no sync");
        ok(OpenSync.due(ist(2026, 11, 12, 8, 1), ist(2026, 11, 12, 7, 30), 40, 480), "8:00 edition setting");
        System.out.println("OpenSyncTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
