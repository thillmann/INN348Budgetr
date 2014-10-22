package com.mad.qut.budgetr.ui.widget;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.utils.MarkerView;
import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.utils.NumberUtils;

import java.text.NumberFormat;

public class LineChartMarkerView extends MarkerView {

    private static final String TAG = LineChartMarkerView.class.getSimpleName();

    private TextView tvContent;

    public LineChartMarkerView(Context context) {
        super(context, R.layout.marker_view_line_chart);

        tvContent = (TextView) findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, int dataSetIndex) {
        if (e.getVal() > 0) {
            tvContent.setTextColor(getResources().getColor(R.color.income));
        } else if (e.getVal() < 0) {
            tvContent.setTextColor(getResources().getColor(R.color.expense));
        } else {
            tvContent.setTextColor(getResources().getColor(R.color.body_text_1));
        }
        String text = NumberUtils.getFormattedCurrency(e.getVal());
        tvContent.setText(text);
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        setOffsets(-getMeasuredWidth()/2, -getMeasuredHeight());
    }

}
