package com.mad.qut.budgetr.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Category;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.SelectionBuilder;

import java.util.ArrayList;
import java.util.Calendar;

public class OverviewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = OverviewFragment.class.getSimpleName();

    private LineChart mLineChart;
    private PieChart mPieChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        mLineChart = (LineChart) view.findViewById(R.id.line_chart);
        setupLineChart();

        mPieChart = (PieChart) view.findViewById(R.id.pie_chart);
        setupPieChart();

        return view;
    }

    private void setupLineChart() {
        mLineChart.setDescription("");
        mLineChart.setDrawYValues(false);
        mLineChart.setHighlightEnabled(true);
        mLineChart.setDrawBorder(true);
        mLineChart.setBorderPositions(new BarLineChartBase.BorderPosition[]{
                BarLineChartBase.BorderPosition.BOTTOM
        });
        mLineChart.setUnit(" A$");
        mLineChart.setDrawUnitsInChart(true);
        mLineChart.setLongClickable(false);

        getLoaderManager().initLoader(LineChartQuery._TOKEN, null, this);
    }

    private void setupLineChartData(Cursor cursor) {
        if (cursor.getCount() > 0) {
            ArrayList<String> xVals = new ArrayList<String>();
            Calendar c = Calendar.getInstance();
            for (int i = 0; i < c.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
                c.set(Calendar.DAY_OF_MONTH, i+1);
                xVals.add(DateUtils.getFormattedDate(c.getTime(), "dd/MM/yyyy")); //+ "/" + c.get(Calendar.MONTH)  + "/" + c.get(Calendar.YEAR)
            }

            ArrayList<Entry> valsIncome = new ArrayList<Entry>();
            ArrayList<Entry> valsExpenses = new ArrayList<Entry>();

            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                String date = cursor.getString(LineChartQuery.DATE);
                Entry entry = new Entry(cursor.getFloat(LineChartQuery.TOTAL_AMOUNT), xVals.indexOf(date));
                if (cursor.getString(LineChartQuery.TYPE).equals(FinanceContract.Transactions.TRANSACTION_TYPE_INCOME)) {
                    valsIncome.add(entry);
                } else {
                    valsExpenses.add(entry);
                }
                Log.d(TAG, "Day: " + date);
                Log.d(TAG, "Amount: " + cursor.getFloat(LineChartQuery.TOTAL_AMOUNT));
                Log.d(TAG, "Type: " + cursor.getString(LineChartQuery.TYPE));
            }

            ArrayList<Entry> newValsIncome = new ArrayList<Entry>();
            ArrayList<Entry> newValsExpenses = new ArrayList<Entry>();

            // add missing
            int j = 0;
            int k = 0;
            for (int i = 0; i < xVals.size(); i++) {
                if (j < valsIncome.size()) {
                    Entry income = valsIncome.get(j);
                    if (income.getXIndex() != i) {
                        Entry newIncome = new Entry(0f, i);
                        newValsIncome.add(newIncome);
                    } else {
                        newValsIncome.add(income);
                        j++;
                    }
                } else {
                    Entry newIncome = new Entry(0f, i);
                    newValsIncome.add(newIncome);
                }
                if (k < valsExpenses.size()) {
                    Entry expense = valsExpenses.get(k);
                    if (expense.getXIndex() != i) {
                        Entry newExpense = new Entry(0f, i);
                        newValsExpenses.add(newExpense);
                    } else {
                        newValsExpenses.add(expense);
                        k++;
                    }
                } else {
                    Entry newExpense = new Entry(0f, i);
                    newValsExpenses.add(newExpense);
                }
            }

            LineDataSet setIncome = new LineDataSet(newValsIncome, "Income");
            setIncome.setColor(getResources().getColor(R.color.income));
            LineDataSet setExpenses = new LineDataSet(newValsExpenses, "Expenses");
            setExpenses.setColor(getResources().getColor(R.color.expense));
            setIncome.setLineWidth(1.0f);
            setIncome.setDrawCircles(false);
            setIncome.setDrawFilled(true);
            setIncome.setFillColor(getResources().getColor(R.color.income));
            setIncome.setFillAlpha(20);
            setExpenses.setLineWidth(1.0f);
            setExpenses.setDrawCircles(false);
            setExpenses.setDrawFilled(true);
            setExpenses.setFillColor(getResources().getColor(R.color.expense));
            setExpenses.setFillAlpha(20);

            ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
            dataSets.add(setIncome);
            dataSets.add(setExpenses);

            LineData data = new LineData(xVals, dataSets);
            mLineChart.setData(data);
        }
    }

    private void setupPieChart() {
        mPieChart.setDescription("");
        mPieChart.setDrawYValues(true);
        mPieChart.setDrawXValues(false);
        mPieChart.setUsePercentValues(true);
        mPieChart.setValueTextSize(14f);
        mPieChart.setValueTextColor(getResources().getColor(R.color.body_text_1));
        mPieChart.setDrawLegend(false);

        getLoaderManager().initLoader(PieChartQuery._TOKEN, null, this);
    }

    private void setupPieChartData(Cursor cursor) {
        if (cursor.getCount() > 0) {
            ArrayList<String> xVals = new ArrayList<String>();
            ArrayList<Entry> yVals = new ArrayList<Entry>();
            ArrayList<Integer> colors = new ArrayList<Integer>();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                xVals.add(cursor.getString(PieChartQuery.CATEGORY_NAME));
                yVals.add(new Entry(cursor.getFloat(PieChartQuery.TOTAL_AMOUNT), cursor.getPosition()));
                colors.add(getResources().getColor(Category.getColor(cursor.getString(PieChartQuery.CATEGORY_ID))));
            }
            PieDataSet set = new PieDataSet(yVals, "");
            set.setSliceSpace(3f);
            set.setColors(colors);

            PieData data = new PieData(xVals, set);
            mPieChart.setData(data);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // TimeSpan
        Calendar c = DateUtils.getClearCalendar();
        c.set(Calendar.DAY_OF_MONTH, 1);
        long startDate = c.getTimeInMillis();
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        long endDate = c.getTimeInMillis();
        SelectionBuilder builder = new SelectionBuilder();
        switch (i) {
            case LineChartQuery._TOKEN:
                builder.where(FinanceContract.Transactions.IN_TIME_INTERVAL_SELECTION,
                        FinanceContract.Transactions.buildInTimeIntervalArgs(startDate, endDate));
                return new CursorLoader(getActivity(),
                        FinanceContract.Transactions.buildTransactionsByDaysUri(),
                        LineChartQuery.PROJECTION,
                        builder.getSelection(),
                        builder.getSelectionArgs(),
                        FinanceContract.Transactions.TRANSACTION_DATE + " ASC");
            case PieChartQuery._TOKEN:
                builder.where(FinanceContract.Transactions.TRANSACTION_TYPE + "=?",
                        FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE);
                builder.where(FinanceContract.Transactions.IN_TIME_INTERVAL_SELECTION,
                        FinanceContract.Transactions.buildInTimeIntervalArgs(startDate, endDate));
                return new CursorLoader(getActivity(),
                        FinanceContract.Transactions.buildTransactionsByCategoriesUri(),
                        PieChartQuery.PROJECTION,
                        builder.getSelection(),
                        builder.getSelectionArgs(),
                        FinanceContract.Transactions.DEFAULT_SORT);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch (cursorLoader.getId()) {
            case LineChartQuery._TOKEN:
                setupLineChartData(cursor);
                break;
            case PieChartQuery._TOKEN:
                setupPieChartData(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface LineChartQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                //FinanceContract.Transactions.TRANSACTION_AMOUNT,
                "SUM(" + FinanceContract.Transactions.TRANSACTION_AMOUNT + ")",
                "strftime('%d/%m/%Y', " + FinanceContract.Transactions.TRANSACTION_DATE + "/1000, 'unixepoch', 'localtime')",
                FinanceContract.Transactions.TRANSACTION_TYPE
        };

        int TOTAL_AMOUNT = 0;
        int DATE = 1;
        int TYPE = 2;
    }

    private interface PieChartQuery {
        int _TOKEN = 0x2;

        String[] PROJECTION = {
                "SUM(" + FinanceContract.Transactions.TRANSACTION_AMOUNT + ")",
                FinanceContract.Categories.CATEGORY_NAME,
                FinanceContract.Categories.CATEGORY_ID,
                FinanceContract.Currencies.CURRENCY_SYMBOL,
                FinanceContract.Currencies.CURRENCY_ID
        };

        int TOTAL_AMOUNT = 0;
        int CATEGORY_NAME = 1;
        int CATEGORY_ID = 2;
        int CURRENCY_ID = 4;
    }

}
