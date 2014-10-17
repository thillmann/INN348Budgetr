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
import android.graphics.Color;
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
import com.mad.qut.budgetr.model.Budget;
import com.mad.qut.budgetr.model.Category;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.widget.NumPad;
import com.mad.qut.budgetr.utils.DateUtils;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.UUID;

public class AddBudgetActivity extends BaseActivity implements DatePickerDialog.OnDateSetListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = AddTransactionActivity.class.getSimpleName();

    private NumPad mNumPad;
    private EditText mEditName;
    private EditText mEditAmount;
    private Button mButtonDate;
    private Button mButtonRepeating;
    private GridLayout mCategories;

    private Budget mBudget = new Budget();

    private View.OnClickListener mCategoryOnClick = new View.OnClickListener() {
        private View last;

        @Override
        public void onClick(View view) {
            if (last != null) {
                last.setBackgroundColor(Color.TRANSPARENT);
            }
            mBudget.category = view.getContentDescription() + "";
            view.setBackgroundColor(getResources().getColor(R.color.button_background_pressed));
            last = view;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_budget);

        mNumPad = new NumPad(this, R.id.keyboard_view, R.xml.numpad);

        mEditAmount = (EditText) findViewById(R.id.amount);
        setEditAmount();

        mCategories = (GridLayout) findViewById(R.id.categories);

        mButtonDate = (Button) findViewById(R.id.button_date);

        mButtonRepeating = (Button) findViewById(R.id.button_repeating);

        // default values for transaction
        mBudget.amount = 0.0;
        mBudget.category = "";
        // TODO: Use currency from settings
        mBudget.currency = "aud";
        Calendar calendar = Calendar.getInstance();
        mBudget.start = calendar.getTime().getTime() / 1000;
        mBudget.repeat = FinanceContract.Transactions.TRANSACTION_REPEAT_NEVER;

        // default values for inputs
        mButtonDate.setText(DateUtils.getFormattedDate(mBudget.start, "dd/MM/yyyy"));

        getLoaderManager().restartLoader(CategoryQuery._TOKEN, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_budget, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_submit) {
            if (mBudget.category.equals("")) {
                Toast.makeText(this, R.string.no_category, Toast.LENGTH_LONG).show();
                return true;
            }
            if (mBudget.amount == 0) {
                Toast.makeText(this, R.string.no_amount, Toast.LENGTH_LONG).show();
                return true;
            }
            // INSERT INTO DB
            ContentValues values = new ContentValues();
            values.put(FinanceContract.Budgets.BUDGET_ID, UUID.randomUUID().toString());
            values.put(FinanceContract.Budgets.BUDGET_NAME, mBudget.name);
            values.put(FinanceContract.Budgets.BUDGET_AMOUNT, mBudget.amount);
            values.put(FinanceContract.Budgets.BUDGET_START, mBudget.start);
            values.put(FinanceContract.Budgets.BUDGET_REPEAT, mBudget.repeat);
            values.put(FinanceContract.Budgets.CATEGORY_ID, mBudget.category);
            values.put(FinanceContract.Budgets.CURRENCY_ID, mBudget.currency);
            getContentResolver().insert(FinanceContract.Budgets.CONTENT_URI, values);
            this.finish();
            return true;
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

                    mBudget.amount = parsed/100;

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
                mCategory.setOnClickListener(mCategoryOnClick);
                mWrapper.addView(mCategory);
            }
            mCategories.addView(mWrapper);
            mCategories.setRowCount(Math.round(data.getCount() / 3));
        }
    }

    public void openDatePicker(View v) {
        DialogFragment datePicker = new DatePickerFragment();
        datePicker.show(getFragmentManager(), "datePicker");
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day);
        mBudget.start = c.getTime().getTime() / 1000;
        mButtonDate.setText(DateUtils.getFormattedDate(mBudget.start, "dd/MM/yyyy"));
    }

    public static class DatePickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity(), (AddBudgetActivity) getActivity(), year, month, day);
        }

    }

    public void openRepeatPicker(View v) {
        DialogFragment repeatPicker = new RepeatPickerFragment();
        repeatPicker.show(getFragmentManager(), "repeatPicker");
    }

    public void onRepeatSet(int selection) {
        mBudget.repeat = getResources().getStringArray(R.array.repeats_alias)[selection];
        mButtonRepeating.setText(getResources().getStringArray(R.array.repeats)[selection]);
    }

    public static class RepeatPickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.dialog_pick_repeat)
                    .setItems(R.array.repeats,  new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((AddBudgetActivity) getActivity()).onRepeatSet(which);
                        }
                    });
            return builder.create();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        String selection = FinanceContract.Categories.CATEGORY_TYPE + "=?";
        String[] args = { FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE };

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
