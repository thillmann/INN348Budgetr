package com.mad.qut.budgetr.model;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mad.qut.budgetr.provider.FinanceContract;

import java.util.ArrayList;
import java.util.HashMap;

public class CategoriesHandler extends JSONHandler {

    private static final String TAG = CategoriesHandler.class.getSimpleName();

    private HashMap<String, Category> mCategories = new HashMap<String, Category>();

    public CategoriesHandler(Context context) {
        super(context);
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = FinanceContract.addCallerIsSyncAdapterParameter(
                FinanceContract.Categories.CONTENT_URI);

        // The list of rooms is not large, so for simplicity we delete all of them and repopulate
        list.add(ContentProviderOperation.newDelete(uri).build());
        for (Category category : mCategories.values()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            builder.withValue(FinanceContract.Categories.CATEGORY_ID, category.id);
            builder.withValue(FinanceContract.Categories.CATEGORY_NAME, category.name);
            builder.withValue(FinanceContract.Categories.CATEGORY_TYPE, category.type);
            list.add(builder.build());
        }
    }

    @Override
    public void process(JsonElement element) {
        for (Category category : new Gson().fromJson(element, Category[].class)) {
            mCategories.put(category.id, category);
        }
    }

}
