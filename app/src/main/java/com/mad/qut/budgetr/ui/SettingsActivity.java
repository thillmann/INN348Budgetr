package com.mad.qut.budgetr.ui;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.utils.NumberUtils;
import com.mad.qut.budgetr.utils.PrefUtils;

import java.util.HashMap;

public class SettingsActivity extends BaseActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    private SettingsFragment mSettingsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        mSettingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mSettingsFragment)
                .commit();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        PrefUtils.registerOnSharedPreferenceChangeListener(this, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PrefUtils.unregisterOnSharedPreferenceChangeListener(this, this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PrefUtils.PREF_PASSWORD_ENABLED.equals(key)) {
            if (!PrefUtils.isPasswordEnabled(this)) {
                PrefUtils.setPassword(this, "");
                mSettingsFragment.setTitle(PrefUtils.PREF_PASSWORD, R.string.pref_title_set_password);
            }
        }
        else if (PrefUtils.PREF_PASSWORD.equals(key)) {
            mSettingsFragment.setTitle(PrefUtils.PREF_PASSWORD, R.string.pref_title_change_password);
        } else if (PrefUtils.PREF_CURRENCY.equals(key)) {
            String currencyId = PrefUtils.getCurrency(this);
            String currencySymbol = mSettingsFragment.getCurrencySymbols().get(currencyId);
            String currencyName = mSettingsFragment.getCurrencyNames().get(currencyId);
            PrefUtils.setCurrencySymbol(this, currencySymbol);
            PrefUtils.setCurrencyName(this, currencyName);
            mSettingsFragment.setSummary(PrefUtils.PREF_CURRENCY, currencyName);
            NumberUtils.resetSymbol();
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private ListPreference mCurrencyPref;

        private HashMap<String, String> mCurrencySymbols = new HashMap<String, String>();
        private HashMap<String,String> mCurrencyNames = new HashMap<String, String>();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            mCurrencyPref = (ListPreference) findPreference(PrefUtils.PREF_CURRENCY);
            getLoaderManager().initLoader(CurrencyQuery._TOKEN, null, this);

            Preference deleteTransactions = findPreference(PrefUtils.PREF_DELETE_TRANSACTIONS);
            deleteTransactions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.dialog_delete_transactions_title);
                    builder.setMessage(R.string.dialog_delete_transactions_message);
                    builder.setNegativeButton(R.string.cancel, null);
                    builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            getActivity().getContentResolver().delete(FinanceContract.Transactions.CONTENT_URI, null, null);
                            Toast.makeText(getActivity(), R.string.toast_transactions_deleted, Toast.LENGTH_SHORT).show();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return false;
                }
            });

            Preference deleteBudget = findPreference(PrefUtils.PREF_DELETE_BUDGETS);
            deleteBudget.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.dialog_delete_budgets_title);
                    builder.setMessage(R.string.dialog_delete_budgets_message);
                    builder.setNegativeButton(R.string.cancel, null);
                    builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            getActivity().getContentResolver().delete(FinanceContract.Budgets.CONTENT_URI, null, null);
                            Toast.makeText(getActivity(), R.string.toast_budgets_deleted, Toast.LENGTH_SHORT).show();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return false;
                }
            });

            if (PrefUtils.isPasswordSet(getActivity())) {
                setTitle(PrefUtils.PREF_PASSWORD, R.string.pref_title_change_password);
            }

            if (PrefUtils.isCurrencySet(getActivity())) {
                setTitle(PrefUtils.PREF_CURRENCY, R.string.pref_title_change_currency);
                setSummary(PrefUtils.PREF_CURRENCY, PrefUtils.getCurrencyName(getActivity()));
            }
        }

        public void setTitle(String key, int title) {
            Preference pref = getPreferenceScreen().findPreference(key);
            pref.setTitle(title);
        }

        public void setSummary(String key, String summary) {
            Preference pref = getPreferenceScreen().findPreference(key);
            pref.setSummary(summary);
        }

        public HashMap<String, String> getCurrencySymbols() {
            return mCurrencySymbols;
        }

        public HashMap<String, String> getCurrencyNames() {
            return mCurrencyNames;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(getActivity(),
                    FinanceContract.Currencies.CONTENT_URI, CurrencyQuery.PROJECTION, null, null, FinanceContract.Currencies.DEFAULT_SORT);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            if (cursor.getCount() > 0) {
                CharSequence[] currencyNames = new CharSequence[cursor.getCount()];
                CharSequence[] currencyIds = new CharSequence[cursor.getCount()];
                while (cursor.moveToNext()) {
                    int pos = cursor.getPosition();
                    String id = cursor.getString(CurrencyQuery.CURRENCY_ID);
                    String symbol = cursor.getString(CurrencyQuery.SYMBOL);
                    String name = cursor.getString(CurrencyQuery.NAME);
                    currencyNames[pos] = name;
                    currencyIds[pos] = id;
                    mCurrencySymbols.put(id, symbol);
                    mCurrencyNames.put(id, name);
                }
                mCurrencyPref.setEntries(currencyNames);
                mCurrencyPref.setEntryValues(currencyIds);
                mCurrencyPref.setEnabled(true);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            mCurrencyPref.setEnabled(false);
        }

        private interface CurrencyQuery {
            int _TOKEN = 0x01;

            String[] PROJECTION = {
                    FinanceContract.Currencies._ID,
                    FinanceContract.Currencies.CURRENCY_ID,
                    FinanceContract.Currencies.CURRENCY_NAME,
                    FinanceContract.Currencies.CURRENCY_SYMBOL
            };

            int _ID = 0;
            int CURRENCY_ID = 1;
            int NAME = 2;
            int SYMBOL = 3;
        }

    }

}
