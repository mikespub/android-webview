package net.mikespub.mywebview;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

class MyAppWebViewClient extends WebViewClient {
    // http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    private final AppCompatActivity activity;
    private WebViewAssetLoader assetLoader;
    private String[][] myMatchCompare;
    private String[][] mySkipCompare;

    MyAppWebViewClient(AppCompatActivity activity) {
        this.activity = activity;
        long lastUpdated = this.checkAssetFiles();
        //this.loadStringConfig();
        this.loadJsonConfig(lastUpdated);
    }

    private String getTimestamp(long lastUpdated) {
        // https://stackoverflow.com/questions/13515168/android-time-in-iso-8601
        // works with Instant
        // Instant instant = Instant.now();
        // String timestamp = (String) instant.format(DateTimeFormatter.ISO_INSTANT);
        //ZonedDateTime zdt = ZonedDateTime.now();
        //String timestamp = zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        // https://stackoverflow.com/questions/3914404/how-to-get-current-moment-in-iso-8601-format-with-date-hour-and-minute
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String timestamp;
        if (lastUpdated > 0) {
            timestamp = df.format(new Date(lastUpdated));
        } else {
            timestamp = df.format(new Date());

        }
        // String timestamp = Instant.now().toString();
        return timestamp;
    }

    private void loadJsonConfig(long lastUpdated) {
        String filename = "web/settings.json";
        String content = this.getJsonSettings(filename);
        try {
            HashMap<String, Object> hashMap = new HashMap<>(MyJsonUtility.jsonToMap(content));
            Log.d("Settings", hashMap.toString());
            if (!hashMap.containsKey("timestamp")) {
                String timestamp = getTimestamp(0);
                Log.d("Timestamp New", timestamp);
                hashMap.put("timestamp", timestamp);
            } else if (lastUpdated > 0) {
                String timestamp = getTimestamp(lastUpdated);
                Log.d("Timestamp Old", timestamp);
                // String timestamp = Instant.now().toString();
                hashMap.put("timestamp", timestamp);
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
        } catch (JSONException e) {
            Log.e("Settings", e.toString());
            this.loadStringConfig();
        }
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

    private long checkAssetFiles() {
        // /data/user/0/net.mikespub.mywebview/files
        // File filesdir = getFilesDir();
        // Log.d("Internal Files Dir", filesdir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files/Documents
        // File extdocsdir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        // Log.d("External Docs Dir", extdocsdir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files
        // File extfilesdir = getExternalFilesDir(null);
        // Log.d("External Files Dir", extfilesdir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files/web
        File extwebdir = new File(this.activity.getExternalFilesDir(null), "web");
        Log.d("External Web Dir", extwebdir.getAbsolutePath());
        // https://stackoverflow.com/questions/5248094/is-it-possible-to-get-last-modified-date-from-an-assets-file - using shared preferences in the end
        // See also https://stackoverflow.com/questions/37953002/mess-with-the-shared-preferences-of-android-which-function-to-use/37953072 for preferences
        long lastUpdated = 0;
        if (!extwebdir.exists()) {
            extwebdir.mkdirs();
            Log.d("External Web Dir", Boolean.toString(extwebdir.exists()));
        } else {
            try {
                PackageManager pm = this.activity.getPackageManager();
                PackageInfo appInfo = pm.getPackageInfo(this.activity.getPackageName(), 0);
                lastUpdated = appInfo.lastUpdateTime;
                Log.d("Package", String.valueOf(lastUpdated));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("Package", e.toString());
            }
        }
        copyAssetFiles(extwebdir, lastUpdated);
        return lastUpdated;
    }

    // https://stackoverflow.com/questions/4447477/how-to-copy-files-from-assets-folder-to-sdcard
    private void copyAssetFiles(File extwebdir, long lastUpdated) {
        AssetManager manager = this.activity.getAssets();
        String[] files;
        try {
            files = manager.list("web");
            Log.d("Web Files", Arrays.toString(files));
        } catch (IOException e) {
            Log.e("Web Files", e.toString());
            return;
        }
        for (String f: files) {
            File extfile = new File(extwebdir, f);
            if (!extfile.exists() || lastUpdated > extfile.lastModified()) {
                Log.d("Web File Missing", extfile.getAbsolutePath());
                try (InputStream in = manager.open("web/" + f); OutputStream out = new FileOutputStream(extfile)) {
                    copyFile(in, out);
                } catch (IOException e) {
                    Log.e("Web File", e.toString());
                    break;
                }
                // NOOP
                // NOOP
                Log.d("Web File Copied", extfile.getAbsolutePath());
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private String getJsonSettings(String filename) {
        AssetManager manager = this.activity.getAssets();
        try {
            Writer writer = new StringWriter();
            try (InputStream input = manager.open(filename)) {
                char[] buffer = new char[2048];
                Reader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            }
            return writer.toString();
        } catch (IOException e) {
            Log.e("getJsonSettings", e.toString());
        }
        return null;
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
                String query = uri.getQuery();
                List<String> titles = uri.getQueryParameters("title[]");
                List<String> urls = uri.getQueryParameters("url[]");
                List<String> match0 = uri.getQueryParameters("match0[]");
                List<String> match1 = uri.getQueryParameters("match1[]");
                List<String> match2 = uri.getQueryParameters("match2[]");
                List<String> skip0 = uri.getQueryParameters("skip0[]");
                List<String> skip1 = uri.getQueryParameters("skip1[]");
                List<String> skip2 = uri.getQueryParameters("skip2[]");
                String other = uri.getQueryParameter("other");
                HashMap<String, Object> hashMap = new HashMap<>();
                List<HashMap<String, String>> sites = new ArrayList<>();
                for (int i = 0; i < titles.size(); i++) {
                    if (titles.get(i).equals("") || urls.get(i).equals("")) {
                        continue;
                    }
                    HashMap<String, String> site = new HashMap<>();
                    site.put("url", urls.get(i));
                    // Warning: Prior to Jelly Bean, this decoded the '+' character as '+' rather than ' '.
                    site.put("title", titles.get(i).replace("+", " "));
                    sites.add(site);
                }
                hashMap.put("sites", sites);
                hashMap.put("other", other);
                List<List<String>> matches = new ArrayList<>();
                for (int i = 0; i < match0.size(); i++) {
                    if (match0.get(i).equals("") || match1.get(i).equals("") || match2.get(i).equals("")) {
                        continue;
                    }
                    List<String> match = new ArrayList<>();
                    match.add(match0.get(i));
                    match.add(match1.get(i));
                    match.add(match2.get(i));
                    matches.add(match);
                }
                hashMap.put("match", matches);
                List<List<String>> skips = new ArrayList<>();
                for (int i = 0; i < skip0.size(); i++) {
                    if (skip0.get(i).equals("") || skip1.get(i).equals("") || skip2.get(i).equals("")) {
                        continue;
                    }
                    List<String> skip = new ArrayList<>();
                    skip.add(skip0.get(i));
                    skip.add(skip1.get(i));
                    skip.add(skip2.get(i));
                    skips.add(skip);
                }
                hashMap.put("skip", skips);
                hashMap.put("source", "updated via webview");
                hashMap.put("timestamp", getTimestamp(0));
                Log.d("Web Settings", hashMap.toString());
                JSONObject jsonObject=new JSONObject(hashMap);
                String jsonString = "";
                // https://stackoverflow.com/questions/16563579/jsonobject-tostring-how-not-to-escape-slashes
                try {
                    jsonString = jsonObject.toString(2).replace("\\","");
                    Log.d("Web Settings", jsonString);
                    File extwebfile = new File(this.activity.getExternalFilesDir(null), "web/settings.json");
                    // https://stackoverflow.com/questions/11371154/outputstreamwriter-vs-filewriter/11371322
                    try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(extwebfile), StandardCharsets.UTF_8)) {
                        osw.write(jsonString);
                    } catch (IOException e) {
                        Log.e("Web Settings", e.toString());
                    }
                } catch (JSONException e) {
                    Log.e("Web Settings", e.toString());
                }
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
                /*
                message += "<p>Query: '" + query + "'</p>";
                message += "<p>Titles: " + Arrays.toString(titles.toArray()) + "</p>";
                message += "<p>Urls: " + Arrays.toString(urls.toArray()) + "</p>";
                message += "<p>Match0: " + Arrays.toString(match0.toArray()) + "</p>";
                message += "<p>Match1: " + Arrays.toString(match1.toArray()) + "</p>";
                message += "<p>Match2: " + Arrays.toString(match2.toArray()) + "</p>";
                message += "<p>Skip0: " + Arrays.toString(skip0.toArray()) + "</p>";
                message += "<p>Skip1: " + Arrays.toString(skip1.toArray()) + "</p>";
                message += "<p>Skip2: " + Arrays.toString(skip2.toArray()) + "</p>";
                message += "<p>Other: " + other + "</p>";
                 */
                message += "<pre>" + jsonString + "</pre>";
                message += "</body></html>";
                ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
                return new WebResourceResponse("text/html", "UTF-8", targetStream);
            }
            return this.assetLoader.shouldInterceptRequest(uri);
        }
        return null;
    }
}