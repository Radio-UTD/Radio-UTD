package com.utd.radio.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.utd.radio.R;
import com.utd.radio.adapters.ShowsFragmentPagerAdapter;

public class TabbedShowsFragment extends Fragment {

    ShowsFragmentPagerAdapter adapter;
    ViewPager viewPager;
    PagerSlidingTabStrip pagerTabs;

    public static TabbedShowsFragment newInstance() {
        TabbedShowsFragment fragment = new TabbedShowsFragment();
        return fragment;
    }

    public TabbedShowsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_tabbed_shows, container, false);

        adapter = new ShowsFragmentPagerAdapter(getFragmentManager());
        viewPager = (ViewPager) rootView.findViewById(R.id.shows_pager);
        viewPager.setAdapter(adapter);
        pagerTabs = (PagerSlidingTabStrip) rootView.findViewById(R.id.shows_pager_tabs);
        pagerTabs.setViewPager(viewPager);

        return rootView;
    }

}
