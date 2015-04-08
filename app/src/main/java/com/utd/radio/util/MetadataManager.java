package com.utd.radio.util;

import android.os.AsyncTask;

import com.utd.radio.RadioActivity;
import com.utd.radio.listeners.OnMetadataChangedListener;
import com.utd.radio.models.Metadata;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MetadataManager
{
    private  static final String METADATA_URL = "http://radioutd.com/nowplaying/index.php";
    private static List<OnMetadataChangedListener> listeners = new ArrayList<OnMetadataChangedListener>();

    public static void addListener(OnMetadataChangedListener listener)
    {
        listeners.add(listener);
    }

    public static void removeListener(OnMetadataChangedListener listener)
    {
        listeners.remove(listener);
    }

    public static void requestMetadata()
    {
        new MetadataGetTask().execute(METADATA_URL);
    }

    private static class MetadataGetTask extends AsyncTask<String, Void, Metadata>
    {
        protected Metadata doInBackground(String... params) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(params[0]);
                conn = (HttpURLConnection) url.openConnection();
                InputStream inputStream = conn.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder builder = new StringBuilder();
                String line;
                while((line = in.readLine()) != null) {
                    builder.append(line);
                }

                JSONObject json = new JSONObject(builder.toString());
                Metadata metadata = new Metadata();
                // TODO: Have a better check to see if we actually got a song back
                // ideally we'd have an api to check
                if(!json.getString("onAir").equals("1"))
                {
                    metadata.showName = "Offair";
                    metadata.song = "Offair Playlist";
                }
                else
                {
                    JSONObject nowPlaying = json.getJSONObject("nowPlaying");
                    metadata.song = nowPlaying.getString("song");
                    metadata.artist = nowPlaying.getString("artist");
                    metadata.album = nowPlaying.getString("album");

                    JSONObject show = json.getJSONObject("show");
                    metadata.avatar = show.getString("avatar");
                    String showStr = show.getString("showName");
                    int splitIndex = showStr.lastIndexOf("with");
                    metadata.showName = showStr.substring(0, splitIndex-1);
                    metadata.showDJ = showStr.substring(splitIndex);

                }
                return metadata;
            } catch (IOException e) {
                RadioActivity.log("Failed to get metadata");
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if(conn != null)
                    conn.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Metadata metadata) {
            if(metadata == null)
                return;
            for(OnMetadataChangedListener listener : listeners)
                listener.onMetadataChanged(metadata);
        }
    }
}