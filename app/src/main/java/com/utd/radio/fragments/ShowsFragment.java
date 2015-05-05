package com.utd.radio.fragments;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.utd.radio.R;
import com.utd.radio.RadioActivity;
import com.utd.radio.models.ThumbnailCard;
import com.utd.radio.adapters.ThumbnailCardAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;

public class ShowsFragment extends Fragment {

    public static final String ARG_DAY = "ARG_DAY";
    public static final String SHOWS_URL = "http://www.radioutd.com/data/lookupShows.php";
    public static final String IMAGE_URL_PREFIX = "http://www.radioutd.com/300/";

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
        RadioActivity.log("ShowsFragment.onCreateView");
        gridView = (GridView) inflater.inflate(R.layout.fragment_shows, container, false);
        gridView.setAdapter(cardAdapter);
        return gridView;
    }

    @Override
    public void onAttach(Activity activity) {
        RadioActivity.log("ShowsFragment.onAttach " + day);
        super.onAttach(activity);
        ((RadioActivity)activity).restoreActionBar(getString(R.string.drawer_title_shows));

        day = Day.values()[getArguments().getInt(ARG_DAY)];
        cardAdapter = new ThumbnailCardAdapter(getActivity());

        Ion.with(activity).load(SHOWS_URL).asDocument().setCallback(new FutureCallback<Document>() {

            private String getSubElementContents(Element e, String tag) {
                try {
                    return java.net.URLDecoder.decode(e.getElementsByTagName(tag).item(0).getTextContent(), "utf8");
                } catch (UnsupportedEncodingException e1) {
                    return e.getElementsByTagName(tag).item(0).getTextContent();
                }
            }

            @Override
            public void onCompleted(Exception e, Document result) {
                RadioActivity.log("ShowsFragment.onAttach.xmlDownload " + day);
                if(e != null)
                    return;
                NodeList shows = result.getElementsByTagName("show");
                for(int k = 0; k < shows.getLength(); k++) {
                    Element show = (Element) shows.item(k);

                    String weekday = getSubElementContents(show, "weekday");
                    // If it's not the right weekday, then return;
                    if(!weekday.equals("" + day.ordinal()))
                        continue;

                    final String name = getSubElementContents(show, "name");
                    final String dj = "with " + getSubElementContents(show, "dj");
                    final String time = "12:00 - 1:00";
                    String image = IMAGE_URL_PREFIX + getSubElementContents(show, "image");

                    Ion.with(getActivity()).load(image).asBitmap().setCallback(new FutureCallback<Bitmap>() {
                        @Override
                        public void onCompleted(Exception e, Bitmap result) {
                            RadioActivity.log("ShowsFragment.onAttach.xmlDownload.imageDownload " + day + " " + name);
                            if(e != null)
                                return;
                            cardAdapter.addCard(new ThumbnailCard(name, dj, time, new BitmapDrawable(getResources(), result)));
                            cardAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }
}
