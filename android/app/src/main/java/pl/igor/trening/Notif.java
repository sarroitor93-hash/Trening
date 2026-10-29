package pl.igor.trening;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

/** Powiadomienia treningu: licznik przerwy (chronometr systemowy) i alarm na koniec przerwy. */
final class Notif {
    static final String CH_STATUS = "status";
    static final String CH_SOUND = "alert_sound";
    static final String CH_VIB = "alert_vib";
    static final String CH_QUIET = "alert_quiet";
    static final int ID_STATUS = 1;
    static final int ID_ALERT = 2;
    static final String PREFS = "trening";

    static void channels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel st = new NotificationChannel(CH_STATUS, "Status treningu", NotificationManager.IMPORTANCE_LOW);
        st.setShowBadge(false);
        nm.createNotificationChannel(st);

        NotificationChannel snd = new NotificationChannel(CH_SOUND, "Koniec przerwy (dźwięk i wibracja)", NotificationManager.IMPORTANCE_HIGH);
        snd.enableVibration(true);
        snd.setVibrationPattern(new long[]{0, 300, 150, 300});
        snd.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        nm.createNotificationChannel(snd);

        NotificationChannel vib = new NotificationChannel(CH_VIB, "Koniec przerwy (tylko wibracja)", NotificationManager.IMPORTANCE_HIGH);
        vib.enableVibration(true);
        vib.setVibrationPattern(new long[]{0, 300, 150, 300});
        vib.setSound(null, null);
        nm.createNotificationChannel(vib);

        NotificationChannel q = new NotificationChannel(CH_QUIET, "Koniec przerwy (bez dźwięku)", NotificationManager.IMPORTANCE_DEFAULT);
        q.enableVibration(false);
        q.setSound(null, null);
        nm.createNotificationChannel(q);
    }

    static PendingIntent openApp(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent stopAction(Context c) {
        Intent i = new Intent(c, RestReceiver.class).setAction(RestReceiver.ACTION_STOP);
        return PendingIntent.getBroadcast(c, 2, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, RestReceiver.class).setAction(RestReceiver.ACTION_END);
        return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Licznik przerwy odliczany przez system + alarm dokładnie na koniec. */
    static void restStart(Context c, long endMs, String title, String next, boolean sound, boolean vibrate) {
        channels(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.cancel(ID_ALERT);
        Notification.Builder b = new Notification.Builder(c, CH_STATUS)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(next)
                .setWhen(endMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_STOPWATCH)
                .setContentIntent(openApp(c))
                .addAction(new Notification.Action.Builder(null, "Stop", stopAction(c)).build());
        nm.notify(ID_STATUS, b.build());

        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString("next", next).putBoolean("sound", sound).putBoolean("vib", vibrate).apply();

        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMs, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMs, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMs, pi);
        }
    }

    /** Alarm końca przerwy: powtarza dźwięk, dopóki nie klikniesz Stop albo nie otworzysz aplikacji. */
    static void restEnd(Context c) {
        channels(c);
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean sound = p.getBoolean("sound", true), vib = p.getBoolean("vib", true);
        String ch = sound ? CH_SOUND : (vib ? CH_VIB : CH_QUIET);
        String next = p.getString("next", "");
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.cancel(ID_STATUS);
        Notification n = new Notification.Builder(c, ch)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Czas na serię!")
                .setContentText(next)
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openApp(c))
                .addAction(new Notification.Action.Builder(null, "Stop", stopAction(c)).build())
                .build();
        if (sound || vib) n.flags |= Notification.FLAG_INSISTENT;
        nm.notify(ID_ALERT, n);
    }

    static void status(Context c, String title, String text) {
        channels(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        Notification n = new Notification.Builder(c, CH_STATUS)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openApp(c))
                .build();
        nm.notify(ID_STATUS, n);
    }

    static void restStop(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.cancel(alarmIntent(c));
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.cancel(ID_ALERT);
    }

    static void clear(Context c) {
        restStop(c);
        c.getSystemService(NotificationManager.class).cancel(ID_STATUS);
    }
}
