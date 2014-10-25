package com.mad.qut.budgetr.ui;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Budget;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.widget.CategoryGridAdapter;
import com.mad.qut.budgetr.ui.widget.CategoryGridView;
import com.mad.qut.budgetr.ui.widget.CurrencyEditText;
import com.mad.qut.budgetr.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class AddBudgetActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = AddBudgetActivity.class.getSimpleName();

    private EditText mNameEdit;
    private CurrencyEditText mAmountEdit;
    private Spinner mTypesSpinner;
    private LinearLayout mStartDateField;
    private Spinner mStartDateSpinner;
    private CategoryGridView mCategoriesGrid;

    private CategoryGridAdapter mGridAdapter;

    private Budget mBudget = new Budget();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_budget);

        assignViews();

        setDefaultValues();

        populateBudgetTypes();

        displayCategories();
    }

    private void assignViews() {
        mNameEdit = (EditText) findViewById(R.id.name);
        mAmountEdit = (CurrencyEditText) findViewById(R.id.amount);
        mTypesSpinner = (Spinner) findViewById(R.id.type);
        mStartDateField = (LinearLayout) findViewById(R.id.start_date_wrapper);
        mStartDateSpinner = (Spinner) findViewById(R.id.start_date);
        mCategoriesGrid = (CategoryGridView) findViewById(R.id.categories);
    }

    private void setDefaultValues() {
        mBudget.amount = 0.0;
        mBudget.type = 0;
        mBudget.startDate = -1;
        mBudget.category = "";
        // TODO: Use currency from settings
        mBudget.currency = "aud";
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
    }

    private void toggleStartDateField(int i) {
        switch (i) {
            case FinanceContract.Budgets.BUDGET_TYPE_BIWEEKLY:
                mStartDateField.setVisibility(View.VISIBLE);
                Animation slideDownInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_in);
                mStartDateField.startAnimation(slideDownInAnimation);
                populateStartDates();
                break;
            default:
                mStartDateField.setVisibility(View.GONE);
                break;
        }
    }

    private void populateStartDates() {
        final List<CharSequence> startingDates = new ArrayList<CharSequence>();
        Calendar c = DateUtils.getClearCalendar();
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        c.add(Calendar.DAY_OF_WEEK, 1);
        startingDates.add(DateUtils.getFormattedDate(c.getTime(), "dd/MM/yyyy"));
        c.add(Calendar.WEEK_OF_YEAR, 1);
        startingDates.add(DateUtils.getFormattedDate(c.getTime(), "dd/MM/yyyy"));
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
        getMenuInflater().inflate(R.menu.add_budget, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_submit) {
            if (mNameEdit.getText().toString().equals("")) {
                Toast.makeText(this, R.string.no_name, Toast.LENGTH_LONG).show();
                return true;
            }
            if (mAmountEdit.getCurrencyValue() == 0) {
                Toast.makeText(this, R.string.no_amount, Toast.LENGTH_LONG).show();
                return true;
            }
            if (mCategoriesGrid.getSelection().equals("")) {
                Toast.makeText(this, R.string.no_category, Toast.LENGTH_LONG).show();
                return true;
            }
            // INSERT INTO DB
            ContentValues values = new ContentValues();
            values.put(FinanceContract.Budgets.BUDGET_ID, UUID.randomUUID().toString());
            values.put(FinanceContract.Budgets.BUDGET_NAME, mNameEdit.getText().toString());
            values.put(FinanceContract.Budgets.BUDGET_AMOUNT, mAmountEdit.getCurrencyValue());
            values.put(FinanceContract.Budgets.BUDGET_TYPE, mBudget.type);
            values.put(FinanceContract.Budgets.BUDGET_START_DATE, mBudget.startDate);
            values.put(FinanceContract.Budgets.CATEGORY_ID, mCategoriesGrid.getSelection());
            values.put(FinanceContract.Budgets.CURRENCY_ID, mBudget.currency);
            getContentResolver().insert(FinanceContract.Budgets.CONTENT_URI, values);
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void displayCategories() {
        mGridAdapter = new CategoryGridAdapter(this, null, 0);
        mCategoriesGrid.setAdapter(mGridAdapter);
        getLoaderManager().restartLoader(CategoryQuery._TOKEN, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        String selection = FinanceContract.Categories.CATEGORY_TYPE + "=?";
        String[] args = { FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE };

        return new CursorLoader(this,
                FinanceContract.Categories.CONTENT_URI, CategoryQuery.PROJECTION, selection, args, FinanceContract.Categories.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mGridAdapter.swapCursor(cursor);
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
