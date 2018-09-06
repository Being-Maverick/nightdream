package com.firebirdberlin.nightdream.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AlarmNotificationService extends JobService {
    private static String TAG = "AlarmNotificationService";

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void scheduleJob(Context context, SimpleTime nextAlarmTime) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        ComponentName serviceComponent = new ComponentName(
                context.getPackageName(), AlarmNotificationService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(Config.JOB_ID_ALARM_NOTIFICATION, serviceComponent);
        builder.setPersisted(true);
        builder.setRequiresCharging(false);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        long nowInMillis = System.currentTimeMillis();
        long nextAlarmTimeMillis = nextAlarmTime.getMillis();

        long minutes_to_start = 10;
        long minLatency = 1000;
        if (nextAlarmTimeMillis - nowInMillis > minutes_to_start * 60000) {
            minLatency = (nextAlarmTimeMillis - minutes_to_start * 60000) - nowInMillis;
        }
        builder.setMinimumLatency(minLatency);
        builder.setOverrideDeadline(minLatency + 1000);


        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }

        int jobResult = jobScheduler.schedule(builder.build());

        if (jobResult == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "scheduled AlarmNotificationService job successfully");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void cancelJob(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }
        jobScheduler.cancel(Config.JOB_ID_ALARM_NOTIFICATION);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");

        DataSource db = new DataSource(this);
        db.open();
        SimpleTime next = db.getNextAlarmToSchedule();
        buildNotification(this, next);
        db.close();

        return false;
    }

    @Override

    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private Notification buildNotification(Context context, SimpleTime nextAlarmTime) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return null;
        }
        Settings settings = new Settings(context);
        String timeFormatted = Utility.formatTime(
                settings.getFullTimeFormat(), nextAlarmTime.getCalendar());
        String text = String.format(
                "%s %s", context.getString(R.string.pendingAlarm), timeFormatted);

        NotificationCompat.Builder note =
                Utility.buildNotification(context, Config.NOTIFICATION_CHANNEL_ID_ALARMS)
                        .setContentText(text)
                        .setSmallIcon(R.drawable.ic_audio)
                        .setWhen(nextAlarmTime.getMillis())
                        .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender().setHintHideIcon(true);

        // TODO Replace by a proper intent
        String textActionSkip = context.getString(R.string.action_skip);
        Intent skipIntent = AlarmHandlerService.getStopIntent(context);
        PendingIntent pSkipIntent = PendingIntent.getService(
                context, 0, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action skipAction =
                new NotificationCompat.Action.Builder(0, textActionSkip, pSkipIntent).build();

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(text);
        note.setStyle(bigStyle);
        note.addAction(skipAction);
        wearableExtender.addAction(skipAction);

        note.extend(wearableExtender);

        Notification notification = note.build();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Config.NOTIFICATION_ID_DISMISS_ALARMS, notification);

        return notification;
    }

}
