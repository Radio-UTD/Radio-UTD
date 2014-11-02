package com.utd.radio.fragments;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.utd.radio.R;
import com.utd.radio.RadioActivity;
import com.utd.radio.RadioService;
import com.utd.radio.listeners.OnMetadataChangedListener;
import com.utd.radio.models.Metadata;
import com.utd.radio.util.MetadataManager;

/**
 * A placeholder fragment containing a simple view.
 */
public class RadioFragment extends Fragment implements OnMetadataChangedListener {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */

    TextView artistTextView;
    TextView songTextView;
    ImageButton playPauseButton;


    RadioService radioService;
    boolean isBound;

    Metadata currentMetadata = new Metadata();

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;

            RadioActivity.log("RadioFragment.onServiceConnected");
            //Toast.makeText(RadioFragment.this.getActivity(), "Sucessfully binded to services!", Toast.LENGTH_SHORT).show();
            radioService = ((RadioService.RadioBinder) service).getService();
            playPauseButton.setImageDrawable(getResources().getDrawable(radioService.isPlaying() ? R.drawable.ic_action_pause_light : R.drawable.ic_action_play_light));
            playPauseButton.setEnabled(false);
            radioService.setOnReadyListener(new RadioService.OnReadyListener() {
                @Override
                public void onReady() {
                    playPauseButton.setEnabled(true);
                }
            });

            if(!radioService.isReady())
                radioService.initMediaPlayer(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            RadioActivity.log("RadioFragment.onServiceDisconnected");
            radioService = null;
            isBound = false;
        }
    };


    public static RadioFragment newInstance() {
        return new RadioFragment();
    }

    public RadioFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        isBound = false;
        RadioActivity.log("RadioFragment.onCreateView");
        View view = inflater.inflate(R.layout.fragment_radio, container, false);


        songTextView = (TextView) view.findViewById(R.id.player_song);
        artistTextView = (TextView) view.findViewById(R.id.player_artist);

        playPauseButton = ((ImageButton)view.findViewById(R.id.playPauseButton));
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioActivity.log("RadioFragment.playPauseButton.onClick");
                if(isBound)
                {
                    if(radioService.isPlaying())
                    {
                        radioService.pause();
                        playPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play_light));
                    }
                    else
                    {
                        radioService.play();
                        playPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_pause_light));
                    }
                }
            }
        });


        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        RadioActivity.log("RadioFragment.onResume");

        Intent intent = new Intent(getActivity(), RadioService.class);
        intent.setAction(RadioService.ACTION_INIT);
        getActivity().startService(intent);

        intent = new Intent(getActivity(), RadioService.class);
        getActivity().getApplicationContext().bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);

        MetadataManager.addListener(this);
        MetadataManager.requestMetadata();
    }

    @Override
    public void onPause() {
        super.onPause();
        RadioActivity.log("RadioFragment.onPause");
        MetadataManager.removeListener(this);
        if(isBound)
        {
            getActivity().getApplicationContext().unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        RadioActivity.log("RadioFragment.onAttach");
//        ((RadioActivity) activity).onSectionAttached(
//                getArguments().getInt(ARG_SECTION_NUMBER));
        ((RadioActivity) activity).restoreActionBar(getString(R.string.app_name));
    }

    @Override
    public void onMetadataChanged(Metadata metadata) {
        currentMetadata = metadata;
        songTextView.setText(metadata.song);
        artistTextView.setText(metadata.artist);
    }
}