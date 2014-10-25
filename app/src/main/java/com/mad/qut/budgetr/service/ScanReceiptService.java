package com.mad.qut.budgetr.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.ui.MainActivity;
import com.mad.qut.budgetr.ui.ReceiptScannerActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanReceiptService extends IntentService implements ImageProcessingAsyncTask.OnTaskCompletedListener,
        TextRecognitionAsyncTask.OnTaskCompletedListener {

    private static final String TAG = ScanReceiptService.class.getSimpleName();

    public static final String BROADCAST_ACTION = "com.mad.qut.budgetr.scan";
    public static final String EXTRA_IMAGE = "imageFromCamera";
    public static final String EXTRA_TEXT = "recognizedText";

    public ScanReceiptService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d(TAG, "service started");
        String imagePath = workIntent.getStringExtra(EXTRA_IMAGE);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
        try {
            ExifInterface exif = new ExifInterface(imagePath);
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
        Log.d(TAG, "image processed");
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
        Log.d(TAG, "text recognized");
        text = text.trim();
        // extract information from text
        // extract dates
        Pattern datePattern = Pattern.compile("\\d{1,2}\\W\\d{1,2}\\W\\d{2,4}");
        Matcher dateMatcher = datePattern.matcher(text);
        List<String> dates = new ArrayList<String>();
        while (dateMatcher.find()) {
            dates.add(dateMatcher.group());
        }
        Log.d(TAG, dates.toString());
        // extract value
        Pattern currencyPattern = Pattern.compile("[\\$€]\\d+\\.\\d{2}");
        Matcher currencyMatcher = currencyPattern.matcher(text);
        List<String> values = new ArrayList<String>();
        while (currencyMatcher.find()) {
            values.add(currencyMatcher.group());
        }
        Log.d(TAG, values.toString());
        // broadcast
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);
        intent.putExtra(EXTRA_TEXT, text);
        sendBroadcast(intent);
        // Create transaction
    }

    @Override
    public void onRecognitionFailed() {
    }

}
