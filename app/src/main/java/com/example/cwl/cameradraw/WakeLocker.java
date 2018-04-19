package com.example.cwl.cameradraw;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by cwl on 18/3/29.
 */

public class WakeLocker {

    private static PowerManager.WakeLock sWakeLocker;

    public static void aquire(Context context){
        if(context == null)
            return ;
        if(sWakeLocker == null){
            PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            int level = PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
            int flag = PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP;
            sWakeLocker = powerManager.newWakeLock(level | flag, context.getApplicationContext().getPackageName());
        }
        if(sWakeLocker != null && !sWakeLocker.isHeld()){
            sWakeLocker.acquire();
        }
    }

    public static void release(){
        if(sWakeLocker != null && sWakeLocker.isHeld()){
            sWakeLocker.release();
        }
    }

    public static boolean isScreenOn(Context context){
        if(context != null){
            PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            return powerManager.isScreenOn();
        }
        return false;
    }
}
