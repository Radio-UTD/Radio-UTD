package com.utd.radio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.utd.radio.listeners.OnMetadataChangedListener;
import com.utd.radio.models.Metadata;
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
    private  static final String METADATA_URL = "http://www.radioutd.com/tuner/reload.php";

    private static final String WIFILOCK_TAG = "RadioService.Wifilock";
    public static final String ACTION_INIT = "com.utd.radio.INIT";
    public static final String ACTION_PLAY = "com.utd.radio.PLAY";
    public static final String ACTION_PAUSE = "com.utd.radio.PAUSE";
    public static final String ACTION_STOP = "com.utd.radio.STOP";

    public static final int NOTIFICATION_ID = 5;

    private boolean playerReady;
    private MediaPlayer mediaPlayer;
    private OnReadyListener onReadyListener;

    private WifiManager.WifiLock wifiLock;

    private RadioBinder binder = new RadioBinder();
    private boolean isBound = false;

    private Metadata currentMetadata = new Metadata();

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

        ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                MetadataManager.requestMetadata(METADATA_URL);
            }
        }, 0, 40, TimeUnit.SECONDS);

        return Service.START_STICKY;
    }

    public void initMediaPlayer(final boolean autoPlay)
    {

        // Already initialized
        if(mediaPlayer != null)
            return;

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
                    String url = URLs.get(0);
                    return url;
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
                            playerReady = true;
                            onReadyListener.onReady();
                            if(autoPlay)
                                play();
                        }
                    });
                    mediaPlayer.setOnCompletionListener(RadioService.this);
                    mediaPlayer.setOnErrorListener(RadioService.this);

                    mediaPlayer.prepareAsync();
                } catch (IOException e) {
                    Toast.makeText(RadioService.this, "Unable to find Radio UTD stream URL", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }.execute();
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
        if(mediaPlayer == null)
            initMediaPlayer(true);
        else
        {
            mediaPlayer.start();

            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            MetadataManager.requestMetadata(METADATA_URL);
            updateNotification();
        }
    }



    public void pause()
    {
        RadioActivity.log("RadioService.Pause");
        if(mediaPlayer != null)
        {
            mediaPlayer.pause();
            MetadataManager.requestMetadata(METADATA_URL);
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
            playerReady = false;

            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.abandonAudioFocus(audioFocusListener);
        }

        hideNotification();

        mediaPlayer = null;

        if(wifiLock != null)
            wifiLock.release();
        wifiLock = null;

        MetadataManager.requestMetadata(METADATA_URL);
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        RadioActivity.log("RadioService.onCreate");
        playerReady = false;
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
        updateNotification();
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

    public boolean isReady() {
        return playerReady;
    }

    public void setOnReadyListener(OnReadyListener listener){
        onReadyListener = listener;
        if(playerReady)
            onReadyListener.onReady();
    }

    public class RadioBinder extends Binder {


        public RadioService getService() {
            return RadioService.this;
        }

    }

    public interface OnReadyListener {

        public void onReady();
    }
    AudioManager.OnAudioFocusChangeListener audioFocusListener =  new AudioManager.OnAudioFocusChangeListener() {
        int duckingVolume = -1;
        AudioManager audio;
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch(focusChange)
            {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                    duckingVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(maxVolume*.3f), 0);
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    duckingVolume = -1;
                    pause();
                    break;

                case AudioManager.AUDIOFOCUS_LOSS:
                    duckingVolume = -1;
                    stop();
                    break;

                case AudioManager.AUDIOFOCUS_GAIN:
                    duckingVolume = -1;
                    play();
                    if(duckingVolume != -1)
                    {
                        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, duckingVolume, 0);
                    }
                    break;

            }
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
                contentView.setImageViewResource(R.id.notification_play_pause_button, R.drawable.ic_action_pause_light);
                playPauseIntent = new Intent(ACTION_PAUSE);
            }
            else {
                contentView.setImageViewResource(R.id.notification_play_pause_button, R.drawable.ic_action_play_light);
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

            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void hideNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
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
}
