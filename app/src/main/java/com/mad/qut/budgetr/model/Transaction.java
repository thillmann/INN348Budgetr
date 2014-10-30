package com.mad.qut.budgetr.model;

import android.text.format.DateFormat;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Transaction implements Comparable<Transaction>  {

    private static final String TAG = Transaction.class.getSimpleName();

    public String id;
    public long date;
    public String type;
    public double amount;
    public String category;
    public int repeat;
    public int reminder;

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
