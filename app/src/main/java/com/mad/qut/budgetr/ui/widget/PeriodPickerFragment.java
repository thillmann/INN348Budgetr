package com.mad.qut.budgetr.ui.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.utils.DateUtils;
import com.wefika.horizontalpicker.HorizontalPicker;

import java.util.Calendar;

public class PeriodPickerFragment extends DialogFragment implements View.OnClickListener {

    private static final String TAG = PeriodPickerFragment.class.getSimpleName();

    public static final String CURRENT_PERIOD = "currentPeriod";
    public static final String CURRENT_START_DATE = "currentStartDate";

    public static final int PERIOD_YEAR = 0;
    public static final int PERIOD_MONTH = 1;
    public static final int PERIOD_WEEK = 2;

    private HorizontalPicker mPicker;
    private Button btYear;
    private Button btMonth;
    private Button btWeek;

    private int mCurrentPeriod;
    private long mCurrentStartDate = -1;

    private CharSequence[] mOptions;
    private long[][] mValues;

    @Override
    public void onClick(View view) {
        if (view.equals(btYear)) {
            setupPicker(PERIOD_YEAR);
            return;
        }
        if (view.equals(btMonth)) {
            setupPicker(PERIOD_MONTH);
            return;
        }
        if (view.equals(btWeek)) {
            setupPicker(PERIOD_WEEK);
        }
    }

    private void setupPicker(int periodType) {
        mCurrentPeriod = periodType;
        int normalColor = getResources().getColor(R.color.body_text_1);
        int highlightColor = getResources().getColor(R.color.theme_accent_1);
        switch (periodType) {
            case PERIOD_YEAR:
                btYear.setTextColor(highlightColor);
                btMonth.setTextColor(normalColor);
                btWeek.setTextColor(normalColor);
                break;
            case PERIOD_MONTH:
                btYear.setTextColor(normalColor);
                btMonth.setTextColor(highlightColor);
                btWeek.setTextColor(normalColor);
                break;
            case PERIOD_WEEK:
                btYear.setTextColor(normalColor);
                btMonth.setTextColor(normalColor);
                btWeek.setTextColor(highlightColor);

                break;
        }
        setValues(periodType);
    }

    private void setValues(int periodType) {
        Calendar c = DateUtils.getClearCalendar();

        int unit;
        String format;

        switch (periodType) {
            case PERIOD_YEAR:
                format = "yyyy";
                unit = Calendar.YEAR;
                c.set(Calendar.DAY_OF_YEAR, 1);
                break;
            case PERIOD_MONTH:
                format = "MMM yyyy";
                unit = Calendar.MONTH;
                c.set(Calendar.DAY_OF_MONTH, 1);
                break;
            default:
                format = "dd/MM";
                unit = Calendar.WEEK_OF_YEAR;
                c.set(Calendar.DAY_OF_WEEK, 2);
                break;
        }

        c.add(unit, -4);

        mOptions = new CharSequence[5];
        int selected = mOptions.length-1;
        mValues = new long[5][2];
        for (int i = 0; i < 5; i++) {
            long start = c.getTimeInMillis();
            if (start == mCurrentStartDate) {
                selected = i;
            }
            c.add(unit, 1);
            c.add(Calendar.DAY_OF_YEAR, -1);
            long end =  c.getTimeInMillis();
            if (periodType == PERIOD_WEEK) {
                mOptions[i] = DateUtils.getFormattedDateRange(start, end, format, "-");
            } else {
                mOptions[i] = DateUtils.getFormattedDate(start, format);
            }
            mValues[i][0] = start;
            mValues[i][1] = end;
            c.add(Calendar.DAY_OF_YEAR, 1);
        }

        mPicker.setValues(mOptions);
        mPicker.setSelectedItem(selected);
    }

    public interface Listener {
        public void onPeriodPicked(long startDate, long endDate, int currentPeriod);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null && !args.isEmpty()) {
            if (args.containsKey(CURRENT_PERIOD)) {
                mCurrentPeriod = args.getInt(CURRENT_PERIOD);
            } else {
                mCurrentPeriod = PERIOD_MONTH;
            }
            if (args.containsKey(CURRENT_START_DATE)) {
                mCurrentStartDate = args.getLong(CURRENT_START_DATE);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.picker_period, null);

        mPicker = (HorizontalPicker) v.findViewById(R.id.picker);
        btYear = (Button) v.findViewById(R.id.year);
        btMonth = (Button) v.findViewById(R.id.month);
        btWeek = (Button) v.findViewById(R.id.week);
        btYear.setOnClickListener(PeriodPickerFragment.this);
        btMonth.setOnClickListener(PeriodPickerFragment.this);
        btWeek.setOnClickListener(PeriodPickerFragment.this);

        setupPicker(mCurrentPeriod);

        builder.setView(v)
                .setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int i = mPicker.getSelectedItem();
                        ((Listener) getTargetFragment()).onPeriodPicked(mValues[i][0], mValues[i][1], mCurrentPeriod);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        return builder.create();
    }

}