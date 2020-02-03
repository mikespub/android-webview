package net.mikespub.mywebview;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

// See also Chrome Custom Tabs https://developer.chrome.com/multidevice/android/customtabs
// and Android Browser Helper https://github.com/GoogleChrome/android-browser-helper
public class MainActivity extends AppCompatActivity {

    // TODO: SavedStateViewModel see https://github.com/googlecodelabs/android-lifecycles/blob/master/app/src/main/java/com/example/android/lifecycles/step6_solution/SavedStateActivity.java

    // https://developer.chrome.com/multidevice/webview/gettingstarted
    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // /data/user/0/net.mikespub.mywebview/files
        // File filesdir = getFilesDir();
        // Log.d("Internal Files Dir", filesdir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files/Documents
        // File extdocsdir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        // Log.d("External Docs Dir", extdocsdir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files
        // File extfilesdir = getExternalFilesDir(null);
        // Log.d("External Files Dir", extfilesdir.getAbsolutePath());
        myWebView = findViewById(R.id.activity_main_webview);
        // Enable Javascript
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // See https://ukacademe.com/MobileApplication/AndroidGUI/Android_WebView
        // webSettings.setBuiltInZoomControls(true);
        WebView.setWebContentsDebuggingEnabled(true);
        // Stop local links and redirects from opening in browser instead of WebView
        myWebView.setWebViewClient(new MyAppWebViewClient(this));
        String myUrl = getString(R.string.website_url);
        // myWebView.loadUrl("http://beta.html5test.com/");
        myWebView.loadUrl(myUrl);
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
