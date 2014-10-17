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
import android.os.Bundle;
import android.provider.BaseColumns;
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
        /*mListView = (ListView) inflater.inflate(R.layout.fragment_budgets_list, container, false);
        mContext = getActivity();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Budget b = (Budget) mListView.getItemAtPosition(position);
                Intent iBudget = new Intent();
                iBudget.setClass(mContext, EditBudgetActivity.class);
                iBudget.putExtra("transaction", b.toJSON());
                startActivity(iBudget);
            }
        });

        return mListView;*/

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
        getLoaderManager().initLoader(BudgetQuery._TOKEN, null, this);
    }

    private interface BudgetQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                FinanceContract.Budgets.BUDGET_ID,
                FinanceContract.Budgets.BUDGET_NAME,
                FinanceContract.Budgets.BUDGET_AMOUNT,
                FinanceContract.Budgets.BUDGET_START,
                FinanceContract.Budgets.BUDGET_END,
                FinanceContract.Budgets.BUDGET_REPEAT,
                FinanceContract.Categories.CATEGORY_NAME,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Currencies.CURRENCY_SYMBOL,
                FinanceContract.Currencies.CURRENCY_ID
        };

        int _ID = 0;
        int BUDGET_ID = 1;
        int NAME = 2;
        int AMOUNT = 3;
        int START = 4;
        int END = 5;
        int REPEAT = 6;
        int CATEGORY_NAME = 7;
        int CATEGORY_ID = 8;
        int CURRENCY_SYMBOL = 9;
        int CURRENCY_ID = 10;
    }

    public class BudgetCursorAdapter extends CursorAdapter {

        LayoutInflater mLayoutInflater;

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
            b.start = cursor.getLong(BudgetQuery.START);
            b.end = cursor.getLong(BudgetQuery.END);
            b.repeat = cursor.getString(BudgetQuery.REPEAT);
            b.category = cursor.getString(BudgetQuery.CATEGORY_ID);
            b.currency = cursor.getString(BudgetQuery.CURRENCY_ID);

            ImageView mCategoryIcon = (ImageView) view.findViewById(R.id.category_icon);
            TextView mName = (TextView) view.findViewById(R.id.name);
            TextView mTimeSpan = (TextView) view.findViewById(R.id.time_span);
            TextView mAmountSpent = (TextView) view.findViewById(R.id.amount_spent);

            Bitmap bm = BitmapFactory.decodeResource(getResources(), Category.getIcon(cursor.getString(BudgetQuery.CATEGORY_ID)));
            Bitmap roundBm = CircleImage.getRoundedShape(bm);
            mCategoryIcon.setImageBitmap(roundBm);

            mName.setText(cursor.getString(BudgetQuery.NAME));

            mTimeSpan.setText(b.getTimeSpan());

            if (b.willExceed()) {
                mAmountSpent.setTextColor(getResources().getColor(R.color.expense));
            }
            mAmountSpent.setText(b.getPercentLeft() + "%");
        }

    }

}
