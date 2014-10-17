package com.mad.qut.budgetr.ui.widget;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.mad.qut.budgetr.R;

/**
 * Created by Timo on 03.09.2014.
 */
public class NumPad {

    private final static String TAG = NumPad.class.getSimpleName();

    public final static int CODE_DELETE = -5; // Keyboard.KEYCODE_DELETE
    public final static int CODE_CANCEL = -3; // Keyboard.KEYCODE_CANCEL
    public final static int CODE_POINT  = 46;

    private Keyboard mKeyboard;
    private KeyboardView mKeyboardView;

    private Activity mActivity;

    private KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override public void onKey(int primaryCode, int[] keyCodes) {
            // Get the EditText and its Editable
            View focusCurrent = mActivity.getWindow().getCurrentFocus();
            if( focusCurrent==null || focusCurrent.getClass()!=EditText.class ) return;
            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();
            int start = edittext.length();
            // Handle key
            if (primaryCode == CODE_CANCEL) {
                hideNumPad();
            } else if (primaryCode == CODE_DELETE) {
                if (editable != null && start > 0) {
                    editable.delete(start - 1, start);
                }
            } else if (primaryCode == CODE_POINT) {
                // check for point in text and add if no ones there
                editable.insert(start, Character.toString((char) primaryCode));
            } else {// Insert character
                editable.insert(start, Character.toString((char) primaryCode));
            }
        }

        @Override public void onPress(int arg0) {
        }

        @Override public void onRelease(int primaryCode) {
        }

        @Override public void onText(CharSequence text) {
        }

        @Override public void swipeDown() {
        }

        @Override public void swipeLeft() {
        }

        @Override public void swipeRight() {
        }

        @Override public void swipeUp() {
        }
    };

    public NumPad(Activity activity, int viewId, int layoutId) {
        mActivity = activity;
        mKeyboard = new Keyboard(mActivity, layoutId);
        mKeyboardView = (KeyboardView) mActivity.findViewById(viewId);
        mKeyboardView.setKeyboard(mKeyboard);
        mKeyboardView.setPreviewEnabled(false);
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
    }

    public void hideNumPad() {
        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);
    }

    public void showNumPad(View v) {
        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
        if (v != null) {
            ((InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public boolean isNumPadVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }

    public void registerEditText(EditText mEditText) {
        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "Focus change");
                if (hasFocus) {
                    showNumPad(v);
                } else {
                    hideNumPad();
                }
            }
        });
        mEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNumPad(v);
            }
        });
        mEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                EditText edittext = (EditText) v;
                int inType = edittext.getInputType();       // Backup the input type
                edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
                edittext.onTouchEvent(event);               // Call native handler
                edittext.setInputType(inType);              // Restore input type
                return true; // Consume touch event
            }
        });
        mEditText.setInputType(mEditText.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }
}
