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

    // Shared preferences key under which we store the timestamp that corresponds to
    // the data we currently have in our content provider.
    private static final String SP_KEY_DATA_TIMESTAMP = "data_timestamp";

    // symbolic timestamp to use when we are missing timestamp data (which means our data is
    // really old or nonexistent)
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

    // Convenience map that maps the key name to its corresponding handler (e.g.
    // "blocks" to mBlocksHandler (to avoid very tedious if-elses)
    HashMap<String, JSONHandler> mHandlerForKey = new HashMap<String, JSONHandler>();

    // Tally of total content provider operations we carried out (for statistical purposes)
    private int mContentProviderOperationsDone = 0;

    public FinanceDataHandler(Context context) {
        mContext = context;
    }

    /**
     * Parses the conference data in the given objects and imports the data into the
     * content provider. The format of the data is documented at https://code.google.com/p/iosched.
     *
     * @param dataBodies The collection of JSON objects to parse and import.
     * @param dataTimestamp The timestamp of the data. This should be in RFC1123 format.
     * @throws java.io.IOException If there is a problem parsing the data.
     */
    public void applyConferenceData(String[] dataBodies, String dataTimestamp) throws IOException {
        // create handlers for each data type
        mHandlerForKey.put(DATA_KEY_CATEGORIES, mCategoriesHandler = new CategoriesHandler(mContext));
        mHandlerForKey.put(DATA_KEY_CURRENCIES, mCurrenciesHandler = new CurrenciesHandler(mContext));
        mHandlerForKey.put(DATA_KEY_TRANSACTIONS, mTransactionsHandler = new TransactionsHandler(mContext));
        mHandlerForKey.put(DATA_KEY_BUDGETS, mBudgetsHandler = new BudgetsHandler(mContext));

        for (int i = 0; i < dataBodies.length; i++) {
            processDataBody(dataBodies[i]);
        }

        // produce the necessary content provider operations
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
            reader.setLenient(true); // To err is human

            // the whole file is a single JSON object
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

    // Returns the timestamp of the data we have in the content provider.
    public String getDataTimestamp() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                SP_KEY_DATA_TIMESTAMP, DEFAULT_TIMESTAMP);
    }

    // Sets the timestamp of the data we have in the content provider.
    public void setDataTimestamp(String timestamp) {
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString(
                SP_KEY_DATA_TIMESTAMP, timestamp).commit();
    }

    // Reset the timestamp of the data we have in the content provider
    public static void resetDataTimestamp(final Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(
                SP_KEY_DATA_TIMESTAMP).commit();
    }

}
