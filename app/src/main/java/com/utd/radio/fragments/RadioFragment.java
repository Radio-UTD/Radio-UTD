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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.utd.radio.R;
import com.utd.radio.RadioActivity;
import com.utd.radio.RadioService;
import com.utd.radio.listeners.OnMetadataChangedListener;
import com.utd.radio.models.Metadata;
import com.utd.radio.util.MetadataManager;

public class RadioFragment extends Fragment implements OnMetadataChangedListener, RadioService.OnStateChangeListener {

    TextView artistTextView;
    TextView songTextView;
    ImageButton playPauseButton;
    CircularProgressView loadingAnim;

    RelativeLayout mainRadioLayout;
    RelativeLayout disconnectedLayout;

    RadioService radioService;
    boolean isBound;

    Metadata currentMetadata = new Metadata();

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;

            RadioActivity.log("RadioFragment.onServiceConnected");
            radioService = ((RadioService.RadioBinder) service).getService();
            radioService.setOnStateChangeListener(RadioFragment.this);
            onStateChange(radioService.getState());
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

        mainRadioLayout = (RelativeLayout) view.findViewById(R.id.main_radio_layout);
        disconnectedLayout = (RelativeLayout) view.findViewById(R.id.disconnected_layout);

        songTextView = (TextView) view.findViewById(R.id.player_song);
        artistTextView = (TextView) view.findViewById(R.id.player_artist);
        loadingAnim = (CircularProgressView) view.findViewById((R.id.loading_anim));
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
                        playPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_light));
                    }
                    else
                    {
                        radioService.play();
                        playPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_light));
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
            radioService.setOnStateChangeListener(null);
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

    @Override
    public void onStateChange(RadioService.RadioState state) {
        switch(state)
        {
            case CONNECTING:
            case BUFFERING:
                mainRadioLayout.setVisibility(View.VISIBLE);
                disconnectedLayout.setVisibility(View.INVISIBLE);
                playPauseButton.setEnabled(false);
                loadingAnim.setVisibility(View.VISIBLE);
                playPauseButton.setImageDrawable(null);
                break;
            case PAUSED:
                mainRadioLayout.setVisibility(View.VISIBLE);
                disconnectedLayout.setVisibility(View.INVISIBLE);
                playPauseButton.setEnabled(true);
                loadingAnim.setVisibility(View.INVISIBLE);
                playPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_light));
                break;
            case PLAYING:
                mainRadioLayout.setVisibility(View.VISIBLE);
                disconnectedLayout.setVisibility(View.INVISIBLE);
                playPauseButton.setEnabled(true);
                loadingAnim.setVisibility(View.INVISIBLE);
                playPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_light));
                break;
            case DISCONNECTED:
                mainRadioLayout.setVisibility(View.INVISIBLE);
                disconnectedLayout.setVisibility(View.VISIBLE);
                playPauseButton.setEnabled(false);
                break;
        }
    }
}