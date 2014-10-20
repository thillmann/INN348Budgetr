package com.mad.qut.budgetr.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Budget;
import com.mad.qut.budgetr.model.Category;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.widget.NumPad;
import com.mad.qut.budgetr.utils.DateUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class EditBudgetActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = EditBudgetActivity.class.getSimpleName();

    private NumPad mNumPad;
    private EditText mNameEdit;
    private EditText mAmountEdit;
    private Spinner mTypesSpinner;
    private LinearLayout mStartDateField;
    private Spinner mStartDateSpinner;
    private GridLayout mCategoriesGrid;
    private Button mDeleteButton;

    private List<Button> mButtons = new ArrayList<Button>();

    private Budget mBudget = new Budget();

    private Uri updateUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_budget);

        mBudget = Budget.fromJSON(getIntent().getStringExtra("budget"));

        updateUri = getIntent().getParcelableExtra(FinanceContract.Budgets.CONTENT_ITEM_TYPE);

        assignViews();

        setEditAmount();

        populateBudgetTypes();

        getLoaderManager().restartLoader(CategoryQuery._TOKEN, null, this);
    }

    private void assignViews() {
        mNumPad = new NumPad(this, R.id.keyboard_view, R.xml.numpad);
        mNameEdit = (EditText) findViewById(R.id.name);
        mNameEdit.setText(mBudget.name);
        mAmountEdit = (EditText) findViewById(R.id.amount);
        mTypesSpinner = (Spinner) findViewById(R.id.type);
        mStartDateField = (LinearLayout) findViewById(R.id.start_date_wrapper);
        mStartDateSpinner = (Spinner) findViewById(R.id.start_date);
        mCategoriesGrid = (GridLayout) findViewById(R.id.categories);
        mDeleteButton = (Button) findViewById(R.id.delete);
    }

    private void populateBudgetTypes() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.budget_types, android.R.layout.simple_spinner_dropdown_item);
        mTypesSpinner.setAdapter(spinnerAdapter);
        mTypesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mBudget.type = i;
                toggleStartDateField(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mTypesSpinner.setSelection(mBudget.type);
    }

    private void toggleStartDateField(int i) {
        switch (i) {
            case FinanceContract.Budgets.BUDGET_TYPE_BIWEEKLY:
                mStartDateField.setVisibility(View.VISIBLE);
                populateStartDates();
                break;
            default:
                mStartDateField.setVisibility(View.GONE);
                break;
        }
    }

    private void populateStartDates() {
        final List<CharSequence> startingDates = new ArrayList<CharSequence>();
        Calendar curr = Calendar.getInstance();
        curr.setTimeInMillis(mBudget.startDate);
        startingDates.add(DateUtils.getFormattedDate(curr.getTime(), "dd/MM/yyyy"));
        Calendar c = DateUtils.getClearCalendar();
        // get last weeks start and next weeks start#
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        c.add(Calendar.DAY_OF_WEEK, 1);
        if (!c.equals(curr)) {
            startingDates.add(DateUtils.getFormattedDate(c.getTime(), "dd/MM/yyyy"));
        }
        c.add(Calendar.WEEK_OF_YEAR, 1);
        if (!c.equals(curr)) {
            startingDates.add(DateUtils.getFormattedDate(c.getTime(), "dd/MM/yyyy"));
        }
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, startingDates);
        mStartDateSpinner.setAdapter(spinnerAdapter);
        mStartDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mBudget.startDate = DateUtils.getTimeStampFromString(startingDates.get(i).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
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
            values.put(FinanceContract.Budgets.BUDGET_NAME, mNameEdit.getText().toString());
            values.put(FinanceContract.Budgets.BUDGET_AMOUNT, mBudget.amount);
            values.put(FinanceContract.Budgets.BUDGET_TYPE, mBudget.type);
            values.put(FinanceContract.Budgets.BUDGET_START_DATE, mBudget.startDate);
            values.put(FinanceContract.Budgets.CATEGORY_ID, mBudget.category);
            values.put(FinanceContract.Budgets.CURRENCY_ID, mBudget.currency);
            getContentResolver().update(updateUri, values, null, null);
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
                mAmountEdit.clearFocus();
            } else {
                this.finish();
            }
        }
    }

    public void onDeleteClick(View v) {
        // TODO: Delete
        getContentResolver().delete(updateUri, null, null);
        this.finish();
    }

    public void setEditAmount() {
        mAmountEdit.setInputType(0);
        mNumPad.registerEditText(mAmountEdit);

        String formatted = NumberFormat.getCurrencyInstance().format(mBudget.amount);
        mAmountEdit.setText(formatted);

        // TODO: Hide numpad when select start etc.

        mAmountEdit.addTextChangedListener(new TextWatcher() {
            private String current = mAmountEdit.getText().toString();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().equals(current)) {
                    mAmountEdit.removeTextChangedListener(this);

                    String cleanString = editable.toString().replaceAll("[$.,]", "");

                    double parsed = Double.parseDouble(cleanString);

                    mBudget.amount = parsed / 100;

                    // TODO: Format according to currency from settings
                    String formatted = NumberFormat.getCurrencyInstance().format(parsed / 100);

                    current = formatted;

                    mAmountEdit.setText(formatted);
                    mAmountEdit.setSelection(formatted.length());

                    mAmountEdit.addTextChangedListener(this);
                }
            }
        });
    }

    public void setCategories(Cursor data) {
        View.OnClickListener categoryOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < mButtons.size(); i++) {
                    mButtons.get(i).setBackgroundColor(Color.TRANSPARENT);
                }
                mBudget.category = view.getContentDescription() + "";
                view.setBackgroundColor(getResources().getColor(R.color.button_background_pressed));
            }
        };
        // TODO: Improve button style
        mCategoriesGrid.removeAllViews();
        data.moveToPosition(-1);
        if (data.getCount() > 0) {
            LinearLayout mWrapper = new LinearLayout(this);
            mWrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            while (data.moveToNext()) {
                if (data.getPosition() % 3 == 0 && data.getPosition() > 1) {
                    mCategoriesGrid.addView(mWrapper);
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
                if (mCategory.getContentDescription().equals(mBudget.category)) {
                    mCategory.setBackgroundColor(getResources().getColor(R.color.button_background_pressed));
                }
                mCategory.setOnClickListener(categoryOnClick);
                mButtons.add(mCategory);
                mWrapper.addView(mCategory);
            }
            mCategoriesGrid.addView(mWrapper);
            mCategoriesGrid.setRowCount(Math.round(data.getCount() / 3));
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
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Categories.CATEGORY_NAME
        };

        int CATEGORY_ID = 0;
        int NAME = 1;
    }

}
