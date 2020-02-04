package net.mikespub.mywebview;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

class MySettingsRepository {
    // http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    private final AppCompatActivity activity;
    private String[][] myMatchCompare;
    private String[][] mySkipCompare;

    MySettingsRepository(AppCompatActivity activity) {
        this.activity = activity;
    }

    HashMap<String, Object> loadJsonSettings() {
        long lastUpdated = this.checkAssetFiles();
        //this.loadStringConfig();
        return this.loadJsonConfig(lastUpdated);
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
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
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

    private HashMap<String, Object> loadJsonConfig(long lastUpdated) {
        String filename = "web/settings.json";
        String content = this.getJsonSettings(filename);
        HashMap<String, Object> hashMap = null;
        try {
            hashMap = new HashMap<>(MyJsonUtility.jsonToMap(content));
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
        } catch (JSONException e) {
            Log.e("Settings", e.toString());
            //this.loadStringConfig();
        }
        return hashMap;
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
                Log.d("Web Package", String.valueOf(lastUpdated));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("Web Package", e.toString());
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

    HashMap<String, Object> parseQueryParameters(Uri uri) {
        // String query = uri.getQuery();
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
        return hashMap;
    }

    String saveJsonSettings(HashMap<String, Object> hashMap) {
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
        return jsonString;
    }
}
