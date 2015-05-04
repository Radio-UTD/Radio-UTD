package com.utd.radio.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.utd.radio.R;
import com.utd.radio.RadioActivity;
import com.utd.radio.models.ThumbnailCard;
import com.utd.radio.adapters.ThumbnailCardAdapter;

/**
 * Created by Rahat on 9/22/2014.
 */
public class ShowsFragment extends Fragment {

    public static final String ARG_DAY = "ARG_DAY";

    private Day day;

    private GridView gridView;
    private ThumbnailCardAdapter cardAdapter;

    public enum Day {
        Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
    }

    public ShowsFragment() {
    }

    public static ShowsFragment newInstance(Day day) {
        ShowsFragment fragment = new ShowsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DAY, day.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        gridView = (GridView) inflater.inflate(R.layout.fragment_shows, container, false);
        cardAdapter = new ThumbnailCardAdapter(getActivity());
        cardAdapter.addCard(new ThumbnailCard("Camping Out", "12PM - 2PM", getResources().getDrawable(R.drawable.temp_album_art)));
        cardAdapter.addCard(new ThumbnailCard("Mumbletown", "2PM - 4PM", getResources().getDrawable(R.drawable.temp_album_art)));
        cardAdapter.addCard(new ThumbnailCard("8-bit Jukebox", "4PM - 6PM", getResources().getDrawable(R.drawable.temp_album_art)));
        cardAdapter.addCard(new ThumbnailCard("Cria Cuervos", "6PM - 8PM", getResources().getDrawable(R.drawable.temp_album_art)));
        cardAdapter.addCard(new ThumbnailCard("Avant Lifestyles Guide", "8PM - 10PM", getResources().getDrawable(R.drawable.temp_album_art)));
        gridView.setAdapter(cardAdapter);
        return gridView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((RadioActivity)activity).restoreActionBar(getString(R.string.drawer_title_shows));
    }
}
