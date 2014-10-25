package com.mad.qut.budgetr.service;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.mad.qut.budgetr.utils.ImageUtils;

public class ImageProcessingAsyncTask extends AsyncTask<Void, Integer, Bitmap> {

    private static final String TAG = ImageProcessingAsyncTask.class.getSimpleName();

    private Bitmap mImage;
    private OnTaskCompletedListener mListener;

    public interface OnTaskCompletedListener {
        void onProcessingFinished(Bitmap image);
        void onProcessingFailed();
    }

    public ImageProcessingAsyncTask(Bitmap image, OnTaskCompletedListener listener) {
        mImage = image;
        mListener = listener;
    }

    @Override
    protected Bitmap doInBackground(Void... voids) {
        return ImageUtils.process(mImage);
    }

    @Override
    protected void onPostExecute(Bitmap image) {
        if (image == null) {
            mListener.onProcessingFailed();
        } else {
            mListener.onProcessingFinished(image);
        }
    }

}
