package com.mad.qut.budgetr.ui.widget;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.model.Category;

public class CategoryGridAdapter extends CursorAdapter {

    private static final String TAG = CategoryGridAdapter.class.getSimpleName();

    private LayoutInflater mLayoutInflater;

    private String mCategory = "";

    public CategoryGridAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public CategoryGridAdapter(Context context, Cursor cursor, int flags, String category) {
        super(context, cursor, flags);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCategory = category;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return mLayoutInflater.inflate(R.layout.gridview_item_category, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String id = cursor.getString(1);
        String name = cursor.getString(2);

        ImageView backgroundView = (ImageView) view.findViewById(R.id.background);
        TextView nameView = (TextView) view.findViewById(R.id.name);

        backgroundView.setImageResource(Category.getIcon(id, 100));
        nameView.setText(name);

        ImageView hover = (ImageView) view.findViewById(R.id.hover);
        if (!mCategory.equals("") && mCategory.equals(id)) {
            hover.setVisibility(View.VISIBLE);
        } else {
            hover.setVisibility(View.INVISIBLE);
        }

        view.setContentDescription(id);
    }

    public void setCategoty(String categoty) {
        mCategory = categoty;
    }

}
