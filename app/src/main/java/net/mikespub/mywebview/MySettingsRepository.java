package net.mikespub.mywebview;

import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

class MySettingsRepository {
    private static final String TAG = "Settings";
    static final String fileName = "web/settings.json";
    // http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    private final AppCompatActivity activity;

    MySettingsRepository(AppCompatActivity activity) {
        this.activity = activity;
    }

    HashMap<String, Object> loadJsonSettings() {
        long lastUpdated = MyAssetUtility.checkAssetFiles(activity);
        //loadStringConfig();
        return loadJsonConfig(lastUpdated);
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
        String content = getJsonSettings();
        HashMap<String, Object> hashMap = null;
        try {
            hashMap = new HashMap<>(MyJsonUtility.jsonToMap(content));
            Log.d(TAG, hashMap.toString());
            if (!hashMap.containsKey("timestamp")) {
                String timestamp = getTimestamp(0);
                Log.d(TAG, "Timestamp New: " + timestamp);
                hashMap.put("timestamp", timestamp);
            } else if (lastUpdated > 0) {
                String timestamp = getTimestamp(lastUpdated);
                Log.d(TAG, "Timestamp Old: " + timestamp);
                // String timestamp = Instant.now().toString();
                hashMap.put("timestamp", timestamp);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            //loadStringConfig();
        }
        return hashMap;
    }

    private String getJsonSettings() {
        try {
            return MyAssetUtility.getFilenameString(activity, fileName);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
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
        String remote_debug = uri.getQueryParameter("remote_debug");
        String console_log = uri.getQueryParameter("console_log");
        String js_interface = uri.getQueryParameter("js_interface");
        String context_menu = uri.getQueryParameter("context_menu");
        String not_matching = uri.getQueryParameter("not_matching");
        String update_zip = uri.getQueryParameter("update_zip");
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
        if (remote_debug != null && remote_debug.equals("true")) {
            hashMap.put("remote_debug", true);
        } else {
            hashMap.put("remote_debug", false);
        }
        if (console_log != null && console_log.equals("true")) {
            hashMap.put("console_log", true);
        } else {
            hashMap.put("console_log", false);
        }
        if (js_interface != null && js_interface.equals("true")) {
            hashMap.put("js_interface", true);
        } else {
            hashMap.put("js_interface", false);
        }
        if (context_menu != null && context_menu.equals("true")) {
            hashMap.put("context_menu", true);
        } else {
            hashMap.put("context_menu", false);
        }
        if (not_matching != null && not_matching.equals("true")) {
            hashMap.put("not_matching", true);
        } else {
            hashMap.put("not_matching", false);
        }
        hashMap.put("update_zip", update_zip);
        hashMap.put("source", "updated via WebView");
        hashMap.put("timestamp", getTimestamp(0));
        return hashMap;
    }

    String saveJsonSettings(HashMap<String, Object> hashMap) {
        hashMap.put("timestamp", getTimestamp(0));
        Log.d(TAG, hashMap.toString());
        JSONObject jsonObject=new JSONObject(hashMap);
        String jsonString = "";
        // https://stackoverflow.com/questions/16563579/jsonobject-tostring-how-not-to-escape-slashes
        try {
            jsonString = jsonObject.toString(2).replace("\\","");
            //Log.d(TAG, jsonString);
            MyAssetUtility.saveFilenameString(activity, fileName, jsonString);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
        return jsonString;
    }
}
