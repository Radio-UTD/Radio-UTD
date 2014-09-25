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
import android.widget.Toast;

import com.utd.radio.R;
import com.utd.radio.RadioActivity;
import com.utd.radio.RadioService;

/**
 * A placeholder fragment containing a simple view.
 */
public class RadioFragment extends Fragment {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */

    ImageButton playPauseButton;

    RadioService radioService;
    boolean isBound;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;

            RadioActivity.log("RadioFragment.onServiceConnected");
            //Toast.makeText(RadioFragment.this.getActivity(), "Sucessfully binded to services!", Toast.LENGTH_SHORT).show();
            radioService = ((RadioService.RadioBinder) service).getService();
            playPauseButton.setImageDrawable(getResources().getDrawable(radioService.isPlaying() ? R.drawable.ic_action_pause : R.drawable.ic_action_play));
            playPauseButton.setEnabled(false);
            radioService.setOnReadyListener(new RadioService.OnReadyListener() {
                @Override
                public void onReady() {
                    playPauseButton.setEnabled(true);
                    //Click it as to auto-play the music
                    //We can remove this for later or put it into a setting
                    playPauseButton.performClick();
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
        Intent intent = new Intent(getActivity(), RadioService.class);
        intent.setAction(RadioService.ACTION_INIT);getActivity().startService(intent);
        View view = inflater.inflate(R.layout.fragment_radio, container, false);


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
                        playPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play));
                    }
                    else
                    {
                        radioService.play();
                        playPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_pause));
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
        getActivity().bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        RadioActivity.log("RadioFragment.onPause");
        if(isBound)
        {
            getActivity().unbindService(serviceConnection);
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
}