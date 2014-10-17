package com.mad.qut.budgetr.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefUtils {

    private static final String TAG = PrefUtils.class.getSimpleName();

    public static final String PREF_PASSWORD_ENABLED = "pref_password_enabled";
    public static final String PREF_PASSWORD = "pref_password";
    public static final String PREF_CURRENCY = "pref_currency";
    public static final String PREF_DATA_BOOTSTRAP_DONE = "pref_data_bootstrap_done";

    private static Context mContext;
    private static SharedPreferences mSharedPreferences;

    public static SharedPreferences getSharedPreferences(final Context context) {
        if (mContext != null && mSharedPreferences != null && context.equals(mContext)) {
            return mSharedPreferences;
        } else {
            mContext = context;
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            return mSharedPreferences;
        }
    }

    public static boolean isPasswordEnabled(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getBoolean(PREF_PASSWORD_ENABLED, false);
    }

    public static boolean isPasswordSet(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getBoolean(PREF_PASSWORD_ENABLED, false) && !sp.getString(PREF_PASSWORD, "").equals("");
    }

    public static String getPassword(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(PREF_PASSWORD, "");
    }

    public static void setPassword(final Context context, String password) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(PREF_PASSWORD, password).apply();
    }

    public static String getCurrency(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(PREF_CURRENCY, "");
    }

    public static boolean isCurrencySet(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return !sp.getString(PREF_CURRENCY, "").equals("");
    }

    public static void setCurrency(final Context context, String currency) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(PREF_CURRENCY, currency).apply();
    }

    public static void registerOnSharedPreferenceChangeListener(final Context context,
                                                                SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterOnSharedPreferenceChangeListener(final Context context,
                                                                  SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static void markDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DATA_BOOTSTRAP_DONE, true).commit();
    }

    public static boolean isDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DATA_BOOTSTRAP_DONE, false);
    }

}
