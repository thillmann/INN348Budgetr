package com.mad.qut.budgetr.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.mad.qut.budgetr.AppController;
import com.mad.qut.budgetr.Config;
import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.JSONHandler;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.sync.FinanceDataHandler;
import com.mad.qut.budgetr.ui.widget.PasswordDialogFragment;
import com.mad.qut.budgetr.utils.PrefUtils;

import java.io.IOException;

public abstract class BaseActivity extends Activity implements PasswordDialogFragment.PasswordDialogListener {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private boolean mActionBarBack = false;

    // data bootstrap thread. Data bootstrap is the process of initializing the database
    // with the data cache that ships with the app.
    Thread mDataBootstrapThread = null;

    @Override
    public void onStart() {
        super.onStart();
        if (!PrefUtils.isDataBootstrapDone(this) && mDataBootstrapThread == null) {
            performDataBootstrap();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        AppController.getInstance().setLockStatus(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        AppController.getInstance().setLockStatus(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLockStatus();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String candidatePassword) {
        String mStoredPassword = PrefUtils.getPassword(this);
        if (candidatePassword.equals(mStoredPassword)) {
          AppController.getInstance().setLockStatus(false);
        } else {
            Toast.makeText(this, R.string.toast_password_failed, Toast.LENGTH_SHORT).show();
        }
        checkLockStatus();
    }

    public void checkLockStatus() {
        if (PrefUtils.isPasswordSet(this) && AppController.getInstance().isLocked()) {
            PasswordDialogFragment dialog = new PasswordDialogFragment();
            dialog.show(getFragmentManager(), "PasswordDialog");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public boolean isActionBarBack() {
        return mActionBarBack;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mActionBarBack = true;
                onBackPressed();
                mActionBarBack = false;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void performDataBootstrap() {
        final Context mContext = getApplicationContext();
        mDataBootstrapThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String bootstrapJson = JSONHandler.parseResource(mContext, R.raw.bootstrap_data);
                    FinanceDataHandler dataHandler = new FinanceDataHandler(mContext);
                    dataHandler.applyConferenceData(new String[]{bootstrapJson},
                            Config.BOOTSTRAP_DATA_TIMESTAMP);
                    getContentResolver().notifyChange(Uri.parse(FinanceContract.CONTENT_AUTHORITY),
                            null, false);
                    PrefUtils.markDataBootstrapDone(mContext);
                } catch (IOException e) {
                    Log.e(TAG, "*** ERROR DURING BOOTSTRAP! Problem in bootstrap data?");
                    PrefUtils.markDataBootstrapDone(mContext);
                }
                mDataBootstrapThread = null;
            }
        });
        mDataBootstrapThread.start();
    }

}
