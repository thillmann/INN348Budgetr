package com.mad.qut.budgetr.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.mad.qut.budgetr.provider.FinanceContract.*;

public class FinanceDatabase extends SQLiteOpenHelper {

    private static final String TAG = FinanceDatabase.class.getSimpleName();

    private static final String DATABASE_NAME = "finance.db";

    private static final int CUR_DATABASE_VERSION = 103;

    private final Context mContext;

    interface Tables {
        String TRANSACTIONS = "transactions";
        String CATEGORIES   = "categories";
        String BUDGETS      = "budgets";
        String CURRENCIES   = "currencies";

        String TRANSACTIONS_JOIN_CATEGORIES_CURRENCIES = "transactions "
                + "LEFT OUTER JOIN categories ON transactions.category_id=categories.category_id "
                + "LEFT OUTER JOIN currencies ON transactions.currency_id=currencies.currency_id";

        String BUDGETS_JOIN_CATEGORIES_CURRENCIES = "budgets "
                + "LEFT OUTER JOIN categories ON budgets.category_id=categories.category_id "
                + "LEFT OUTER JOIN currencies ON budgets.currency_id=currencies.currency_id";

        String TRANSACTIONS_JOIN_CATEGORIES_BUDGETS = "transactions "
                + "LEFT OUTER JOIN categories ON transactions.category_id=categories.category_id "
                + "LEFT OUTER JOIN budgets ON transaction.category_id=budgets.category_id";
    }

    interface References {
        String CATEGORY_ID = "REFERENCES " + Tables.CATEGORIES + "(" + Categories.CATEGORY_ID + ")";
        String CURRENCY_ID = "REFERENCES " + Tables.CURRENCIES + "(" + Currencies.CURRENCY_ID + ")";
    }

    public FinanceDatabase(Context context) {
        super(context, DATABASE_NAME, null, CUR_DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.TRANSACTIONS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TransactionsColumns.TRANSACTION_ID + " TEXT NOT NULL,"
                + TransactionsColumns.TRANSACTION_DATE + " INTEGER NOT NULL,"
                + TransactionsColumns.TRANSACTION_TYPE + " TEXT,"
                + TransactionsColumns.TRANSACTION_AMOUNT + " DOUBLE NOT NULL DEFAULT 0,"
                + TransactionsColumns.TRANSACTION_REPEAT + " TEXT,"
                + TransactionsColumns.TRANSACTION_REMINDER + " TEXT,"
                + Transactions.CATEGORY_ID + " TEXT " + References.CATEGORY_ID + ","
                + Transactions.CURRENCY_ID + " TEXT " + References.CURRENCY_ID + ","
                + "UNIQUE (" + TransactionsColumns.TRANSACTION_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.BUDGETS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + BudgetsColumns.BUDGET_ID + " TEXT NOT NULL,"
                + BudgetsColumns.BUDGET_START + " INTEGER NOT NULL,"
                + BudgetsColumns.BUDGET_END + " INTEGER NOT NULL,"
                + BudgetsColumns.BUDGET_NAME + " TEXT NOT NULL,"
                + BudgetsColumns.BUDGET_AMOUNT + " DOUBLE NOT NULL DEFAULT 0,"
                + BudgetsColumns.BUDGET_REPEAT + " TEXT,"
                + Budgets.CATEGORY_ID + " TEXT " + References.CATEGORY_ID + ","
                + Budgets.CURRENCY_ID + " TEXT " + References.CURRENCY_ID + ","
                + "UNIQUE (" + BudgetsColumns.BUDGET_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.CATEGORIES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CategoriesColumns.CATEGORY_ID + " TEXT NOT NULL,"
                + CategoriesColumns.CATEGORY_NAME + " TEXT NOT NULL,"
                + CategoriesColumns.CATEGORY_TYPE + " TEXT,"
                + "UNIQUE (" + CategoriesColumns.CATEGORY_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.CURRENCIES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CurrenciesColumns.CURRENCY_ID + " TEXT NOT NULL,"
                + CurrenciesColumns.CURRENCY_NAME + " TEXT NOT NULL,"
                + CurrenciesColumns.CURRENCY_SYMBOL + " TEXT NOT NULL,"
                + "UNIQUE (" + CurrenciesColumns.CURRENCY_ID + ") ON CONFLICT REPLACE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version = oldVersion;

        if (version != CUR_DATABASE_VERSION) {
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TRANSACTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.BUDGETS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.CATEGORIES);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.CURRENCIES);
        }

        onCreate(db);
        version = CUR_DATABASE_VERSION;
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }

}
