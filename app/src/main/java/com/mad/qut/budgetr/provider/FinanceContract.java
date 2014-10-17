package com.mad.qut.budgetr.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;

public class FinanceContract {

    private static final String TAG = FinanceContract.class.getSimpleName();

    /**
     * Query parameter to create a distinct query.
     */
    public static final String QUERY_PARAMETER_DISTINCT = "distinct";

    interface TransactionsColumns {
        String TRANSACTION_ID       = "transaction_id";
        String TRANSACTION_DATE     = "transaction_date";
        String TRANSACTION_TYPE     = "transaction_type";
        String TRANSACTION_AMOUNT   = "transaction_amount";
        String TRANSACTION_REPEAT   = "transaction_repeat";
        String TRANSACTION_REMINDER = "transaction_reminder";
    }

    interface CategoriesColumns {
        String CATEGORY_ID   = "category_id";
        String CATEGORY_NAME = "category_name";
        String CATEGORY_TYPE = "category_type";
    }

    interface BudgetsColumns {
        String BUDGET_ID        = "budget_id";
        String BUDGET_NAME      = "budget_name";
        String BUDGET_START     = "budget_start";
        String BUDGET_END       = "budget_end";
        String BUDGET_REPEAT    = "budget_repeat";
        String BUDGET_AMOUNT    = "budget_amount";
    }

    interface CurrenciesColumns {
        String CURRENCY_ID      = "currency_id";
        String CURRENCY_NAME    = "currency_name";
        String CURRENCY_SYMBOL  = "currency_symbol";
    }

    public static final String CONTENT_AUTHORITY = "com.mad.qut.budgetr";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_TRANSACTIONS    = "transactions";
    public static final String PATH_CATEGORIES      = "categories";
    public static final String PATH_BUDGETS         = "budgets";
    public static final String PATH_CURRENCIES      = "currencies";

    public static final String[] TOP_LEVEL_PATHS = {
            PATH_TRANSACTIONS,
            PATH_CATEGORIES,
            PATH_BUDGETS,
            PATH_CURRENCIES
    };

    public static class Transactions implements TransactionsColumns, CategoriesColumns,
            CurrenciesColumns, BaseColumns {
        public static final String TRANSACTION_TYPE_INCOME      = "income";
        public static final String TRANSACTION_TYPE_EXPENSE     = "expense";

        public static final String TRANSACTION_REPEAT_NEVER     = "never";
        public static final String TRANSACTION_REPEAT_DAILY     = "daily";
        public static final String TRANSACTION_REPEAT_WEEKLY    = "weekly";
        public static final String TRANSACTION_REPEAT_BIWEEKLY  = "biweekly";
        public static final String TRANSACTION_REPEAT_MONTHLY   = "monthly";
        public static final String TRANSACTION_REPEAT_YEARLY    = "yearly";

        public static final String TRANSACTION_REMINDER_NEVER   = "never";
        public static final String TRANSACTION_REMINDER_1       = "1";
        public static final String TRANSACTION_REMINDER_2       = "2";
        public static final String TRANSACTION_REMINDER_3       = "3";
        public static final String TRANSACTION_REMINDER_4       = "4";
        public static final String TRANSACTION_REMINDER_5       = "5";
        public static final String TRANSACTION_REMINDER_6       = "6";
        public static final String TRANSACTION_REMINDER_7       = "7";

        public static final boolean isValidTransactionType(String type) {
            return TRANSACTION_TYPE_INCOME.equals(type) || TRANSACTION_TYPE_EXPENSE.equals(type);
        }

        public static final boolean isValidTransactionRepeat(String repeat) {
            return TRANSACTION_REPEAT_DAILY.equals(repeat)
                    || TRANSACTION_REPEAT_WEEKLY.equals(repeat)
                    || TRANSACTION_REPEAT_BIWEEKLY.equals(repeat)
                    || TRANSACTION_REPEAT_MONTHLY.equals(repeat)
                    || TRANSACTION_REPEAT_YEARLY.equals(repeat)
                    || TRANSACTION_REPEAT_NEVER.equals(repeat);
        }

        public static final boolean isValidTransactionReminder(String reminder) {
            return TRANSACTION_REMINDER_NEVER.equals(reminder)
                    || TRANSACTION_REMINDER_1.equals(reminder)
                    || TRANSACTION_REMINDER_2.equals(reminder)
                    || TRANSACTION_REMINDER_3.equals(reminder)
                    || TRANSACTION_REMINDER_4.equals(reminder)
                    || TRANSACTION_REMINDER_5.equals(reminder)
                    || TRANSACTION_REMINDER_6.equals(reminder)
                    || TRANSACTION_REMINDER_7.equals(reminder);
        }

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRANSACTIONS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.budgetr.transaction";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.budgetr.transaction";

        public static final String CATEGORY_ID = "category_id";

        public static final String CURRENCY_ID = "currency_id";

        /** "ORDER BY" clauses. */
        public static final String DEFAULT_SORT = TransactionsColumns.TRANSACTION_DATE + " DESC";

        // Used to fetch transactions within a specific time interval
        public static final String IN_TIME_INTERVAL_SELECTION =
                TRANSACTION_DATE + " >= ? and " + TRANSACTION_DATE + " <= ?";

        // Builds selectionArgs for {@link IN_TIME_INTERVAL_SELECTION}
        public static String[] buildInTimeIntervalArgs(long intervalStart, long intervalEnd) {
            return new String[] { String.valueOf(intervalStart), String.valueOf(intervalEnd) };
        }

        /** Build {@link Uri} for requested {@link #TRANSACTION_ID}. */
        public static Uri buildTransactionUri(String transactionId) {
            return CONTENT_URI.buildUpon().appendPath(transactionId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Categories} associated
         * with the requested {@link #TRANSACTION_ID}.
         */
        public static Uri buildCategoriesDirUri(String transactionId) {
            return CONTENT_URI.buildUpon().appendPath(transactionId).appendPath(PATH_CATEGORIES).build();
        }

        /**
         * Build {@link Uri} that references any {@link Categories} associated
         * with the requested {@link #TRANSACTION_ID}.
         */
        public static Uri buildCurrenciesDirUri(String transactionId) {
            return CONTENT_URI.buildUpon().appendPath(transactionId).appendPath(PATH_CURRENCIES).build();
        }

        public static String getCategory(Uri uri){
            return uri.getPathSegments().get(5);
        }

        public static String getCurrency(Uri uri){
            return uri.getPathSegments().get(6);
        }

        /** Read {@link #TRANSACTION_ID} from {@link Transactions} {@link Uri}. */
        public static String getTransactionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Budgets implements BudgetsColumns, CategoriesColumns,
            CurrenciesColumns, BaseColumns {

        public static final boolean isValidBudgetRepeat(String repeat) {
            return Transactions.TRANSACTION_REPEAT_DAILY.equals(repeat)
                    || Transactions.TRANSACTION_REPEAT_WEEKLY.equals(repeat)
                    || Transactions.TRANSACTION_REPEAT_BIWEEKLY.equals(repeat)
                    || Transactions.TRANSACTION_REPEAT_MONTHLY.equals(repeat)
                    || Transactions.TRANSACTION_REPEAT_YEARLY.equals(repeat)
                    || Transactions.TRANSACTION_REPEAT_NEVER.equals(repeat);
        }

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BUDGETS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.budgetr.budget";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.budgetr.budget";

        public static final String CATEGORY_ID = "category_id";

        public static final String CURRENCY_ID = "currency_id";

        /** "ORDER BY" clauses. */
        public static final String DEFAULT_SORT = BudgetsColumns.BUDGET_NAME + " ASC";

        /** Build {@link Uri} for requested {@link #BUDGET_ID}. */
        public static Uri buildBudgetUri(String budgetId) {
            return CONTENT_URI.buildUpon().appendPath(budgetId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Categories} associated
         * with the requested {@link #BUDGET_ID}.
         */
        public static Uri buildCategoriesDirUri(String budgetId) {
            return CONTENT_URI.buildUpon().appendPath(budgetId).appendPath(PATH_CATEGORIES).build();
        }

        /**
         * Build {@link Uri} that references any {@link Categories} associated
         * with the requested {@link #BUDGET_ID}.
         */
        public static Uri buildCurrenciesDirUri(String budgetId) {
            return CONTENT_URI.buildUpon().appendPath(budgetId).appendPath(PATH_CURRENCIES).build();
        }

        public static String getCategory(Uri uri){
            return uri.getPathSegments().get(5);
        }

        public static String getCurrency(Uri uri){
            return uri.getPathSegments().get(6);
        }

        /** Read {@link #BUDGET_ID} from {@link Budgets} {@link Uri}. */
        public static String getBudgetId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Categories implements CategoriesColumns, BaseColumns {

        public static final boolean isValidTransactionType(String type) {
            return Transactions.TRANSACTION_TYPE_INCOME.equals(type)
                    || Transactions.TRANSACTION_TYPE_EXPENSE.equals(type);
        }

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CATEGORIES).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.budgetr.category";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.budgetr.category";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = CategoriesColumns.CATEGORY_NAME + " COLLATE NOCASE ASC";

        /** Build {@link Uri} for requested {@link #CATEGORY_ID}. */
        public static Uri buildCategoryUri(String categoryId) {
            return CONTENT_URI.buildUpon().appendPath(categoryId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Transactions} associated
         * with the requested {@link #CATEGORY_ID}.
         */
        public static Uri buildTransactionsDirUri(String categoryId) {
            return CONTENT_URI.buildUpon().appendPath(categoryId).appendPath(PATH_TRANSACTIONS).build();
        }

        /**
         * Build {@link Uri} that references any {@link Budgets} associated
         * with the requested {@link #CATEGORY_ID}.
         */
        public static Uri buildBudgetsDirUri(String categoryId) {
            return CONTENT_URI.buildUpon().appendPath(categoryId).appendPath(PATH_BUDGETS).build();
        }

        /** Read {@link #CATEGORY_ID} from {@link Categories} {@link Uri}. */
        public static String getCategoryId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Currencies implements CurrenciesColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CURRENCIES).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.budgetr.currency";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.budgetr.currency";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = CurrenciesColumns.CURRENCY_NAME + " COLLATE NOCASE ASC";

        /** Build {@link Uri} for requested {@link #CURRENCY_ID}. */
        public static Uri buildCurrencyUri(String currencyId) {
            return CONTENT_URI.buildUpon().appendPath(currencyId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Transactions} associated
         * with the requested {@link #CURRENCY_ID}.
         */
        public static Uri buildTransactionsDirUri(String currencyId) {
            return CONTENT_URI.buildUpon().appendPath(currencyId).appendPath(PATH_TRANSACTIONS).build();
        }

        /**
         * Build {@link Uri} that references any {@link Budgets} associated
         * with the requested {@link #CURRENCY_ID}.
         */
        public static Uri buildBudgetsDirUri(String currencyId) {
            return CONTENT_URI.buildUpon().appendPath(currencyId).appendPath(PATH_BUDGETS).build();
        }

        /** Read {@link #CURRENCY_ID} from {@link Currencies} {@link Uri}. */
        public static String getCurrencyId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
                ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    public static boolean hasCallerIsSyncAdapterParameter(Uri uri) {
        return TextUtils.equals("true",
                uri.getQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER));
    }

    private FinanceContract() {
    }

}
