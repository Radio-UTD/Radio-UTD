package com.utd.radio.util;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
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

    public static void requestMetadata(Context context)
    {
        Ion.with(context).load(METADATA_URL).asJsonObject().setCallback(new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject json) {
                if(e != null)
                    return;
                Metadata metadata = new Metadata();
                if(json.get("onAir").getAsInt() != 1)
                {
                    metadata.showName = "Offair";
                    metadata.song = "Offair Playlist";
                }
                else
                {
                    JsonObject nowPlaying = json.get("nowPlaying").getAsJsonObject();
                    metadata.song = nowPlaying.get("song").getAsString();
                    metadata.artist = nowPlaying.get("artist").getAsString();
                    metadata.album = nowPlaying.get("album").getAsString();

                    JsonObject show = json.get("show").getAsJsonObject();
                    metadata.avatar = show.get("avatar").getAsString();
                    String showStr = show.get("showName").getAsString();
                    int splitIndex = showStr.lastIndexOf("with");
                    metadata.showName = showStr.substring(0, splitIndex-1);
                    metadata.showDJ = showStr.substring(splitIndex);

                }
                for(OnMetadataChangedListener listener : listeners)
                    listener.onMetadataChanged(metadata);
            }
        });
    }
}