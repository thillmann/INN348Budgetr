package com.mad.qut.budgetr.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mad.qut.budgetr.Config;
import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.ReceiptScannerActivity;
import com.mad.qut.budgetr.utils.DateUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanReceiptService extends IntentService implements ImageProcessingAsyncTask.OnTaskCompletedListener,
        TextRecognitionAsyncTask.OnTaskCompletedListener {

    private static final String TAG = ScanReceiptService.class.getSimpleName();

    public static final String BROADCAST_ACTION = "com.mad.qut.budgetr.scan";
    public static final String EXTRA_IMAGE = "imageFromCamera";
    public static final String EXTRA_TEXT = "recognizedText";
    public static final String EXTRA_CATGEORY = "category";

    private String mCategory;
    private String mImagePath;

    public ScanReceiptService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d(TAG, "service started");

        mImagePath = workIntent.getStringExtra(EXTRA_IMAGE);
        mCategory = workIntent.getStringExtra(EXTRA_CATGEORY);

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(mImagePath, bmOptions);
        try {
            ExifInterface exif = new ExifInterface(mImagePath);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            String test = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            Log.v(TAG, "Orient 2: " + test);

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
                // tesseract req. ARGB_8888
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            }
            new ImageProcessingAsyncTask(bitmap, this).execute();
        } catch (IOException e) {
            Log.e(TAG, "Rotate or coversion failed: " + e.toString());
        }
    }

    @Override
    public void onProcessingFinished(Bitmap image) {
        new TextRecognitionAsyncTask(this, image, this).execute();
    }

    @Override
    public void onProcessingFailed() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Scanning failed")
                        .setContentText("Unfortunately the scanning process failed. Please try it again.")
                        .setAutoCancel(true);
        Intent resultIntent = new Intent(this, ReceiptScannerActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        int notificationId = 500;
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, mBuilder.build());
    }

    @Override
    public void onRecognitionFinished(String text) {
        text = text.trim();

        // extract value
        Pattern currencyPattern = Pattern.compile("[\\$€]\\d+\\.\\d{2}");
        Matcher currencyMatcher = currencyPattern.matcher(text);
        List<String> values = new ArrayList<String>();
        while (currencyMatcher.find()) {
            values.add(currencyMatcher.group());
        }
        Collections.sort(values);
        Log.d(TAG, values.toString());
        if (values.size() == 0) {
            onProcessingFailed();
            return;
        }
        double parsedValue;
        if (values.size() > 1) {
            for (String value : values) {
                int i = text.indexOf(value);
            }
            parsedValue = 0;
        } else {
            parsedValue = Double.parseDouble(values.get(0).replace("[$€,]", ""));
        }

        // extract dates
        Pattern datePattern = Pattern.compile("\\d{1,2}\\W\\d{1,2}\\W\\d{2,4}");
        Matcher dateMatcher = datePattern.matcher(text);
        List<String> dates = new ArrayList<String>();
        while (dateMatcher.find()) {
            dates.add(dateMatcher.group());
        }
        Log.d(TAG, dates.toString());
        long date;
        if (dates.size() > 0) {
            // always select first found date
            date = DateUtils.getTimeStampFromString(dates.get(0), "");
        } else {
            // if no date found assume current date
            date = DateUtils.getCurrentTimeStamp();
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
        transaction.put(FinanceContract.Transactions.CURRENCY_ID, Config.CURRENCY_ID);
        getContentResolver().insert(FinanceContract.Transactions.CONTENT_URI, transaction);
        getContentResolver().notifyChange(FinanceContract.Budgets.CONTENT_URI, null);
    }

    @Override
    public void onRecognitionFailed() {
    }

}
