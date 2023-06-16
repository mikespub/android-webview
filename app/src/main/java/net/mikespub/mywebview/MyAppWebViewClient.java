package net.mikespub.mywebview;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import net.mikespub.myutils.MyAssetUtility;
import net.mikespub.myutils.MyReflectUtility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * WebViewClient Methods + link to settings & downloads
 *
 * Some inspiration: https://github.com/react-native-community/react-native-webview/tree/master/android/src/main/java/com/reactnativecommunity/webview
 * See also https://android.googlesource.com/platform/packages/apps/Browser2/+/refs/heads/master/src/org/chromium/webview_shell/WebViewBrowserActivity.java
 */
class MyAppWebViewClient extends WebViewClient {
    //static final String[] linkNames = {"sites", "files", "web", "local", "root", "content", "media", "refresh"};
    final String domainName;
    final String domainUrl;

    // http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    final MainActivity activity;
    //private WebViewAssetLoader assetLoader;
    private String[][] myMatchCompare;
    private String[][] mySkipCompare;
    // SavedStateViewModel see https://github.com/googlecodelabs/android-lifecycles/blob/master/app/src/main/java/com/example/android/lifecycles/step6_solution/SavedStateActivity.java
    // or with AndroidViewModel see https://github.com/husaynhakeem/Androidx-SavedState-Playground/blob/master/app/src/main/java/com/husaynhakeem/savedstateplayground/AndroidViewModelWithSavedState.kt
    final MySavedStateModel mySavedStateModel;
    // https://androidclarified.com/android-downloadmanager-example/
    DownloadManager mDownloadManager;
    //private File mDownloadFile;
    long mDownloadId = -1;
    boolean mDownloadExtract = true;
    Map<String, Object> myCustomWebSettings;
    //private Map<String, Object> myLocalSitesConfig;
    private final MyRequestHandler myRequestHandler;

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
        this.myRequestHandler = new MyRequestHandler(this);
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
        Map<String, Object> myDefaultWebSettings = MyReflectUtility.getValues(webSettings);
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
    void loadSettings() {
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
     * Load home page
     *
     * @param view  current WebView context
     */
    void loadHomePage(WebView view) {
        String myUrl = domainUrl + activity.getResources().getString(R.string.start_uri);
        view.loadUrl(myUrl);
    }

    /**
     * Load document
     *
     * @param view  current WebView context
     * @param contentName the document to load
     */
    void loadDocument(WebView view, String contentName) {
        String myUrl = domainUrl + "document/" + contentName;
        view.loadUrl(myUrl);
    }

    /**
     * Load generic content
     *
     * @param view  current WebView context
     * @param contentName the content to load
     */
    void loadContent(WebView view, String contentName) {
        String myUrl = domainUrl + "content/" + contentName;
        view.loadUrl(myUrl);
    }

    /**
     * Load app link
     *
     * @param view  current WebView context
     * @param appLinkData the app link to load
     * @param getNotFound get not found link if not a valid app link
     * @return      true if loading valid app link, false if not
     */
    boolean loadAppLink(WebView view, Uri appLinkData, Boolean getNotFound) {
        String myUrl = getSiteUrlFromAppLink(appLinkData);
        if (myUrl.isEmpty()) {
            if (getNotFound) {
                myUrl = domainUrl + "assets/local/404.jsp?link=" + appLinkData.getEncodedAuthority() + appLinkData.getEncodedPath();
                view.loadUrl(myUrl);
            }
            return false;
        }
        view.loadUrl(myUrl);
        return true;
    }

    /**
     * Load 404 Not Found
     *
     * @param view  current WebView context
     * @param contentName the content not found
     */
    void loadNotFound(WebView view, String contentName) {
        String myUrl = domainUrl + "assets/local/404.jsp?link=" + contentName;
        view.loadUrl(myUrl);
    }

    /**
     * Get the site url corresponding to an app link
     *
     * @param appLinkData the app link received via Intent
     * @return  corresponding site url or empty
     */
    String getSiteUrlFromAppLink(Uri appLinkData) {
        String scheme = appLinkData.getScheme();
        String host = appLinkData.getEncodedAuthority();
        String path = appLinkData.getEncodedPath();
        if (path.isEmpty()) {
            path = "/";
        }
        if (!path.contains("/")) {
            path += "/";
        }
        if (scheme.equals(activity.getString(R.string.link_scheme))) {
            Log.d("AppLink", "Path: " + host + path);
        } else if (host.equals(activity.getString(R.string.link_host))) {
            File f = new File(path);
            String canonicalPath;
            try {
                canonicalPath = f.getCanonicalPath();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + path);
            }
            // check for missing trailing / if index.html is not specified
            if (canonicalPath.length() > 1 && canonicalPath.indexOf("/", 1) < 0) {
                canonicalPath += "/";
            }
            if (canonicalPath.startsWith(activity.getString(R.string.link_prefix))) {
                Log.d("AppLink", "Link: " + host + path);
                host = "link";
                path = activity.getString(R.string.link_uri) + canonicalPath.substring(activity.getString(R.string.link_prefix).length());
            } else {
                Log.e("AppLink", "Link: " + host + path + "!=" + canonicalPath);
                return "";
            }
        } else if (host.equals(activity.getString(R.string.link2_host))) {
            File f = new File(path);
            String canonicalPath;
            try {
                canonicalPath = f.getCanonicalPath();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + path);
            }
            // check for missing trailing / if index.html is not specified
            if (canonicalPath.length() > 1 && canonicalPath.indexOf("/", 1) < 0) {
                canonicalPath += "/";
            }
            if (canonicalPath.startsWith(activity.getString(R.string.link2_prefix))) {
                Log.d("AppLink", "Link2: " + host + path);
                host = "link2";
                path = activity.getString(R.string.link2_uri) + canonicalPath.substring(activity.getString(R.string.link2_prefix).length());
            } else {
                Log.e("AppLink", "Link2: " + host + path + "!=" + canonicalPath);
                return "";
            }
        } else {
            Log.d("AppLink", "Site: " + host + path);
            return "";
        }
        //https://stackoverflow.com/questions/1128723/how-do-i-determine-whether-an-array-contains-a-particular-value-in-java
        //final List<String> linkList = Arrays.asList(linkNames);
        String myUrl;
        switch (host) {
            // mywebview://web/...
            // mywebview://local/...
            case "web":
            case "local":
                myUrl = domainUrl + "assets/" + host + path;
                break;
            // mywebview://sites/...
            case "sites":
                // check for missing trailing / if index.html is not specified
                if (path.length() > 1 && path.indexOf("/", 1) < 0) {
                    path += "/";
                }
                myUrl = domainUrl + host + path;
                break;
            // mywebview://
            case "":
                myUrl = domainUrl + activity.getString(R.string.start_uri);
                break;
            // mywebview://files/...
            //case "files":
            //    myUrl = domainUrl + host + path;
            //    break;
            // mywebview://root/...
            //case "root":
            //    myUrl = domainUrl + "assets" + path;
            //    break;
            // link_scheme://link_host + link_prefix + path
            case "link":
            case "link2":
                myUrl = domainUrl + path;
                break;
            case "other":
            default:
                return "";
        }
        return myUrl;
    }

    /**
     * Check if we need to override a particular url and/or create an Intent for it
     *
     * @param view  current WebView context
     * @param request   web resource request to check for override
     * @return      decision to override or not
     */
    // https://stackoverflow.com/questions/41972463/android-web-view-shouldoverrideurlloading-deprecated-alternative/41973017
    //@RequiresApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        return testOverrideUrlLoading(view, url);
    }

    /**
     * Check if we need to override a particular url and/or create an Intent for it
     *
     * @param view  current WebView context
     * @param url   url to check for override
     * @return      decision to override or not
     */
    // https://stackoverflow.com/questions/41972463/android-web-view-shouldoverrideurlloading-deprecated-alternative/41973017
    /**
    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return testOverrideUrlLoading(view, url);
    }
     */

    /**
     * Check if we need to override a particular url and/or create an Intent for it
     *
     * @param view  current WebView context
     * @param url   url to check for override
     * @return      decision to override or not
     */
    private boolean testOverrideUrlLoading(WebView view, String url) {
        Log.d("Web Override", url);
        if(url.startsWith(domainUrl) || url.startsWith("http://localhost/") ) {
            // should be handled here already or not?
            final Uri uri = Uri.parse(url);
            if (myRequestHandler.getIntentPrefixFromUri(uri) != null) {
                return myRequestHandler.handleIntentUri(view, uri);
            }
            return false;
        }
        // if we put deep links in web, local or sites pages, reload here with site link to avoid CORS et al.
        String myUrl = getSiteUrlFromAppLink(Uri.parse(url));
        if (!myUrl.isEmpty()) {
            Log.d("Web Override", "Reload with site link " + myUrl);
            view.loadUrl(myUrl);
            return true;
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
            String title = uri + "\n\nOpen with";
            Intent chooser = Intent.createChooser(intent, title);
            view.getContext().startActivity(chooser);
            return true;
        }

        if (hasNotMatching()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            // Create intent to show chooser
            String title = uri + "\n\nOpen with";
            Intent chooser = Intent.createChooser(intent, title);
            view.getContext().startActivity(chooser);
        } else {
            String message = uri + "\n\nLink not matching. You can allow opening via regular browser in Advanced Options.";
            Toast toast = Toast.makeText(
                    view.getContext().getApplicationContext(),
                    message,
                    Toast.LENGTH_LONG);
            toast.show();
        }
        return true;
    }

    // https://stackoverflow.com/questions/8938119/changing-html-in-a-webview-programmatically/8938191#8938191
    /**
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
     * @param request   web resource request to check for intercept
     * @return      WebResourceResponse or null
     */
    // https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        return testInterceptRequest(view, url);
    }
    /**
     * Check if we need to intercept a particular request and handle it ourselves
     *
     * @param view  current WebView context
     * @param url   url to check for intercept
     * @return      WebResourceResponse or null
     */
    // https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
    /**
    @SuppressWarnings("deprecation")
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return testInterceptRequest(view, url);
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
    private WebResourceResponse testInterceptRequest(WebView view, String url) {
        Log.d("Web Intercept", url);
        // if we put deep links in web, local or sites pages and don't reload above, we'll have CORS issues et al. here
        //if (url.startsWith(activity.getString(R.string.link_scheme) + "://")) {
        //    url = getSiteUrlFromAppLink(Uri.parse(url));
        //}
        if(!url.startsWith(domainUrl) && !url.startsWith("http://localhost/")) {
            return null;
        }
        final Uri uri = Uri.parse(url);
        return myRequestHandler.handleRequest(view, uri);
    }

}