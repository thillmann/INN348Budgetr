package com.mad.qut.budgetr.ui;

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
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Transaction;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.widget.CategoryGridAdapter;
import com.mad.qut.budgetr.ui.widget.CategoryGridView;
import com.mad.qut.budgetr.ui.widget.CurrencyEditText;
import com.mad.qut.budgetr.utils.DateUtils;

import java.util.Calendar;
import java.util.UUID;

public class AddTransactionActivity extends BaseActivity implements DatePickerDialog.OnDateSetListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = AddTransactionActivity.class.getSimpleName();

    private CurrencyEditText mAmountEdit;
    private Button mButtonDate;
    private Button mButtonRepeating;
    private Button mButtonReminder;
    private CategoryGridView mCategoriesGrid;

    private CategoryGridAdapter mGridAdapter;

    private Transaction mTransaction = new Transaction();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        assignViews();

        setDefaultValues();

        displayCategories();
    }

    private void assignViews() {
        mAmountEdit = (CurrencyEditText) findViewById(R.id.amount);
        mButtonDate = (Button) findViewById(R.id.button_date);
        mButtonRepeating = (Button) findViewById(R.id.button_repeating);
        mButtonReminder = (Button) findViewById(R.id.button_reminder);
        Button mDelete = (Button) findViewById(R.id.delete);
        mDelete.setVisibility(View.GONE);
        mAmountEdit = (CurrencyEditText) findViewById(R.id.amount);
        mCategoriesGrid = (CategoryGridView) findViewById(R.id.categories);
    }

    private void setDefaultValues() {
        // default values for transaction
        mTransaction.amount = 0.0;
        mTransaction.category = "";
        mTransaction.type = FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE;
        Calendar calendar = DateUtils.getClearCalendar();
        mTransaction.date = calendar.getTime().getTime();
        mTransaction.repeat = FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER;
        mTransaction.reminder = FinanceContract.Transactions.TRANSACTION_REMINDER_NEVER;

        // default values for inputs
        mButtonDate.setText(DateUtils.getFormattedDate(mTransaction.date, "dd/MM/yyyy"));
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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
            ContentValues values = new ContentValues();
            values.put(FinanceContract.Transactions.TRANSACTION_ID, UUID.randomUUID().toString());
            values.put(FinanceContract.Transactions.TRANSACTION_AMOUNT, mAmountEdit.getCurrencyValue());
            values.put(FinanceContract.Transactions.TRANSACTION_DATE, mTransaction.date);
            values.put(FinanceContract.Transactions.TRANSACTION_REPEAT, mTransaction.repeat);
            values.put(FinanceContract.Transactions.TRANSACTION_REMINDER, mTransaction.reminder);
            values.put(FinanceContract.Transactions.TRANSACTION_TYPE, mTransaction.type);
            values.put(FinanceContract.Transactions.CATEGORY_ID, mCategoriesGrid.getSelection());
            getContentResolver().insert(FinanceContract.Transactions.CONTENT_URI, values);
            // Notify changes for budgets
            getContentResolver().notifyChange(FinanceContract.Budgets.CONTENT_URI, null);

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
        mTransaction.date = c.getTime().getTime();
        mButtonDate.setText(DateUtils.getFormattedDate(mTransaction.date, "dd/MM/yyyy"));
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

            return new DatePickerDialog(getActivity(), (AddTransactionActivity) getActivity(), year, month, day);
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
            mTransaction.reminder = FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER;
            mButtonReminder.setText(getResources().getStringArray(R.array.transaction_reminders)[mTransaction.reminder]);
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
                            ((AddTransactionActivity) getActivity()).onRepeatSet(which);
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
                            ((AddTransactionActivity) getActivity()).onReminderSet(which);
                        }
                    });
            return builder.create();
        }

    }

    public void displayCategories() {
        mGridAdapter = new CategoryGridAdapter(this, null, 0);
        mCategoriesGrid.setAdapter(mGridAdapter);
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
                BaseColumns._ID,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Categories.CATEGORY_NAME
        };

        int _ID = 0;
        int CATEGORY_ID = 1;
        int NAME = 2;
    }

}
