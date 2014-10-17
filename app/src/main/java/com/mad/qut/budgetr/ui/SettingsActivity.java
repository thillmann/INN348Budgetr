package com.mad.qut.budgetr.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.utils.PrefUtils;

public class SettingsActivity extends BaseActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    private SettingsFragment mSettingsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
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
                //mSettingsFragment.setSummary(PrefUtils.PREF_PASSWORD, R.string.pref_summary_set_password);
            }
        }
        else if (PrefUtils.PREF_PASSWORD.equals(key)) {
            mSettingsFragment.setTitle(PrefUtils.PREF_PASSWORD, R.string.pref_title_change_password);
            //mSettingsFragment.setSummary(PrefUtils.PREF_PASSWORD, R.string.pref_summary_change_password);
        }
        else if (PrefUtils.PREF_CURRENCY.equals(key)) {
            String[] currencies = getResources().getStringArray(R.array.currencies);
            String[] currenciesAlias = getResources().getStringArray(R.array.currencies_alias);
            String defaultCurrency = PrefUtils.getCurrency(this);
            int j = 0;
            for (int i = 0; i < currenciesAlias.length; i++) {
                if (currenciesAlias[i].equals(defaultCurrency)) {
                    j = i;
                    break;
                }
            }
            mSettingsFragment.setSummary(PrefUtils.PREF_CURRENCY, currencies[j]);
        }
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            //PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
            if (PrefUtils.isCurrencySet(getActivity())) {
                String[] currencies = getResources().getStringArray(R.array.currencies);
                String[] currenciesAlias = getResources().getStringArray(R.array.currencies_alias);
                String defaultCurrency = PrefUtils.getCurrency(getActivity());
                int j = 0;
                for (int i = 0; i < currenciesAlias.length; i++) {
                    if (currenciesAlias[i].equals(defaultCurrency)) {
                        j = i;
                        break;
                    }
                }
                setSummary(PrefUtils.PREF_CURRENCY, currencies[j]);
            }
            if (PrefUtils.isPasswordSet(getActivity())) {
                setTitle(PrefUtils.PREF_PASSWORD, R.string.pref_title_change_password);
            }

        }

        public void setTitle(String key, int title) {
            Preference pref = getPreferenceScreen().findPreference(key);
            pref.setTitle(title);
        }

        public void setSummary(String key, int summary) {
            Preference pref = getPreferenceScreen().findPreference(key);
            pref.setSummary(summary);
        }

        public void setSummary(String key, String summary) {
            Preference pref = getPreferenceScreen().findPreference(key);
            pref.setSummary(summary);
        }

    }

}
