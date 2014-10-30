package com.mad.qut.budgetr.model;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mad.qut.budgetr.provider.FinanceContract;

import java.util.ArrayList;
import java.util.HashMap;

public class TransactionsHandler extends JSONHandler {

    private static final String TAG = TransactionsHandler.class.getSimpleName();

    private HashMap<String, Transaction> mTransactions = new HashMap<String, Transaction>();

    public TransactionsHandler(Context context) {
        super(context);
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = FinanceContract.addCallerIsSyncAdapterParameter(
                FinanceContract.Transactions.CONTENT_URI);

        // The list of rooms is not large, so for simplicity we delete all of them and repopulate
        list.add(ContentProviderOperation.newDelete(uri).build());
        for (Transaction transaction : mTransactions.values()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            builder.withValue(FinanceContract.Transactions.TRANSACTION_ID, transaction.id);
            builder.withValue(FinanceContract.Transactions.TRANSACTION_AMOUNT, transaction.amount);
            builder.withValue(FinanceContract.Transactions.TRANSACTION_DATE, transaction.date);
            builder.withValue(FinanceContract.Transactions.TRANSACTION_REMINDER, transaction.reminder);
            builder.withValue(FinanceContract.Transactions.TRANSACTION_REPEAT, transaction.repeat);
            builder.withValue(FinanceContract.Transactions.CATEGORY_ID, transaction.category);
            list.add(builder.build());
        }
    }

    @Override
    public void process(JsonElement element) {
        for (Transaction transaction : new Gson().fromJson(element, Transaction[].class)) {
            mTransactions.put(transaction.id, transaction);
        }
    }

}
