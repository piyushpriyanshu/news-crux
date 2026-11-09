package app.newsbreak;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import java.util.concurrent.atomic.AtomicInteger;

/* loaded from: classes.dex */
public class WorkService extends Service {
    static final String CH_WORK = "work";
    static final int NOTIF_ID = 4242;
    static final AtomicInteger active = new AtomicInteger();

    static void start(Context context, String str, String str2, String str3) {
        Context applicationContext = context.getApplicationContext();
        Intent putExtra = new Intent(applicationContext, (Class<?>) WorkService.class).setAction(str).putExtra("date", str2).putExtra("reason", str3);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                applicationContext.startForegroundService(putExtra);
            } else {
                applicationContext.startService(putExtra);
            }
        } catch (Throwable th) {
            runInline(applicationContext, str, str2, str3);
        }
    }

    static void runInline(final Context context, final String str, final String str2, final String str3) {
        new Thread(new Runnable() { // from class: app.newsbreak.WorkService.1
            @Override // java.lang.Runnable
            public void run() {
                WorkService.work(context, str, str2, str3);
            }
        }, "nb-" + str).start();
    }

    static void work(Context context, String str, String str2, String str3) {
        if (!"ai".equals(str)) {
            if (!"aitest".equals(str)) {
                if (!"aicard".equals(str)) {
                    if (str3 == null) {
                        str3 = "manual";
                    }
                    String optString = Engine.sync(context, str3).optString("aiDate", "");
                    if (optString.length() > 0) {
                        Engine.runAi(context, optString, false);
                        return;
                    }
                    return;
                }
                Engine.runAiCard(context, str2, str3);
                return;
            }
            Engine.testAi(context);
            return;
        }
        Engine.runAi(context, str2, true);
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        Notification notification = notification(this);
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, notification, 1);
            } else {
                startForeground(NOTIF_ID, notification);
            }
        } catch (Throwable th) {
        }
        final String action = (intent == null || intent.getAction() == null) ? "sync" : intent.getAction();
        final String stringExtra = intent != null ? intent.getStringExtra("date") : null;
        final String stringExtra2 = intent != null ? intent.getStringExtra("reason") : null;
        active.incrementAndGet();
        new Thread(new Runnable() { // from class: app.newsbreak.WorkService.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    WorkService.work(WorkService.this.getApplicationContext(), action, stringExtra, stringExtra2);
                    if (WorkService.active.decrementAndGet() <= 0) {
                        try {
                            if (Build.VERSION.SDK_INT >= 24) {
                                WorkService.this.stopForeground(1);
                            } else {
                                WorkService.this.stopForeground(true);
                            }
                        } catch (Throwable th2) {
                        }
                        WorkService.this.stopSelf();
                    }
                } catch (Throwable th3) {
                    if (WorkService.active.decrementAndGet() <= 0) {
                        try {
                            if (Build.VERSION.SDK_INT >= 24) {
                                WorkService.this.stopForeground(1);
                            } else {
                                WorkService.this.stopForeground(true);
                            }
                        } catch (Throwable th4) {
                        }
                        WorkService.this.stopSelf();
                    }
                }
            }
        }, "nb-work-" + action).start();
        return 2;
    }

    static Notification notification(Context context) {
        NotificationManager notificationManager;
        if (Build.VERSION.SDK_INT >= 26 && (notificationManager = (NotificationManager) context.getSystemService("notification")) != null && notificationManager.getNotificationChannel(CH_WORK) == null) {
            NotificationChannel notificationChannel = new NotificationChannel(CH_WORK, "Updating (shown briefly)", 1);
            notificationChannel.setDescription("Shown only while News Crux is reading sources or writing the cards");
            notificationChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, CH_WORK) : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_bolt).setColor(-11625338).setContentTitle("Updating News Crux…").setOngoing(true).setShowWhen(false);
        if (Build.VERSION.SDK_INT < 26) {
            builder.setPriority(-2);
        }
        return builder.build();
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }
}
