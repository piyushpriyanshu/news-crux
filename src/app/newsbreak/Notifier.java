package app.newsbreak;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import app.newsbreak.core.Ist;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
final class Notifier {
    static final int ACCENT = -11625338;
    static final String CH_ALERTS = "alerts";
    static final String CH_EDITION = "editions";
    static final String CH_UPDATE = "updates";

    private Notifier() {
    }

    static void ensureChannels(Context context) {
        NotificationManager notificationManager;
        if (Build.VERSION.SDK_INT >= 26 && (notificationManager = (NotificationManager) context.getSystemService("notification")) != null) {
            NotificationChannel notificationChannel = new NotificationChannel(CH_EDITION, "Daily editions & amendments", 3);
            notificationChannel.setDescription("When the day's UPSC digest is ready, or an amendment is added");
            notificationManager.createNotificationChannel(notificationChannel);
            NotificationChannel notificationChannel2 = new NotificationChannel(CH_ALERTS, "High-yield news alerts", 4);
            notificationChannel2.setDescription("New stories that strongly match UPSC PYQ themes");
            notificationManager.createNotificationChannel(notificationChannel2);
            NotificationChannel notificationChannel3 = new NotificationChannel(CH_UPDATE, "App updates", 3);
            notificationChannel3.setDescription("When a new version of News Crux is out");
            notificationManager.createNotificationChannel(notificationChannel3);
        }
    }

    static Notification.Builder builder(Context context, String str) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, str) : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_bolt).setColor(ACCENT).setAutoCancel(true).setShowWhen(true);
        if (Build.VERSION.SDK_INT < 26) {
            builder.setPriority(CH_ALERTS.equals(str) ? 1 : 0).setDefaults(-1);
        }
        return builder;
    }

    static PendingIntent open(Context context, String str, int i) {
        Intent intent = new Intent(context, (Class<?>) MainActivity.class);
        intent.setFlags(872415232);
        intent.putExtra("open", str);
        return PendingIntent.getActivity(context, i, intent, 201326592);
    }

    static void post(Context context, int i, Notification notification) {
        NotificationManager notificationManager;
        try {
            if ((Build.VERSION.SDK_INT < 33 || context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == 0) && (notificationManager = (NotificationManager) context.getSystemService("notification")) != null) {
                notificationManager.notify(i, notification);
            }
        } catch (Exception e) {
        }
    }

    static void edition(Context context, JSONObject jSONObject) {
        JSONArray optJSONArray = jSONObject.optJSONArray("topics");
        int length = optJSONArray == null ? 0 : optJSONArray.length();
        Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
        for (int i = 0; optJSONArray != null && i < optJSONArray.length() && i < 6; i++) {
            inboxStyle.addLine("• " + optJSONArray.optJSONObject(i).optString("head"));
        }
        int i2 = 0;
        int i3 = 0;
        while (optJSONArray != null && i2 < optJSONArray.length()) {
            int i4 = optJSONArray.optJSONObject(i2).has("ai") ? i3 + 1 : i3;
            i2++;
            i3 = i4;
        }
        JSONArray optJSONArray2 = jSONObject.optJSONArray("catchup");
        inboxStyle.setSummaryText(length + " stories, all checked" + ((optJSONArray2 == null || optJSONArray2.length() <= 0) ? "" : ", +" + optJSONArray2.length() + " in catch-up") + (i3 > 0 ? ", " + i3 + " rewritten by AI" : ""));
        post(context, 1100, builder(context, CH_EDITION).setContentTitle("Edition ready: " + Ist.heading(jSONObject.optString("date"))).setContentText(length + " stories, coaching topics included. Tap to revise.").setStyle(inboxStyle).setContentIntent(open(context, "edition:" + jSONObject.optString("date"), 11)).build());
    }

    static void amendment(Context context, String str, String str2) {
        post(context, Math.abs(str2.hashCode() % 50) + 1200, builder(context, CH_EDITION).setContentTitle("Added to the " + Ist.shortDay(str) + " edition").setContentText(str2).setStyle(new Notification.BigTextStyle().bigText(str2)).setContentIntent(open(context, "edition:" + str, 12)).build());
    }

    static void alerts(Context context, List<JSONObject> list) {
        Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
        for (JSONObject jSONObject : list) {
            inboxStyle.addLine("[" + jSONObject.optString("s") + "] " + jSONObject.optString("t"));
        }
        JSONObject jSONObject2 = list.get(0);
        String str = list.size() == 1 ? "High-yield · " + jSONObject2.optString("gs") : list.size() + " high-yield updates for UPSC";
        inboxStyle.setBigContentTitle(str);
        Notification.Builder contentIntent = builder(context, CH_ALERTS).setContentTitle(str).setContentText(jSONObject2.optString("t")).setStyle(inboxStyle).setContentIntent(open(context, "live", 13));
        if (list.size() > 1) {
            contentIntent.setNumber(list.size());
        }
        post(context, 1300, contentIntent.build());
    }

    static void aiReady(Context context, String str) {
        post(context, 1400, builder(context, CH_EDITION).setContentTitle("Cards rewritten: " + Ist.shortDay(str)).setContentText("Every card was rewritten and fact-checked.").setContentIntent(open(context, "ai:" + str, 14)).build());
    }

    static void test(Context context) {
        ensureChannels(context);
        post(context, 1500, builder(context, CH_ALERTS).setContentTitle("News Crux notifications are on").setContentText("You'll be alerted for new editions and high-yield UPSC news.").setContentIntent(open(context, "live", 15)).build());
    }

    static void update(Context context, String version, String notes) {
        ensureChannels(context);
        String first = notes == null ? "" : notes.replaceAll("(?m)^[#*\\-\\s]+", "").trim();
        int nl = first.indexOf('\n');
        if (nl > 0) first = first.substring(0, nl);
        String text = first.length() > 0 ? first : "Tap to install. Your editions and settings stay.";
        post(context, 1600, builder(context, CH_UPDATE).setContentTitle("News Crux " + version + " is out").setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(open(context, "update", 16)).build());
    }
}
