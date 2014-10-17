package com.mad.qut.budgetr.model;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mad.qut.budgetr.provider.FinanceContract;

import java.util.ArrayList;
import java.util.HashMap;

public class CurrenciesHandler extends JSONHandler {

    private static final String TAG = CurrenciesHandler.class.getSimpleName();

    private HashMap<String, Currency> mCurrencies = new HashMap<String, Currency>();

    public CurrenciesHandler(Context context) {
        super(context);
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = FinanceContract.addCallerIsSyncAdapterParameter(
                FinanceContract.Currencies.CONTENT_URI);

        // The list of rooms is not large, so for simplicity we delete all of them and repopulate
        list.add(ContentProviderOperation.newDelete(uri).build());
        for (Currency currency : mCurrencies.values()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            builder.withValue(FinanceContract.Currencies.CURRENCY_ID, currency.id);
            builder.withValue(FinanceContract.Currencies.CURRENCY_NAME, currency.name);
            builder.withValue(FinanceContract.Currencies.CURRENCY_SYMBOL, currency.symbol);
            list.add(builder.build());
        }
    }

    @Override
    public void process(JsonElement element) {
        for (Currency currency : new Gson().fromJson(element, Currency[].class)) {
            mCurrencies.put(currency.id, currency);
        }
    }

}
