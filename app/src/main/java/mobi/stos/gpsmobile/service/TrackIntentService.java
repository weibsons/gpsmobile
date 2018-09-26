package mobi.stos.gpsmobile.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import mobi.stos.gpsmobile.MainActivity;


public class TrackIntentService extends IntentService {

    public TrackIntentService() {
        super("TrackIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty()) {
                String event = extras.getString("event");
                switch (event) {
                    case "stop":
                        stopService(new Intent(this, TrackService.class));
                        break;
                    case "open":
                        Intent i = new Intent(this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        break;
                }
            }
        }
        stopSelf();
    }

}
