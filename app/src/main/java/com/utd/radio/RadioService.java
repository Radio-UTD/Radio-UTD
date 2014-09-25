package com.utd.radio;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.utd.radio.util.NaivePlsParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class RadioService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public static final String urlStr = "http://ghost.wavestreamer.com:5674/listen.pls?sid=1";
    private static final String WIFILOCK_TAG = "RadioService.Wifilock";
    public static final String ACTION_INIT = "ACTION_INIT";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";

    private boolean playerReady;
    private MediaPlayer mediaPlayer;

    private OnReadyListener onReadyListener;

    private WifiManager.WifiLock wifiLock;

    // TODO: cpu lock
    // TODO: wake lock

    private RadioBinder binder = new RadioBinder();

    public static String CurrentArtist = "";
    public static String CurrentAlbum = "";
    public static String CurrentSong = "";
    public static String CurrentShow = "";
    public static String CurrentShowTime = "";

    public RadioService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RadioActivity.log("RadioService.onStartCommand");
        RadioActivity.log("\tReceived intent: " + intent);
        RadioActivity.log("\tReceived intent action: " + ((intent != null) ? intent.getAction() : "no action"));
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

        return Service.START_REDELIVER_INTENT;
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
                    NaivePlsParser parser = new NaivePlsParser(new URL(urlStr));
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

            updateCurrentlyPlaying(true);
        }
    }


    public void updateCurrentlyPlaying(boolean isPlaying) {

        if(isPlaying == false) {
            CurrentArtist = "";
            CurrentAlbum = "";
            CurrentSong = "";
            CurrentShow = "";
        } else {
            AsyncTask<String, Void, String> getting = new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    try
                    {
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpGet httpget = new HttpGet("http://www.radioutd.com/tuner/reload.php");
                        HttpResponse response = httpclient.execute(httpget);
                        HttpEntity entity = response.getEntity();
                        InputStream is = entity.getContent();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                        StringBuilder sb = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null)
                            sb.append(line + "\n");
                        String bodyHtml = sb.toString();
                        is.close();


                        CurrentArtist = bodyHtml.substring(bodyHtml.indexOf("<artist>") + 8, bodyHtml.indexOf("</artist>"));
                        CurrentAlbum = bodyHtml.substring(bodyHtml.indexOf("<song>") + 6, bodyHtml.indexOf("</song>"));
                        CurrentSong = bodyHtml.substring(bodyHtml.indexOf("<album>") + 6, bodyHtml.indexOf("</album>"));
                        CurrentShow = "???";
                        CurrentShowTime = "???";

                        Toast.makeText(RadioService.this, CurrentSong, Toast.LENGTH_LONG).show();

                    } catch (MalformedURLException e) {
                        Toast.makeText(RadioService.this, "Dumb thing number 1", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    } catch (IOException e) {
                        Toast.makeText(RadioService.this, "Dumb thing number 2", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }


                    return null;
                }

                @Override
                protected void onPostExecute(String url) {
                    Intent avrcp = new Intent("com.android.music.metachanged");
                    avrcp.putExtra("track", CurrentSong);
                    avrcp.putExtra("artist", CurrentArtist);
                    avrcp.putExtra("album", CurrentAlbum);
                }
            }.execute();
        }
    }

    public void pause()
    {
        RadioActivity.log("RadioService.Pause");
        if(mediaPlayer != null)
            mediaPlayer.pause();
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
        mediaPlayer = null;

        if(wifiLock != null)
            wifiLock.release();
        wifiLock = null;

        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        RadioActivity.log("RadioService.onCreate");
        playerReady = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        RadioActivity.log("RadioService.onBind");
        return binder;
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
}
