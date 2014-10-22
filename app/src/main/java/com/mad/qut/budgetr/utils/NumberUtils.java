package com.mad.qut.budgetr.utils;

import java.text.NumberFormat;

public class NumberUtils {

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance();
    private static final String STANDARD_SYMBOL = NumberFormat.getCurrencyInstance().getCurrency().getSymbol();
    private static final String SYMBOL = "A$";

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
