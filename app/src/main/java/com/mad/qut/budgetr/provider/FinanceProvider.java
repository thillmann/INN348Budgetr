package com.mad.qut.budgetr.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import com.mad.qut.budgetr.provider.FinanceContract.*;
import com.mad.qut.budgetr.provider.FinanceDatabase.*;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.SelectionBuilder;

public class FinanceProvider extends ContentProvider {

    private static final String TAG = FinanceProvider.class.getSimpleName();

    private FinanceDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int TRANSACTIONS = 100;
    private static final int TRANSACTIONS_ID = 101;
    private static final int TRANSACTIONS_BY_CATEGORIES = 102;
    private static final int TRANSACTIONS_BY_DAYS = 103;

    private static final int BUDGETS = 200;
    private static final int BUDGETS_ID = 201;
    private static final int BUDGETS_ID_TRANSACTIONS = 202;

    private static final int CATEGORIES = 300;
    private static final int CATEGORIES_ID = 301;

    private static final int CURRENCIES = 400;
    private static final int CURRENCIES_ID = 401;

    public FinanceProvider() {
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = FinanceContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "transactions", TRANSACTIONS);
        matcher.addURI(authority, "transactions/categories", TRANSACTIONS_BY_CATEGORIES);
        matcher.addURI(authority, "transactions/days", TRANSACTIONS_BY_DAYS);
        matcher.addURI(authority, "transactions/*", TRANSACTIONS_ID);

        matcher.addURI(authority, "budgets", BUDGETS);
        matcher.addURI(authority, "budgets/*", BUDGETS_ID);
        matcher.addURI(authority, "budgets/*/transactions", BUDGETS_ID_TRANSACTIONS);

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
        mOpenHelper.close();
        Context context = getContext();
        FinanceDatabase.deleteDatabase(context);
        mOpenHelper = new FinanceDatabase(getContext());
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRANSACTIONS:
            case TRANSACTIONS_BY_CATEGORIES:
            case TRANSACTIONS_BY_DAYS:
                return Transactions.CONTENT_TYPE;
            case TRANSACTIONS_ID:
                return Transactions.CONTENT_ITEM_TYPE;
            case BUDGETS:
                return Budgets.CONTENT_TYPE;
            case BUDGETS_ID:
                return Budgets.CONTENT_ITEM_TYPE;
            case BUDGETS_ID_TRANSACTIONS:
                return Transactions.CONTENT_TYPE;
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
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);

        int retVal = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri);
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRANSACTIONS_ID: {
                notifyChange(Transactions.CONTENT_URI);
            }
            case BUDGETS_ID: {
                notifyChange(Budgets.CONTENT_URI);
            }
        }
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
                return Transactions.buildTransactionUri(values.getAsString(Transactions._ID));
            }
            case BUDGETS: {
                db.insertOrThrow(Tables.BUDGETS, null, values);
                notifyChange(uri);
                return Budgets.buildBudgetUri(values.getAsString(Budgets._ID));
            }
            case CATEGORIES: {
                db.insertOrThrow(Tables.CATEGORIES, null, values);
                notifyChange(uri);
                return Categories.buildCategoryUri(values.getAsString(Categories._ID));
            }
            case CURRENCIES: {
                db.insertOrThrow(Tables.CURRENCIES, null, values);
                notifyChange(uri);
                return Currencies.buildCurrencyUri(values.getAsString(Currencies._ID));
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

        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        notifyChange(uri);
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRANSACTIONS_ID: {
                notifyChange(Transactions.CONTENT_URI);
            }
            case BUDGETS_ID: {
                notifyChange(Budgets.CONTENT_URI);
            }
        }
        return retVal;
    }

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

    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case TRANSACTIONS: {
                return builder.table(Tables.TRANSACTIONS_JOIN_CATEGORIES)
                        .mapToTable(Transactions._ID, Tables.TRANSACTIONS)
                        .mapToTable(Transactions.CATEGORY_ID, Tables.TRANSACTIONS);
            }
            case TRANSACTIONS_ID: {
                final String transactionId = Transactions.getTransactionId(uri);
                return builder.table(Tables.TRANSACTIONS_JOIN_CATEGORIES)
                        .mapToTable(Transactions._ID, Tables.TRANSACTIONS)
                        .mapToTable(Transactions.CATEGORY_ID, Tables.TRANSACTIONS)
                        .where(Tables.TRANSACTIONS + "." + Transactions._ID + "=?", transactionId);
            }
            case TRANSACTIONS_BY_CATEGORIES: {
                long currentDate = DateUtils.getCurrentTimeStamp() * 1000;
                return builder.table(Tables.TRANSACTIONS_JOIN_CATEGORIES)
                        .mapToTable(Tables.TRANSACTIONS + "." + Transactions._ID, Tables.TRANSACTIONS)
                        .mapToTable(Transactions.CATEGORY_ID, Tables.TRANSACTIONS)
                        .where(Transactions.TRANSACTION_DATE + "<=?", currentDate+"")
                        .groupBy(Tables.TRANSACTIONS + "." + Transactions.CATEGORY_ID);
            }
            case TRANSACTIONS_BY_DAYS: {
                long currentDate = DateUtils.getCurrentTimeStamp() * 1000;
                return builder.table(Tables.TRANSACTIONS_JOIN_CATEGORIES)
                        .mapToTable(Tables.TRANSACTIONS + "." + Transactions._ID, Tables.TRANSACTIONS)
                        .mapToTable(Transactions.CATEGORY_ID, Tables.TRANSACTIONS)
                        .where(Transactions.TRANSACTION_DATE + "<=?", currentDate+"")
                        .groupBy("strftime('%d%m%Y', " + Transactions.TRANSACTION_DATE + "/1000, 'unixepoch', 'localtime'), " + Transactions.TRANSACTION_TYPE);
            }
            case BUDGETS: {
                return builder.table(Tables.BUDGETS_JOIN_CATEGORIES)
                        .mapToTable(Budgets._ID, Tables.BUDGETS)
                        .mapToTable(Budgets.CATEGORY_ID, Tables.BUDGETS);
            }
            case BUDGETS_ID: {
                final String budgetId = Budgets.getBudgetId(uri);
                return builder.table(Tables.BUDGETS)
                        .where(Budgets._ID + "=?", budgetId);
            }
            case BUDGETS_ID_TRANSACTIONS: {
                final String budgetId = Budgets.getBudgetId(uri);
                return builder.table(Tables.TRANSACTIONS_JOIN_BUDGETS)
                        .mapToTable(Transactions._ID, Tables.TRANSACTIONS)
                        .where(Tables.BUDGETS + "." + Budgets._ID + "=?", budgetId)
                        .where(Transactions.TRANSACTION_TYPE + "=?", Transactions.TRANSACTION_TYPE_EXPENSE);
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
        if (!FinanceContract.hasCallerIsSyncAdapterParameter(uri)) {
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);
        }
    }

}
