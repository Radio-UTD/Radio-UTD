package com.utd.radio.adapters;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.utd.radio.R;
import com.utd.radio.models.ThumbnailCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Rahat on 9/22/2014.
 */
public class ThumbnailCardAdapter extends BaseAdapter {
    
    Activity context;
    List<ThumbnailCard> list;

    public ThumbnailCardAdapter(Activity context)
    {
        super();
        this.context = context;
        this.list = new ArrayList<ThumbnailCard>();
    }

    public ThumbnailCardAdapter(Activity context, List<ThumbnailCard> list) {
        this(context);
        this.list.addAll(list);
    }

    public ThumbnailCardAdapter(Activity context, ThumbnailCard[] array) {
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

    public void addCard(ThumbnailCard card)
    {
        list.add(card);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        if(view == null)
        {
            view = context.getLayoutInflater().inflate(R.layout.thumbnail_card, parent, false);
        }
        ((TextView)view.findViewById(R.id.show_card_title)).setText(list.get(i).getTitle());
        ((TextView)view.findViewById(R.id.show_card_subtitle)).setText(list.get(i).getSubtitle());
        ((TextView)view.findViewById(R.id.show_card_subsubtitle)).setText(list.get(i).getSubsubtitle());
        ((ImageView)view.findViewById(R.id.show_card_image)).setImageDrawable(list.get(i).getThumbnail());
        return view;
    }
}
