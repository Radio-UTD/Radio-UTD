package com.utd.radio;

import android.support.v4.app.Fragment;

/**
 * Created by Rahat on 9/22/2014.
 */
public abstract class NavigationDrawerItem {
    // TODO: add icons to items
    private String title;
    private Class<? extends Fragment> fragmentClass;

    NavigationDrawerItem(String title, Class<? extends Fragment> fragmentClass)
    {
        this.title = title;
        this.fragmentClass = fragmentClass;
    }

    public String getTitle() {
        return title;
    }

    public Class<? extends Fragment> getFragmentClass() {
        return fragmentClass;
    }

    public abstract Fragment getFragmentInstance();
}
