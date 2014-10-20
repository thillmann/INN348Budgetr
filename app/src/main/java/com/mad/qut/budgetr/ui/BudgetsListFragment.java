package com.mad.qut.budgetr.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Budget;
import com.mad.qut.budgetr.model.Category;
import com.mad.qut.budgetr.model.Currency;
import com.mad.qut.budgetr.model.Transaction;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.utils.CircleImage;
import com.mad.qut.budgetr.utils.DateUtils;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;

public class BudgetsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = BudgetsListFragment.class.getSimpleName();

    public ListView mListView;
    public View mEmptyView;
    public Context mContext;

    private BudgetCursorAdapter mListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View root = inflater.inflate(R.layout.fragment_budgets_list, container, false);
        mListView = (ListView) root.findViewById(R.id.budgets_list_view);
        mEmptyView = root.findViewById(android.R.id.empty);
        displayListView();
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(getActivity(),
                FinanceContract.Budgets.CONTENT_URI, BudgetQuery.PROJECTION, null, null, FinanceContract.Budgets.DEFAULT_SORT);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        boolean isEmpty = data.getCount() == 0;
        mListAdapter.swapCursor(data);
        mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mListAdapter.swapCursor(null);
        mEmptyView.setVisibility(View.VISIBLE);
    }

    private void displayListView() {
        // TODO: Lazy List Load
        // TODO: Delete with long click
        // TODO: Detail View
        mListAdapter = new BudgetCursorAdapter(getActivity(), null, 0);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent iBudget = new Intent();
                iBudget.setClass(getActivity(), EditBudgetActivity.class);

                Cursor cursor = (Cursor) mListView.getItemAtPosition(i);

                Category category = new Category();
                category.id = cursor.getString(BudgetQuery.CATEGORY_ID);
                category.name = cursor.getString(BudgetQuery.CATEGORY_NAME);
                iBudget.putExtra("category", category.toJSON());

                Currency currency = new Currency();
                currency.id = cursor.getString(BudgetQuery.CURRENCY_ID);
                currency.symbol = cursor.getString(BudgetQuery.CURRENCY_SYMBOL);
                iBudget.putExtra("currency", currency.toJSON());

                Budget budget = new Budget();
                budget.id = cursor.getString(BudgetQuery.BUDGET_ID);
                budget.amount = cursor.getDouble(BudgetQuery.AMOUNT);
                budget.name = cursor.getString(BudgetQuery.NAME);
                budget.type = cursor.getInt(BudgetQuery.TYPE);
                budget.startDate = cursor.getLong(BudgetQuery.START_DATE);
                budget.category = category.id;
                budget.currency = currency.id;
                iBudget.putExtra("budget", budget.toJSON());

                Uri updateUri = FinanceContract.Budgets.buildBudgetUri(l+"");
                iBudget.putExtra(FinanceContract.Budgets.CONTENT_ITEM_TYPE, updateUri);

                startActivity(iBudget);
            }
        });
        getLoaderManager().initLoader(BudgetQuery._TOKEN, null, this);
    }

    private interface BudgetQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                FinanceContract.Budgets.BUDGET_ID,
                FinanceContract.Budgets.BUDGET_NAME,
                FinanceContract.Budgets.BUDGET_AMOUNT,
                FinanceContract.Budgets.BUDGET_TYPE,
                FinanceContract.Budgets.BUDGET_START_DATE,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Categories.CATEGORY_NAME,
                FinanceContract.Currencies.CURRENCY_ID,
                FinanceContract.Currencies.CURRENCY_SYMBOL,
        };

        int _ID             = 0;
        int BUDGET_ID       = 1;
        int NAME            = 2;
        int AMOUNT          = 3;
        int TYPE            = 4;
        int START_DATE      = 5;
        int CATEGORY_ID     = 6;
        int CATEGORY_NAME   = 7;
        int CURRENCY_ID     = 8;
        int CURRENCY_SYMBOL = 9;
    }

    private interface TransactionQuery {
        int _TOKEN = 0x2;

        String[] PROJECTION = {
                "SUM(" + FinanceContract.Transactions.TRANSACTION_AMOUNT + ")",
                FinanceContract.Budgets.BUDGET_ID
        };

        int SUM = 0;
        int BUDGET_ID = 1;
    }

    private class BudgetCursorAdapter extends CursorAdapter {

        LayoutInflater mLayoutInflater;
        LoaderManager.LoaderCallbacks mCallback;

        public BudgetCursorAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return mLayoutInflater.inflate(R.layout.list_item_budget, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Budget b = new Budget();
            b.id = cursor.getString(BudgetQuery.BUDGET_ID);
            b.name = cursor.getString(BudgetQuery.NAME);
            b.amount = cursor.getDouble(BudgetQuery.AMOUNT);
            b.type = cursor.getInt(BudgetQuery.TYPE);
            b.startDate = cursor.getLong(BudgetQuery.START_DATE);
            b.category = cursor.getString(BudgetQuery.CATEGORY_ID);
            b.currency = cursor.getString(BudgetQuery.CURRENCY_ID);

            Log.d(TAG, b.toJSON().toString());

            ImageView mCategoryIcon = (ImageView) view.findViewById(R.id.category_icon);
            TextView mName = (TextView) view.findViewById(R.id.name);
            TextView mTimeSpan = (TextView) view.findViewById(R.id.time_span);
            TextView mAmountSpent = (TextView) view.findViewById(R.id.amount_spent);

            Bitmap bm = BitmapFactory.decodeResource(getResources(), Category.getIcon(cursor.getString(BudgetQuery.CATEGORY_ID)));
            Bitmap roundBm = CircleImage.getRoundedShape(bm);
            mCategoryIcon.setImageBitmap(roundBm);

            mName.setText(cursor.getString(BudgetQuery.NAME));

            if (b.type == FinanceContract.Budgets.BUDGET_TYPE_ENDLESS) {
                mTimeSpan.setVisibility(View.GONE);
            }
            mTimeSpan.setText(b.getCurrentPeriod());

            new TransactionTask(mAmountSpent, b, cursor.getString(BudgetQuery._ID)).execute();
        }

        private class TransactionTask extends AsyncTask<Object, Integer, Double> {

            private TextView mTextView;
            private Budget mBudget;
            private String mId;

            public TransactionTask(TextView textView, Budget budget, String id) {
                mTextView = textView;
                mBudget = budget;
                mId = id;
            }

            @Override
            protected Double doInBackground(Object... args0) {
                Cursor c = getActivity().getContentResolver().query(FinanceContract.Budgets.buildBudgetTransactionsUri(mId),
                        TransactionQuery.PROJECTION,
                        FinanceContract.Transactions.IN_TIME_INTERVAL_SELECTION,
                        FinanceContract.Transactions.buildInTimeIntervalArgs(mBudget.getCurrentStartDate(), mBudget.getCurrentEndDate()),
                        null);
                if (c != null) {
                    c.moveToFirst();
                    double amountSpent = c.getDouble(TransactionQuery.SUM);
                    c.close();
                    return amountSpent;
                }
                return -1.0;
            }

            @Override
            protected void onPostExecute(Double amountSpent) {
                mTextView.setText(mBudget.getPercentLeft(amountSpent) + "%");
            }

        }

    }

}
