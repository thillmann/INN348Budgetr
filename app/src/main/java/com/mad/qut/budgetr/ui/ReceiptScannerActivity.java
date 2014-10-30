package com.mad.qut.budgetr.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.service.ScanReceiptService;
import com.mad.qut.budgetr.ui.widget.CategoryGridAdapter;
import com.mad.qut.budgetr.ui.widget.CategoryGridView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ReceiptScannerActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ReceiptScannerActivity.class.getSimpleName();

    private static final int CAPUTRE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    private String mCurrentPhotoPath;
    private CategoryGridView mCategoriesGrid;
    private CategoryGridAdapter mGridAdapter;

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
        setContentView(R.layout.activity_receipt_scanner);

        mCategoriesGrid = (CategoryGridView) findViewById(R.id.categories);
        displayCategories();

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
                if (resultCode != Activity.RESULT_OK) {
                    finish();
                }
                break;
            default:
                finish();
                break;
        }
    }

    public void displayCategories() {
        mGridAdapter = new CategoryGridAdapter(this, null, 0);
        mCategoriesGrid.setAdapter(mGridAdapter);
        final Activity activity = this;
        mCategoriesGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ((ImageView) view.findViewById(R.id.hover)).setVisibility(View.VISIBLE);
                Intent mServiceIntent = new Intent(activity, ScanReceiptService.class);
                mServiceIntent.putExtra(ScanReceiptService.EXTRA_IMAGE, mCurrentPhotoPath);
                mServiceIntent.putExtra(ScanReceiptService.EXTRA_CATGEORY, view.getContentDescription().toString());
                startService(mServiceIntent);
                Toast.makeText(activity, R.string.toast_scan, Toast.LENGTH_SHORT).show();
                activity.finish();
            }
        });
        getLoaderManager().restartLoader(CategoryQuery._TOKEN, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String selection = FinanceContract.Categories.CATEGORY_TYPE + "=?";
        String[] args = { FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE };

        return new CursorLoader(this,
                FinanceContract.Categories.CONTENT_URI, CategoryQuery.PROJECTION, selection, args, FinanceContract.Categories.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mGridAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mGridAdapter.swapCursor(null);
    }

    private interface CategoryQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                FinanceContract.Categories._ID,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Categories.CATEGORY_NAME
        };

        int _ID = 0;
        int CATEGORY_ID = 1;
        int NAME = 2;
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

}
