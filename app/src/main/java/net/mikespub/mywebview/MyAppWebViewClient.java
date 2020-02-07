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

class MyAppWebViewClient extends WebViewClient {
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

    MyAppWebViewClient(MainActivity activity) {
        this.activity = activity;
        Log.d("Web Create", activity.toString());
        // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
        this.mySavedStateModel = activity.getSavedStateModel();
        loadSettings();
        setReceiver();
    }

    private void setReceiver() {
        activity.stopReceiver();
        activity.onDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Fetching the download id received with the broadcast
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                //Checking if the received broadcast is for our enqueued download by matching download id
                if (id > -1 && mDownloadId == id && mDownloadManager != null) {
                    Log.d("Web Update", "Receive: " + mDownloadId);
                /*
                // query download status
                Cursor cursor = mDownloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if(status == DownloadManager.STATUS_SUCCESSFUL){

                        // download is successful
                        String uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        File file = new File(Uri.parse(uri).getPath());
                    }
                    else {
                        // download is assumed cancelled
                    }
                }
                else {
                    // download is assumed cancelled
                }
                 */
                    Uri uri = mDownloadManager.getUriForDownloadedFile(mDownloadId);
                    Log.d("Web Update", "Uri: " + uri);
                    // CHECKME: could be automatically renamed if the file already exists, e.g. assets-1.zip instead of assets.zip
                    if (uri != null) {
                    /*
                    Columns: 0 name: _id type: 1 value: 47
                    Columns: 1 name: entity type: 0 value: null
                    Columns: 2 name: _data type: 3 value: /storage/emulated/0/Android/data/net.mikespub.mywebview/files/assets.zip
                    Columns: 3 name: mimetype type: 3 value: application/zip
                    Columns: 4 name: visibility type: 1 value: 0
                    Columns: 5 name: destination type: 1 value: 4
                    Columns: 6 name: control type: 0 value: null
                    Columns: 7 name: status type: 1 value: 200
                    Columns: 8 name: lastmod type: 1 value: 540540424
                    Columns: 9 name: notificationpackage type: 3 value: net.mikespub.mywebview
                    Columns: 10 name: notificationclass type: 0 value: null
                    Columns: 11 name: total_bytes type: 1 value: 6313
                    Columns: 12 name: current_bytes type: 1 value: 6313
                    Columns: 13 name: title type: 3 value: assets.zip
                    Columns: 14 name: description type: 3 value:
                    Columns: 15 name: uri type: 3 value: https://github.com/mikespub/android-webview/raw/master/app/release/updates/assets.zip
                    Columns: 16 name: is_visible_in_downloads_ui type: 1 value: 1
                    Columns: 17 name: hint type: 3 value: file:///storage/emulated/0/Android/data/net.mikespub.mywebview/files/assets.zip
                    Columns: 18 name: mediaprovider_uri type: 0 value: null
                    Columns: 19 name: deleted type: 1 value: 0
                    Columns: 20 name: _display_name type: 3 value: assets.zip
                    Columns: 21 name: _size type: 1 value: 6313
                     */
                    /*
                    Cursor cursor = activity.getContentResolver().query(uri,null,null,null,null);
                    if (cursor.moveToFirst()) {
                        //String local_uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        //Log.d("Web Update Local URI", local_uri);
                        //String[] columns = cursor.getColumnNames();
                        //Log.d("Web Update Columns", Arrays.toString(columns));
                        for (int i=0; i < cursor.getColumnCount(); i++) {
                            switch (cursor.getType(i)) {
                                case Cursor.FIELD_TYPE_NULL:
                                    Log.d("Web Update Columns", i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: null");
                                    break;
                                case Cursor.FIELD_TYPE_INTEGER:
                                    Log.d("Web Update Columns", i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: " + cursor.getInt(i));
                                    break;
                                case Cursor.FIELD_TYPE_FLOAT:
                                    Log.d("Web Update Columns", i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: " + cursor.getFloat(i));
                                    break;
                                case Cursor.FIELD_TYPE_STRING:
                                    Log.d("Web Update Columns", i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: " + cursor.getString(i));
                                    break;
                                case Cursor.FIELD_TYPE_BLOB:
                                    Log.d("Web Update Columns", i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: " + cursor.getBlob(i).toString());
                                    break;
                                default:
                                    Log.d("Web Update Columns", i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: ?");
                                    break;
                            }
                        }
                        Log.d("Web Update Extras", cursor.getExtras().toString());
                    }
                    cursor.close();
                     */
                        // unzip
                        //Log.d("Web Update", mDownloadFile.getAbsolutePath());
                        try {
                            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                            File targetDirectory = activity.getExternalFilesDir(null);
                            MySettingsRepository.unzipStream(inputStream, targetDirectory);
                            if (inputStream != null)
                                inputStream.close();
                            // delete entry in Downloads to avoid multiple duplicates there
                            activity.getContentResolver().delete(uri,null,null);
                        } catch (Exception e) {
                            Log.e("Web Update", e.toString());
                        }
                    }
                }
            }
        };
        Log.d("Web Create", "register receiver");
        activity.registerReceiver(activity.onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    Boolean hasDebuggingEnabled() {
        return (Boolean) mySavedStateModel.getValue("remote_debug");
    }

    Boolean hasConsoleLog() {
        return (Boolean) mySavedStateModel.getValue("console_log");
    }

    Boolean hasJavascriptInterface() {
        return (Boolean) mySavedStateModel.getValue("js_interface");
    }

    Boolean hasContextMenu() {
        return (Boolean) mySavedStateModel.getValue("context_menu");
    }

    Boolean hasNotMatching() {
        return (Boolean) mySavedStateModel.getValue("not_matching");
    }

    String hasUpdateZip() {
        return (String) mySavedStateModel.getValue("update_zip");
    }

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

    // https://stackoverflow.com/questions/41972463/android-web-view-shouldoverrideurlloading-deprecated-alternative/41973017
    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.d("Web Override", url);
        if(url.startsWith("https://appassets.androidplatform.net/")) {
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

    // https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        Log.d("Web Intercept", url);
        if(!url.startsWith("https://appassets.androidplatform.net/")) {
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
            String jsonString = this.mySavedStateModel.setSettings(this.activity, hashMap);
            loadSettings();
            String update_zip = hasUpdateZip();
            mDownloadId = -1;
            if (update_zip != null && update_zip.startsWith("http")) {
                // start download request - https://medium.com/@trionkidnapper/android-webview-downloading-images-f0ec21ac75d2
                Uri update_uri = Uri.parse(update_zip);
                if (update_uri != null && URLUtil.isNetworkUrl(update_uri.toString())) {
                    String update_name = URLUtil.guessFileName(update_uri.toString(), null, null);
                    File mDownloadFile = new File(this.activity.getExternalFilesDir(null), update_name);
                    if (mDownloadFile.exists()) {
                        Log.d("Web Update", mDownloadFile.getAbsolutePath() + " exists");
                        mDownloadFile.delete();
                    } else {
                        Log.d("Web Update", mDownloadFile.getAbsolutePath() + " does not exist");
                    }
                    DownloadManager.Request request = new DownloadManager.Request(update_uri);
                    request.setDestinationInExternalFilesDir(this.activity, null, update_name);
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
            String message = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                    "    <!-- No complains about missing favicon.ico from https requests. -->\n" +
                    "    <link rel=\"icon\" href=\"data:,\">\n" +
                    "    <title>Update Settings</title>\n" +
                    "    <style>\n" +
                    "    @media screen and (min-width: 600px) {\n" +
                    "        #main {\n" +
                    "            max-width: 600px;\n" +
                    "            margin: auto;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div id=\"main\">\n" +
                    "    <h1><a href=\"index.html\">My WebView</a></h1>\n" +
                    "    <h2><a href=\"update.html\">Update Settings</a></h2>\n";
            message += "    <pre>" + jsonString + "</pre>\n";
            /*
            message += "<script>\n" +
                    "    if(typeof androidAppProxy !== \"undefined\"){\n" +
                    "        androidAppProxy.showMessage(\"Settings Updated\");\n" +
                    "    } else {\n" +
                    "        alert(\"Running outside Android app\");\n" +
                    "    }\n" +
                    "</script>\n";
             */
            message += "    </div>\n";
            message += "</body></html>";
            ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
            return new WebResourceResponse("text/html", "UTF-8", targetStream);
        }
        if (path.startsWith("/assets/web/")) {
            File extwebfile = new File(this.activity.getExternalFilesDir(null), path.substring("/assets/".length()));
            if (extwebfile.exists()) {
                String type;
                String extension = MimeTypeMap.getFileExtensionFromUrl(extwebfile.getName());
                if (extension != null) {
                    extension = extension.toLowerCase();
                }
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                if (extension != null && mime.hasExtension(extension)) {
                    type = mime.getMimeTypeFromExtension(extension);
                } else if (extwebfile.getName().endsWith(".json")) {
                    type = "application/json";
                } else {
                    type = "TODO";
                }
                if (!type.equals("TODO")) {
                    try {
                        InputStream targetStream = new FileInputStream(extwebfile);
                        Log.d("WebResource External", extwebfile + " mimetype: " + type);
                        if (type.startsWith("image/")) {
                            return new WebResourceResponse(type, null, targetStream);
                        } else {
                            return new WebResourceResponse(type, "UTF-8", targetStream);
                        }
                    } catch (Exception e) {
                        Log.e("WebResource", e.toString());
                    }
                }
                Log.d("WebResource External", extwebfile + " mimetype: " + type);
            }
            Log.d("WebResource Assets", extwebfile + " exists: " + extwebfile.exists());
        }
        if (this.assetLoader == null) {
            this.assetLoader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this.activity))
                    //.addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(this.activity))
                    .build();
        }
        return this.assetLoader.shouldInterceptRequest(uri);
    }
}