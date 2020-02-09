package net.mikespub.mywebview;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
class MyAppWebViewClient extends WebViewClient {
    static final String domainName;
    static final String domainUrl;

    static {
        domainName = "appassets.androidplatform.net";
        domainUrl = "https://" + domainName + "/";
    }

    // http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    private final MainActivity activity;
    private WebViewAssetLoader assetLoader;
    private String[][] myMatchCompare;
    private String[][] mySkipCompare;
    // SavedStateViewModel see https://github.com/googlecodelabs/android-lifecycles/blob/master/app/src/main/java/com/example/android/lifecycles/step6_solution/SavedStateActivity.java
    // or with AndroidViewModel see https://github.com/husaynhakeem/Androidx-SavedState-Playground/blob/master/app/src/main/java/com/husaynhakeem/savedstateplayground/AndroidViewModelWithSavedState.kt
    private final MySavedStateModel mySavedStateModel;
    // https://androidclarified.com/android-downloadmanager-example/
    private DownloadManager mDownloadManager;
    //private File mDownloadFile;
    private long mDownloadId = -1;
    private Map<String, Object> myDefaultWebSettings;
    private Map<String, Object> myCustomWebSettings;

    /**
     * @param activity  current Activity context
     */
    MyAppWebViewClient(MainActivity activity) {
        this.activity = activity;
        Log.d("Web Create", activity.toString());
        // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
        this.mySavedStateModel = activity.getSavedStateModel();
        loadSettings();
        setReceiver();
    }

    /**
     * Set BroadcastReceiver for download complete
     */
    private void setReceiver() {
        activity.stopReceiver();
        activity.onDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Fetching the download id received with the broadcast
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                //Checking if the received broadcast is for our enqueued download by matching download id
                if ((id <= -1) || (mDownloadId != id) || (mDownloadManager == null)) {
                    return;
                }
                Log.d("Web Update", "Receive: " + mDownloadId);
                // query download status
                //status = MyContentUtility.getDownloadStatus(activity, mDownloadId);
                Uri uri = mDownloadManager.getUriForDownloadedFile(mDownloadId);
                // Format: content://downloads/all_downloads/63
                Log.d("Web Update", "Uri: " + uri);
                // Note: this could be automatically renamed if the file already exists, e.g. assets-1.zip instead of assets.zip
                if (uri == null) {
                    return;
                }
                //MyContentUtility.showContent(activity, uri);
                // unzip
                //Log.d("Web Update", mDownloadFile.getAbsolutePath());
                try {
                    InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                    File targetDirectory = activity.getExternalFilesDir(null);
                    MyAssetUtility.unzipStream(inputStream, targetDirectory);
                    if (inputStream != null)
                        inputStream.close();
                    // delete entry in Downloads to avoid multiple duplicates there
                    activity.getContentResolver().delete(uri,null,null);
                } catch (Exception e) {
                    Log.e("Web Update", e.toString());
                }
            }
        };
        Log.d("Web Create", "register receiver");
        activity.registerReceiver(activity.onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    /**
     * Get setting
     *
     * @return  setting
     */
    Boolean hasDebuggingEnabled() {
        return (Boolean) mySavedStateModel.getValue("remote_debug");
    }

    /**
     * Get setting
     *
     * @return  setting
     */
    Boolean hasConsoleLog() {
        return (Boolean) mySavedStateModel.getValue("console_log");
    }

    /**
     * Get setting
     *
     * @return  setting
     */
    Boolean hasJavascriptInterface() {
        return (Boolean) mySavedStateModel.getValue("js_interface");
    }

    /**
     * Get setting
     *
     * @return  setting
     */
    Boolean hasContextMenu() {
        return (Boolean) mySavedStateModel.getValue("context_menu");
    }

    /**
     * Get setting
     *
     * @return  setting
     */
    Boolean hasNotMatching() {
        return (Boolean) mySavedStateModel.getValue("not_matching");
    }

    /**
     * Get setting
     *
     * @return  setting
     */
    String getUpdateZip() {
        return (String) mySavedStateModel.getValue("update_zip");
    }

    /**
     * Get custom Web Settings
     *
     * @return  setting
     */
    Map<String, Object> getWebSettings() {
        return (Map<String, Object>) mySavedStateModel.getValue("web_settings");
    }

    /**
     * Set custom Web Settings
     *
     * @param webSettings   current WebSettings
     */
    void setWebSettings(WebSettings webSettings) {
        myCustomWebSettings = getWebSettings();
        if (myCustomWebSettings == null) {
            Log.d("WebSettings", "No custom settings");
            return;
        }
        Log.d("WebSettings", myCustomWebSettings.toString());
        //Map<String, Object> defaultSettings = MyReflectUtility.getValues(webSettings);
        myDefaultWebSettings = MyReflectUtility.getValues(webSettings);
        //mySavedStateModel.setValue("web_settings", defaultSettings);
        //Log.d("WebView", "WebSettings: " + defaultSettings.toString());
        for (String key: myCustomWebSettings.keySet()) {
            if (!myDefaultWebSettings.containsKey(key)) {
                Log.d("WebSettings", "Unknown key: " + key);
                continue;
            }
            Log.d("WebSettings", "Key: " + key + " - default: " + myDefaultWebSettings.get(key) + " - custom: " + myCustomWebSettings.get(key));
            if ((myCustomWebSettings.get(key) == null) || (myDefaultWebSettings.get(key) == myCustomWebSettings.get(key)) || ((myDefaultWebSettings.get(key) instanceof String) && myDefaultWebSettings.get(key).equals(myCustomWebSettings.get(key)))) {
            } else {
                MyReflectUtility.set(webSettings, key, myCustomWebSettings.get(key));
            }
        }
        //for (String key: myDefaultWebSettings.keySet()) {
        //    MyReflectUtility.set(webSettings, key, myDefaultWebSettings.get(key));
        //}
    }

    /**
     * Load current settings from Saved State in ViewModel
     */
    private void loadSettings() {
        HashMap<String, Object> hashMap = mySavedStateModel.getSettings(this.activity);
        Log.d("State Get", hashMap.toString());
        if (hashMap == null) {
            loadStringConfig();
            return;
        }
        // create array of arrays once for comparison
        ArrayList<Object> matchArray = (ArrayList<Object>) hashMap.get("match");
        this.myMatchCompare = new String[matchArray.size()][3];
        int i = 0;
        for (Object m: matchArray) {
            int j = 0;
            for (String c: (ArrayList<String>) m) {
                this.myMatchCompare[i][j] = c;
                j++;
            }
            i++;
        }
        Log.d("Settings", Arrays.deepToString(this.myMatchCompare));
        ArrayList<Object> skipArray = (ArrayList<Object>) hashMap.get("skip");
        this.mySkipCompare = new String[skipArray.size()][3];
        i = 0;
        for (Object s: skipArray) {
            int j = 0;
            for (String c: (ArrayList<String>) s) {
                this.mySkipCompare[i][j] = c;
                j++;
            }
            i++;
        }
        Log.d("Settings", Arrays.deepToString(this.mySkipCompare));
    }

    /**
     * Load fallback settings from strings
     */
    private void loadStringConfig() {
        String[] myMatchArr = this.activity.getResources().getStringArray(R.array.url_match);
        String[] mySkipArr = this.activity.getResources().getStringArray(R.array.url_skip);
        // create array of arrays once for comparison
        this.myMatchCompare = new String[myMatchArr.length][3];
        for (int i = 0; i < myMatchArr.length; i++) {
            this.myMatchCompare[i] = myMatchArr[i].split("\\|");
        }
        Log.d("Settings", Arrays.deepToString(this.myMatchCompare));
        this.mySkipCompare = new String[mySkipArr.length][3];
        for (int j = 0; j < mySkipArr.length; j++) {
            this.mySkipCompare[j] = mySkipArr[j].split("\\|");
        }
        Log.d("Settings", Arrays.deepToString(this.mySkipCompare));
    }

    /**
     * @param var   variable to compare
     * @param oper  operator for comparison
     * @param value value to compare with
     * @return      comparison
     */
    // https://stackoverflow.com/questions/160970/how-do-i-invoke-a-java-method-when-given-the-method-name-as-a-string
    // TODO: create hashmap of methods?
    private boolean compare(String var, String oper, String value) {
        if (var == null) {
            return false;
        }
        // with single parameter, return boolean
        try {
            Method method;
            if (oper.equals("equals")) {
                method = var.getClass().getMethod(oper, Object.class);
            } else if (oper.equals("contains")) {
                method = var.getClass().getMethod(oper, CharSequence.class);
            } else {
                method = var.getClass().getMethod(oper, value.getClass());
            }
            Log.d("WebView Compare", "Method " + oper + ": " + method);
            boolean result = (boolean) method.invoke(var, value); // pass arg
            Log.d("WebView Compare", "Result " + value + ": " + result);
            return result;
        } catch (Exception e) {
            Log.e("WebView Compare", e.toString());
            return false;
        }
    }

    /**
     * Check if we need to override a particular url and/or create an Intent
     *
     * @param view  current WebView context
     * @param url   url to check for override
     * @return      decision to override or not
     */
    // https://stackoverflow.com/questions/41972463/android-web-view-shouldoverrideurlloading-deprecated-alternative/41973017
    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.d("Web Override", url);
        if(url.startsWith(domainUrl)) {
            return false;
        }
        final Uri uri = Uri.parse(url);
        // if(Uri.parse(url).getHost().endsWith("html5rocks.com")) {
        //final String host = uri.getHost();
        //final String path = uri.getPath();
        //final String query = uri.getQuery();
        HashMap<String, String> pieces = new HashMap<>();
        pieces.put("host", uri.getHost());
        pieces.put("path", uri.getPath());
        pieces.put("query", uri.getQuery());
        pieces.put("url", uri.toString());
        boolean isMatch = false;
        boolean isSkip = false;
        // https://stackoverflow.com/questions/160970/how-do-i-invoke-a-java-method-when-given-the-method-name-as-a-string
        //for (String m: this.myMatchArr) {
        //    String[] compare = m.split("\\|");
        for (String[] compare: this.myMatchCompare) {
            String value = pieces.get(compare[0]);
            Log.d("WebView Match", "Value " + compare[0] + ": " + value);
            boolean result = this.compare(value, compare[1], compare[2]);
            if (!result) {
                continue;
            }
            isMatch = true;
            isSkip = false;
            //for (String s: this.mySkipArr) {
            //    String[] check = s.split("\\|");
            for (String[] check: this.mySkipCompare) {
                value = pieces.get(check[0]);
                Log.i("WebView Skip", "Value " + check[0] + ": " + value);
                result = this.compare(value, check[1], check[2]);
                if (result) {
                    isSkip = true;
                    break;
                }
            }
            if (!isSkip) {
                return false;
            }
        }
        // skip epub files - TODO: save in media directory https://developer.android.com/training/data-storage/app-specific#media ?
        if (isMatch && isSkip) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            // Create intent to show chooser
            String title = uri.toString() + "\n\nOpen with";
            Intent chooser = Intent.createChooser(intent, title);
            view.getContext().startActivity(chooser);
            return true;
        }

        if (hasNotMatching()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            // Create intent to show chooser
            String title = uri.toString() + "\n\nOpen with";
            Intent chooser = Intent.createChooser(intent, title);
            view.getContext().startActivity(chooser);
        } else {
            String message = uri.toString() + "\n\nLink not matching. You can allow opening via regular browser in Advanced Options.";
            Toast toast = Toast.makeText(
                    view.getContext().getApplicationContext(),
                    message,
                    Toast.LENGTH_LONG);
            toast.show();
        }
        return true;
    }

    // https://stackoverflow.com/questions/8938119/changing-html-in-a-webview-programmatically/8938191#8938191
    /*
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if("file:///android_asset/web/index.html".equals(url)){
            // this.urlCache.load("http://tutorials.jenkov.com/java/index.html");
            // view.loadUrl("javascript:addSite('replace1', 'new content 1')");
            // view.loadUrl("javascript:addSite('replace1', 'new content 1')");
        }
    }
     */

    /**
     * Check if we need to intercept a particular request and handle it ourselves
     *
     * @param view  current WebView context
     * @param url   url to check for intercept
     * @return      WebResourceResponse or null
     */
    // https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        Log.d("Web Intercept", url);
        if(!url.startsWith(domainUrl)) {
            return null;
        }
        final Uri uri = Uri.parse(url);
        String path = uri.getPath();
        Log.d("WebResource", path);
        // ByteArrayInputStream str = new ByteArrayInputStream(message.getBytes());
        // return new WebResourceResponse("text/plain", "utf-8", str);
        // InputStream localStream = assetMgr.open(path);
        // return new WebResourceResponse((url.contains(".js") ? "text/javascript" : "text/css"), "UTF-8", localStream);
        // Note: we could also have used the Javascript interface, but then this might be available for all sites
        if (path.equals("/assets/web/fake_post.jsp")) {
            // String query = uri.getQuery();
            HashMap<String, Object> hashMap = this.mySavedStateModel.parseQuery(this.activity, uri);
            // add custom web settings here?
            hashMap.put("web_settings", myCustomWebSettings);
            String jsonString = this.mySavedStateModel.setSettings(this.activity, hashMap);
            loadSettings();
            String updateZip = getUpdateZip();
            mDownloadId = -1;
            if (updateZip != null && updateZip.startsWith("http")) {
                // start download request - https://medium.com/@trionkidnapper/android-webview-downloading-images-f0ec21ac75d2
                Uri updateUri = Uri.parse(updateZip);
                if (updateUri != null && URLUtil.isNetworkUrl(updateUri.toString())) {
                    String updateName = URLUtil.guessFileName(updateUri.toString(), null, null);
                    File mDownloadFile = new File(this.activity.getExternalFilesDir(null), updateName);
                    if (mDownloadFile.exists()) {
                        Log.d("Web Update", mDownloadFile.getAbsolutePath() + " exists");
                        mDownloadFile.delete();
                    } else {
                        Log.d("Web Update", mDownloadFile.getAbsolutePath() + " does not exist");
                    }
                    DownloadManager.Request request = new DownloadManager.Request(updateUri);
                    request.setDestinationInExternalFilesDir(this.activity, null, updateName);
                    // not easy to verify final path from DownloadManager in onDownloadComplete - use description to preset
                    request.setDescription(mDownloadFile.getAbsolutePath());
                    try {
                        mDownloadManager = (DownloadManager) this.activity.getSystemService(Context.DOWNLOAD_SERVICE);
                        mDownloadId = mDownloadManager.enqueue(request);
                        Log.d("Web Update", "Enqueue: " + mDownloadId);
                    } catch (Exception e) {
                        Log.e("Web Update", e.toString());
                    }
                }
            }
            // use template file for response here
            String templateName = "web/fake_post.html";
            Map<String, String> valuesMap = new HashMap<>();
            valuesMap.put("output", jsonString);
            String message = MyAssetUtility.getTemplateFile(this.activity, templateName, valuesMap);
            ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
            return new WebResourceResponse("text/html", "UTF-8", targetStream);
        }
        if (path.startsWith("/assets/")) {
            File extWebFile = new File(this.activity.getExternalFilesDir(null), path.substring("/assets/".length()));
            if (extWebFile.exists()) {
                String type;
                String extension = MimeTypeMap.getFileExtensionFromUrl(extWebFile.getName());
                if (extension != null) {
                    extension = extension.toLowerCase();
                }
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                if (extension != null && mime.hasExtension(extension)) {
                    type = mime.getMimeTypeFromExtension(extension);
                } else if (extWebFile.getName().endsWith(".json")) {
                    type = "application/json";
                } else {
                    type = "TODO";
                }
                if (!type.equals("TODO")) {
                    try {
                        InputStream targetStream = new FileInputStream(extWebFile);
                        Log.d("WebResource External", extWebFile + " mimetype: " + type);
                        if (type.startsWith("image/")) {
                            return new WebResourceResponse(type, null, targetStream);
                        } else {
                            return new WebResourceResponse(type, "UTF-8", targetStream);
                        }
                    } catch (Exception e) {
                        Log.e("WebResource", e.toString());
                    }
                }
                Log.d("WebResource External", extWebFile + " mimetype: " + type);
            }
            Log.d("WebResource Assets", extWebFile + " exists: " + extWebFile.exists());
        }
        if (this.assetLoader == null) {
            this.assetLoader = new WebViewAssetLoader.Builder()
                    //.setDomain(domainName)
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this.activity))
                    //.addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(this.activity))
                    .build();
        }
        return this.assetLoader.shouldInterceptRequest(uri);
    }
}