package com.utd.radio.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.utd.radio.RadioService;

public class ConnectionChangeReceiver extends BroadcastReceiver {
    public ConnectionChangeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // When receiving a connection change broadcast,
        // try reinitializing the service
        Intent serviceIntent = new Intent(context, RadioService.class);
        serviceIntent.setAction(RadioService.ACTION_INIT);
        context.startService(serviceIntent);
    }
}
