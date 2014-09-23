package com.utd.radio.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.utd.radio.R;
import com.utd.radio.RadioActivity;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_TITLE = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlaceholderFragment newInstance() {
        return new PlaceholderFragment();
    }

    public PlaceholderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_radio, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        ((RadioActivity) activity).onSectionAttached(
//                getArguments().getInt(ARG_SECTION_NUMBER));
        ((RadioActivity) activity).restoreActionBar(getString(R.string.app_name));
    }
}