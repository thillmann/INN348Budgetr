package com.mad.qut.budgetr.ui;

import android.app.DialogFragment;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Category;
import com.mad.qut.budgetr.provider.FinanceContract;
import com.mad.qut.budgetr.ui.widget.LineChartMarkerView;
import com.mad.qut.budgetr.ui.widget.PeriodPickerFragment;
import com.mad.qut.budgetr.utils.DateUtils;
import com.mad.qut.budgetr.utils.NumberUtils;
import com.mad.qut.budgetr.utils.SelectionBuilder;

import java.util.ArrayList;
import java.util.Calendar;

public class OverviewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener, PeriodPickerFragment.Listener {

    private static final String TAG = OverviewFragment.class.getSimpleName();

    private static final String STATE_PERIOD = "period";
    private static final String STATE_START_DATE = "startDate";
    private static final String STATE_END_DATE = "endDate";

    private Button btPeriod;
    private TextView tvBalance;
    private TextView tvBalanceValue;
    private LineChart mBalanceChart;
    private TextView tvIncome;
    private TextView tvIncomeValue;
    private TextView tvExpenses;
    private TextView tvExpensesValue;
    private PieChart mExpenseChart;

    private int mPeriod;
    private long mStartDate;
    private long mEndDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mPeriod = savedInstanceState.getInt(STATE_PERIOD);
            mStartDate = savedInstanceState.getLong(STATE_START_DATE);
            mEndDate = savedInstanceState.getLong(STATE_END_DATE);
        } else {
            // TimeSpan
            Calendar c = DateUtils.getClearCalendar();
            c.set(Calendar.DAY_OF_MONTH, 1);
            mStartDate = c.getTimeInMillis();
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            mEndDate = c.getTimeInMillis();

            mPeriod = PeriodPickerFragment.PERIOD_MONTH;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "SAVE");
        savedInstanceState.putInt(STATE_PERIOD, mPeriod);
        savedInstanceState.putLong(STATE_START_DATE, mStartDate);
        savedInstanceState.putLong(STATE_END_DATE, mEndDate);

        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        btPeriod = (Button) view.findViewById(R.id.period_picker);
        setPeriodButton();
        btPeriod.setOnClickListener(this);

        tvBalance = (TextView) view.findViewById(R.id.balance);
        tvBalanceValue = (TextView) view.findViewById(R.id.balance_value);
        tvIncome = (TextView) view.findViewById(R.id.income);
        tvIncomeValue = (TextView) view.findViewById(R.id.income_value);
        tvExpenses = (TextView) view.findViewById(R.id.expenses);
        tvExpensesValue = (TextView) view.findViewById(R.id.expenses_value);

        mBalanceChart = (LineChart) view.findViewById(R.id.line_chart);
        setupBalanceChart();

        mExpenseChart = (PieChart) view.findViewById(R.id.pie_chart);
        setupExpenseChart();

        return view;
    }

    private void resetCharts() {
        mBalanceChart.fitScreen();
        mBalanceChart.setVisibility(View.GONE);

        tvBalance.setText(getResources().getQuantityString(R.plurals.number_transactions, 0, 0));
        tvBalanceValue.setText(NumberUtils.getFormattedCurrency(0));
        tvBalanceValue.setTextColor(getResources().getColor(R.color.body_text_1));

        tvIncome.setText(getResources().getQuantityString(R.plurals.number_transactions, 0, 0));
        tvIncomeValue.setText(NumberUtils.getFormattedCurrency(0));

        mExpenseChart.setVisibility(View.GONE);
        mExpenseChart.setCenterText("");

        tvExpenses.setText(getResources().getQuantityString(R.plurals.number_transactions, 0, 0));

        tvExpensesValue.setText(NumberUtils.getFormattedCurrency(0));
    }

    @Override
    public void onClick(View view) {
        DialogFragment periodPicker = new PeriodPickerFragment();
        periodPicker.setTargetFragment(this, 1);
        Bundle args = new Bundle();
        args.putInt(PeriodPickerFragment.CURRENT_PERIOD, mPeriod);
        args.putLong(PeriodPickerFragment.CURRENT_START_DATE, mStartDate);
        periodPicker.setArguments(args);
        periodPicker.show(getFragmentManager(), "periodPicker");
    }


    @Override
    public void onPeriodPicked(long startDate, long endDate, int currentPeriod) {
        if (mStartDate != startDate || mEndDate != endDate) {
            mStartDate = startDate;
            mEndDate = endDate;
            mPeriod = currentPeriod;
            setPeriodButton();

            resetCharts();

            getLoaderManager().restartLoader(BalanceChartQuery._TOKEN, null, this);
            getLoaderManager().restartLoader(ExpenseChartQuery._TOKEN, null, this);
        }
    }

    private void setPeriodButton() {
        switch (mPeriod) {
            case PeriodPickerFragment.PERIOD_YEAR:
                btPeriod.setText(DateUtils.getFormattedDate(mStartDate, "yyyy"));
                break;
            case PeriodPickerFragment.PERIOD_MONTH:
                btPeriod.setText(DateUtils.getFormattedDate(mStartDate, "MMMM yyyy"));
                break;
            case PeriodPickerFragment.PERIOD_WEEK:
                btPeriod.setText(DateUtils.getFormattedDateRange(mStartDate, mEndDate, "dd MMM", "-"));
                break;
        }
    }

    private void setupBalanceChart() {
        mBalanceChart.setDescription("");
        mBalanceChart.setDrawYValues(false);
        mBalanceChart.setHighlightEnabled(true);
        mBalanceChart.setHighlightIndicatorEnabled(false);
        mBalanceChart.setDrawBorder(false);
        mBalanceChart.setUnit(" " + NumberUtils.getCurrencySymbol());
        mBalanceChart.setDrawUnitsInChart(true);
        mBalanceChart.setStartAtZero(false);
        mBalanceChart.setDrawLegend(false);
        mBalanceChart.setDrawVerticalGrid(false);
        LineChartMarkerView mv = new LineChartMarkerView(getActivity());
        mBalanceChart.setMarkerView(mv);

        getLoaderManager().initLoader(BalanceChartQuery._TOKEN, null, this);
    }

    private void setupBalanceChartData(Cursor cursor) {
        if (cursor.getCount() > 0) {
            mBalanceChart.setVisibility(View.VISIBLE);
            Animation scaleInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.scale_in);
            mBalanceChart.startAnimation(scaleInAnimation);

            int count = 0;

            ArrayList<String> xVals = new ArrayList<String>();
            for (long i = mStartDate; i <= mEndDate; i = i + 1000*60*60*24) {
                xVals.add(DateUtils.getFormattedDate(i, "dd MMM"));
            }

            ArrayList<Entry> valsIncome = new ArrayList<Entry>();
            ArrayList<Entry> valsExpenses = new ArrayList<Entry>();

            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                long date = cursor.getLong(BalanceChartQuery.DATE);
                Entry entry = new Entry(cursor.getFloat(BalanceChartQuery.TOTAL_AMOUNT), xVals.indexOf(DateUtils.getFormattedDate(date, "dd MMM")));
                if (cursor.getString(BalanceChartQuery.TYPE).equals(FinanceContract.Transactions.TRANSACTION_TYPE_INCOME)) {
                    valsIncome.add(entry);
                } else {
                    valsExpenses.add(entry);
                }
                count += cursor.getInt(BalanceChartQuery.COUNT);
            }

            ArrayList<Entry> valsBalance = new ArrayList<Entry>();
            float totalValue = 0;
            float totalIncome = 0;
            int j = 0;
            int k = 0;
            for (int i = 0; i < xVals.size(); i++) {
                double incomeVal = 0;
                double expenseVal = 0;
                if (j < valsIncome.size()) {
                    Entry income = valsIncome.get(j);
                    if (income.getXIndex() == i) {
                        incomeVal = income.getVal();
                        j++;
                    }
                }
                if (k < valsExpenses.size()) {
                    Entry expense = valsExpenses.get(k);
                    if (expense.getXIndex() == i) {
                        expenseVal = expense.getVal();
                        k++;
                    }
                }
                valsBalance.add(new Entry((float) (incomeVal - expenseVal), i));
                totalIncome += incomeVal;
                totalValue += incomeVal - expenseVal;
            }

            LineDataSet setBalance = new LineDataSet(valsBalance, "Balance");
            setBalance.setColor(getResources().getColor(R.color.balance));
            setBalance.setLineWidth(1.0f);
            setBalance.setDrawCircles(false);
            setBalance.setDrawFilled(true);
            setBalance.setFillColor(getResources().getColor(R.color.balance));
            setBalance.setFillAlpha(20);
            float max = Math.max(Math.abs(setBalance.getYMax()), Math.abs(setBalance.getYMin()))*2;
            mBalanceChart.setYRange(-max, max, false);

            ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
            dataSets.add(setBalance);

            LineData data = new LineData(xVals, dataSets);
            mBalanceChart.setData(data);
            mBalanceChart.invalidate();

            tvBalance.setText(getResources().getQuantityString(R.plurals.number_transactions, count, count));

            tvBalanceValue.setText(NumberUtils.getFormattedCurrency(totalValue));
            if (totalValue > 0) {
                tvBalanceValue.setTextColor(getResources().getColor(R.color.income));
            } else if (totalValue < 0) {
                tvBalanceValue.setTextColor(getResources().getColor(R.color.expense));
            } else {
                tvBalanceValue.setTextColor(getResources().getColor(R.color.body_text_1));
            }

            tvIncome.setText(getResources().getQuantityString(R.plurals.number_transactions, valsIncome.size(), valsIncome.size()));

            tvIncomeValue.setText(NumberUtils.getFormattedCurrency(totalIncome));
        }
    }

    private void setupExpenseChart() {
        mExpenseChart.setDescription("");
        mExpenseChart.setDrawCenterText(true);
        mExpenseChart.setCenterTextSize(14f);
        mExpenseChart.setDrawYValues(true);
        mExpenseChart.setDrawXValues(false);
        mExpenseChart.setUsePercentValues(true);
        mExpenseChart.setValueTextSize(14f);
        mExpenseChart.setValueTextColor(getResources().getColor(R.color.body_text_1));
        mExpenseChart.setDrawLegend(false);
        mExpenseChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, int i) {
                PieData data = mExpenseChart.getDataOriginal();
                mExpenseChart.setCenterText(data.getXVals().get(entry.getXIndex()));
            }

            @Override
            public void onNothingSelected() {
            }
        });

        getLoaderManager().initLoader(ExpenseChartQuery._TOKEN, null, this);
    }

    private void setupExpenseChartData(Cursor cursor) {
        if (cursor.getCount() > 0) {
            mExpenseChart.setVisibility(View.VISIBLE);
            Animation scaleInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.scale_in);
            mExpenseChart.startAnimation(scaleInAnimation);

            int count = 0;
            float totalValue = 0;

            ArrayList<String> xVals = new ArrayList<String>();
            ArrayList<Entry> yVals = new ArrayList<Entry>();
            ArrayList<Integer> colors = new ArrayList<Integer>();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                xVals.add(cursor.getString(ExpenseChartQuery.CATEGORY_NAME));
                yVals.add(new Entry(cursor.getFloat(ExpenseChartQuery.TOTAL_AMOUNT), cursor.getPosition()));
                colors.add(getResources().getColor(Category.getColor(cursor.getString(ExpenseChartQuery.CATEGORY_ID))));
                count += cursor.getInt(ExpenseChartQuery.COUNT);
                totalValue -= cursor.getFloat(ExpenseChartQuery.TOTAL_AMOUNT);
            }
            PieDataSet set = new PieDataSet(yVals, "");
            set.setSliceSpace(3f);
            set.setColors(colors);

            PieData data = new PieData(xVals, set);
            mExpenseChart.setData(data);
            mExpenseChart.invalidate();

            tvExpenses.setText(getResources().getQuantityString(R.plurals.number_transactions, count, count));

            tvExpensesValue.setText(NumberUtils.getFormattedCurrency(totalValue));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        SelectionBuilder builder = new SelectionBuilder();
        switch (i) {
            case BalanceChartQuery._TOKEN:
                builder.where(FinanceContract.Transactions.IN_TIME_INTERVAL_SELECTION,
                        FinanceContract.Transactions.buildInTimeIntervalArgs(mStartDate, mEndDate));
                return new CursorLoader(getActivity(),
                        FinanceContract.Transactions.buildTransactionsByDaysUri(),
                        BalanceChartQuery.PROJECTION,
                        builder.getSelection(),
                        builder.getSelectionArgs(),
                        FinanceContract.Transactions.TRANSACTION_DATE + " ASC");
            case ExpenseChartQuery._TOKEN:
                builder.where(FinanceContract.Transactions.TRANSACTION_TYPE + "=?",
                        FinanceContract.Transactions.TRANSACTION_TYPE_EXPENSE);
                builder.where(FinanceContract.Transactions.IN_TIME_INTERVAL_SELECTION,
                        FinanceContract.Transactions.buildInTimeIntervalArgs(mStartDate, mEndDate));
                return new CursorLoader(getActivity(),
                        FinanceContract.Transactions.buildTransactionsByCategoriesUri(),
                        ExpenseChartQuery.PROJECTION,
                        builder.getSelection(),
                        builder.getSelectionArgs(),
                        FinanceContract.Transactions.DEFAULT_SORT);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch (cursorLoader.getId()) {
            case BalanceChartQuery._TOKEN:
                setupBalanceChartData(cursor);
                break;
            case ExpenseChartQuery._TOKEN:
                setupExpenseChartData(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        resetCharts();
    }

    private interface BalanceChartQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                "SUM(" + FinanceContract.Transactions.TRANSACTION_AMOUNT + ")",
                FinanceContract.Transactions.TRANSACTION_DATE,
                FinanceContract.Transactions.TRANSACTION_TYPE,
                "COUNT(*)"
        };

        int TOTAL_AMOUNT = 0;
        int DATE = 1;
        int TYPE = 2;
        int COUNT = 3;
    }

    private interface ExpenseChartQuery {
        int _TOKEN = 0x2;

        String[] PROJECTION = {
                "SUM(" + FinanceContract.Transactions.TRANSACTION_AMOUNT + ")",
                FinanceContract.Categories.CATEGORY_NAME,
                FinanceContract.Categories.CATEGORY_ID,
                "COUNT(*)"
        };

        int TOTAL_AMOUNT = 0;
        int CATEGORY_NAME = 1;
        int CATEGORY_ID = 2;
        int COUNT = 3;
    }

}
