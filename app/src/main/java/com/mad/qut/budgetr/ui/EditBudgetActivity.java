package com.mad.qut.budgetr.ui;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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

public class EditBudgetActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = EditBudgetActivity.class.getSimpleName();

    private EditText mNameEdit;
    private CurrencyEditText mAmountEdit;
    private Spinner mTypesSpinner;
    private LinearLayout mStartDateField;
    private Spinner mStartDateSpinner;
    private CategoryGridView mCategoriesGrid;

    private CategoryGridAdapter mGridAdapter;

    private Budget mBudget = new Budget();

    private Uri updateUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_budget);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mBudget = Budget.fromJSON(getIntent().getStringExtra("budget"));

        updateUri = getIntent().getParcelableExtra(FinanceContract.Budgets.CONTENT_ITEM_TYPE);

        assignViews();

        populateBudgetTypes();

        displayCategories();
    }

    private void assignViews() {
        mNameEdit = (EditText) findViewById(R.id.name);
        mNameEdit.setText(mBudget.name);
        mAmountEdit = (CurrencyEditText) findViewById(R.id.amount);
        mAmountEdit.setText(mBudget.amount*10+"");
        mTypesSpinner = (Spinner) findViewById(R.id.type);
        mStartDateField = (LinearLayout) findViewById(R.id.start_date_wrapper);
        mStartDateSpinner = (Spinner) findViewById(R.id.start_date);
        mCategoriesGrid = (CategoryGridView) findViewById(R.id.categories);
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
        Calendar curr = Calendar.getInstance();
        if (mBudget.startDate > 0) {
            curr.setTimeInMillis(mBudget.startDate);
            startingDates.add(DateUtils.getFormattedDate(curr.getTime(), "dd/MM/yyyy"));
        }
        Calendar c = DateUtils.getClearCalendar();
        // get last weeks start and next weeks start#
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        c.add(Calendar.DAY_OF_WEEK, 1);
        if (mBudget.startDate < 1 || !c.equals(curr)) {
            startingDates.add(DateUtils.getFormattedDate(c.getTime(), "dd/MM/yyyy"));
        }
        c.add(Calendar.WEEK_OF_YEAR, 1);
        if (mBudget.startDate < 1 || !c.equals(curr)) {
            startingDates.add(DateUtils.getFormattedDate(c.getTime(), "dd/MM/yyyy"));
        }
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, startingDates);
        mStartDateSpinner.setAdapter(spinnerAdapter);
        mStartDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mBudget.startDate = DateUtils.getTimeStampFromString(startingDates.get(i).toString(), "dd/MM/yyyy");
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
            if (mNameEdit.getText().toString().equals("")) {
                Toast.makeText(this, R.string.toast_no_name, Toast.LENGTH_LONG).show();
                return true;
            }
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
            values.put(FinanceContract.Budgets.BUDGET_ID, UUID.randomUUID().toString());
            values.put(FinanceContract.Budgets.BUDGET_NAME, mNameEdit.getText().toString());
            values.put(FinanceContract.Budgets.BUDGET_AMOUNT, mAmountEdit.getCurrencyValue());
            values.put(FinanceContract.Budgets.BUDGET_TYPE, mBudget.type);
            values.put(FinanceContract.Budgets.BUDGET_START_DATE, mBudget.startDate);
            values.put(FinanceContract.Budgets.CATEGORY_ID, mCategoriesGrid.getSelection());
            getContentResolver().update(updateUri, values, null, null);
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onDeleteClick(View v) {
        // TODO: Delete
        getContentResolver().delete(updateUri, null, null);
        this.finish();
    }

    public void displayCategories() {
        mGridAdapter = new CategoryGridAdapter(this, null, 0, mBudget.category);
        mCategoriesGrid.setAdapter(mGridAdapter);
        mCategoriesGrid.setSelection(mBudget.category);
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
