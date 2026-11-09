package app.newsbreak;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import app.newsbreak.core.Ist;

/* loaded from: classes.dex */
public class DeliverReceiver extends BroadcastReceiver {
    static final String ACTION = "app.newsbreak.DELIVER";
    static final String RELEASE = "app.newsbreak.RELEASE";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        final Context applicationContext = context.getApplicationContext();
        if (intent != null && RELEASE.equals(intent.getAction())) {
            final BroadcastReceiver.PendingResult goAsync = goAsync();
            new Thread(new Runnable() { // from class: app.newsbreak.DeliverReceiver.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        Engine.releaseAll(applicationContext);
                        goAsync.finish();
                    } catch (Throwable th) {
                        goAsync.finish();
                        throw th;
                    }
                }
            }, "nb-release").start();
        } else {
            arm(applicationContext);
            final BroadcastReceiver.PendingResult goAsync2 = goAsync();
            new Thread(new Runnable() { // from class: app.newsbreak.DeliverReceiver.2
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        Engine.deliver(applicationContext);
                        String str = Engine.aiAfterDeliver;
                        if (str != null) {
                            Engine.aiAfterDeliver = null;
                            SyncJobService.runAi(applicationContext, str);
                        }
                        SyncJobService.runOnce(applicationContext);
                        goAsync2.finish();
                    } catch (Throwable th) {
                        String str2 = Engine.aiAfterDeliver;
                        if (str2 != null) {
                            Engine.aiAfterDeliver = null;
                            SyncJobService.runAi(applicationContext, str2);
                        }
                        SyncJobService.runOnce(applicationContext);
                        goAsync2.finish();
                    }
                }
            }, "nb-deliver").start();
        }
    }

    static void armRelease(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
            if (alarmManager != null) {
                PendingIntent broadcast = PendingIntent.getBroadcast(context, 8, new Intent(context, (Class<?>) DeliverReceiver.class).setAction(RELEASE), (Build.VERSION.SDK_INT >= 23 ? 67108864 : 0) | 134217728);
                long currentTimeMillis = System.currentTimeMillis() + 2460000;
                if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setAndAllowWhileIdle(0, currentTimeMillis, broadcast);
                } else {
                    alarmManager.set(0, currentTimeMillis, broadcast);
                }
            }
        } catch (Throwable th) {
        }
    }

    static long next(long j, int i) {
        String dateKey = Ist.dateKey(j);
        long startOf = Ist.startOf(dateKey) + (i * 60000);
        return startOf <= j + 60000 ? Ist.startOf(Ist.addDays(dateKey, 1)) + (i * 60000) : startOf;
    }

    static void arm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
            if (alarmManager != null) {
                PendingIntent broadcast = PendingIntent.getBroadcast(context, 7, new Intent(context, (Class<?>) DeliverReceiver.class).setAction(ACTION), (Build.VERSION.SDK_INT >= 23 ? 67108864 : 0) | 134217728);
                long next = next(System.currentTimeMillis(), Store.prefs(context).getInt("deliverAt", 360));
                alarmManager.cancel(broadcast);
                if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setAndAllowWhileIdle(0, next, broadcast);
                } else {
                    alarmManager.set(0, next, broadcast);
                }
            }
        } catch (Throwable th) {
        }
    }
}
