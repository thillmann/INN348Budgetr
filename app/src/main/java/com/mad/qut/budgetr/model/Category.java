package com.mad.qut.budgetr.model;

import android.util.Log;

import com.google.gson.Gson;
import com.mad.qut.budgetr.R;
import com.mad.qut.budgetr.provider.FinanceContract;

import java.util.HashMap;

public class Category {

    private static final String TAG = Category.class.getSimpleName();

    public String id;
    public String name;
    public String type;

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Category fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Category.class);
    }

    private static HashMap<String, Integer> icons = new HashMap<String, Integer>();
    private static HashMap<String, Integer> colors = new HashMap<String, Integer>();

    private static void initIcons() {
        icons.put("ex_rent", R.drawable.icon_rent);
        icons.put("ex_car", R.drawable.icon_car);
        icons.put("ex_travel", R.drawable.icon_travel);
        icons.put("ex_groceries", R.drawable.icon_groceries);
        icons.put("ex_shopping", R.drawable.icon_shopping);
        icons.put("ex_personal", R.drawable.icon_personal);
        icons.put("ex_bills", R.drawable.icon_bills);
        icons.put("ex_entertainment", R.drawable.icon_entertainment);
        icons.put("ex_other", R.drawable.icon_other);
        icons.put("in_salary", R.drawable.icon_salary);
        icons.put("in_business", R.drawable.icon_business);
        icons.put("in_other", R.drawable.icon_other);
    }

    private static void initColors() {
        colors.put("ex_rent", R.color.category_rent);
        colors.put("ex_car", R.color.category_car);
        colors.put("ex_travel", R.color.category_travel);
        colors.put("ex_groceries", R.color.category_groceries);
        colors.put("ex_shopping", R.color.category_shopping);
        colors.put("ex_personal", R.color.category_personal);
        colors.put("ex_bills", R.color.category_bills);
        colors.put("ex_entertainment", R.color.category_entertainment);
        colors.put("ex_other", R.color.category_other);
        colors.put("in_salary", R.color.category_salary);
        colors.put("in_business", R.color.category_business);
        colors.put("in_other", R.color.category_other);
    }

    public static int getIcon(String categoryId) {
        if (icons.isEmpty()) {
            initIcons();
        }
        return icons.get(categoryId);
    }

    public static int getColor(String categoryId) {
        if (colors.isEmpty()) {
            initColors();
        }
        return colors.get(categoryId);
    }

}
