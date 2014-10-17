package com.mad.qut.budgetr.provider;

import android.app.ActionBar;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mad.qut.budgetr.model.Transaction;
import com.mad.qut.budgetr.provider.FinanceContract.*;
import com.mad.qut.budgetr.provider.FinanceDatabase.*;
import com.mad.qut.budgetr.utils.SelectionBuilder;

public class FinanceProvider extends ContentProvider {

    private static final String TAG = FinanceProvider.class.getSimpleName();

    private FinanceDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int TRANSACTIONS = 100;
    private static final int TRANSACTIONS_ID = 101;

    private static final int BUDGETS = 200;
    private static final int BUDGETS_ID = 201;

    private static final int CATEGORIES = 300;
    private static final int CATEGORIES_ID = 301;

    private static final int CURRENCIES = 400;
    private static final int CURRENCIES_ID = 401;

    public FinanceProvider() {
    }

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = FinanceContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "transactions", TRANSACTIONS);
        matcher.addURI(authority, "transactions/*", TRANSACTIONS_ID);

        matcher.addURI(authority, "budgets", BUDGETS);
        matcher.addURI(authority, "budgets/*", BUDGETS_ID);

        matcher.addURI(authority, "categories", CATEGORIES);
        matcher.addURI(authority, "categories/*", CATEGORIES_ID);

        matcher.addURI(authority, "currencies", CURRENCIES);
        matcher.addURI(authority, "currencies/*", CURRENCIES_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new FinanceDatabase(getContext());
        return true;
    }

    private void deleteDatabase() {
        // TODO: wait for content provider operations to finish, then tear down
        mOpenHelper.close();
        Context context = getContext();
        FinanceDatabase.deleteDatabase(context);
        mOpenHelper = new FinanceDatabase(getContext());
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRANSACTIONS:
                return Transactions.CONTENT_TYPE;
            case TRANSACTIONS_ID:
                return Transactions.CONTENT_ITEM_TYPE;
            case BUDGETS:
                return Budgets.CONTENT_TYPE;
            case BUDGETS_ID:
                return Budgets.CONTENT_ITEM_TYPE;
            case CATEGORIES:
                return Categories.CONTENT_TYPE;
            case CATEGORIES_ID:
                return Categories.CONTENT_ITEM_TYPE;
            case CURRENCIES:
                return Currencies.CONTENT_TYPE;
            case CURRENCIES_ID:
                return Currencies.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uri == FinanceContract.BASE_CONTENT_URI) {
            // Handle whole database deletes (e.g. when signing out)
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);

        int retVal = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri);
        return retVal;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        boolean syncToNetwork = !FinanceContract.hasCallerIsSyncAdapterParameter(uri);
        switch (match) {
            case TRANSACTIONS: {
                db.insertOrThrow(Tables.TRANSACTIONS, null, values);
                notifyChange(uri);
                return Transactions.buildTransactionUri(values.getAsString(Transactions.TRANSACTION_ID));
            }
            case BUDGETS: {
                db.insertOrThrow(Tables.BUDGETS, null, values);
                notifyChange(uri);
                return Budgets.buildBudgetUri(values.getAsString(Budgets.BUDGET_ID));
            }
            case CATEGORIES: {
                db.insertOrThrow(Tables.CATEGORIES, null, values);
                notifyChange(uri);
                return Categories.buildCategoryUri(values.getAsString(Categories.CATEGORY_ID));
            }
            case CURRENCIES: {
                db.insertOrThrow(Tables.CURRENCIES, null, values);
                notifyChange(uri);
                return Currencies.buildCurrencyUri(values.getAsString(Currencies.CURRENCY_ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);

        switch (match) {
            default: {
                final SelectionBuilder builder = buildExpandedSelection(uri, match);

                boolean distinct = !TextUtils.isEmpty(
                        uri.getQueryParameter(FinanceContract.QUERY_PARAMETER_DISTINCT));

                Cursor cursor = builder
                        .where(selection, selectionArgs)
                        .query(db, distinct, projection, sortOrder, null);
                Context context = getContext();
                if (null != context) {
                    cursor.setNotificationUri(context.getContentResolver(), uri);
                }
                return cursor;
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        Log.d(TAG, "update");
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        notifyChange(uri);
        return retVal;
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case TRANSACTIONS: {
                return builder.table(Tables.TRANSACTIONS);
            }
            case TRANSACTIONS_ID: {
                final String transactionId = Transactions.getTransactionId(uri);
                return builder.table(Tables.TRANSACTIONS)
                        .where(Transactions._ID + "=?", transactionId);
            }
            case BUDGETS: {
                return builder.table(Tables.BUDGETS);
            }
            case BUDGETS_ID: {
                final String budgetId = Budgets.getBudgetId(uri);
                return builder.table(Tables.BUDGETS)
                        .where(Budgets._ID + "=?", budgetId);
            }
            case CATEGORIES: {
                return builder.table(Tables.CATEGORIES);
            }
            case CATEGORIES_ID: {
                final String categoryId = Categories.getCategoryId(uri);
                return builder.table(Tables.CATEGORIES)
                        .where(Categories._ID + "=?", categoryId);
            }
            case CURRENCIES: {
                return builder.table(Tables.CURRENCIES);
            }
            case CURRENCIES_ID: {
                final String currencyId = Currencies.getCurrencyId(uri);
                return builder.table(Tables.CURRENCIES)
                        .where(Currencies._ID + "=?", currencyId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri for " + match + ": " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case TRANSACTIONS: {
                return builder.table(Tables.TRANSACTIONS_JOIN_CATEGORIES_CURRENCIES)
                        .mapToTable(Transactions._ID, Tables.TRANSACTIONS)
                        .mapToTable(Transactions.CATEGORY_ID, Tables.TRANSACTIONS)
                        .mapToTable(Transactions.CURRENCY_ID, Tables.TRANSACTIONS);
            }
            case TRANSACTIONS_ID: {
                final String transactionId = Transactions.getTransactionId(uri);
                return builder.table(Tables.TRANSACTIONS_JOIN_CATEGORIES_CURRENCIES)
                        .mapToTable(Transactions._ID, Tables.TRANSACTIONS)
                        .mapToTable(Transactions.CATEGORY_ID, Tables.TRANSACTIONS)
                        .mapToTable(Transactions.CURRENCY_ID, Tables.TRANSACTIONS)
                        .where(Transactions._ID + "=?", transactionId);
            }
            case BUDGETS: {
                return builder.table(Tables.BUDGETS_JOIN_CATEGORIES_CURRENCIES)
                        .mapToTable(Transactions._ID, Tables.BUDGETS)
                        .mapToTable(Transactions.CATEGORY_ID, Tables.BUDGETS)
                        .mapToTable(Transactions.CURRENCY_ID, Tables.BUDGETS);
            }
            case BUDGETS_ID: {
                final String budgetId = Budgets.getBudgetId(uri);
                return builder.table(Tables.BUDGETS)
                        .where(Budgets._ID + "=?", budgetId);
            }
            case CATEGORIES: {
                return builder.table(Tables.CATEGORIES);
            }
            case CATEGORIES_ID: {
                final String categoryId = Categories.getCategoryId(uri);
                return builder.table(Tables.CATEGORIES)
                        .where(Categories._ID + "=?", categoryId);
            }
            case CURRENCIES: {
                return builder.table(Tables.CURRENCIES);
            }
            case CURRENCIES_ID: {
                final String currencyId = Currencies.getCurrencyId(uri);
                return builder.table(Tables.CURRENCIES)
                        .where(Currencies._ID + "=?", currencyId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri for " + match + ": " + uri);
            }
        }
    }

    private void notifyChange(Uri uri) {
        // We only notify changes if the caller is not the sync adapter.
        // The sync adapter has the responsibility of notifying changes (it can do so
        // more intelligently than we can -- for example, doing it only once at the end
        // of the sync instead of issuing thousands of notifications for each record).
        if (!FinanceContract.hasCallerIsSyncAdapterParameter(uri)) {
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);
        }
    }

}
