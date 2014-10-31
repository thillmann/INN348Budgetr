package com.mad.qut.budgetr.service;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Transaction;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.MainActivity;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.NumberUtils;
import com.mad.qut.budgetr.utils.SelectionBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

public class ReminderService extends Service implements Loader.OnLoadCompleteListener<Cursor> {

    private static final String TAG = ReminderService.class.getSimpleName();

    private static final int NOTIFICATION_REMINDER = 999;
    private static final String GROUP_KEY_TRANSACTIONS = "group_key_transactions";

    private AlarmManager mAlarmManager;
    private NotificationManager mNotificationManager;
    private CursorLoader mReminderLoader;
    private CursorLoader mRepeaterLoader;

    private long mCurrentDate;

    @Override
    public void onCreate() {
        super.onCreate();
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "start service");
        Intent serviceIntent = new Intent(ReminderService.this, ReminderService.class);
        PendingIntent restartServiceIntent = PendingIntent.getService(ReminderService.this, 0, serviceIntent, 0);
        // cancel previous alarm
        mAlarmManager.cancel(restartServiceIntent);

        Calendar calendar = Calendar.getInstance();
        // check for repeating transactions today
        checkRepeatingTransactions();
        // check for any reminders today
        checkForTodaysReminders();
        // next alarm tomorrow
        calendar.add(Calendar.DATE, 1);

        // schedule the new alarm
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), restartServiceIntent);

        return START_REDELIVER_INTENT;
    }

    private void checkRepeatingTransactions() {
        Log.d(TAG, "check repeating");
        Calendar c = DateUtils.getClearCalendar();
        mCurrentDate = c.getTimeInMillis();
        // get all transactions that have no next transaction set, but are repeating
        SelectionBuilder builder = new SelectionBuilder();
        builder.where(FinanceContract.Transactions.TRANSACTION_REPEAT + "!=?", FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER+"");
        builder.where(FinanceContract.Transactions.TRANSACTION_DATE + "<=?", mCurrentDate+"");
        builder.isNull(FinanceContract.Transactions.TRANSACTION_NEXT);
        mRepeaterLoader = new CursorLoader(this,
                FinanceContract.Transactions.CONTENT_URI,
                TransactionQuery.PROJECTION,
                builder.getSelection(),
                builder.getSelectionArgs(),
                FinanceContract.Transactions.DEFAULT_SORT);
        mRepeaterLoader.registerListener(TransactionQuery.REPEAT_QUERY, this);
        mRepeaterLoader.startLoading();
    }

    private void checkForTodaysReminders() {
        Log.d(TAG, "check today reminder");
        // get all transactions between today and next week
        Calendar c = DateUtils.getClearCalendar();
        mCurrentDate = c.getTimeInMillis();
        c.add(Calendar.DATE, 7);
        long end = c.getTimeInMillis();
        SelectionBuilder builder = new SelectionBuilder();
        builder.where(FinanceContract.Transactions.IN_TIME_INTERVAL_SELECTION, FinanceContract.Transactions.buildInTimeIntervalArgs(mCurrentDate, end));
        // only select transactions, which have reminder set
        builder.where(FinanceContract.Transactions.TRANSACTION_REMINDER + "!=?", FinanceContract.Transactions.TRANSACTION_REMINDER_NEVER+"");
        mReminderLoader = new CursorLoader(this,
                FinanceContract.Transactions.CONTENT_URI,
                TransactionQuery.PROJECTION,
                builder.getSelection(),
                builder.getSelectionArgs(),
                FinanceContract.Transactions.DEFAULT_SORT);
        mReminderLoader.registerListener(TransactionQuery.REMINDER_QUERY, this);
        mReminderLoader.startLoading();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mRepeaterLoader != null) {
            mRepeaterLoader.unregisterListener(this);
            mRepeaterLoader.cancelLoad();
            mRepeaterLoader.stopLoading();
        }
        if (mReminderLoader != null) {
            mReminderLoader.unregisterListener(this);
            mReminderLoader.cancelLoad();
            mReminderLoader.stopLoading();
        }
    }

    @Override
    public void onLoadComplete(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG, "load complete");
        switch (cursorLoader.getId()) {
            case TransactionQuery.REPEAT_QUERY:
                createRepeat(cursor);
                break;
            case TransactionQuery.REMINDER_QUERY:
                createReminder(cursor);
                break;
        }
        cursorLoader.stopLoading();

    }

    private void createRepeat(Cursor cursor) {
        Log.d(TAG, "create transactions");
        if (cursor.getCount() > 0) {
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            Uri uri = FinanceContract.Transactions.CONTENT_URI;
            while (cursor.moveToNext()) {
                // determine next transaction
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
                Transaction transaction = new Transaction();
                transaction.id = UUID.randomUUID().toString();
                transaction.date = cursor.getLong(TransactionQuery.DATE);
                transaction.amount = cursor.getDouble(TransactionQuery.AMOUNT);
                transaction.repeat = cursor.getInt(TransactionQuery.REPEAT);
                transaction.reminder = cursor.getInt(TransactionQuery.REMINDER);
                transaction.type = cursor.getString(TransactionQuery.TYPE);
                String root = cursor.getString(TransactionQuery.ROOT);
                if (root == null || root.equals("")) {
                    transaction.root = cursor.getString(TransactionQuery.TRANSACTION_ID);
                } else {
                    transaction.root = root;
                }
                transaction.next = null;
                transaction.category = cursor.getString(TransactionQuery.CATEGORY_ID);
                transaction.nextDate();
                builder.withValues(transaction.create());
                operations.add(builder.build());
                // update previous transaction
                Uri updateUri = FinanceContract.Transactions.buildTransactionUri(cursor.getInt(TransactionQuery._ID)+"");
                ContentProviderOperation.Builder updateBuilder = ContentProviderOperation.newUpdate(updateUri);
                updateBuilder.withValue(FinanceContract.Transactions.TRANSACTION_NEXT, transaction.id);
                operations.add(updateBuilder.build());
            }
            try {
                if (operations.size() > 0) {
                    getContentResolver().applyBatch(FinanceContract.CONTENT_AUTHORITY, operations);
                }
            } catch (RemoteException re) {
                Log.e(TAG, re.getMessage());
            } catch (OperationApplicationException oae) {
                Log.e(TAG, oae.getMessage());
            }
        }
    }

    private void createReminder(Cursor cursor) {
        Log.d(TAG, "create notifications");
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
        int REMINDER_QUERY = 0x01;
        int REPEAT_QUERY = 0x02;

        String[] PROJECTION = {
                FinanceContract.Transactions._ID,
                FinanceContract.Transactions.TRANSACTION_ID,
                FinanceContract.Transactions.TRANSACTION_DATE,
                FinanceContract.Transactions.TRANSACTION_TYPE,
                FinanceContract.Transactions.TRANSACTION_AMOUNT,
                FinanceContract.Transactions.TRANSACTION_REPEAT,
                FinanceContract.Transactions.TRANSACTION_REMINDER,
                FinanceContract.Transactions.TRANSACTION_ROOT,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Categories.CATEGORY_NAME
        };

        int _ID = 0;
        int TRANSACTION_ID = 1;
        int DATE = 2;
        int TYPE = 3;
        int AMOUNT = 4;
        int REPEAT = 5;
        int REMINDER = 6;
        int ROOT = 7;
        int CATEGORY_ID = 8;
        int CATEGORY_NAME = 9;

    }

}
