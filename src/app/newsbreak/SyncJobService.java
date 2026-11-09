package app.newsbreak;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class SyncJobService extends JobService {
    static final int AI_ID = 3003;
    static final int JOB_ID = 3001;
    static final int ONCE_ID = 3002;
    static final int UPDATE_ID = 3004;

    @Override // android.app.job.JobService
    public boolean onStartJob(final JobParameters jobParameters) {
        new Thread(new Runnable() { // from class: app.newsbreak.SyncJobService.1
            @Override // java.lang.Runnable
            public void run() {
                boolean z = false;
                try {
                    Context applicationContext = SyncJobService.this.getApplicationContext();
                    if (jobParameters.getJobId() == SyncJobService.UPDATE_ID) {
                        Updater.check(applicationContext, true);
                    } else if (jobParameters.getJobId() == SyncJobService.AI_ID) {
                        String string = jobParameters.getExtras() != null ? jobParameters.getExtras().getString("date") : null;
                        if (string != null) {
                            Engine.runAi(applicationContext, string, false);
                        }
                    } else {
                        JSONObject sync = Engine.sync(applicationContext, jobParameters.getJobId() == SyncJobService.ONCE_ID ? "morning" : "background");
                        if (sync.optBoolean("busy") && jobParameters.getJobId() == SyncJobService.ONCE_ID) {
                            z = true;
                        }
                        String optString = sync.optString("aiDate", "");
                        if (optString.length() > 0) {
                            SyncJobService.runAi(applicationContext, optString);
                        }
                    }
                } catch (Throwable th) {
                } finally {
                    SyncJobService.this.jobFinished(jobParameters, false);
                }
            }
        }, "nb-job").start();
        return true;
    }

    static void runAi(Context context, String str) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
            if (jobScheduler != null && !Engine.aiRunning.get() && jobScheduler.getPendingJob(AI_ID) == null) {
                PersistableBundle persistableBundle = new PersistableBundle();
                persistableBundle.putString("date", str);
                jobScheduler.schedule(new JobInfo.Builder(AI_ID, new ComponentName(context, (Class<?>) SyncJobService.class)).setRequiredNetworkType(1).setExtras(persistableBundle).build());
            }
        } catch (Exception e) {
        }
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    static void runOnce(Context context) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
            if (jobScheduler != null) {
                jobScheduler.schedule(new JobInfo.Builder(ONCE_ID, new ComponentName(context, (Class<?>) SyncJobService.class)).setRequiredNetworkType(1).build());
            }
        } catch (Exception e) {
        }
    }

    static void schedule(Context context, boolean z) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
            if (jobScheduler != null) {
                long j = Store.prefs(context).getInt("intervalHours", 3) * 3600 * 1000;
                JobInfo pendingJob = jobScheduler.getPendingJob(JOB_ID);
                if (z || pendingJob == null || pendingJob.getIntervalMillis() != j) {
                    jobScheduler.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(context, (Class<?>) SyncJobService.class)).setRequiredNetworkType(1).setPersisted(true).setPeriodic(j, 1200000L).build());
                }
                JobInfo up = jobScheduler.getPendingJob(UPDATE_ID);
                if (up == null || up.getIntervalMillis() != Updater.BG_GAP) {
                    jobScheduler.schedule(new JobInfo.Builder(UPDATE_ID, new ComponentName(context, (Class<?>) SyncJobService.class)).setRequiredNetworkType(1).setPersisted(true).setPeriodic(Updater.BG_GAP, 3600000L).build());
                }
            }
        } catch (Exception e) {
        }
    }
}
