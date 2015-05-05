package com.utd.radio.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.utd.radio.fragments.ShowsFragment;

public class ShowsFragmentPagerAdapter extends FragmentPagerAdapter {

    public ShowsFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return ShowsFragment.newInstance(ShowsFragment.Day.values()[position]);
    }

    @Override
    public int getCount() {
        // you never know how many days are gonna be in a week
        return ShowsFragment.Day.values().length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return ShowsFragment.Day.values()[position].toString();
    }
}
