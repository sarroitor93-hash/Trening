package pl.igor.trening;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    static final String URL = "https://sarroitor93-hash.github.io/Trening/";
    WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Notif.channels(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " TreningApp/1");
        web.addJavascriptInterface(new Bridge(this), "TreningNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (u.toString().startsWith(URL)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });
        setContentView(web);
        if (b != null) web.restoreState(b); else web.loadUrl(URL);
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); web.saveState(o); }

    @Override
    protected void onResume() {
        super.onResume();
        // Otwarcie aplikacji wycisza alarm końca przerwy (dalej sygnalizuje sama aplikacja).
        getSystemService(android.app.NotificationManager.class).cancel(Notif.ID_ALERT);
        if (getSharedPreferences(Notif.PREFS, Context.MODE_PRIVATE).getBoolean("stopped", false)) {
            getSharedPreferences(Notif.PREFS, Context.MODE_PRIVATE).edit().putBoolean("stopped", false).apply();
            web.evaluateJavascript("window.nativeStopped&&window.nativeStopped()", null);
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    static class Bridge {
        final Activity a;
        Bridge(Activity a) { this.a = a; }

        @JavascriptInterface public int version() { return 1; }

        @JavascriptInterface
        public void restStart(double endMs, String title, String next, boolean sound, boolean vibrate) {
            Notif.restStart(a.getApplicationContext(), (long) endMs, title, next, sound, vibrate);
        }

        @JavascriptInterface public void restStop() { Notif.restStop(a.getApplicationContext()); }

        @JavascriptInterface public void status(String title, String text) { Notif.status(a.getApplicationContext(), title, text); }

        @JavascriptInterface public void clear() { Notif.clear(a.getApplicationContext()); }

        @JavascriptInterface
        public void vibrate(int ms) {
            Vibrator v = a.getSystemService(Vibrator.class);
            if (v != null) v.vibrate(VibrationEffect.createOneShot(Math.max(20, ms), VibrationEffect.DEFAULT_AMPLITUDE));
        }

        @JavascriptInterface
        public void keepScreenOn(final boolean on) {
            a.runOnUiThread(() -> {
                if (on) a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }
    }
}
