package com.mad.qut.budgetr.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mad.qut.budgetr.model.BudgetsHandler;
import com.mad.qut.budgetr.model.CategoriesHandler;
import com.mad.qut.budgetr.model.CurrenciesHandler;
import com.mad.qut.budgetr.model.JSONHandler;
import com.mad.qut.budgetr.model.TransactionsHandler;
import com.mad.qut.budgetr.provider.FinanceContract;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

public class FinanceDataHandler {

    private static final String TAG = FinanceDataHandler.class.getSimpleName();

    private static final String SP_KEY_DATA_TIMESTAMP = "data_timestamp";

    private static final String DEFAULT_TIMESTAMP = "Sat, 1 Jan 2000 00:00:00 GMT";

    private static final String DATA_KEY_CATEGORIES = "categories";
    private static final String DATA_KEY_CURRENCIES = "currencies";
    private static final String DATA_KEY_TRANSACTIONS = "transactions";
    private static final String DATA_KEY_BUDGETS = "budgets";

    private static final String[] DATA_KEYS_IN_ORDER = {
            DATA_KEY_CATEGORIES,
            DATA_KEY_CURRENCIES,
            DATA_KEY_TRANSACTIONS,
            DATA_KEY_BUDGETS
    };

    private Context mContext = null;

    CategoriesHandler mCategoriesHandler = null;
    CurrenciesHandler mCurrenciesHandler = null;
    TransactionsHandler mTransactionsHandler = null;
    BudgetsHandler mBudgetsHandler = null;

    HashMap<String, JSONHandler> mHandlerForKey = new HashMap<String, JSONHandler>();

    private int mContentProviderOperationsDone = 0;

    public FinanceDataHandler(Context context) {
        mContext = context;
    }

    public void applyFinanceData(String[] dataBodies, String dataTimestamp) throws IOException {
        mHandlerForKey.put(DATA_KEY_CATEGORIES, mCategoriesHandler = new CategoriesHandler(mContext));
        mHandlerForKey.put(DATA_KEY_CURRENCIES, mCurrenciesHandler = new CurrenciesHandler(mContext));
        mHandlerForKey.put(DATA_KEY_TRANSACTIONS, mTransactionsHandler = new TransactionsHandler(mContext));
        mHandlerForKey.put(DATA_KEY_BUDGETS, mBudgetsHandler = new BudgetsHandler(mContext));

        for (int i = 0; i < dataBodies.length; i++) {
            processDataBody(dataBodies[i]);
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        for (String key : DATA_KEYS_IN_ORDER) {
            mHandlerForKey.get(key).makeContentProviderOperations(batch);
        }

        try {
            int operations = batch.size();
            if (operations > 0) {
                mContext.getContentResolver().applyBatch(FinanceContract.CONTENT_AUTHORITY, batch);
            }
            mContentProviderOperationsDone += operations;
        } catch (RemoteException ex) {
            Log.e(TAG, "RemoteException while applying content provider operations.");
            throw new RuntimeException("Error executing content provider batch operation", ex);
        } catch (OperationApplicationException ex) {
            Log.e(TAG, "OperationApplicationException while applying content provider operations.");
            throw new RuntimeException("Error executing content provider batch operation", ex);
        }

        ContentResolver resolver = mContext.getContentResolver();
        for (String path : FinanceContract.TOP_LEVEL_PATHS) {
            Uri uri = FinanceContract.BASE_CONTENT_URI.buildUpon().appendPath(path).build();
            resolver.notifyChange(uri, null);
        }

        setDataTimestamp(dataTimestamp);
    }

    private void processDataBody(String dataBody) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(dataBody));
        JsonParser parser = new JsonParser();
        try {
            reader.setLenient(true);

            reader.beginObject();

            while (reader.hasNext()) {
                String key = reader.nextName();
                if (mHandlerForKey.containsKey(key)) {
                    // pass the value to the corresponding handler
                    mHandlerForKey.get(key).process(parser.parse(reader));
                } else {
                    Log.w(TAG, "Skipping unknown key in conference data json: " + key);
                    reader.skipValue();
                }
            }
            reader.endObject();
        } finally {
            reader.close();
        }
    }

    public String getDataTimestamp() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                SP_KEY_DATA_TIMESTAMP, DEFAULT_TIMESTAMP);
    }

    public void setDataTimestamp(String timestamp) {
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString(
                SP_KEY_DATA_TIMESTAMP, timestamp).commit();
    }

    public static void resetDataTimestamp(final Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(
                SP_KEY_DATA_TIMESTAMP).commit();
    }

}
