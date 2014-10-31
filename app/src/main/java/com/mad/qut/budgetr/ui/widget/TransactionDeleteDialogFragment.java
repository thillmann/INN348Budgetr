package com.mad.qut.budgetr.ui.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.mad.qut.budgetr.R;

public class TransactionDeleteDialogFragment extends DialogFragment {

    private static final String TAG = TransactionDeleteDialogFragment.class.getSimpleName();

    public static final String SHOW_OPTIONS = "show_options";

    private boolean mShowOptions = false;

    private int mSelectedItem = 0;

    public interface Listener {
        public void onDelete(int id, boolean options);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null && !args.isEmpty()) {
            if (args.containsKey(SHOW_OPTIONS)) {
                mShowOptions = args.getBoolean(SHOW_OPTIONS);
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_delete_title);
        if (mShowOptions) {
            builder.setSingleChoiceItems(R.array.transaction_delete, mSelectedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mSelectedItem = i;
                }
            });
        } else {
            builder.setMessage(R.string.dialog_message_delete_single);
        }
        builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ((Listener) getActivity()).onDelete(mSelectedItem, mShowOptions);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        return builder.create();
    }

}
