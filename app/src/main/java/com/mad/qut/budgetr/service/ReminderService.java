package com.mad.qut.budgetr.service;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.MainActivity;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.NumberUtils;
import com.mad.qut.budgetr.utils.SelectionBuilder;

import java.util.Calendar;

public class ReminderService extends Service implements Loader.OnLoadCompleteListener<Cursor> {

    private static final String TAG = ReminderService.class.getSimpleName();

    private static final int NOTIFICATION_REMINDER = 999;
    private static final String GROUP_KEY_TRANSACTIONS = "group_key_transactions";

    private AlarmManager mAlarmManager;
    private NotificationManager mNotificationManager;
    private CursorLoader mCursorLoader;

    private long mCurrentDate;

    @Override
    public void onCreate() {
        super.onCreate();
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }


    @Override
    public void onStart(Intent intent, int startId) {
        onStartCommand(intent, 0, startId);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent serviceIntent = new Intent(ReminderService.this,ReminderService.class);
        PendingIntent restartServiceIntent = PendingIntent.getService(ReminderService.this, 0, serviceIntent,0);
        // cancel previous alarm
        mAlarmManager.cancel(restartServiceIntent);

        Calendar calendar = Calendar.getInstance();
        // check for any reminders today
        checkForTodaysReminders();
        // next alarm tomorrow
        calendar.add(Calendar.DATE, 1);

        // schedule the new alarm
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), restartServiceIntent);

        return START_REDELIVER_INTENT;
    }

    private void checkForTodaysReminders() {
        // get all transactions between today and next week
        Calendar c = DateUtils.getClearCalendar();
        mCurrentDate = c.getTimeInMillis();
        c.add(Calendar.DATE, 7);
        long end = c.getTimeInMillis();
        SelectionBuilder builder = new SelectionBuilder();
        builder.where(FinanceContract.Transactions.IN_TIME_INTERVAL_SELECTION, FinanceContract.Transactions.buildInTimeIntervalArgs(mCurrentDate, end));
        // only select transactions, which have reminder set
        builder.where(FinanceContract.Transactions.TRANSACTION_REMINDER + "!=?", FinanceContract.Transactions.TRANSACTION_REMINDER_NEVER+"");
        mCursorLoader = new CursorLoader(this,
                FinanceContract.Transactions.CONTENT_URI,
                TransactionQuery.PROJECTION,
                builder.getSelection(),
                builder.getSelectionArgs(),
                FinanceContract.Transactions.DEFAULT_SORT);
        mCursorLoader.registerListener(105, this);
        mCursorLoader.startLoading();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mCursorLoader != null) {
            mCursorLoader.unregisterListener(this);
            mCursorLoader.cancelLoad();
            mCursorLoader.stopLoading();
        }
    }

    @Override
    public void onLoadComplete(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                // calculate reminder date
                long transactionDate = cursor.getLong(TransactionQuery.DATE);
                int transactionReminder = cursor.getInt(TransactionQuery.REMINDER);
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(transactionDate);
                c.add(Calendar.DATE, -transactionReminder);
                long reminderDate = c.getTimeInMillis();
                if (reminderDate == mCurrentDate) {
                    // notify
                    double amount = cursor.getLong(TransactionQuery.AMOUNT);
                    int id = cursor.getInt(TransactionQuery._ID);
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(this)
                                    .setSmallIcon(R.drawable.ic_notification)
                                    .setContentTitle(getResources().getString(R.string.notification_title_transaction))
                                    .setContentText(getResources().getQuantityString(R.plurals.notification_message_transaction, 1, NumberUtils.getFormattedCurrency(amount)))
                                    .setGroup(GROUP_KEY_TRANSACTIONS)
                                    .setAutoCancel(true);

                    Intent resultIntent = new Intent(this, MainActivity.class);
                    int fragment = MainActivity.TAB_TRANSACTIONS;
                    resultIntent.putExtra("fragment", fragment);
                    PendingIntent resultPendingIntent = PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);

                    mNotificationManager.notify(NOTIFICATION_REMINDER + id, mBuilder.build());
                }
            }
        }
    }

    private interface TransactionQuery {
        int _TOKEN = 0x01;

        String[] PROJECTION = {
                FinanceContract.Transactions._ID,
                FinanceContract.Transactions.TRANSACTION_ID,
                FinanceContract.Transactions.TRANSACTION_DATE,
                FinanceContract.Transactions.TRANSACTION_TYPE,
                FinanceContract.Transactions.TRANSACTION_AMOUNT,
                FinanceContract.Transactions.TRANSACTION_REMINDER,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Categories.CATEGORY_NAME
        };

        int _ID = 0;
        int TRANSACTION_ID = 1;
        int DATE = 2;
        int TYPE = 3;
        int AMOUNT = 4;
        int REMINDER = 5;
        int CATEGORY_ID = 6;
        int CATEGORY_NAME = 7;

    }

}
