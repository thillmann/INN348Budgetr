package com.mad.qut.budgetr.ui;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Budget;
import com.mad.qut.budgetr.model.Category;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.utils.NumberUtils;
import com.mad.qut.budgetr.utils.SelectionBuilder;

import java.util.ArrayList;

public class BudgetDetailActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private Budget mBudget;
    private Uri updateUri;

    private TextView tvPeriod;
    private ImageView ivCategoryIcon;
    private TextView tvSpent;
    private TextView tvAverageSpent;
    private TextView tvAverageLeft;
    private TextView tvLeft;
    private PieChart mBudgetChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        mBudget = Budget.fromJSON(getIntent().getStringExtra("budget"));

        updateUri = getIntent().getParcelableExtra(FinanceContract.Budgets.CONTENT_ITEM_TYPE);

        assignViews();

        setupBudgetChart();
    }

    private void setupBudgetChart() {
        mBudgetChart.setDescription("");
        mBudgetChart.setCenterText(NumberUtils.getFormattedCurrency(mBudget.amount));
        mBudgetChart.setCenterTextSize(16f);
        mBudgetChart.setDrawCenterText(true);
        mBudgetChart.setDrawYValues(true);
        mBudgetChart.setDrawXValues(false);
        mBudgetChart.setUsePercentValues(true);
        mBudgetChart.setValueTextSize(14f);
        mBudgetChart.setValueTextColor(getResources().getColor(R.color.body_text_1_inverse));
        mBudgetChart.setDrawLegend(false);
        mBudgetChart.setTouchEnabled(false);

        getLoaderManager().initLoader(TransactionQuery._TOKEN, null, this);
    }

    private void assignViews() {
        ((LinearLayout) findViewById(R.id.header)).setBackgroundResource(Category.getColor(mBudget.category));
        setTitle(mBudget.name);
        tvPeriod = (TextView) findViewById(R.id.period);
        tvPeriod.setText(mBudget.getCurrentPeriod());
        ivCategoryIcon = (ImageView) findViewById(R.id.category_icon);
        ivCategoryIcon.setImageResource(Category.getIcon(mBudget.category, Category.ICON_SMALL));
        tvSpent = (TextView) findViewById(R.id.spent_value);
        tvAverageSpent = (TextView) findViewById(R.id.avgerage_spent);
        tvLeft = (TextView) findViewById(R.id.left_value);
        tvAverageLeft = (TextView) findViewById(R.id.average_left);
        mBudgetChart = (PieChart) findViewById(R.id.pie_chart);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.budget, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            Intent iBudget = new Intent();
            iBudget.setClass(this, EditBudgetActivity.class);
            iBudget.putExtra("budget", mBudget.toJSON());
            iBudget.putExtra(FinanceContract.Budgets.CONTENT_ITEM_TYPE, updateUri);
            startActivity(iBudget);
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        SelectionBuilder builder = new SelectionBuilder();
        if (mBudget.type != FinanceContract.Budgets.BUDGET_TYPE_ENDLESS) {
            builder.where(FinanceContract.Transactions.IN_TIME_INTERVAL_SELECTION,
                    FinanceContract.Transactions.buildInTimeIntervalArgs(mBudget.getCurrentStartDate(), mBudget.getCurrentEndDate()));
        }
        return new CursorLoader(this, FinanceContract.Budgets.buildBudgetTransactionsUri(updateUri.getLastPathSegment()),
                TransactionQuery.PROJECTION,
                builder.getSelection(),
                builder.getSelectionArgs(),
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor.getCount() > 0) {
            mBudgetChart.setVisibility(View.VISIBLE);
            Animation scaleInAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_in);
            mBudgetChart.startAnimation(scaleInAnimation);

            ArrayList<String> xVals = new ArrayList<String>();
            ArrayList<Entry> yVals = new ArrayList<Entry>();
            ArrayList<Integer> colors = new ArrayList<Integer>();
            cursor.moveToFirst();
            float amountSpent = cursor.getFloat(TransactionQuery.SUM);

            xVals.add("Spent");
            yVals.add(new Entry(amountSpent, 0));
            colors.add(getResources().getColor(R.color.expense));
            xVals.add("Remaining");
            yVals.add(new Entry((float) mBudget.getAmountLeft(amountSpent), 1));
            colors.add(getResources().getColor(R.color.income));

            PieDataSet set = new PieDataSet(yVals, "");
            set.setSliceSpace(3f);
            set.setColors(colors);

            PieData data = new PieData(xVals, set);
            mBudgetChart.setData(data);
            mBudgetChart.invalidate();

            tvSpent.setText(NumberUtils.getFormattedCurrency(amountSpent));
            tvAverageSpent.setText(getResources().getQuantityString(R.plurals.average_spent, 1, NumberUtils.getFormattedCurrency(mBudget.getAverageSpent(amountSpent))));
            tvLeft.setText(NumberUtils.getFormattedCurrency(mBudget.getAmountLeft(amountSpent)));
            tvAverageLeft.setText(getResources().getQuantityString(R.plurals.average_left, 1, NumberUtils.getFormattedCurrency(mBudget.getAverageLeft(amountSpent))));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        resetCharts();
    }

    private void resetCharts() {
        mBudgetChart.setVisibility(View.GONE);

        tvSpent.setText(NumberUtils.getFormattedCurrency(0));
        tvAverageSpent.setText(getResources().getQuantityString(R.plurals.average_spent, 0, 0));
    }

    private interface TransactionQuery {
        int _TOKEN = 0x2;

        String[] PROJECTION = {
                "COUNT(*)",
                "SUM(" + FinanceContract.Transactions.TRANSACTION_AMOUNT + ")",
                FinanceContract.Budgets.BUDGET_ID
        };

        int COUNT = 0;
        int SUM = 1;
        int BUDGET_ID = 2;
    }

}
