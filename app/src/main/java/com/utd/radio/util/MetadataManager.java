package com.utd.radio.util;

import android.os.AsyncTask;

import com.utd.radio.RadioActivity;
import com.utd.radio.listeners.OnMetadataChangedListener;
import com.utd.radio.models.Metadata;

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
    private  static final String METADATA_URL = "http://www.radioutd.com/tuner/reload.php";
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

                String text = builder.toString();
                Metadata metadata = new Metadata();
                // TODO: Have a better check to see if we actually got a song back
                // ideally we'd have an api to check
                if(!text.contains("<artist>"))
                {
                    metadata.song = "Offair";
                    metadata.artist = "Offair Playlist";
                }
                else
                {
                    metadata.song = text.substring(text.indexOf("<song>") + 6, text.indexOf("</song>"));
                    metadata.artist = text.substring(text.indexOf("<artist>") + 8, text.indexOf("</artist>"));
                    metadata.album = text.substring(text.indexOf("<album>") + 7, text.indexOf("</album>"));
                }
                return metadata;
            } catch (IOException e) {
                RadioActivity.log("Failed to get metadata");
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