package ru.merlinkoss.remotec;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Admin on 28.04.2016.
 */
public class AutoRun extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

            // получили boot_completed - запустили FoneService
            context.startService(new Intent(context, FoneService.class));
            Log.i("chat", "+ AutoRun - отработал");
        }
    }
}
