package com.utd.radio.adapters;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.utd.radio.R;
import com.utd.radio.models.NavigationDrawerItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Rahat on 9/22/2014.
 */
public class NavigationDrawerAdapter extends BaseAdapter {
    List<NavigationDrawerItem> list;
    Activity context;

    public NavigationDrawerAdapter(Activity context)
    {
        super();
        this.context = context;
        this.list = new ArrayList<NavigationDrawerItem>();
    }

    public NavigationDrawerAdapter(Activity context, List<NavigationDrawerItem> list) {
        this(context);
        this.list.addAll(list);
    }

    public NavigationDrawerAdapter(Activity context, NavigationDrawerItem[] array) {
        this(context, Arrays.asList(array));
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void addItem(NavigationDrawerItem item)
    {
        list.add(item);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        // TODO: set icon
        if(view == null)
        {
            view = context.getLayoutInflater().inflate(R.layout.navigation_drawer_item, parent, false);
        }
        ((TextView)view.findViewById(R.id.nav_drawer_item_label)).setText(list.get(i).getTitle());
        return view;
    }
}
