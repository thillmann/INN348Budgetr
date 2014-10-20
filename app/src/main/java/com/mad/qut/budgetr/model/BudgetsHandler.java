package com.mad.qut.budgetr.model;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mad.qut.budgetr.provider.FinanceContract;

import java.util.ArrayList;
import java.util.HashMap;

public class BudgetsHandler extends JSONHandler {

    private static final String TAG = BudgetsHandler.class.getSimpleName();

    private HashMap<String, Budget> mBudgets = new HashMap<String, Budget>();

    public BudgetsHandler(Context context) {
        super(context);
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = FinanceContract.addCallerIsSyncAdapterParameter(
                FinanceContract.Budgets.CONTENT_URI);

        // The list of rooms is not large, so for simplicity we delete all of them and repopulate
        list.add(ContentProviderOperation.newDelete(uri).build());
        for (Budget budget : mBudgets.values()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            builder.withValue(FinanceContract.Budgets.BUDGET_ID, budget.id);
            builder.withValue(FinanceContract.Budgets.BUDGET_AMOUNT, budget.amount);
            builder.withValue(FinanceContract.Budgets.BUDGET_NAME, budget.name);
            builder.withValue(FinanceContract.Budgets.BUDGET_TYPE, budget.type);
            builder.withValue(FinanceContract.Budgets.BUDGET_START_DATE, budget.startDate);
            builder.withValue(FinanceContract.Budgets.CATEGORY_ID, budget.category);
            builder.withValue(FinanceContract.Budgets.CURRENCY_ID, budget.currency);
            list.add(builder.build());
        }
    }

    @Override
    public void process(JsonElement element) {
        for (Budget budget : new Gson().fromJson(element, Budget[].class)) {
            mBudgets.put(budget.id, budget);
        }
    }

}
