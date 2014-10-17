package com.mad.qut.budgetr.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import com.mad.qut.budgetr.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReceiptScannerActivity extends Activity {

    private static final String TAG = ReceiptScannerActivity.class.getSimpleName();

    private static final int CAPUTRE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    private ImageView mPhoto;
    private String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmsss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_photo);

        mPhoto = (ImageView) findViewById(R.id.photo);

        Intent iCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (iCamera.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                iCamera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(iCamera, CAPUTRE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAPUTRE_IMAGE_ACTIVITY_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    setPic();
                }
        }
    }

    private void setPic() {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mPhoto.setImageBitmap(bitmap);
        /*
        ViewTreeObserver vto = mPhoto.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                int targetW = mPhoto.getMeasuredWidth();
                int targetH = mPhoto.getMeasuredHeight();
                Log.e(TAG,"Height: " + photoH + "/" + targetH + " Width: " + photoW + "/" + targetW);
                int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPurgeable = true;


                return true;
            }
        });*/
    }

}
