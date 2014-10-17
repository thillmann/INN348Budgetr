package com.mad.qut.budgetr.ui.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.mad.qut.budgetr.R;

public class PasswordDialogFragment extends DialogFragment {

    private PasswordDialogListener mListener;

    public interface PasswordDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, String candidatePassword);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (PasswordDialogListener) activity;
        } catch(ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PasswordDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false)
                .setTitle(R.string.dialog_enter_password_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EditText mEditPassword = (EditText) PasswordDialogFragment.this.getDialog().findViewById(R.id.password);
                        String mCandidatePassword = mEditPassword.getText().toString();
                        mListener.onDialogPositiveClick(PasswordDialogFragment.this, mCandidatePassword);
                    }
                })
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        // Prevent back button from closing the dialog
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                            return true;
                        return false;
                    }
                });
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.dialog_password, null);
        builder.setView(dialogView);
        AlertDialog mPasswordDialog = builder.create();
        mPasswordDialog.setCanceledOnTouchOutside(false);
        return mPasswordDialog;
    }

}
