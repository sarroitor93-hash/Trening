package pl.igor.trening;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RestReceiver extends BroadcastReceiver {
    static final String ACTION_END = "pl.igor.trening.REST_END";
    static final String ACTION_STOP = "pl.igor.trening.REST_STOP";

    @Override
    public void onReceive(Context c, Intent i) {
        if (ACTION_END.equals(i.getAction())) {
            Notif.restEnd(c);
        } else if (ACTION_STOP.equals(i.getAction())) {
            Notif.restStop(c);
            c.getSystemService(android.app.NotificationManager.class).cancel(Notif.ID_STATUS);
            c.getSharedPreferences(Notif.PREFS, Context.MODE_PRIVATE).edit().putBoolean("stopped", true).apply();
        }
    }
}
