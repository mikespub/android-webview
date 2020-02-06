package net.mikespub.mywebview;

import android.app.Activity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

// http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
class AppJavaScriptProxy {

    private final Activity activity;
    private final WebView  webView;

    public AppJavaScriptProxy(Activity activity, WebView webview) {

        this.activity = activity;
        this.webView  = webview;
    }

    @JavascriptInterface
    public void showMessage(final String message) {

        final Activity myActivity = this.activity;
        final WebView myWebView = this.webView;

        // https://developer.android.com/guide/webapps/migrating#Threads
        this.activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(!myWebView.getUrl().startsWith("https://appassets.androidplatform.net/")){
                    Log.d("WebView", "Javascript Interface for " + myWebView.getUrl());
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
