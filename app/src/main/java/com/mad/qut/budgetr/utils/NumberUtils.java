package com.mad.qut.budgetr.utils;

import android.util.Log;

import com.mad.qut.budgetr.AppController;
import com.mad.qut.budgetr.provider.FinanceContract;

import java.text.NumberFormat;

public class NumberUtils {

    private static final String TAG = NumberUtils.class.getSimpleName();

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance();
    private static final String STANDARD_SYMBOL = NumberFormat.getCurrencyInstance().getCurrency().getSymbol();
    private static String mSymbol = PrefUtils.getCurrencySymbol(AppController.getInstance().getBaseContext());

    /**
     * Get formatted currency string.
     *
     * @param amount Amount to format
     * @return Formatted string
     */
    public static String getFormattedCurrency(double amount) {
        return CURRENCY.format(amount).replace(STANDARD_SYMBOL, mSymbol);
    }

    /**
     * Get formatted currency string.
     *
     * @param amount Amount to format
     * @return Formatted string
     */
    public static String getFormattedCurrency(float amount) {
        return CURRENCY.format(amount).replace(STANDARD_SYMBOL, mSymbol);
    }

    /**
     * Clean a string of any currency related characters and parse it as double.
     *
     * @param amount String containing the amount
     * @return Amount as double
     */
    public static double getCleanedValue(String amount) {
        return Double.parseDouble(amount.replaceAll("[" + mSymbol + ".,]", "")) / 100;
    }

    /**
     * Return the currency symbol (e.g. A$).
     *
     * @return Currency symbol
     */
    public static String getCurrencySymbol() {
        return mSymbol;
    }

    public static void resetSymbol() {
        mSymbol = PrefUtils.getCurrencySymbol(AppController.getInstance().getBaseContext());
        AppController.getInstance().getContentResolver().notifyChange(FinanceContract.Transactions.CONTENT_URI, null, false);
    }

}
