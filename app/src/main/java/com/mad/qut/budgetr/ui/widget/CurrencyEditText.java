package com.mad.qut.budgetr.ui.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.mad.qut.budgetr.utils.NumberUtils;

import java.text.NumberFormat;

public class CurrencyEditText extends EditText implements TextWatcher {

    private String current = getText().toString();
    private double value = 0;

    public CurrencyEditText(Context context) {
        super(context);
        init();
    }

    public CurrencyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CurrencyEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        String formatted = NumberUtils.getFormattedCurrency(0);
        setText(formatted);
        setSelection(formatted.length());
        addTextChangedListener(this);
    }

    @Override
    public void onSelectionChanged(int start, int end) {
        CharSequence text = getText();
        if (text != null) {
            if (start != text.length() || end != text.length()) {
                setSelection(text.length(), text.length());
                return;
            }
        }
        super.onSelectionChanged(start, end);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (!editable.toString().equals(current)) {
            removeTextChangedListener(this);
            value = NumberUtils.getCleanedValue(editable.toString());
            current = NumberUtils.getFormattedCurrency(value);
            setText(current);
            setSelection(current.length());
            addTextChangedListener(this);
        }
    }

    public double getCurrencyValue() {
        return value;
    }
}
