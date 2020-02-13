package net.mikespub.mywebview;

import android.net.Uri;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * JavaScript Interface Proxy to Android App
 */
// http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
class AppJavaScriptProxy {

    private final AppCompatActivity activity;
    private final WebView  webView;

    /**
     * @param activity  current Activity context
     * @param webview   current WebView context
     */
    AppJavaScriptProxy(AppCompatActivity activity, WebView webview) {

        this.activity = activity;
        this.webView  = webview;
    }

    /**
     * @param message   javascript message to show with Toast
     */
    @JavascriptInterface
    public void showMessage(final String message) {

        final AppCompatActivity myActivity = this.activity;
        final WebView myWebView = this.webView;

        // https://developer.android.com/guide/webapps/migrating#Threads
        this.activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Uri uri = Uri.parse(myWebView.getUrl());
                if(!uri.getHost().equals(myActivity.getString(R.string.app_host))){
                    Log.d("WebView", "No Javascript Interface for " + uri.toString());
                    return;
                }

                Toast toast = Toast.makeText(
                        myActivity.getApplicationContext(),
                        message,
                        Toast.LENGTH_SHORT);

                toast.show();
            }
        });
    }
}
