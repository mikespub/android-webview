package net.mikespub.mywebview;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateHandle;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Repository for Settings from assets file "settings.json"
 *
 * Warning: static variables/methods cannot be overridden in Java,
 * see https://stackoverflow.com/questions/2223386/why-doesnt-java-allow-overriding-of-static-methods
 */
class MyLocalConfigRepository extends MyJsonFileRepository {
    static final String TAG = "LocalConfig";
    static final String fileName = "local/config.json";
    static final String dirName = "local";
    // http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    //private final AppCompatActivity activity;

    /*
     * @param activity  current Activity context
     */
    //MyLocalConfigRepository(AppCompatActivity activity) {
    //    this.activity = activity;
    //}

    /**
     * Check that the local asset files are available and load config from local/config.json
     *
     * @param activity current Activity context
     * @return  configuration
     */
    static HashMap<String, Object> loadConfiguration(AppCompatActivity activity) {
        //MyAssetUtility.showMyExternalFiles(activity, Environment.DIRECTORY_DOWNLOADS, false);
        HashMap<String, String> siteMap = findLocalSites(activity);
        Log.d(TAG, "Sites: " + siteMap.toString());
        HashMap<String, Object> localConfig = loadJsonFile(activity, fileName, dirName);
        HashMap<String, String> localSites = (HashMap<String, String>) localConfig.get("sites");
        boolean hasAdded = false;
        for (String url: siteMap.keySet()) {
            if (!localSites.containsKey(url)) {
                localSites.put(url, siteMap.get(url));
                hasAdded = true;
            }
        }
        localConfig.put("sites", localSites);
        if (hasAdded) {
            saveConfiguration(activity, localConfig);
        }
        return localConfig;
    }

    static HashMap<String, String> findLocalSites(AppCompatActivity activity) {
        File extDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        Log.d(TAG, "Sites: " + extDir.getAbsolutePath());
        HashMap<String, String> siteMap = new HashMap<>();
        for (File file: extDir.listFiles()) {
            if (file.isDirectory()) {
                String url = "/sites/" + file.getName().replace(" ", "+") + "/index.html";
                String title = file.getName();
                siteMap.put(url, title);
            } else if (file.getName().endsWith(".html")){
                String url = "/sites/" + file.getName().replace(" ", "+");
                String title = file.getName().replace(".html", "");
                siteMap.put(url, title);
            }
        }
        return siteMap;
    }

    /**
     * Save configuration to JSON file
     *
     * @param activity  current Activity context
     * @param hashMap   configuration to save
     * @return          json string with the new configuration
     */
    static String saveConfiguration(AppCompatActivity activity, HashMap<String, Object> hashMap) {
        return saveJsonFile(activity, hashMap, fileName);
    }

    /**
     * Set Saved State values from Config map
     *
     * @param hashMap   configuration to set
     * @param mState    Saved State Handle
     */
    static void setValuesFromMap(HashMap<String, Object> hashMap, SavedStateHandle mState) {
        mState.set("local_config", hashMap.get("local_config"));
    }

    /**
     * Get Config map from Saved State values
     *
     * @param hashMap   configuration to get
     * @param mState    Saved State Handle
     */
    static void getMapFromValues(HashMap<String, Object> hashMap, SavedStateHandle mState) {
        HashMap<String, Object> localConfig = mState.get("local_config");
        hashMap.put("local_config", localConfig);
    }

    /**
     * Parse configuration from query uri
     *
     * @param uri   query uri to parse the configuration from
     * @return      configuration parsed
     */
    static HashMap<String, Object> parseQueryParameters(Uri uri) {
        // String query = uri.getQuery();
        List<String> titles = uri.getQueryParameters("title[]");
        List<String> urls = uri.getQueryParameters("url[]");
        // web_settings are handled in MyAppWebViewClient for now...
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
        // web_settings are handled in MyAppWebViewClient for now...
        hashMap.put("timestamp", getTimestamp(0));
        return hashMap;
    }

}
