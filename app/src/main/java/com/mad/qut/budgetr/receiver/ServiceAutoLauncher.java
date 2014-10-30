package com.mad.qut.budgetr.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mad.qut.budgetr.service.ReminderService;

public class ServiceAutoLauncher extends BroadcastReceiver {

    private static final String TAG = ServiceAutoLauncher.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, ReminderService.class);
        context.startService(serviceIntent);
    }

}
