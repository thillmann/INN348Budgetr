package com.mad.qut.budgetr.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Category;
import com.mad.qut.budgetr.model.Currency;
import com.mad.qut.budgetr.model.Transaction;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.NumberUtils;
import com.melnykov.fab.FloatingActionButton;

public class TransactionsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = TransactionsListFragment.class.getSimpleName();

    private ListView mListView;
    private FloatingActionButton btAdd;
    private TransactionCursorAdapter mListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View root = inflater.inflate(R.layout.fragment_transactions_list, container, false);
        mListView = (ListView) root.findViewById(R.id.transactions_list_view);
        View mEmptyView = root.findViewById(android.R.id.empty);
        mListView.setEmptyView(mEmptyView);
        btAdd = (FloatingActionButton) root.findViewById(R.id.add);
        btAdd.attachToListView(mListView);
        displayListView();
        return root;
    }

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (visible) {
            btAdd.show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                FinanceContract.Transactions.CONTENT_URI, TransactionQuery.PROJECTION, null, null, FinanceContract.Transactions.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.swapCursor(null);
    }

    private void displayListView() {
        // TODO: Lazy List Load
        // TODO: Delete with long click
        // TODO: Detail View
        mListAdapter = new TransactionCursorAdapter(getActivity(), null, 0);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent iTransaction = new Intent();
                iTransaction.setClass(getActivity(), EditTransactionActivity.class);

                Cursor cursor = (Cursor) mListView.getItemAtPosition(i);

                Category category = new Category();
                category.id = cursor.getString(TransactionQuery.CATEGORY_ID);
                category.name = cursor.getString(TransactionQuery.CATEGORY_NAME);
                iTransaction.putExtra("category", category.toJSON());

                Transaction transaction = new Transaction();
                transaction.id = cursor.getString(TransactionQuery.TRANSACTION_ID);
                transaction.amount = cursor.getDouble(TransactionQuery.AMOUNT);
                transaction.date = cursor.getLong(TransactionQuery.DATE);
                transaction.type = cursor.getString(TransactionQuery.TYPE);
                transaction.category = category.id;
                transaction.repeat = cursor.getInt(TransactionQuery.REPEAT);
                transaction.reminder = cursor.getInt(TransactionQuery.REMINDER);
                transaction.next = cursor.getString(TransactionQuery.NEXT);
                transaction.root = cursor.getString(TransactionQuery.ROOT);
                iTransaction.putExtra("transaction", transaction.toJSON());

                Uri updateUri = Uri.parse(FinanceContract.Transactions.CONTENT_URI + "/" + l);
                iTransaction.putExtra(FinanceContract.Transactions.CONTENT_ITEM_TYPE, updateUri);

                startActivity(iTransaction);
            }
        });
        getLoaderManager().initLoader(TransactionQuery._TOKEN, null, this);
    }

    private interface TransactionQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                FinanceContract.Transactions.TRANSACTION_ID,
                FinanceContract.Transactions.TRANSACTION_DATE,
                FinanceContract.Transactions.TRANSACTION_TYPE,
                FinanceContract.Transactions.TRANSACTION_AMOUNT,
                FinanceContract.Transactions.TRANSACTION_REPEAT,
                FinanceContract.Transactions.TRANSACTION_REMINDER,
                FinanceContract.Transactions.TRANSACTION_NEXT,
                FinanceContract.Transactions.TRANSACTION_ROOT,
                FinanceContract.Categories.CATEGORY_NAME,
                FinanceContract.Categories.CATEGORY_ID
        };

        int _ID = 0;
        int TRANSACTION_ID = 1;
        int DATE = 2;
        int TYPE = 3;
        int AMOUNT = 4;
        int REPEAT = 5;
        int REMINDER = 6;
        int NEXT = 7;
        int ROOT = 8;
        int CATEGORY_NAME = 9;
        int CATEGORY_ID = 10;
    }

    public class TransactionCursorAdapter extends CursorAdapter {

        private LayoutInflater mLayoutInflater;

        private final long mCurrentDate = DateUtils.getCurrentTimeStamp() * 1000;

        public TransactionCursorAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return mLayoutInflater.inflate(R.layout.list_item_transaction, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView ivCategoryIcon = (ImageView) view.findViewById(R.id.category_icon);
            TextView tvCategoryName = (TextView) view.findViewById(R.id.category_name);
            TextView tvAmount = (TextView) view.findViewById(R.id.amount);
            TextView tvDate = (TextView) view.findViewById(R.id.date);

            ivCategoryIcon.setImageResource(Category.getIcon(cursor.getString(TransactionQuery.CATEGORY_ID), Category.ICON_SMALL));

            tvCategoryName.setText(cursor.getString(TransactionQuery.CATEGORY_NAME));

            double amount = cursor.getDouble(TransactionQuery.AMOUNT);
            if (cursor.getString(TransactionQuery.TYPE).equals(FinanceContract.Transactions.TRANSACTION_TYPE_INCOME)) {
                tvAmount.setTextColor(getResources().getColor(R.color.income));
            } else {
                tvAmount.setTextColor(getResources().getColor(R.color.expense));
                amount = -amount;
            }
            tvAmount.setText(NumberUtils.getFormattedCurrency(amount));

            long date = cursor.getLong(TransactionQuery.DATE);
            tvDate.setText(DateUtils.getFormattedDate(date, "dd/MM/yyyy"));

            if (mCurrentDate < date) {
                view.setAlpha(0.5f);
            } else {
                view.setAlpha(1f);
            }
        }

    }

}
