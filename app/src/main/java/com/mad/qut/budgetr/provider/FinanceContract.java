package com.mad.qut.budgetr.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;

import java.util.Arrays;

public class FinanceContract {

    private static final String TAG = FinanceContract.class.getSimpleName();

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
        String BUDGET_ID         = "budget_id";
        String BUDGET_NAME       = "budget_name";
        String BUDGET_TYPE       = "budget_type";
        String BUDGET_START_DATE = "budget_start_date";
        String BUDGET_AMOUNT     = "budget_amount";
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

        public static final int TRANSACTION_REPEAT_NEVER     = 0;
        public static final int TRANSACTION_REPEAT_DAILY     = 1;
        public static final int TRANSACTION_REPEAT_WEEKLY    = 2;
        public static final int TRANSACTION_REPEAT_BIWEEKLY  = 3;
        public static final int TRANSACTION_REPEAT_MONTHLY   = 4;
        public static final int TRANSACTION_REPEAT_YEARLY    = 5;

        public static final int[] TRANSACTION_REPEATS = {
                TRANSACTION_REPEAT_NEVER,
                TRANSACTION_REPEAT_DAILY,
                TRANSACTION_REPEAT_WEEKLY,
                TRANSACTION_REPEAT_BIWEEKLY,
                TRANSACTION_REPEAT_MONTHLY,
                TRANSACTION_REPEAT_YEARLY
        };

        public static final int TRANSACTION_REMINDER_NEVER   = 0;
        public static final int TRANSACTION_REMINDER_1       = 1;
        public static final int TRANSACTION_REMINDER_2       = 2;
        public static final int TRANSACTION_REMINDER_3       = 3;
        public static final int TRANSACTION_REMINDER_4       = 4;
        public static final int TRANSACTION_REMINDER_5       = 5;
        public static final int TRANSACTION_REMINDER_6       = 6;
        public static final int TRANSACTION_REMINDER_7       = 7;

        public static final int[] TRANSACTION_REMINDERS = {
                TRANSACTION_REMINDER_NEVER,
                TRANSACTION_REMINDER_1,
                TRANSACTION_REMINDER_2,
                TRANSACTION_REMINDER_3,
                TRANSACTION_REMINDER_4,
                TRANSACTION_REMINDER_5,
                TRANSACTION_REMINDER_6,
                TRANSACTION_REMINDER_7
        };

        public static final boolean isValidTransactionType(String type) {
            return TRANSACTION_TYPE_INCOME.equals(type) || TRANSACTION_TYPE_EXPENSE.equals(type);
        }

        public static final boolean isValidTransactionRepeat(int repeat) {
            return Arrays.asList(TRANSACTION_REPEATS).contains(repeat);
        }

        public static final boolean isValidTransactionReminder(int reminder) {
            return Arrays.asList(TRANSACTION_REMINDERS).contains(reminder);
        }

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRANSACTIONS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.budgetr.transaction";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.budgetr.transaction";

        public static final String CATEGORY_ID = "category_id";

        public static final String DEFAULT_SORT = TransactionsColumns.TRANSACTION_DATE + " DESC";

        public static final String IN_TIME_INTERVAL_SELECTION =
                TRANSACTION_DATE + " >= ? and " + TRANSACTION_DATE + " <= ?";

        public static String[] buildInTimeIntervalArgs(long intervalStart, long intervalEnd) {
            return new String[] { String.valueOf(intervalStart), String.valueOf(intervalEnd) };
        }

        public static Uri buildTransactionUri(String transactionId) {
            return CONTENT_URI.buildUpon().appendPath(transactionId).build();
        }

        public static Uri buildTransactionsByCategoriesUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_CATEGORIES).build();
        }

        public static Uri buildTransactionsByDaysUri() {
            return CONTENT_URI.buildUpon().appendPath("days").build();
        }

        public static Uri buildCategoriesDirUri(String transactionId) {
            return CONTENT_URI.buildUpon().appendPath(transactionId).appendPath(PATH_CATEGORIES).build();
        }

        public static String getCategory(Uri uri){
            return uri.getPathSegments().get(5);
        }

        public static String getTransactionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Budgets implements BudgetsColumns, CategoriesColumns,
            CurrenciesColumns, BaseColumns {

        public static final int BUDGET_TYPE_WEEKLY   = 0;
        public static final int BUDGET_TYPE_BIWEEKLY = 1;
        public static final int BUDGET_TYPE_MONTHLY  = 2;
        public static final int BUDGET_TYPE_YEARLY   = 3;
        public static final int BUDGET_TYPE_ENDLESS  = 4;

        public static final int[] BUDGET_TYPES = {
                BUDGET_TYPE_WEEKLY,
                BUDGET_TYPE_BIWEEKLY,
                BUDGET_TYPE_MONTHLY,
                BUDGET_TYPE_YEARLY,
                BUDGET_TYPE_ENDLESS
        };

        public static final boolean isValidBudgetType(int type) {
            return Arrays.asList(BUDGET_TYPES).contains(type);
        }

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BUDGETS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.budgetr.budget";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.budgetr.budget";

        public static final String CATEGORY_ID = "category_id";

        public static final String DEFAULT_SORT = BudgetsColumns.BUDGET_NAME + " ASC";

        public static Uri buildBudgetUri(String budgetId) {
            return CONTENT_URI.buildUpon().appendPath(budgetId).build();
        }

        public static Uri buildBudgetTransactionsUri(String budgetId) {
            return CONTENT_URI.buildUpon().appendPath(budgetId).appendPath(PATH_TRANSACTIONS).build();
        }

        public static Uri buildCategoriesDirUri(String budgetId) {
            return CONTENT_URI.buildUpon().appendPath(budgetId).appendPath(PATH_CATEGORIES).build();
        }

        public static String getCategory(Uri uri){
            return uri.getPathSegments().get(5);
        }

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

        public static final String DEFAULT_SORT = CategoriesColumns.CATEGORY_NAME + " COLLATE NOCASE ASC";

        public static Uri buildCategoryUri(String categoryId) {
            return CONTENT_URI.buildUpon().appendPath(categoryId).build();
        }

        public static Uri buildTransactionsDirUri(String categoryId) {
            return CONTENT_URI.buildUpon().appendPath(categoryId).appendPath(PATH_TRANSACTIONS).build();
        }

        public static Uri buildBudgetsDirUri(String categoryId) {
            return CONTENT_URI.buildUpon().appendPath(categoryId).appendPath(PATH_BUDGETS).build();
        }

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

        public static final String DEFAULT_SORT = CurrenciesColumns.CURRENCY_NAME + " COLLATE NOCASE ASC";

        public static Uri buildCurrencyUri(String currencyId) {
            return CONTENT_URI.buildUpon().appendPath(currencyId).build();
        }

        public static Uri buildTransactionsDirUri(String currencyId) {
            return CONTENT_URI.buildUpon().appendPath(currencyId).appendPath(PATH_TRANSACTIONS).build();
        }

        public static Uri buildBudgetsDirUri(String currencyId) {
            return CONTENT_URI.buildUpon().appendPath(currencyId).appendPath(PATH_BUDGETS).build();
        }

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
