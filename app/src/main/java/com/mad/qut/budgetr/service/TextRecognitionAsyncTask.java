package com.mad.qut.budgetr.service;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TextRecognitionAsyncTask extends AsyncTask<Void, Integer, String> {

    private static final String TAG = TextRecognitionAsyncTask.class.getSimpleName();

    /**
     * Storage location on sdcard.
     */
    private static final String BASE_PATH = Environment.getExternalStorageDirectory() + "/budgetr/";
    private static final String DATA_DIR = "tessdata/";
    private static final String LANG = "eng";

    /**
     * Engine mode:
     *  0 for default (only tesseract)
     *  1 for cube only
     *  2 for combined (very slow)
     */
    private static final int ENGINE_MODE = 0; // 2 for combined (Cube + Default)

    /**
     * Data files for tesseract.
     */
    private static final String[] DATA_FILES = {
            ".cube.bigrams",
            ".cube.fold",
            ".cube.lm",
            ".cube.nn",
            ".cube.params",
            ".cube.size",
            ".cube.word-freq",
            ".tesseract_cube.nn",
            ".traineddata"
    };

    private Context mContext;
    private Bitmap mImage;
    private OnTaskCompletedListener mListener;

    public interface OnTaskCompletedListener {
        void onRecognitionFinished(String text);
        void onRecognitionFailed();
    }

    public TextRecognitionAsyncTask(Context context, Bitmap image, OnTaskCompletedListener listener) {
        mContext = context;
        mImage = image;
        mListener = listener;
    }

    @Override
    protected String doInBackground(Void... voids) {
        // Check if all data for OCR engine is on device
        boolean isAFileMissing = false;
        File dataFile;
        for (String s : DATA_FILES) {
            dataFile = new File(BASE_PATH + DATA_DIR + LANG + s);
            if (!dataFile.exists()) {
                isAFileMissing = true;
            }
        }
        if (isAFileMissing) {
            // install data from assets
            try {
                installFromAssets();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        // create instance of tesseract
        TessBaseAPI baseApi = new TessBaseAPI();
        //baseApi.setDebug(true);
        baseApi.init(BASE_PATH, LANG, ENGINE_MODE);

        if (mImage != null) {
            baseApi.setImage(mImage);
            String recognizedText = baseApi.getUTF8Text();
            baseApi.end();
            return recognizedText;
        }
        return "";
    }

    @Override
    protected void onPostExecute(String text) {
        if (text.equals("")) {
            // if no text found call error method
            mListener.onRecognitionFailed();
        } else {
            // otherwise call success method
            mListener.onRecognitionFinished(text);
        }
    }

    private void installFromAssets() throws IOException {
        // Check folder structure
        String[] paths = new String[] {BASE_PATH, BASE_PATH + DATA_DIR };

        // create directories if they don't exist
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }

        }

        Log.d(TAG, "installing from zip");
        // install training data from zip file in assets directory
        String sourceFile = "tesseract-ocr-3.02." + LANG + ".zip";
        installZipFromAssets(sourceFile);

    }

    private void installZipFromAssets(String sourceFile) throws IOException{
        String destination = BASE_PATH + DATA_DIR;

        // Read zip
        ZipInputStream inputStream = new ZipInputStream(mContext.getAssets().open(sourceFile));
        for (ZipEntry entry = inputStream.getNextEntry(); entry != null; entry = inputStream.getNextEntry()) {
            // get file in zip
            File destinationFile = new File(destination, entry.getName());

            long zippedFileSize = entry.getSize();

            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            final int BUFFER = 8192;

            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER);
            int unzippedSize = 0;

            // Write the contents
            int count = 0;
            Integer percentComplete = 0;
            Integer percentCompleteLast = 0;
            byte[] data = new byte[BUFFER];
            while ((count = inputStream.read(data, 0, BUFFER)) != -1) {
                bufferedOutputStream.write(data, 0, count);
                unzippedSize += count;
                percentComplete = (int) ((unzippedSize / (long) zippedFileSize) * 100);
                if (percentComplete > percentCompleteLast) {
                    percentCompleteLast = percentComplete;
                }
            }
            bufferedOutputStream.close();
            inputStream.closeEntry();
        }
        inputStream.close();
    }

}
