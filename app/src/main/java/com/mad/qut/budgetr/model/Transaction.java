package com.mad.qut.budgetr.model;

import android.content.ContentValues;

import com.google.gson.Gson;
import com.mad.qut.budgetr.provider.FinanceContract;

import java.util.Calendar;
import java.util.UUID;

public class Transaction implements Comparable<Transaction>  {

    private static final String TAG = Transaction.class.getSimpleName();

    public String id;
    public long date;
    public String type;
    public double amount;
    public String category;
    public int repeat;
    public int reminder;
    public String next;
    public String root;

    @Override
    public int compareTo(Transaction another) {
        return this.date < another.date ? -1 :
                ( this.date > another.date ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Transaction)) {
            return false;
        }
        Transaction t = (Transaction) o;
        return type == t.type &&
                id == t.id &&
                date == t.date &&
                amount == t.amount;
    }

    public void nextDate() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        switch (repeat) {
            case FinanceContract.Transactions.TRANSACTION_REPEAT_DAILY:
                c.add(Calendar.DATE, 1);
                break;
            case FinanceContract.Transactions.TRANSACTION_REPEAT_WEEKLY:
                c.add(Calendar.DATE, 7);
                break;
            case FinanceContract.Transactions.TRANSACTION_REPEAT_BIWEEKLY:
                c.add(Calendar.DATE, 14);
                break;
            case FinanceContract.Transactions.TRANSACTION_REPEAT_MONTHLY:
                c.add(Calendar.MONTH, 1);
                break;
            case FinanceContract.Transactions.TRANSACTION_REPEAT_YEARLY:
                c.add(Calendar.YEAR, 1);
                break;
        }
        date = c.getTimeInMillis();
    }

    public void setId() {
        id = UUID.randomUUID().toString();
    }

    public ContentValues create() {
        ContentValues values = new ContentValues();
        if (id == null || id.equals("")) {
            setId();
        }
        values.put(FinanceContract.Transactions.TRANSACTION_ID, id);
        values.put(FinanceContract.Transactions.TRANSACTION_DATE, date);
        values.put(FinanceContract.Transactions.TRANSACTION_TYPE, type);
        values.put(FinanceContract.Transactions.TRANSACTION_AMOUNT, amount);
        values.put(FinanceContract.Transactions.CATEGORY_ID, category);
        values.put(FinanceContract.Transactions.TRANSACTION_REPEAT, repeat);
        values.put(FinanceContract.Transactions.TRANSACTION_REMINDER, reminder);
        values.put(FinanceContract.Transactions.TRANSACTION_NEXT, next);
        values.put(FinanceContract.Transactions.TRANSACTION_ROOT, root);
        return values;
    }

    public Transaction clone() {
        Transaction t = new Transaction();
        t.date = date;
        t.type = type;
        t.amount = amount;
        t.category = category;
        t.repeat = repeat;
        t.reminder = reminder;
        return t;
    }

    @Override
    public String toString() {
        return String.format("Transaction [id=%s, type=%s, amount=%f, category=%s]",
                id, type, amount, category);
    }

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Transaction fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Transaction.class);
    }

}
