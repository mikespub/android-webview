package net.mikespub.mywebview;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import net.mikespub.myutils.MyAssetUtility;
import net.mikespub.myutils.MyJsonUtility;

import org.json.JSONException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

class MyJsonFileRepository {
    static final String TAG = "JsonFileRepo";
    private static final String timeZone = "UTC";
    private static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Check that the dirName asset files are available and load configuration from fileName
     *
     * @param activity current Activity context
     * @param fileName JSON file containing the configuration
     * @param dirName  directory containing the asset files
     * @return  configuration settings
     */
    static HashMap<String, Object> loadJsonFile(AppCompatActivity activity, String fileName, String dirName) {
        long lastUpdated = MyAssetUtility.checkAssetFiles(activity, fileName, dirName);
        String jsonString = getJsonFile(activity, fileName);
        return parseJsonConfig(jsonString, lastUpdated);
    }

    /**
     * Get an ISO-8601 formatted datetime string from a timestamp or now
     *
     * @param lastUpdated   last update time of the current package or 0 for current time
     * @return              ISO-8601 formatted datetime string
     */
    static String getTimestamp(long lastUpdated) {
        // https://stackoverflow.com/questions/13515168/android-time-in-iso-8601
        // works with Instant
        // Instant instant = Instant.now();
        // String timestamp = (String) instant.format(DateTimeFormatter.ISO_INSTANT);
        //ZonedDateTime zdt = ZonedDateTime.now();
        //String timestamp = zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        // https://stackoverflow.com/questions/3914404/how-to-get-current-moment-in-iso-8601-format-with-date-hour-and-minute
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        DateFormat df = new SimpleDateFormat(dateFormat, Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
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

    /**
     * Parse configuration from JSON string
     *
     * @param jsonString    json string with configuration
     * @param lastUpdated   last update time of the configuration
     * @return              configuration
     */
    static private HashMap<String, Object> parseJsonConfig(String jsonString, long lastUpdated) {
        HashMap<String, Object> hashMap = null;
        try {
            hashMap = new HashMap<>(MyJsonUtility.jsonToMap(jsonString));
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
            Log.e(TAG, "Load Config", e);
            //loadStringConfig();
        }
        return hashMap;
    }

    /**
     * Get JSON string from fileName
     *
     * @param activity current Activity context
     * @param fileName JSON file containing the configuration
     * @return  json string with the current settings
     */
    static private String getJsonFile(AppCompatActivity activity, String fileName) {
        try {
            return MyAssetUtility.getFilenameString(activity, fileName);
        } catch (IOException e) {
            Log.e(TAG, "Get Config: " + fileName, e);
        }
        return null;
    }

    /**
     * Save configuration to JSON file
     *
     * @param activity  current Activity context
     * @param hashMap   configuration to save
     * @param fileName JSON file containing the configuration
     * @return          json string with the new configuration
     */
    static String saveJsonFile(AppCompatActivity activity, HashMap<String, Object> hashMap, String fileName) {
        hashMap.put("timestamp", getTimestamp(0));
        Log.d(TAG, hashMap.toString());
        //JSONObject jsonObject = new JSONObject(hashMap);
        String jsonString = "";
        // https://stackoverflow.com/questions/16563579/jsonobject-tostring-how-not-to-escape-slashes
        try {
            //jsonString = jsonObject.toString(2).replace("\\","");
            jsonString = MyJsonUtility.toJsonString(hashMap);
            //Log.d(TAG, jsonString);
            MyAssetUtility.saveFilenameString(activity, fileName, jsonString);
        } catch (JSONException e) {
            Log.e(TAG, "Save Config: " + fileName, e);
        }
        return jsonString;
    }
}
