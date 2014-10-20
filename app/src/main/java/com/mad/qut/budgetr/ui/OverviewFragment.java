package com.mad.qut.budgetr.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

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
import com.mad.qut.budgetr.model.Transaction;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.SelectionBuilder;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
        mLineChart.setUnit(" $");
        mLineChart.setDrawUnitsInChart(true);

        getLoaderManager().initLoader(LineChartQuery._TOKEN, null, this);
    }

    private void setupLineChartData(Cursor cursor) {
        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<Entry> valsIncome = new ArrayList<Entry>();
        ArrayList<Entry> valsExpenses = new ArrayList<Entry>();

        for (int i = 0; i < 31; i++) {
            Entry income = new Entry((float) Math.random()*100, i); // 0 == quarter 1
            valsIncome.add(income);
            Entry expense = new Entry((float) Math.random()*100, i); // 0 == quarter 1
            valsExpenses.add(expense);
            xVals.add((i+1) + "/10/2014");
        }

        LineDataSet setIncome = new LineDataSet(valsIncome, "Income");
        setIncome.setColor(getResources().getColor(R.color.income));
        LineDataSet setExpenses = new LineDataSet(valsExpenses, "Expenses");
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
                Log.d(TAG, cursor.getDouble(PieChartQuery.TOTAL_AMOUNT)+"");
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
                        FinanceContract.Transactions.CONTENT_URI,
                        LineChartQuery.PROJECTION,
                        builder.getSelection(),
                        builder.getSelectionArgs(),
                        FinanceContract.Transactions.DEFAULT_SORT);
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
                BaseColumns._ID,
                FinanceContract.Transactions.TRANSACTION_ID,
                FinanceContract.Transactions.TRANSACTION_DATE,
                FinanceContract.Transactions.TRANSACTION_TYPE,
                FinanceContract.Transactions.TRANSACTION_AMOUNT,
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
