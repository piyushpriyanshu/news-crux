package app.newsbreak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* loaded from: classes.dex */
public class BootReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        SyncJobService.schedule(context.getApplicationContext(), true);
        DeliverReceiver.arm(context.getApplicationContext());
    }
}
