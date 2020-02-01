package net.mikespub.mywebview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends AppCompatActivity {

    // https://developer.chrome.com/multidevice/webview/gettingstarted
    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myWebView = (WebView) findViewById(R.id.activity_main_webview);
        // Enable Javascript
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // See https://ukacademe.com/MobileApplication/AndroidGUI/Android_WebView
        // webSettings.setBuiltInZoomControls(true);
        // Stop local links and redirects from opening in browser instead of WebView
        myWebView.setWebViewClient(new MyAppWebViewClient());
        // myWebView.loadUrl("http://beta.html5test.com/");
        myWebView.loadUrl("http://www.html5rocks.com/");
    }

    // Note: this is different from https://developer.android.com/guide/webapps/webview#java
    // and http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    @Override
    public void onBackPressed() {
        if(myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

}
