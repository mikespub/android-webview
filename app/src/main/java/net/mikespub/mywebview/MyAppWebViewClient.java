package net.mikespub.mywebview;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import net.mikespub.myutils.MyAssetUtility;
import net.mikespub.myutils.MyContentUtility;
import net.mikespub.myutils.MyJsonUtility;
import net.mikespub.myutils.MyReflectUtility;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebViewClient Methods + link to settings & downloads
 */
class MyAppWebViewClient extends WebViewClient {
    final String domainName;
    final String domainUrl;

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
    private boolean mDownloadExtract = true;
    private Map<String, Object> myDefaultWebSettings;
    private Map<String, Object> myCustomWebSettings;
    //private Map<String, Object> myLocalSitesConfig;

    /**
     * @param activity  current Activity context
     */
    MyAppWebViewClient(MainActivity activity) {
        this.activity = activity;
        Log.d("Web Create", activity.toString());
        // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
        this.mySavedStateModel = activity.getSavedStateModel();
        this.domainName = activity.getString(R.string.app_host);
        this.domainUrl = "https://" + this.domainName + "/";
        loadSettings();
        setReceiver();
    }

    /**
     * Set BroadcastReceiver for download complete
     */
    private void setReceiver() {
        activity.startDownloadReceiver(new BroadcastReceiver() {
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
                if (!mDownloadExtract) {
                    return;
                }
                // unzip
                //Log.d("Web Update", mDownloadFile.getAbsolutePath());
                try {
                    InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                    File targetDirectory = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    String[] skipNames = { MySettingsRepository.fileName };
                    MyAssetUtility.unzipStream(inputStream, targetDirectory, skipNames);
                    if (inputStream != null)
                        inputStream.close();
                    // delete entry in Downloads to avoid multiple duplicates there
                    //activity.getContentResolver().delete(uri,null,null);
                } catch (Exception e) {
                    Log.e("Web Update", e.toString());
                }
            }
        });
    }

    /**
     * Get setting for remote_debug
     *
     * @return  setting
     */
    Boolean hasDebuggingEnabled() {
        return (Boolean) mySavedStateModel.getValue("remote_debug");
    }

    /**
     * Get setting for console_log
     *
     * @return  setting
     */
    Boolean hasConsoleLog() {
        return (Boolean) mySavedStateModel.getValue("console_log");
    }

    /**
     * Get setting for js_interface
     *
     * @return  setting
     */
    Boolean hasJavascriptInterface() {
        return (Boolean) mySavedStateModel.getValue("js_interface");
    }

    /**
     * Get setting for context_menu
     *
     * @return  setting
     */
    Boolean hasContextMenu() {
        return (Boolean) mySavedStateModel.getValue("context_menu");
    }

    /**
     * Get setting for not_matching
     *
     * @return  setting
     */
    Boolean hasNotMatching() {
        return (Boolean) mySavedStateModel.getValue("not_matching");
    }

    /**
     * Get setting for local_sites
     *
     * @return  setting
     */
    Boolean hasLocalSites() {
        return (Boolean) mySavedStateModel.getValue("local_sites");
    }

    /**
     * Get setting for update_zip
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
     * Get local sites config
     *
     * @return  setting
     */
    Map<String, Object> getLocalConfig() {
        return (Map<String, Object>) mySavedStateModel.getValue("local_config");
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
     * Get the site url corresponding to an app link
     *
     * @param appLinkData the app link received via Intent
     * @return  corresponding site url
     */
    String getSiteUrlFromAppLink(Uri appLinkData) {
        String host = appLinkData.getEncodedAuthority();
        String path = appLinkData.getEncodedPath();
        if (path.isEmpty()) {
            path = "/";
        }
        if (!path.contains("/")) {
            path += "/";
        }
        Log.d("Intent", "Path: " + host + path);
        String myUrl;
        // mywebview://
        if (host.isEmpty()) {
            myUrl = domainUrl + activity.getString(R.string.start_uri);
        // mywebview://web/...
        } else if (host.equals("web")) {
            myUrl = domainUrl + "assets/" + host + path;
        // mywebview://local/...
        } else if (host.equals("local")) {
            myUrl = domainUrl + "assets/" + host + path;
        // mywebview://sites/...
        } else if (host.equals("sites")) {
            myUrl = domainUrl + host + path;
        } else {
            myUrl = domainUrl + "assets/local/404.jsp?link=" + host + path;
        }
        return myUrl;
    }

    /**
     * Check if we need to override a particular url and/or create an Intent for it
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
        if(url.startsWith(domainUrl) || url.startsWith("http://localhost/")) {
            // should be handled here already or not?
            final Uri uri = Uri.parse(url);
            if (getIntentPrefixFromUri(uri) != null) {
                return handleIntentUri(view, uri);
            }
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
            boolean result = MyReflectUtility.stringCompare(value, compare[1], compare[2]);
            if (!result) {
                continue;
            }
            isMatch = true;
            isSkip = false;
            //for (String s: this.mySkipArr) {
            //    String[] check = s.split("\\|");
            for (String[] check: this.mySkipCompare) {
                value = pieces.get(check[0]);
                Log.d("WebView Skip", "Value " + check[0] + ": " + value);
                result = MyReflectUtility.stringCompare(value, check[1], check[2]);
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
        if(!url.startsWith(domainUrl) && !url.startsWith("http://localhost/")) {
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
            return handleUpdateSettings(uri);
        } else if (path.equals("/assets/local/get_config.jsp")) {
            return handleGetLocalConfig(uri);
        } else if (path.equals("/assets/local/download.jsp")) {
            return handleDownloadBundle(uri);
        } else if (path.equals("/assets/local/extract.jsp")) {
            return handleExtractBundle(uri);
        } else if (path.equals("/assets/local/delete.jsp")) {
            return handleDeleteBundle(uri);
        } else if (path.equals("/assets/local/cleanup.jsp")) {
            return handleCleanUpDownloads(uri);
        } else if (path.equals("/assets/local/404.jsp")) {
            return handleFileNotFound(uri);
        } else if (path.startsWith("/assets/")) {
            return handleAssetFileRequest(uri);
        } else if (hasLocalSites() && path.startsWith("/sites/")) {
            // handle local sites if not already under /assets/...
            return handleLocalSiteRequest(uri);
        } else if (getIntentPrefixFromUri(uri) != null) {
            // should be handled in shouldOverrideUrlLoading already or not?
            return handleIntentRequest(view, uri);
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

    public WebResourceResponse handleAssetFileRequest(Uri uri) {
        String fileName = uri.getPath().substring("/assets/".length());
        return handleFileRequest(null, fileName);
    }

    public WebResourceResponse handleLocalSiteRequest(Uri uri) {
        String fileName = uri.getPath().substring("/sites/".length());
        return handleFileRequest(Environment.DIRECTORY_DOCUMENTS, fileName);
    }

    public WebResourceResponse handleFileRequest(@Nullable String dirName, @NonNull String fileName) {
        File extFile = new File(this.activity.getExternalFilesDir(dirName), fileName);
        if (fileName.endsWith("/") && extFile.exists() && extFile.isDirectory()) {
            Log.d("File Request", extFile + " is directory - trying with index.html");
            if (fileName.startsWith("local/")) {
                fileName += "sites.html";
            } else {
                fileName += "index.html";
            }
            extFile = new File(this.activity.getExternalFilesDir(dirName), fileName);
        }
        if (!extFile.exists() || extFile.isDirectory()) {
            Log.d("File Request", extFile + " exists: " + extFile.exists());
            return null;
        }
        String type = getMimeType(extFile.getName());
        if (type.equals("TODO")) {
            Log.d("File Request", extFile + " type: " + type);
            return null;
        }
        try {
            InputStream targetStream = new FileInputStream(extFile);
            Log.d("File Request", extFile + " mimetype: " + type);
            if (type.startsWith("image/") || type.startsWith("font/")) {
                return new WebResourceResponse(type, null, targetStream);
            } else {
                return new WebResourceResponse(type, "UTF-8", targetStream);
            }
        } catch (Exception e) {
            Log.e("File Request", extFile.getAbsolutePath(), e);
        }
        return null;
    }

    public WebResourceResponse handleIntentRequest(WebView view, Uri uri) {
        Boolean isHandled = handleIntentUri(view, uri);
        // use template file for response here
        String templateName = "web/result.html";
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("output", "Intent: " + uri.toString() + "\nHandled: " + isHandled);
        String message = MyAssetUtility.getTemplateFile(this.activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    private String getIntentPrefixFromUri(Uri uri) {
        //https://stackoverflow.com/questions/1128723/how-do-i-determine-whether-an-array-contains-a-particular-value-in-java
        final String[] intentNames = {"intent", "view", "send", "pick"};
        final List<String> intentList = Arrays.asList(intentNames);
        String[] parts = uri.getPath().split("/", 3);
        if (intentList.contains(parts[1])) {
            return "/" + parts[1];
        }
        return null;
    }

    private File getExternalFileFromPath(String path) {
        if (path.startsWith("/assets/")) {
            String fileName = path.substring("/assets/".length());
            return new File(this.activity.getExternalFilesDir(null), fileName);
        } else if (path.startsWith("/sites/")) {
            String dirName = Environment.DIRECTORY_DOCUMENTS;
            String fileName = path.substring("/sites/".length());
            return new File(this.activity.getExternalFilesDir(dirName), fileName);
        }
        return null;
    }

    public Boolean handleIntentUri(WebView view, Uri uri) {
        Log.d("WebResource Intent", uri.toString());
        String prefix = getIntentPrefixFromUri(uri);
        if (prefix == null) {
            return false;
        }
        String path = uri.getPath().substring(prefix.length());
        File extFile = getExternalFileFromPath(path);
        if (extFile == null) {
            Log.d("Intent", "File Not Found");
            return false;
        }
        Log.d("Intent", "File: " + extFile.getAbsolutePath());
        Uri contentUri;
        try {
            contentUri = FileProvider.getUriForFile(activity, "net.mikespub.mywebview.fileprovider", extFile);
            MyContentUtility.showContent(activity, contentUri);
        } catch (Exception e) {
            Log.e("Intent", extFile.getAbsolutePath(), e);
            contentUri = Uri.fromFile(extFile);
        }
        /*
        */
        if (prefix.equals("/view")) {
            //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(extFile));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            //intent.setDataAndType(Uri.fromFile(extFile), getMimeType(extFile.getName()));
            intent.setDataAndType(contentUri, getMimeType(extFile.getName()));
            // Grant temporary read permission to the content URI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            //view.getContext().startActivity(intent);
            // Create intent to show chooser
            String title = uri.toString() + "\n\nOpen with";
            Intent chooser = Intent.createChooser(intent, title);
            view.getContext().startActivity(chooser);
            return true;
        }
        if (prefix.equals("/pick")) {
            //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(extFile));
            Intent intent = new Intent(Intent.ACTION_PICK);
            activity.startActivityForResult(intent, 1);
            return true;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setDataAndType(contentUri, getMimeType(extFile.getName()));
        // Grant temporary read permission to the content URI
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        view.getContext().startActivity(intent);
        // Create intent to show chooser
        //String title = uri.toString() + "\n\nOpen with";
        //Intent chooser = Intent.createChooser(intent, title);
        //view.getContext().startActivity(chooser);
        return true;
    }

    public WebResourceResponse handleGetLocalConfig(Uri uri) {
        String message;
        try {
            Map<String, Object> localConfig = getLocalConfig();
            localConfig.put("sites", MyLocalConfigRepository.findLocalSites(activity));
            localConfig.put("bundles", MyLocalConfigRepository.findAvailableBundles(activity));
            message = MyJsonUtility.toJsonString(localConfig);
        } catch (Exception e) {
            Log.e("Config", e.toString());
            message = e.toString();
        }
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("application/json", "UTF-8", targetStream);
    }

    public WebResourceResponse handleDownloadBundle(Uri uri) {
        String updateZip = uri.getQueryParameter("update_zip");
        String extract = uri.getQueryParameter("extract");
        Log.d("Download Bundle", "URL: " + updateZip + " Extract: " + extract);
        if (updateZip != null && updateZip.startsWith("http")) {
            // start download request - https://medium.com/@trionkidnapper/android-webview-downloading-images-f0ec21ac75d2
            Uri updateUri = Uri.parse(updateZip);
            if (updateUri != null && URLUtil.isHttpsUrl(updateUri.toString())) {
                if (extract != null && extract.equals("true")) {
                    requestUriDownload(updateUri, true);
                } else {
                    requestUriDownload(updateUri, false);
                }
            }
        }
        // use template file for response here
        String templateName = "local/result.html";
        Map<String, String> valuesMap = new HashMap<>();
        Map<String, String> output = new HashMap<>();
        output.put("update_zip", updateZip);
        output.put("extract", extract);
        try {
            valuesMap.put("output", MyJsonUtility.toJsonString(output));
        } catch (Exception e) {
            valuesMap.put("output", e.toString());
        }
        String message = MyAssetUtility.getTemplateFile(this.activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    public WebResourceResponse handleExtractBundle(Uri uri) {
        String bundle = uri.getQueryParameter("bundle");
        File extFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), bundle);
        Log.d("Extract", extFile.getAbsolutePath());
        try {
            FileInputStream inputStream = new FileInputStream(extFile);
            File targetDirectory = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            String[] skipNames = { MySettingsRepository.fileName };
            MyAssetUtility.unzipStream(inputStream, targetDirectory, skipNames);
            if (inputStream != null)
                inputStream.close();
        } catch (Exception e) {
            Log.e("Extract", e.toString());
        }
        // use template file for response here
        String templateName = "local/result.html";
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("output", "Extracted: " + extFile.getName());
        String message = MyAssetUtility.getTemplateFile(this.activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    public WebResourceResponse handleDeleteBundle(Uri uri) {
        String bundle = uri.getQueryParameter("bundle");
        File extFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), bundle);
        Log.d("Delete", extFile.getAbsolutePath());
        try {
            // do we need to find the equivalent Download file first?
            extFile.delete();
        } catch (Exception e) {
            Log.e("Delete", e.toString());
        }
        // use template file for response here
        String templateName = "local/result.html";
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("output", "Deleted: " + extFile.getName());
        String message = MyAssetUtility.getTemplateFile(this.activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    public WebResourceResponse handleCleanUpDownloads(Uri uri) {
        try {
            MyContentUtility.showMyDownloadFiles(this.activity, true);
        } catch (Exception e) {
            Log.e("Cleanup", e.toString());
        }
        // use template file for response here
        String templateName = "local/result.html";
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("output", "Cleaned up...");
        String message = MyAssetUtility.getTemplateFile(this.activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    public WebResourceResponse handleFileNotFound(Uri uri) {
        String link = uri.getQueryParameter("link");
        // use template file for response here
        String templateName = "local/result.html";
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("output", "File not found:" + link.replace("<", "&lt;"));
        String message = MyAssetUtility.getTemplateFile(this.activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        if (Build.VERSION.SDK_INT >= 21) {
            return new WebResourceResponse("text/html", "UTF-8", 404, "Not Found", null, targetStream);
        }
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    public WebResourceResponse handleUpdateSettings(Uri uri) {
        // String query = uri.getQuery();
        HashMap<String, Object> hashMap = MySettingsRepository.parseQueryParameters(uri);
        // add custom web settings here?
        hashMap.put("web_settings", myCustomWebSettings);
        // add local config here?
        hashMap.put("local_config", getLocalConfig());
        String jsonString = this.mySavedStateModel.setSettings(this.activity, hashMap);
        loadSettings();
        String updateZip = getUpdateZip();
        mDownloadId = -1;
        if (updateZip != null && updateZip.startsWith("http")) {
            // start download request - https://medium.com/@trionkidnapper/android-webview-downloading-images-f0ec21ac75d2
            Uri updateUri = Uri.parse(updateZip);
            if (updateUri != null && URLUtil.isHttpsUrl(updateUri.toString())) {
                requestUriDownload(updateUri, true);
            }
        }
        // use template file for response here
        String templateName = "web/result.html";
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("output", jsonString);
        String message = MyAssetUtility.getTemplateFile(this.activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    public long requestUriDownload(Uri updateUri, boolean extract) {
        String updateName = URLUtil.guessFileName(updateUri.toString(), null, null);
        File mDownloadFile = new File(this.activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), updateName);
        if (mDownloadFile.exists()) {
            Log.d("Web Update", mDownloadFile.getAbsolutePath() + " exists");
            String lastModified = String.valueOf(mDownloadFile.lastModified() / 1000);
            File renameFile = new File(this.activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), updateName.replace(".zip", "." + lastModified + ".zip"));
            mDownloadFile.renameTo(renameFile);
            // update corresponding Downloads entry as well via getContentResolver.update()
        } else {
            Log.d("Web Update", mDownloadFile.getAbsolutePath() + " does not exist");
        }
        DownloadManager.Request request = new DownloadManager.Request(updateUri);
        request.setDestinationInExternalFilesDir(this.activity, Environment.DIRECTORY_DOWNLOADS, updateName);
        // not easy to verify final path from DownloadManager in onDownloadComplete - use description to preset
        request.setDescription(mDownloadFile.getAbsolutePath());
        try {
            mDownloadManager = (DownloadManager) this.activity.getSystemService(Context.DOWNLOAD_SERVICE);
            mDownloadExtract = extract;
            mDownloadId = mDownloadManager.enqueue(request);
            Log.d("Web Update", "Enqueue: " + mDownloadId);
        } catch (Exception e) {
            Log.e("Web Update", e.toString());
        }
        return mDownloadId;
    }

    static String getMimeType(String fileName) {
        String type;
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (extension != null) {
            extension = extension.toLowerCase();
        }
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        // https://www.iana.org/assignments/media-types/media-types.xhtml
        if (extension != null && mime.hasExtension(extension)) {
            type = mime.getMimeTypeFromExtension(extension);
        } else if (fileName.endsWith(".json")) {
            type = "application/json";
        } else if (fileName.endsWith(".js")) {
            type = "application/javascript";
        } else if (fileName.endsWith(".ttf")) {
            //type = "application/x-font-ttf";
            type = "font/ttf";
        } else if (fileName.endsWith(".woff")) {
            //type = "application/font-woff";
            type = "font/woff";
        } else if (fileName.endsWith(".woff2")) {
            type = "font/woff2";
        } else if (fileName.endsWith(".svg")) {
            type = "image/svg+xml";
        } else {
            type = "TODO";
            Log.d("WebResource External", fileName + " extension: " + extension + " mimetype: " + type);
        }
        return type;
    }
}