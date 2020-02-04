package net.mikespub.mywebview;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

// http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
class AppJavaScriptProxy {

    private Activity activity;
    private WebView  webView;

    public AppJavaScriptProxy(Activity activity, WebView webview) {

        this.activity = activity;
        this.webView  = webview;
    }

    @JavascriptInterface
    public void showMessage(final String message) {

        final Activity theActivity = this.activity;
        final WebView theWebView = this.webView;

        // https://developer.android.com/guide/webapps/migrating#Threads
        this.activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(!theWebView.getUrl().startsWith("https://appassets.androidplatform.net/")){
                    return;
                }

                Toast toast = Toast.makeText(
                        theActivity.getApplicationContext(),
                        message,
                        Toast.LENGTH_SHORT);

                toast.show();
            }
        });
    }
}
