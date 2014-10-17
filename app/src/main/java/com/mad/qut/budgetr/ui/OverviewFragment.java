package com.mad.qut.budgetr.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mad.qut.budgetr.R;

public class OverviewFragment extends Fragment {

    private static final String TAG = OverviewFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);
        return view;
    }

}
