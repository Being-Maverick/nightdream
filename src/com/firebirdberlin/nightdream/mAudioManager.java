package com.firebirdberlin.nightdream;

import android.content.Context;
import android.media.AudioManager;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

public class mAudioManager{
    final static String TAG = "mAudioManager";
    private Context mContext;
    private AudioManager audiomanage;
    private int currentRingerMode;
    private EventBus bus = EventBus.getDefault();

    private class OnSetRingerModeSilent {
         private int currentRingerMode;

         public OnSetRingerModeSilent(int currentRingerMode) {
             this.currentRingerMode = currentRingerMode;
         }
    }

    public mAudioManager(Context context){
        this.mContext = context;
        audiomanage = null;
        audiomanage = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (audiomanage != null) {
            currentRingerMode = audiomanage.getRingerMode();
        }
    }

    public void setRingerModeSilent(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if ( notificationManager == null ||
                    !notificationManager.isNotificationPolicyAccessGranted() ) {
                return;
            }
        }

        currentRingerMode = audiomanage.getRingerMode(); // ringer mode to restore
        Log.i(TAG, String.format(" currentRingerMode = %d", currentRingerMode));
        Log.i(TAG, "setRingerModeSilent()");
        audiomanage.setRingerMode(AudioManager.RINGER_MODE_SILENT);

        bus.postSticky(new OnSetRingerModeSilent(currentRingerMode));
    }

    public void restoreRingerMode(){
        Log.i(TAG, "restoreRingerMode()");
        OnSetRingerModeSilent event = bus.removeStickyEvent(OnSetRingerModeSilent.class);
        // nothing to do
        if (event == null) return;

        currentRingerMode = event.currentRingerMode;
        Log.i(TAG, String.format(" > currentRingerMode = %d", currentRingerMode));

        // initial ringer mode was silent, don't have to do anything
        if (currentRingerMode == AudioManager.RINGER_MODE_SILENT) return;

        // The expected ringer mode is silent. Is it still valid ?
        // If not, another app may have changed it. R-E-S-P-E-C-T this setting.
        if (audiomanage.getRingerMode() != AudioManager.RINGER_MODE_SILENT) return;

        // otherwise we will reset the ringer mode
        audiomanage.setRingerMode(currentRingerMode);
    }

    void activateDnDMode(boolean enabled, boolean allowPriority) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (
                notificationManager != null
                        && notificationManager.isNotificationPolicyAccessGranted()
        ) {
            int dndmode = allowPriority ? NotificationManager.INTERRUPTION_FILTER_PRIORITY
                                        : NotificationManager.INTERRUPTION_FILTER_ALARMS;
            notificationManager.setInterruptionFilter(
                    enabled ? dndmode : NotificationManager.INTERRUPTION_FILTER_ALL
            );
        }
    }
}
