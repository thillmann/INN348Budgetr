package com.mad.qut.budgetr.utils;

import com.mad.qut.budgetr.Config;

import java.text.NumberFormat;

public class NumberUtils {

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance();
    private static final String STANDARD_SYMBOL = NumberFormat.getCurrencyInstance().getCurrency().getSymbol();
    private static final String SYMBOL = Config.CURRENCY_SYMBOL;

    public static String getFormattedCurrency(double amount) {
        return CURRENCY.format(amount).replace(STANDARD_SYMBOL, SYMBOL);
    }

    public static String getFormattedCurrency(float amount) {
        return CURRENCY.format(amount).replace(STANDARD_SYMBOL, SYMBOL);
    }

    public static double getCleanedValue(String amount) {
        return Double.parseDouble(amount.replaceAll("[" + SYMBOL + ".,]", "")) / 100;
    }

    public static String getCurrencySymbol() {
        return SYMBOL;
    }

}
