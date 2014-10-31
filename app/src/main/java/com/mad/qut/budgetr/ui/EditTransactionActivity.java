package com.mad.qut.budgetr.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Transaction;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.widget.CategoryGridAdapter;
import com.mad.qut.budgetr.ui.widget.CategoryGridView;
import com.mad.qut.budgetr.ui.widget.CurrencyEditText;
import com.mad.qut.budgetr.ui.widget.TransactionDeleteDialogFragment;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.SelectionBuilder;

import java.util.Calendar;

public class EditTransactionActivity extends BaseActivity implements DatePickerDialog.OnDateSetListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        TransactionDeleteDialogFragment.Listener,
        View.OnClickListener {

    private static final String TAG = EditTransactionActivity.class.getSimpleName();

    private CurrencyEditText mAmountEdit;
    private Button mButtonDate;
    private Button mButtonRepeating;
    private Button mButtonReminder;
    private CategoryGridView mCategoriesGrid;

    private CategoryGridAdapter mGridAdapter;

    private Transaction mTransaction;

    private Uri updateUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mTransaction = Transaction.fromJSON(getIntent().getStringExtra("transaction"));

        updateUri = getIntent().getParcelableExtra(FinanceContract.Transactions.CONTENT_ITEM_TYPE);

        assignViews();

        displayCategories();
    }

    private void assignViews() {
        mAmountEdit = (CurrencyEditText) findViewById(R.id.amount);
        mAmountEdit.setText(mTransaction.amount*10+"");
        mButtonDate = (Button) findViewById(R.id.button_date);
        mButtonDate.setText(DateUtils.getFormattedDate(mTransaction.date, "dd/MM/yyyy"));
        mButtonRepeating = (Button) findViewById(R.id.button_repeating);
        mButtonRepeating.setText(getResources().getStringArray(R.array.transaction_repeats)[mTransaction.repeat]);
        if (mTransaction.repeat != FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER) {
            mButtonDate.setEnabled(false);
            mButtonRepeating.setEnabled(false);
        }
        mButtonReminder = (Button) findViewById(R.id.button_reminder);
        mButtonReminder.setText(getResources().getStringArray(R.array.transaction_repeats)[mTransaction.reminder]);
        if (mTransaction.reminder != FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER
                || mTransaction.repeat != FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER) {
            mButtonReminder.setEnabled(true);
        }
        Button mDelete = (Button) findViewById(R.id.delete);
        mDelete.setOnClickListener(this);
        mAmountEdit = (CurrencyEditText) findViewById(R.id.amount);
        mCategoriesGrid = (CategoryGridView) findViewById(R.id.categories);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.transaction, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mTransaction.type.equals(FinanceContract.Transactions.TRANSACTION_TYPE_INCOME)) {
            menu.findItem(R.id.action_type).setTitle(getResources().getString(R.string.income));
        } else {
            menu.findItem(R.id.action_type).setTitle(getResources().getString(R.string.expense));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_submit) {
            if (mAmountEdit.getCurrencyValue() == 0) {
                Toast.makeText(this, R.string.toast_no_amount, Toast.LENGTH_LONG).show();
                return true;
            }
            if (mCategoriesGrid.getSelection().equals("")) {
                Toast.makeText(this, R.string.toast_no_category, Toast.LENGTH_LONG).show();
                return true;
            }
            // INSERT INTO DB
            mTransaction.amount = mAmountEdit.getCurrencyValue();
            mTransaction.category = mCategoriesGrid.getSelection();
            if (mTransaction.repeat == FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER) {
                getContentResolver().update(updateUri, mTransaction.create(), null, null);
            } else {
                String root = mTransaction.id;
                if (mTransaction.root != null) {
                    root = mTransaction.root;
                }
                SelectionBuilder builder = new SelectionBuilder();
                builder.where(FinanceContract.Transactions.TRANSACTION_DATE + ">=?", mTransaction.date+"");
                builder.where(FinanceContract.Transactions.TRANSACTION_ROOT + "=? OR "
                        + FinanceContract.Transactions.TRANSACTION_ID + "=?", root, root);
                ContentValues values = new ContentValues();
                // Right now, we do not support full editing of repeating transactions.
                // Editing is limited to amount, category, type and reminder.
                values.put(FinanceContract.Transactions.TRANSACTION_AMOUNT, mTransaction.amount);
                values.put(FinanceContract.Transactions.CATEGORY_ID, mTransaction.category);
                values.put(FinanceContract.Transactions.TRANSACTION_TYPE, mTransaction.type);
                values.put(FinanceContract.Transactions.TRANSACTION_REMINDER, mTransaction.reminder);
                values.put(FinanceContract.Transactions.TRANSACTION_ROOT, mTransaction.id);
                getContentResolver().update(FinanceContract.Transactions.CONTENT_URI, values, builder.getSelection(), builder.getSelectionArgs());
            }
            this.finish();
            return true;
        }
        if (id == R.id.action_type) {
            mCategoriesGrid.setSelection("");
            mGridAdapter.setCategoty("");
            if (mTransaction.type.equals(FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE)) {
                mTransaction.type = FinanceContract.Transactions.TRANSACTION_TYPE_INCOME;
                item.setTitle(getResources().getString(R.string.income));
            } else {
                mTransaction.type = FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE;
                item.setTitle(getResources().getString(R.string.expense));
            }
            getLoaderManager().restartLoader(CategoryQuery._TOKEN, null, this);
        }
        return super.onOptionsItemSelected(item);
    }

    public void openDatePicker(View v) {
        DialogFragment datePicker = new DatePickerFragment();
        Bundle args = new Bundle();
        args.putLong("timestamp", mTransaction.date);
        datePicker.setArguments(args);
        datePicker.show(getFragmentManager(), "datePicker");
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar c = DateUtils.getClearCalendar();
        c.set(year, month, day);
        mTransaction.date = c.getTimeInMillis();
        mButtonDate.setText(DateUtils.getFormattedDate(mTransaction.date, "dd/MM/yyyy"));
    }

    @Override
    public void onDelete(int id, boolean options) {
        if (options && id == 1) {
            // delete all related transactions
            String root = mTransaction.id;
            if (mTransaction.root != null) {
                root = mTransaction.root;
            }
            SelectionBuilder builder = new SelectionBuilder();
            builder.where(FinanceContract.Transactions.TRANSACTION_ROOT + "=? OR "
                    + FinanceContract.Transactions.TRANSACTION_ID + "=?", root, root);
            getContentResolver().delete(FinanceContract.Transactions.CONTENT_URI, builder.getSelection(), builder.getSelectionArgs());
        } else {
            if (mTransaction.root != null) {
                // update last related transactions and
                // set repeat to "never"
                SelectionBuilder builder = new SelectionBuilder();
                builder.where(FinanceContract.Transactions.TRANSACTION_NEXT + "=?", mTransaction.id);
                ContentValues values = new ContentValues();
                values.put(FinanceContract.Transactions.TRANSACTION_REPEAT, FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER);
                getContentResolver().update(FinanceContract.Transactions.CONTENT_URI, values, builder.getSelection(), builder.getSelectionArgs());
            }
            getContentResolver().delete(updateUri, null, null);
        }
        finish();
    }

    /**
     * On delete button click
     *
     * @param view
     */
    @Override
    public void onClick(View view) {
        DialogFragment deleteDialog = new TransactionDeleteDialogFragment();
        if (mTransaction.next != null || mTransaction.root != null) {
            Bundle args = new Bundle();
            args.putBoolean(TransactionDeleteDialogFragment.SHOW_OPTIONS, true);
            deleteDialog.setArguments(args);
        }
        deleteDialog.show(getFragmentManager(), "deleteDialog");
    }

    public static class DatePickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            Calendar c = Calendar.getInstance();
            if (args != null) {
                c.setTimeInMillis(args.getLong("timestamp"));
            }
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity(), (EditTransactionActivity) getActivity(), year, month, day);
        }

    }

    public void openRepeatPicker(View v) {
        DialogFragment repeatPicker = new RepeatPickerFragment();
        repeatPicker.show(getFragmentManager(), "repeatPicker");
    }

    public void onRepeatSet(int selection) {
        mTransaction.repeat = selection;
        mButtonRepeating.setText(getResources().getStringArray(R.array.transaction_repeats)[selection]);
        if (selection == FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER) {
            mButtonReminder.setEnabled(false);
            mTransaction.reminder = selection;
            mButtonReminder.setText(getResources().getStringArray(R.array.transaction_reminders)[selection]);
        } else {
            mButtonReminder.setEnabled(true);
        }
    }

    public static class RepeatPickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.dialog_pick_repeat)
                    .setItems(R.array.transaction_repeats,  new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((EditTransactionActivity) getActivity()).onRepeatSet(which);
                        }
                    });
            return builder.create();
        }

    }

    public void openReminderPicker(View v) {
        DialogFragment reminderPicker = new ReminderPickerFragment();
        reminderPicker.show(getFragmentManager(), "reminderPicker");
    }

    public void onReminderSet(int selection) {
        mTransaction.reminder = selection;
        mButtonReminder.setText(getResources().getStringArray(R.array.transaction_reminders)[selection]);
    }

    public static class ReminderPickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.dialog_pick_reminder)
                    .setItems(R.array.transaction_reminders,  new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((EditTransactionActivity) getActivity()).onReminderSet(which);
                        }
                    });
            return builder.create();
        }

    }

    public void displayCategories() {
        mGridAdapter = new CategoryGridAdapter(this, null, 0, mTransaction.category);
        mCategoriesGrid.setAdapter(mGridAdapter);
        mCategoriesGrid.setSelection(mTransaction.category);
        getLoaderManager().restartLoader(CategoryQuery._TOKEN, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        String selection = FinanceContract.Categories.CATEGORY_TYPE + "=?";

        String type = FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE;
        if (mTransaction.type.equals(FinanceContract.Transactions.TRANSACTION_TYPE_INCOME)) {
            type = FinanceContract.Transactions.TRANSACTION_TYPE_INCOME;
        }
        String[] args = { type };

        return new CursorLoader(this,
                FinanceContract.Categories.CONTENT_URI, CategoryQuery.PROJECTION, selection, args, FinanceContract.Categories.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mGridAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mGridAdapter.swapCursor(null);
    }

    private interface CategoryQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                FinanceContract.Categories._ID,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Categories.CATEGORY_NAME
        };

        int _ID = 0;
        int CATEGORY_ID = 1;
        int NAME = 2;
    }

}
