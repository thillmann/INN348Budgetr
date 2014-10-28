package com.mad.qut.budgetr.model;

import android.util.Log;

import com.google.gson.Gson;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.utils.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Budget {

    private static final String TAG = Budget.class.getSimpleName();

    public String id;
    public String name;
    public double amount;
    public int type;
    public long startDate;
    public String category;
    public String currency;

    public Budget() {
    }

    public long getCurrentStartDate() {
        long currentStartDate = -1;
        Calendar c = DateUtils.getClearCalendar();
        switch (type) {
            case FinanceContract.Budgets.BUDGET_TYPE_WEEKLY:
                c.set(Calendar.DAY_OF_WEEK, 2);
                currentStartDate = c.getTimeInMillis();
                break;
            case FinanceContract.Budgets.BUDGET_TYPE_BIWEEKLY:
                if (startDate >= c.getTimeInMillis()) {
                    currentStartDate = startDate;
                } else {
                    Calendar c2 = DateUtils.getClearCalendar();
                    c2.setTimeInMillis(startDate);
                    do {
                        startDate = c2.getTimeInMillis();
                        c2.add(Calendar.WEEK_OF_YEAR, 2);
                    } while (c2.getTimeInMillis() < c.getTimeInMillis());
                    currentStartDate = startDate;
                }
                break;
            case FinanceContract.Budgets.BUDGET_TYPE_MONTHLY:
                c.set(Calendar.DAY_OF_MONTH, 1);
                currentStartDate = c.getTimeInMillis();
                break;
            case FinanceContract.Budgets.BUDGET_TYPE_YEARLY:
                c.set(Calendar.DAY_OF_YEAR, 1);
                currentStartDate = c.getTimeInMillis();
                break;
        }
        return currentStartDate;
    }

    public long getCurrentEndDate() {
        long currentEndDate = -1;
        Calendar c = DateUtils.getClearCalendar();
        switch (type) {
            case FinanceContract.Budgets.BUDGET_TYPE_WEEKLY:
                c.set(Calendar.DAY_OF_WEEK, 7);
                c.add(Calendar.DAY_OF_MONTH, 1);
                currentEndDate = c.getTimeInMillis();
                break;
            case FinanceContract.Budgets.BUDGET_TYPE_BIWEEKLY:
                c.setTimeInMillis(getCurrentStartDate());
                c.add(Calendar.WEEK_OF_YEAR, 2);
                c.add(Calendar.DAY_OF_YEAR, -1);
                currentEndDate = c.getTimeInMillis();
                break;
            case FinanceContract.Budgets.BUDGET_TYPE_MONTHLY:
                c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                currentEndDate = c.getTimeInMillis();
                break;
            case FinanceContract.Budgets.BUDGET_TYPE_YEARLY:
                c.set(Calendar.DAY_OF_YEAR, c.getActualMaximum(Calendar.DAY_OF_YEAR));
                currentEndDate = c.getTimeInMillis();
                break;
        }
        return currentEndDate;
    }

    public String getCurrentPeriod() {
        if (type == FinanceContract.Budgets.BUDGET_TYPE_ENDLESS) {
            return "";
        }
        return DateUtils.getFormattedDate(getCurrentStartDate(), "dd/MM/yyyy") + " - "
                + DateUtils.getFormattedDate(getCurrentEndDate(), "dd/MM/yyyy");
    }

    public double getAmountLeft(double amountSpent) {
        return amount-amountSpent;
    }

    public double getPercentLeft(double amountSpent) {
        return Math.round(((getAmountLeft(amountSpent) / amount)) * 100d * 10d) / 10d;
    }

    public double getAverageSpent(double amountSpent) {
        long currentDate = DateUtils.getCurrentTimeStamp();
        long startDate = getCurrentStartDate() / 1000;
        long daysElapsed = (currentDate - startDate) / (60 * 60 * 24) + 1;
        return Math.round((amountSpent / daysElapsed) * 10d) / 10d;
    }

    public double getAverageLeft(double amountSpent) {
        long currentDate = DateUtils.getCurrentTimeStamp();
        long endDate = getCurrentEndDate() / 1000;
        long daysLeft = (endDate - currentDate) / (60 * 60 * 24) + 1;
        return Math.round((getAmountLeft(amountSpent) / daysLeft) * 10d) / 10d;
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
