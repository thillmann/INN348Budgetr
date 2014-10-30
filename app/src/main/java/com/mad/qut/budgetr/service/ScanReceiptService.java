package com.mad.qut.budgetr.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.mad.qut.budgetr.Config;
import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.MainActivity;
import com.mad.qut.budgetr.ui.ReceiptScannerActivity;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.PrefUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanReceiptService extends IntentService implements ImageProcessingAsyncTask.OnTaskCompletedListener,
        TextRecognitionAsyncTask.OnTaskCompletedListener {

    private static final String TAG = ScanReceiptService.class.getSimpleName();

    public static final String EXTRA_IMAGE = "imageFromCamera";
    //public static final String EXTRA_TEXT = "recognizedText";
    public static final String EXTRA_CATGEORY = "category";

    public static final int NOTIFICATION_PROGRESS = 100;

    private String mCategory;
    private String mImagePath;
    private NotificationManager mNotificationManager;
    private boolean mImageDeleted = false;

    public ScanReceiptService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mImagePath = workIntent.getStringExtra(EXTRA_IMAGE);
        mCategory = workIntent.getStringExtra(EXTRA_CATGEORY);

        // read image file to bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(mImagePath, bmOptions);
        try {
            // check orientation
            // (doesn't work on my htc one m7)
            ExifInterface exif = new ExifInterface(mImagePath);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Log.v(TAG, "Orient: " + exifOrientation);

            int rotate = 0;
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {
                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
                // tesseract requires ARGB_8888
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            }

            // Process image
            new ImageProcessingAsyncTask(bitmap, this).execute();

            // notify user
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(getResources().getString(R.string.notification_title_scan))
                            .setContentText(getResources().getString(R.string.notification_message_scan_progress))
                            .setProgress(0, 0, true);
            mNotificationManager.notify(NOTIFICATION_PROGRESS, mBuilder.build());
        } catch (IOException e) {
            Log.e(TAG, "Rotate or coversion failed: " + e.toString());
        }
    }

    @Override
    public void onProcessingFinished(Bitmap image) {
        // do text recognition after processing
        new TextRecognitionAsyncTask(this, image, this).execute();
    }

    @Override
    public void onProcessingFailed() {
        // delete image file
        if (!mImageDeleted) {
            File image = new File(mImagePath);
            mImageDeleted = image.delete();
        }

        // notify user
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(getResources().getString(R.string.notification_title_scan))
                        .setContentText(getResources().getString(R.string.notification_message_scan_failed))
                        .setAutoCancel(true);
        Intent resultIntent = new Intent(this, ReceiptScannerActivity.class);
        // notification opens receipt scanner
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(NOTIFICATION_PROGRESS, mBuilder.build());
    }

    @Override
    public void onRecognitionFinished(String text) {
        // delete image file
        if (!mImageDeleted) {
            File image = new File(mImagePath);
            mImageDeleted = image.delete();
        }

        text = text.trim();
        //Log.d(TAG, text);

        // extract value
        Pattern currencyPattern = Pattern.compile("[\\$€]\\d+\\.\\d{2}");
        Matcher currencyMatcher = currencyPattern.matcher(text);
        List<String> values = new ArrayList<String>();
        while (currencyMatcher.find()) {
            values.add(currencyMatcher.group());
        }
        Collections.sort(values);
        if (values.size() == 0) {
            onRecognitionFailed();
            return;
        }
        double parsedValue = 0;
        if (values.size() > 1) {
            for (String value : values) {
                int i = text.indexOf(value);
                String cleanString = value.replaceAll("[$€,]", "");
                double parsed = Double.parseDouble(cleanString);
                if (parsed > 0 && parsed > parsedValue) {
                    // assume biggest value
                    parsedValue = parsed;
                }
            }
        } else {
            // only one value then we're choosing it
            String cleanString = values.get(0).replaceAll("[$€,]", "");
            parsedValue = Double.parseDouble(cleanString);
        }
        if (parsedValue <= 0) {
            onRecognitionFailed();
            return;
        }

        // extract dates
        List<String[]> candidateDates = new ArrayList<String[]>();
        String lcText = text.toLowerCase();
        for (String regexp : DateUtils.DATE_FORMAT_REGEXPS.keySet()) {
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(lcText);
            while (matcher.find()) {
                String[] date = { matcher.group(), DateUtils.DATE_FORMAT_REGEXPS.get(regexp) };
                candidateDates.add(date);
            }
        }
        long date = 0;
        if (candidateDates.size() > 0) {
            // always select first found date
            String[] dateArray = candidateDates.get(0);
            date = DateUtils.getTimeStampFromString(dateArray[0], dateArray[1]);
        }
        if (date == 0) {
            // if no date found assume current date
            date = DateUtils.getCurrentTimeStamp() * 1000;
        }

        // Create transaction
        ContentValues transaction = new ContentValues();
        transaction.put(FinanceContract.Transactions.TRANSACTION_ID, UUID.randomUUID().toString());
        transaction.put(FinanceContract.Transactions.TRANSACTION_AMOUNT, parsedValue);
        transaction.put(FinanceContract.Transactions.TRANSACTION_DATE, date);
        transaction.put(FinanceContract.Transactions.TRANSACTION_REPEAT, FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER);
        transaction.put(FinanceContract.Transactions.TRANSACTION_REMINDER, FinanceContract.Transactions.TRANSACTION_REMINDER_NEVER);
        transaction.put(FinanceContract.Transactions.TRANSACTION_TYPE, FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE);
        transaction.put(FinanceContract.Transactions.CATEGORY_ID, mCategory);
        transaction.put(FinanceContract.Transactions.CURRENCY_ID, PrefUtils.getCurrency(this));
        getContentResolver().insert(FinanceContract.Transactions.CONTENT_URI, transaction);
        getContentResolver().notifyChange(FinanceContract.Budgets.CONTENT_URI, null);

        // notify user
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(getResources().getString(R.string.notification_title_scan))
                        .setContentText(getResources().getString(R.string.notification_message_scan_completed))
                        .setAutoCancel(true);
        // notification opens the transaction list
        Intent resultIntent = new Intent(this, MainActivity.class);
        int fragment = MainActivity.TAB_TRANSACTIONS;
        resultIntent.putExtra("fragment", fragment);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(NOTIFICATION_PROGRESS, mBuilder.build());
    }

    @Override
    public void onRecognitionFailed() {
        // delete image file
        if (!mImageDeleted) {
            File image = new File(mImagePath);
            mImageDeleted = image.delete();
        }

        // notify user
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(getResources().getString(R.string.notification_title_scan))
                        .setContentText(getResources().getString(R.string.notification_message_scan_failed))
                        .setAutoCancel(true);
        // notification opens receipt scanner
        Intent resultIntent = new Intent(this, ReceiptScannerActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(NOTIFICATION_PROGRESS, mBuilder.build());
    }

}
