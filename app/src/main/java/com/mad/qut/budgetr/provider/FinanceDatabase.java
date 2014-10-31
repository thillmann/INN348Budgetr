package com.mad.qut.budgetr.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.mad.qut.budgetr.provider.FinanceContract.*;
import com.mad.qut.budgetr.utils.PrefUtils;

public class FinanceDatabase extends SQLiteOpenHelper {

    private static final String TAG = FinanceDatabase.class.getSimpleName();

    private static final String DATABASE_NAME = "finance.db";

    private static final int CUR_DATABASE_VERSION = 113;

    private final Context mContext;

    interface Tables {
        String TRANSACTIONS = "transactions";
        String CATEGORIES   = "categories";
        String BUDGETS      = "budgets";
        String CURRENCIES   = "currencies";

        String TRANSACTIONS_JOIN_CATEGORIES = "transactions "
                + "LEFT OUTER JOIN categories ON transactions.category_id=categories.category_id ";

        String BUDGETS_JOIN_CATEGORIES = "budgets "
                + "LEFT OUTER JOIN categories ON budgets.category_id=categories.category_id ";

        String TRANSACTIONS_JOIN_BUDGETS = "transactions "
                + "LEFT OUTER JOIN budgets ON transactions.category_id=budgets.category_id";
    }

    interface References {
        String TRANSACTION_ID = "REFERENCES " + Tables.TRANSACTIONS + "(" + Transactions.TRANSACTION_ID + ")";
        String CATEGORY_ID = "REFERENCES " + Tables.CATEGORIES + "(" + Categories.CATEGORY_ID + ")";
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
                + TransactionsColumns.TRANSACTION_REPEAT + " INTEGER NOT NULL,"
                + TransactionsColumns.TRANSACTION_REMINDER + " INTEGER NOT NULL,"
                + TransactionsColumns.TRANSACTION_NEXT + " TEXT " + References.TRANSACTION_ID + ","
                + TransactionsColumns.TRANSACTION_ROOT + " TEXT " + References.TRANSACTION_ID + ","
                + Transactions.CATEGORY_ID + " TEXT " + References.CATEGORY_ID + ","
                + "UNIQUE (" + TransactionsColumns.TRANSACTION_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.BUDGETS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + BudgetsColumns.BUDGET_ID + " TEXT NOT NULL,"
                + BudgetsColumns.BUDGET_NAME + " TEXT NOT NULL,"
                + BudgetsColumns.BUDGET_AMOUNT + " DOUBLE NOT NULL DEFAULT 0,"
                + BudgetsColumns.BUDGET_TYPE + " SMALLINT NOT NULL,"
                + BudgetsColumns.BUDGET_START_DATE + " INTEGER NOT NULL DEFAULT -1,"
                + Budgets.CATEGORY_ID + " TEXT " + References.CATEGORY_ID + ","
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

            if (PrefUtils.isDataBootstrapDone(mContext)) {
                PrefUtils.markDataBootstrapNecessary(mContext);
            }
        }

        onCreate(db);
        version = CUR_DATABASE_VERSION;
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }

}
