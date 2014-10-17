package com.mad.qut.budgetr.model;

import android.util.Log;

import com.google.gson.Gson;
import com.mad.qut.budgetr.provider.FinanceContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Budget {

    private static final String TAG = Budget.class.getSimpleName();

    public String id;
    public String name;
    public String category;
    public double amount;
    public String repeat;
    public long start;
    public long end;
    public String currency;

    public Budget() {
    }

    /* DUMMY METHODS */

    private double amountLeft = -1;

    /**
     * What's left of the budget.
     *
     * @return double
     */
    public double getAmountLeft() {
        if (amountLeft == -1) {
            amountLeft = Math.random()*amount;
        }
        Log.d(TAG, amountLeft + "");
        return amountLeft;
    }

    /**
     * What's left of the budget as percentage.
     *
     * @return double
     */
    public double getPercentLeft() {
        return Math.round(((getAmountLeft() / amount)) * 100d * 100d) / 100;
    }

    /**
     * Determines whether or not the budget will
     * be exceeded through linear regression.
     *
     * @return boolean
     */
    public boolean willExceed() {
        double plannedDaily;
        double realDaily;
        if (FinanceContract.Transactions.TRANSACTION_REPEAT_WEEKLY.equals(repeat)) {
            plannedDaily = amount / 7;
        } else if (FinanceContract.Transactions.TRANSACTION_REPEAT_BIWEEKLY.equals(repeat)) {
            plannedDaily = amount / 14;
        } else {
            return false;
        }
        realDaily = (amount - getAmountLeft()) / getDaysElapsed();
        return realDaily > plannedDaily;
    }

    /**
     * Calculate the amount of days that have
     * elapsed.
     *
     * @return long
     */
    public long getDaysElapsed() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start *1000);
        Calendar now = Calendar.getInstance();
        long time = now.getTime().getTime() - c.getTime().getTime();
        long days = Math.round((double) time / (24. * 60.*60.*1000.));
        Log.d(TAG, days+"");
        return days;
    }

    /**
     * Time span of budget as string.
     *
     * @return String
     */
    public String getTimeSpan() {
        Date d = new Date(start *1000);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        String result = df.format(d);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start *1000);
        if (FinanceContract.Transactions.TRANSACTION_REPEAT_WEEKLY.equals(repeat)) {
            c.add(Calendar.WEEK_OF_YEAR, 1);
        } else if (FinanceContract.Transactions.TRANSACTION_REPEAT_BIWEEKLY.equals(repeat)) {
            c.add(Calendar.WEEK_OF_YEAR, 2);
        } else if (FinanceContract.Transactions.TRANSACTION_REPEAT_MONTHLY.equals(repeat)) {
            c.add(Calendar.MONTH, 1);
        } else if (FinanceContract.Transactions.TRANSACTION_REPEAT_YEARLY.equals(repeat)) {
            c.add(Calendar.YEAR, 1);
        } else {
            return "";
        }
        c.add(Calendar.DAY_OF_MONTH, -1);
        result += " - ";
        result += df.format(c.getTime());
        return result;
    }

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Budget fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Budget.class);
    }

}
