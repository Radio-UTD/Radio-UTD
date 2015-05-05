package com.utd.radio.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.utd.radio.fragments.ShowsFragment;


/**
 * Created by Jonathan Holmes on 4/8/2015.
 */
public class ShowsPagerAdapter extends FragmentStatePagerAdapter {
    public ShowsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        Fragment fragment = new ShowsFragment();
        Bundle args = new Bundle();
        args.putInt("day", i);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {
        return 7;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "OBJECT " + (position + 1);
    }
}
