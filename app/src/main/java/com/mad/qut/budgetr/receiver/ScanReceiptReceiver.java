package com.mad.qut.budgetr.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mad.qut.budgetr.service.ScanReceiptService;

public class ScanReceiptReceiver extends BroadcastReceiver {

    private static final String TAG = ScanReceiptReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String text = intent.getStringExtra(ScanReceiptService.EXTRA_TEXT);
        Log.d(TAG, text);
    }

}
