package net.mikespub.mywebview;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
    private MySavedStateModel mySavedStateModel;

    MyAppWebViewClient(MainActivity activity) {
        this.activity = activity;
        Log.d("Web Create", activity.toString());
        // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
        this.mySavedStateModel = activity.getSavedStateModel();
        loadSettings();
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
            boolean isSkip = false;
            //for (String s: this.mySkipArr) {
            //    String[] check = s.split("\\|");
            for (String[] check: this.mySkipCompare) {
                value = pieces.get(check[0]);
                Log.i("WebView Skip", "Value " + compare[0] + ": " + value);
                result = this.compare(value, check[1], check[2]);
                if (result) {
                    isSkip = true;
                    break;
                }
            }
            if (isSkip) {
                continue;
            }
            return false;
        }
        /*
        if (host != null) {
            String[] myHostEndsArr = this.activity.getResources().getStringArray(R.array.urlhost_endswith);
            for (String s : myHostEndsArr) {
                if (!host.endsWith(s)) {
                    continue;
                }
                // skip epub files - TODO: save in media directory https://developer.android.com/training/data-storage/app-specific#media ?
                // if (!path.endsWith(".epub") && (query == null || !query.contains("type=epub"))) {
                //     return false;
                // }
                boolean isMatch = false;
                if (path != null) {
                    String[] myPathNotEndsArr = this.activity.getResources().getStringArray(R.array.urlpath_not_endswith);
                    for (String t : myPathNotEndsArr) {
                        if (path.endsWith(t)) {
                            isMatch = true;
                            break;
                        }
                    }
                    if (isMatch) {
                        continue;
                    }
                }
                if (query != null) {
                    String[] myQueryNotContainsArr = this.activity.getResources().getStringArray(R.array.urlquery_not_contains);
                    for (String q : myQueryNotContainsArr) {
                        if (query.contains(q)) {
                            isMatch = true;
                            break;
                        }
                    }
                    if (isMatch) {
                        continue;
                    }
                }
                return false;
            }
        }
         */

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        view.getContext().startActivity(intent);
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
        if(url.startsWith("https://appassets.androidplatform.net/")) {
            if (this.assetLoader == null) {
                this.assetLoader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this.activity))
                    //.addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(this.activity))
                    .build();
            }
            final Uri uri = Uri.parse(url);
            String path = uri.getPath();
            Log.d("WebResource", path);
            // ByteArrayInputStream str = new ByteArrayInputStream(message.getBytes());
            // return new WebResourceResponse("text/plain", "utf-8", str);
            // InputStream localStream = assetMgr.open(path);
            // return new WebResourceResponse((url.contains(".js") ? "text/javascript" : "text/css"), "UTF-8", localStream);
            if (path.equals("/assets/web/settings.json")) {
                // TODO: return WebResourceResponse
                File extwebfile = new File(this.activity.getExternalFilesDir(null), "web/settings.json");
                if (extwebfile.exists()) {
                    try {
                        InputStream targetStream = new FileInputStream(extwebfile);
                        Log.d("WebResource External", Boolean.toString(extwebfile.exists()));
                        return new WebResourceResponse("application/json", "UTF-8", targetStream);
                    } catch (Exception e) {
                        Log.e("WebResource", e.toString());
                    }
                }
                Log.d("WebResource Assets", Boolean.toString(extwebfile.exists()));
            }
            // Note: we could also have used the Javascript interface, but then this might be available for all sites
            if (path.equals("/assets/web/fake_post.jsp")) {
                // String query = uri.getQuery();
                HashMap<String, Object> hashMap = this.mySavedStateModel.parseQuery(this.activity, uri);
                String jsonString = this.mySavedStateModel.setSettings(this.activity, hashMap);
                String message = "<!DOCTYPE html>\n" +
                        "<html lang=\"en\">\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                        "    <!-- No complains about missing favicon.ico from https requests. -->\n" +
                        "    <link rel=\"icon\" href=\"data:,\">\n" +
                        "    <title>Update Settings</title>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <h1><a href=\"index.html\">My WebView</a></h1>\n" +
                        "    <h2><a href=\"update.html\">Update Settings</a></h2>\n";
                message += "<pre>" + jsonString + "</pre>";
                /*
                message += "<script>\n" +
                        "    if(typeof androidAppProxy !== \"undefined\"){\n" +
                        "        androidAppProxy.showMessage(\"Settings Updated\");\n" +
                        "    } else {\n" +
                        "        alert(\"Running outside Android app\");\n" +
                        "    }\n" +
                        "</script>\n";
                 */
                message += "</body></html>";
                ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
                return new WebResourceResponse("text/html", "UTF-8", targetStream);
            }
            return this.assetLoader.shouldInterceptRequest(uri);
        }
        return null;
    }
}