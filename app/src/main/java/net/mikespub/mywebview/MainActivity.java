package net.mikespub.mywebview;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

// See also Chrome Custom Tabs https://developer.chrome.com/multidevice/android/customtabs
// and Android Browser Helper https://github.com/GoogleChrome/android-browser-helper
public class MainActivity extends AppCompatActivity {

    // TODO: SavedStateViewModel see https://github.com/googlecodelabs/android-lifecycles/blob/master/app/src/main/java/com/example/android/lifecycles/step6_solution/SavedStateActivity.java
    // or with AndroidViewModel see https://github.com/husaynhakeem/Androidx-SavedState-Playground/blob/master/app/src/main/java/com/husaynhakeem/savedstateplayground/AndroidViewModelWithSavedState.kt
    //private MySavedStateModel mySavedStateModel;

    // https://developer.chrome.com/multidevice/webview/gettingstarted
    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
        //this.mySavedStateModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(MySavedStateModel.class);
        //HashMap<String, Object> hashMap = mySavedStateModel.getSettings(this);
        //Log.d("State Get", hashMap.toString());

        myWebView = findViewById(R.id.activity_main_webview);
        // Enable Javascript
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // See https://ukacademe.com/MobileApplication/AndroidGUI/Android_WebView
        // webSettings.setBuiltInZoomControls(true);
        WebView.setWebContentsDebuggingEnabled(true);
        // Stop local links and redirects from opening in browser instead of WebView
        myWebView.setWebViewClient(new MyAppWebViewClient(this));
        // Add Javascript interface
        // myWebView.addJavascriptInterface(new AppJavaScriptProxy(this, myWebView), "androidAppProxy");
        // https://stackoverflow.com/questions/36987144/preventing-webview-reload-on-rotate-android-studio/46849736#46849736
        if (savedInstanceState == null) {
            // myWebView.loadUrl("http://beta.html5test.com/");
            String myUrl = getString(R.string.website_url);
            myWebView.loadUrl(myUrl);
        } else {
            // Bundle bundle = savedInstanceState.getBundle("webViewState");
            // Log.d("Web Create", bundle.toString());
            // myWebView = findViewById(R.id.activity_main_webview);
            // Stop local links and redirects from opening in browser instead of WebView
            // myWebView.setWebViewClient(new MyAppWebViewClient(this));
            // myWebView.restoreState(bundle);
        }
        // https://stackoverflow.com/questions/19365668/getting-webview-history-from-webbackforwardlist
        // https://stackoverflow.com/questions/33326833/save-state-of-webview-when-switching-activity
    }

    // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
    public MySavedStateModel getSavedStateModel() {
        return new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(MySavedStateModel.class);
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

    // https://stackoverflow.com/questions/39086084/save-webview-state-on-screen-rotation
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        myWebView.saveState(outState);
        Log.d("Web Save", outState.toString());
        //Bundle bundle = new Bundle();
        //myWebView.saveState(bundle);
        //Log.d("Web Save", bundle.toString());
        //outState.putBundle("webViewState", bundle);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("Web Restore", savedInstanceState.toString());
        myWebView.restoreState(savedInstanceState);
        //Bundle bundle = savedInstanceState.getBundle("webViewState");
        //Log.d("Web Restore", bundle.toString());
        // myWebView.restoreState(bundle);
    }
}
