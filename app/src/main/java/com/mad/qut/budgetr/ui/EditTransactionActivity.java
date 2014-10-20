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
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Category;
import com.mad.qut.budgetr.model.Transaction;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.widget.NumPad;
import com.mad.qut.budgetr.utils.DateUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EditTransactionActivity extends BaseActivity implements DatePickerDialog.OnDateSetListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = EditTransactionActivity.class.getSimpleName();

    private NumPad mNumPad;
    private EditText mEditAmount;
    private Button mButtonDate;
    private Button mButtonRepeating;
    private Button mButtonReminder;
    private GridLayout mCategories;
    private Transaction mTransaction;
    private View.OnClickListener mCategoryOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            for (int i = 0; i < mButtons.size(); i++) {
                mButtons.get(i).setBackgroundColor(Color.TRANSPARENT);
            }
            mTransaction.category = view.getContentDescription() + "";
            view.setBackgroundColor(getResources().getColor(R.color.button_background_pressed));
        }
    };
    private List<Button> mButtons = new ArrayList<Button>();
    private Uri updateUri;
    private Activity mActivity = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        mTransaction = Transaction.fromJSON(getIntent().getStringExtra("transaction"));

        updateUri = getIntent().getParcelableExtra(FinanceContract.Transactions.CONTENT_ITEM_TYPE);

        mNumPad = new NumPad(this, R.id.keyboard_view, R.xml.numpad);

        mEditAmount = (EditText) findViewById(R.id.amount);
        setEditAmount();

        mCategories = (GridLayout) findViewById(R.id.categories);

        mButtonDate = (Button) findViewById(R.id.button_date);
        mButtonDate.setText(DateUtils.getFormattedDate(mTransaction.date, "dd/MM/yyyy"));

        mButtonRepeating = (Button) findViewById(R.id.button_repeating);
        // TODO: get real string from array
        mButtonRepeating.setText(mTransaction.repeat);

        mButtonReminder = (Button) findViewById(R.id.button_reminder);
        // TODO: get real string from array
        mButtonReminder.setText(mTransaction.reminder);

        Button mDelete = (Button) findViewById(R.id.delete);
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Delete
                getContentResolver().delete(updateUri, null, null);
                mActivity.finish();
            }
        });

        getLoaderManager().restartLoader(CategoryQuery._TOKEN, null, this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.transaction, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setType(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_submit) {
            if (mTransaction.category.equals("")) {
                Toast.makeText(this, R.string.no_category, Toast.LENGTH_LONG).show();
                return true;
            }
            if (mTransaction.amount == 0) {
                Toast.makeText(this, R.string.no_amount, Toast.LENGTH_LONG).show();
                return true;
            }
            // INSERT INTO DB
            ContentValues values = new ContentValues();
            values.put(FinanceContract.Transactions.TRANSACTION_ID, mTransaction.id);
            values.put(FinanceContract.Transactions.TRANSACTION_AMOUNT, mTransaction.amount);
            values.put(FinanceContract.Transactions.TRANSACTION_DATE, mTransaction.date);
            values.put(FinanceContract.Transactions.TRANSACTION_REPEAT, mTransaction.repeat);
            values.put(FinanceContract.Transactions.TRANSACTION_REMINDER, mTransaction.reminder);
            values.put(FinanceContract.Transactions.TRANSACTION_TYPE, mTransaction.type);
            values.put(FinanceContract.Transactions.CATEGORY_ID, mTransaction.category);
            values.put(FinanceContract.Transactions.CURRENCY_ID, mTransaction.currency);
            getContentResolver().update(updateUri, values, null, null);
            this.finish();
            return true;
        }
        if (id == R.id.action_type) {
            toggleType(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isActionBarBack()) {
            this.finish();
        } else {
            if (mNumPad.isNumPadVisible()) {
                mNumPad.hideNumPad();
                mEditAmount.clearFocus();
            } else {
                this.finish();
            }
        }
    }

    public void setEditAmount() {
        mEditAmount.setInputType(0);
        mNumPad.registerEditText(mEditAmount);

        String formatted = NumberFormat.getCurrencyInstance().format(mTransaction.amount);
        mEditAmount.setText(formatted);

        // TODO: Hide numpad when select start etc.

        mEditAmount.addTextChangedListener(new TextWatcher() {
            private String current = mEditAmount.getText().toString();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().equals(current)) {
                    mEditAmount.removeTextChangedListener(this);

                    String cleanString = editable.toString().replaceAll("[$.,]", "");

                    double parsed = Double.parseDouble(cleanString);

                    mTransaction.amount = parsed/100;

                    // TODO: Format according to currency from settings
                    String formatted = NumberFormat.getCurrencyInstance().format(parsed/100);

                    current = formatted;

                    mEditAmount.setText(formatted);
                    mEditAmount.setSelection(formatted.length());

                    mEditAmount.addTextChangedListener(this);
                }
            }
        });
    }

    public void setCategories(Cursor data) {
        // TODO: Improve button style
        mCategories.removeAllViews();
        data.moveToPosition(-1);
        if (data.getCount() > 0) {
            LinearLayout mWrapper = new LinearLayout(this);
            mWrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            while (data.moveToNext()) {
                if (data.getPosition() % 3 == 0 && data.getPosition() > 1) {
                    mCategories.addView(mWrapper);
                    mWrapper = new LinearLayout(this);
                    mWrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                }
                Button mCategory = new Button(this);
                mCategory.setContentDescription(data.getString(CategoryQuery.CATEGORY_ID));
                mCategory.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                mCategory.setTextAppearance(this, R.style.ButtonStyle);
                mCategory.setBackgroundResource(R.drawable.button);
                mCategory.setText(data.getString(CategoryQuery.NAME));
                mCategory.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                mCategory.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(Category.getIcon(data.getString(CategoryQuery.CATEGORY_ID))), null, null);
                mCategory.setCompoundDrawablePadding(16);
                if (mCategory.getContentDescription().equals(mTransaction.category)) {
                    mCategory.setBackgroundColor(getResources().getColor(R.color.button_background_pressed));
                }
                mCategory.setOnClickListener(mCategoryOnClick);
                mButtons.add(mCategory);
                mWrapper.addView(mCategory);
            }
            mCategories.addView(mWrapper);
            mCategories.setRowCount(Math.round(data.getCount() / 3));
        }
    }

    private void setType(MenuItem item) {
        if (mTransaction.type.equals(FinanceContract.Transactions.TRANSACTION_TYPE_INCOME)) {
            item.setTitle(getResources().getString(R.string.income));
        } else {
            item.setTitle(getResources().getString(R.string.expense));
        }
    }

    private void setType(Menu menu) {
        if (mTransaction.type.equals(FinanceContract.Transactions.TRANSACTION_TYPE_INCOME)) {
            menu.findItem(R.id.action_type).setTitle(getResources().getString(R.string.income));
        } else {
            menu.findItem(R.id.action_type).setTitle(getResources().getString(R.string.expense));
        }
    }

    private void toggleType(MenuItem item) {
        mTransaction.category = "";
        if (mTransaction.type.equals(FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE)) {
            mTransaction.type = FinanceContract.Transactions.TRANSACTION_TYPE_INCOME;
        } else {
            mTransaction.type = FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE;
        }
        setType(item);
        getLoaderManager().restartLoader(CategoryQuery._TOKEN, null, this);
    }

    public void openDatePicker(View v) {
        DialogFragment datePicker = new DatePickerFragment();
        datePicker.show(getFragmentManager(), "datePicker");
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day);
        mTransaction.date = c.getTime().getTime() / 1000;
        mButtonDate.setText(DateUtils.getFormattedDate(mTransaction.date, "dd/MM/yyyy"));
    }

    public static class DatePickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
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
        mTransaction.repeat = getResources().getStringArray(R.array.repeats_alias)[selection];
        mButtonRepeating.setText(getResources().getStringArray(R.array.repeats)[selection]);
    }

    public static class RepeatPickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.dialog_pick_repeat)
                    .setItems(R.array.repeats,  new DialogInterface.OnClickListener() {
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
        mTransaction.reminder = getResources().getStringArray(R.array.reminders_alias)[selection];
        mButtonReminder.setText(getResources().getStringArray(R.array.reminders)[selection]);
    }

    public static class ReminderPickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.dialog_pick_reminder)
                    .setItems(R.array.reminders,  new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((AddTransactionActivity) getActivity()).onReminderSet(which);
                        }
                    });
            return builder.create();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        String selection = FinanceContract.Categories.CATEGORY_TYPE + "=?";
        String[] args = { mTransaction.type };

        return new CursorLoader(this,
                FinanceContract.Categories.CONTENT_URI, CategoryQuery.PROJECTION, selection, args, FinanceContract.Categories.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        setCategories(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.

    }

    private interface CategoryQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Categories.CATEGORY_NAME,
                FinanceContract.Categories.CATEGORY_TYPE
        };

        int _ID = 0;
        int CATEGORY_ID = 1;
        int NAME = 2;
        int TYPE = 3;
    }

}
