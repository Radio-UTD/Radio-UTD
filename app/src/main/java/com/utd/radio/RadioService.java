package com.utd.radio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.utd.radio.listeners.OnMetadataChangedListener;
import com.utd.radio.models.Metadata;
import com.utd.radio.receivers.ConnectionChangeReceiver;
import com.utd.radio.util.MetadataManager;
import com.utd.radio.util.NaivePlsParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RadioService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, OnMetadataChangedListener {
    private static final String STREAM_URL = "http://ghost.wavestreamer.com:5674/listen.pls?sid=1";
    private static final String WIFILOCK_TAG = "RadioService.Wifilock";
    public static final String ACTION_INIT = "com.utd.radio.INIT";
    public static final String ACTION_PLAY = "com.utd.radio.PLAY";
    public static final String ACTION_PAUSE = "com.utd.radio.PAUSE";
    public static final String ACTION_STOP = "com.utd.radio.STOP";

    public static final int NOTIFICATION_ID = 5;

    public enum RadioState {
        DISCONNECTED,
        CONNECTING,
        BUFFERING,
        PLAYING,
        PAUSED
    }

    private MediaPlayer mediaPlayer;
    private OnStateChangeListener onStateChangeListener;

    private WifiManager.WifiLock wifiLock;

    private RadioBinder binder = new RadioBinder();
    private boolean isBound = false;

    private Metadata currentMetadata = new Metadata();

    private ScheduledExecutorService scheduleTaskExecutor;

    private RadioState currentState;

    public RadioService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RadioActivity.log("RadioService.onStartCommand");

        if(null != intent && null != intent.getAction())
        {
            if(intent.getAction().equals(ACTION_INIT))
                initMediaPlayer(false);
            else if(intent.getAction().equals(ACTION_PLAY))
                play();
            else if(intent.getAction().equals(ACTION_PAUSE))
                pause();
            else if(intent.getAction().equals(ACTION_STOP))
                stop();
        }
        else {
            initMediaPlayer(true);
        }

        if(scheduleTaskExecutor == null)
        {
            scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
            scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    if(isPlaying())
                        MetadataManager.requestMetadata();
                }
            }, 0, 45, TimeUnit.SECONDS);
        }

        return Service.START_STICKY;
    }

    public void initMediaPlayer(final boolean autoPlay)
    {

        // Already initialized
        if(mediaPlayer != null)
            return;

        if(!isConnectionAvailable())
        {
            setState(RadioState.DISCONNECTED);
            return;
        }

        setState(RadioState.CONNECTING);

        RadioActivity.log("RadioService.initMediaPlayer");
        AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                try
                {
                    RadioActivity.log("RadioService.AsyncTask");
                    NaivePlsParser parser = new NaivePlsParser(new URL(STREAM_URL));
                    List<String> URLs = parser.getURLs();
                    if(URLs.isEmpty())
                        return null;
                    return URLs.get(0);
                } catch (MalformedURLException e) {
                    Toast.makeText(RadioService.this, "Something dumb happened", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (IOException e) {
                    Toast.makeText(RadioService.this, "Unable to find Radio UTD stream URL", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String url) {
                super.onPostExecute(url);
                try
                {
                    RadioActivity.log("RadioService.AsyncTask.PostExecute");
                    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

                    wifiLock = wifiManager.createWifiLock(WIFILOCK_TAG);
                    wifiLock.acquire();

                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setWakeMode(RadioService.this, PowerManager.PARTIAL_WAKE_LOCK);
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setDataSource(url);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            setState(RadioState.PAUSED);
                            if(autoPlay)
                                play();
                        }
                    });
                    mediaPlayer.setOnCompletionListener(RadioService.this);
                    mediaPlayer.setOnErrorListener(RadioService.this);
                    setState(RadioState.BUFFERING);
                    mediaPlayer.prepareAsync();
                } catch (IOException e) {
                    Toast.makeText(RadioService.this, "Unable to find Radio UTD stream URL", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        };
        task.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RadioActivity.log("RadioService.onDestroy");
        MetadataManager.removeListener(this);
    }

    public void play()
    {
        RadioActivity.log("RadioService.Play");
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int result = am.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        {
            RadioActivity.log("RadioService.Play Audio Focus Request Granted");
            if(mediaPlayer == null)
                initMediaPlayer(true);
            else
            {
                mediaPlayer.start();
                setState(RadioState.PLAYING);
                MetadataManager.requestMetadata();
                updateNotification();
            }
        }
        else if(result == AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        {
            RadioActivity.log("RadioService.Play Audio Focus Request Failed");
        }
    }



    public void pause()
    {
        RadioActivity.log("RadioService.Pause");
        if(mediaPlayer != null)
        {
            mediaPlayer.pause();
            setState(RadioState.PAUSED);
//            MetadataManager.requestMetadata();
            updateNotification();
        }

    }

    public void stop()
    {
        RadioActivity.log("RadioService.Stop");
        if(mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();

            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.abandonAudioFocus(audioFocusListener);
        }

        hideNotification();

        mediaPlayer = null;

        if(wifiLock != null)
            wifiLock.release();
        wifiLock = null;

//        MetadataManager.requestMetadata();
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        RadioActivity.log("RadioService.onCreate");
        MetadataManager.addListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        RadioActivity.log("RadioService.onBind");
        isBound = true;
        updateNotification();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        RadioActivity.log("RadioService.onUnbind");
        isBound = false;
        if(isPlaying())
            updateNotification();
        else
            stopSelf();
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        RadioActivity.log("RadioService.onRebind");
        isBound = true;
        updateNotification();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        RadioActivity.log("RadioService.onError");
        return false;
    }

    public boolean isPlaying() {
        if(mediaPlayer != null)
            return RadioService.this.mediaPlayer.isPlaying();
        return false;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener){
        onStateChangeListener = listener;
    }

    public class RadioBinder extends Binder {
        public RadioService getService() {
            return RadioService.this;
        }
    }

    public interface OnStateChangeListener {
        public void onStateChange(RadioState state);
    }

    AudioManager.OnAudioFocusChangeListener audioFocusListener =  new AudioManager.OnAudioFocusChangeListener() {

        AudioManager audio;
        int lastKnownAudioFocusState;
        int duckingVolume = -1;
        boolean wasPlayingWhenTransientLoss = false;

        @Override
        public void onAudioFocusChange(int focusChange) {
            RadioActivity.log("RadioService.onAudioFocusChange " +focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:

                    switch(lastKnownAudioFocusState) {
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if(wasPlayingWhenTransientLoss) {
                                play();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            restoreVolume();
                            break;
                        default:
                            if(!isPlaying()) {
                                play();
                            }
                            break;
                    }

                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    stop();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    wasPlayingWhenTransientLoss = isPlaying();
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    duckVolume();
                    break;
            }
            lastKnownAudioFocusState = focusChange;
        }

        private void duckVolume()
        {
            audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            duckingVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(duckingVolume*.3f), 0);
        }

        private void restoreVolume()
        {
            audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, duckingVolume, 0);
        }
    };

    private void updateNotification()
    {
        if(isBound)
        {
            hideNotification();
        }
        else
        {
            RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_radio_player);
            contentView.setTextViewText(R.id.notification_title, currentMetadata.song);
            contentView.setTextViewText(R.id.notification_subtitle, currentMetadata.artist);
            Intent playPauseIntent;
            Intent stopIntent = new Intent(ACTION_STOP);
            if(isPlaying()) {
                int icon = (Build.VERSION.SDK_INT >= 21) ? R.drawable.ic_pause : R.drawable.ic_pause_light;
                contentView.setImageViewResource(R.id.notification_play_pause_button, icon);
                playPauseIntent = new Intent(ACTION_PAUSE);
            }
            else {
                int icon = (Build.VERSION.SDK_INT >= 21) ? R.drawable.ic_play_arrow : R.drawable.ic_play_arrow_light;
                contentView.setImageViewResource(R.id.notification_play_pause_button, icon);
                playPauseIntent = new Intent(ACTION_PLAY);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, RadioActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            contentView.setOnClickPendingIntent(R.id.notification_play_pause_button, playPausePendingIntent);
            contentView.setOnClickPendingIntent(R.id.notification_close_button, stopPendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContent(contentView)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void hideNotification() {
        stopForeground(true);
    }

    @Override
    public void onMetadataChanged(Metadata metadata) {
        RadioActivity.log("RadioService.onMetadataChanged");
        currentMetadata = metadata;
        Intent avrcp = new Intent("com.android.music.metachanged");
            avrcp.putExtra("track", metadata.song);
            avrcp.putExtra("artist", metadata.artist);
            avrcp.putExtra("album", metadata.album);
        sendBroadcast(avrcp);
        updateNotification();
    }

    public RadioState getState()
    {
        return currentState;
    }


    private void setState(RadioState newState)
    {
        RadioState oldState = currentState;
        currentState = newState;
        if(onStateChangeListener != null && oldState != newState)
            onStateChangeListener.onStateChange(newState);

        setConnectionChangeListener(newState == RadioState.DISCONNECTED);
    }

    private void setConnectionChangeListener(boolean enabled)
    {
        ComponentName receiver = new ComponentName(this, ConnectionChangeReceiver.class);

        PackageManager pm = getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private boolean isConnectionAvailable()
    {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
