package com.mad.qut.budgetr.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

import com.mad.qut.budgetr.R;

public class CategoryGridView extends GridView implements AdapterView.OnItemClickListener {

    private static final String TAG = CategoryGridView.class.getSimpleName();

    private String selection = "";

    public CategoryGridView(Context context) {
        super(context);
        setOnItemClickListener(this);
    }

    public CategoryGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnItemClickListener(this);
    }

    public CategoryGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnItemClickListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSpec;

        if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            heightSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,MeasureSpec.AT_MOST);
        } else {
            heightSpec = heightMeasureSpec;
        }

        super.onMeasure(widthMeasureSpec, heightSpec);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        selection = view.getContentDescription().toString();

        for (int j = 0; j < getChildCount(); j++) {
            View v = getChildAt(j);
            ImageView hover = (ImageView) v.findViewById(R.id.hover);
            if (j == i) {
                hover.setVisibility(View.VISIBLE);
            } else {
                hover.setVisibility(View.INVISIBLE);
            }
        }
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String category) {
        selection = category;
    }
}
