package app.newsbreak.core;

/** Should opening the app start a sync? Not when the news on the phone is already fresh. */
public final class OpenSync {
    private OpenSync() {}
    public static final long FRESH = 60 * 60000L;  // a good sync less than an hour old is fresh
    public static final long RETRY = 10 * 60000L;  // after a sync that reached no source, try again sooner
    static final long IST = 19800000L, DAY = 86400000L;

    /** now/last in epoch ms; feedsOk from the last sync; deliverAt = morning edition time, minutes after IST midnight. */
    public static boolean due(long now, long last, int feedsOk, int deliverAt) {
        if (last <= 0 || last > now) return true;
        long age = now - last;
        if (feedsOk <= 0) return age > RETRY;
        // First open after today's morning-edition time, when the last sync came before it: refresh once.
        long deliver = (now + IST) / DAY * DAY - IST + deliverAt * 60000L;
        if (now >= deliver && last < deliver) return true;
        return age > FRESH;
    }
}
