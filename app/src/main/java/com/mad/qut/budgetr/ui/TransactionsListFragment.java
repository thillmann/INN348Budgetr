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
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.mad.qut.budgetr.provider.FinanceProvider;
import com.mad.qut.budgetr.utils.CircleImage;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.NumberUtils;
import com.melnykov.fab.FloatingActionButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransactionsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = TransactionsListFragment.class.getSimpleName();

    public ListView mListView;
    public View mEmptyView;

    private TransactionCursorAdapter mListAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View root = inflater.inflate(R.layout.fragment_transactions_list, container, false);
        mListView = (ListView) root.findViewById(R.id.transactions_list_view);
        mEmptyView = root.findViewById(android.R.id.empty);
        FloatingActionButton addButton = (FloatingActionButton) root.findViewById(R.id.add);
        addButton.attachToListView(mListView);
        displayListView();
        return root;
    }

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (visible) {
            // DO WHEN VISIBLE
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(getActivity(),
                FinanceContract.Transactions.CONTENT_URI, TransactionQuery.PROJECTION, null, null, FinanceContract.Transactions.DEFAULT_SORT);
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

                Currency currency = new Currency();
                currency.id = cursor.getString(TransactionQuery.CURRENCY_ID);
                currency.symbol = cursor.getString(TransactionQuery.CURRENCY_SYMBOL);
                iTransaction.putExtra("currency", currency.toJSON());

                Transaction transaction = new Transaction();
                transaction.id = cursor.getString(TransactionQuery.TRANSACTION_ID);
                transaction.amount = cursor.getDouble(TransactionQuery.AMOUNT);
                transaction.date = cursor.getLong(TransactionQuery.DATE);
                transaction.type = cursor.getString(TransactionQuery.TYPE);
                transaction.category = category.id;
                transaction.repeat = cursor.getString(TransactionQuery.REPEAT);
                transaction.reminder = cursor.getString(TransactionQuery.REMINDER);
                transaction.currency = currency.id;
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
                FinanceContract.Categories.CATEGORY_NAME,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Currencies.CURRENCY_SYMBOL,
                FinanceContract.Currencies.CURRENCY_ID
        };

        int _ID = 0;
        int TRANSACTION_ID = 1;
        int DATE = 2;
        int TYPE = 3;
        int AMOUNT = 4;
        int REPEAT = 5;
        int REMINDER = 6;
        int CATEGORY_NAME = 7;
        int CATEGORY_ID = 8;
        int CURRENCY_SYMBOL = 9;
        int CURRENCY_ID = 10;
    }

    public class TransactionCursorAdapter extends CursorAdapter {

        LayoutInflater mLayoutInflater;

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

            ImageView mCategoryIcon = (ImageView) view.findViewById(R.id.category_icon);
            TextView mCategoryName = (TextView) view.findViewById(R.id.category_name);
            TextView mAmount = (TextView) view.findViewById(R.id.amount);
            TextView mDate = (TextView) view.findViewById(R.id.date);

            mCategoryIcon.setImageResource(Category.getIcon(cursor.getString(TransactionQuery.CATEGORY_ID), 48));

            mCategoryName.setText(cursor.getString(TransactionQuery.CATEGORY_NAME));

            mAmount.setText(NumberUtils.getFormattedCurrency(-cursor.getDouble(TransactionQuery.AMOUNT)));

            mDate.setText(DateUtils.getFormattedDate(cursor.getLong(TransactionQuery.DATE), "dd/MM/yyyy"));
        }

    }

}
