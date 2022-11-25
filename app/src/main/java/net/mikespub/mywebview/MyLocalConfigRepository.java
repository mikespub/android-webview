package net.mikespub.mywebview;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateHandle;

import net.mikespub.myutils.MyAssetUtility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
        HashMap<String, Object> localConfig = loadJsonFile(activity, fileName, dirName);
        HashMap<String, String> localSites = (HashMap<String, String>) localConfig.get("sites");
        //MyAssetUtility.showMyExternalFiles(activity, Environment.DIRECTORY_DOWNLOADS, false);
        checkDemoSite(activity, 0);
        boolean hasAdded = updateLocalSites(activity, localSites);
        //List<String> localBundles = (ArrayList<String>) localConfig.get("bundles");
        localConfig.put("bundles", findAvailableBundles(activity));
        if (hasAdded) {
            saveConfiguration(activity, localConfig);
        }
        return localConfig;
    }

    static void refreshDemoSite(AppCompatActivity activity) {
        long lastUpdated = System.currentTimeMillis();
        checkDemoSite(activity, lastUpdated);
    }

    static void checkDemoSite(AppCompatActivity activity, long lastUpdated) {
        File extDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (!extDir.exists() && !extDir.mkdirs()) {
            Log.e(TAG, "Dir Create: FAIL " + extDir.getAbsolutePath());
            return;
        }
        File extFile = new File(extDir, ".installed");
        if (extFile.exists() && lastUpdated < 1) {
            Log.d(TAG, "The demo site was installed once already");
            return;
        }
        try (OutputStream out = new FileOutputStream(extFile)) {
            out.write(33);
        } catch (IOException e) {
            Log.e(TAG, "File Error: " + extFile.getAbsolutePath(), e);
            return;
        }
        // save icon to Pictures?
        MyAssetUtility.saveIconToMedia(activity);
        String dirName = "demo";
        File targetDir = new File(extDir, dirName);
        MyAssetUtility.copyAssetDir(activity, dirName, targetDir, lastUpdated);
    }

    static boolean updateLocalSites(AppCompatActivity activity, HashMap<String, String> localSites) {
        HashMap<String, String> siteMap = findLocalSites(activity);
        Log.d(TAG, "Sites: " + siteMap);
        boolean hasAdded = false;
        for (String url: siteMap.keySet()) {
            if (!localSites.containsKey(url)) {
                localSites.put(url, siteMap.get(url));
                hasAdded = true;
            }
        }
        return hasAdded;
    }

    static HashMap<String, String> findLocalSites(AppCompatActivity activity) {
        File extDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        Log.d(TAG, "Sites: " + extDir.getAbsolutePath());
        HashMap<String, String> siteMap = new HashMap<>();
        for (File file: extDir.listFiles()) {
            if (file.isDirectory()) {
                String url = file.getName().replace(" ", "+") + "/index.html";
                String title = file.getName();
                siteMap.put(url, title);
            } else if (file.getName().endsWith(".html")){
                String url = file.getName().replace(" ", "+");
                String title = file.getName().replace(".html", "");
                siteMap.put(url, title);
            }
        }
        return siteMap;
    }

    static List<String> findAvailableBundles(AppCompatActivity activity) {
        File extDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        Log.d(TAG, "Bundles: " + extDir.getAbsolutePath());
        List<String> bundles = new ArrayList<>();
        for (File file: extDir.listFiles()) {
            if (file.isDirectory()) {
            } else if (file.getName().endsWith(".zip")){
                bundles.add(file.getName());
            } else if (file.getName().endsWith(".wbn")){
                bundles.add(file.getName());
            }
        }
        return bundles;
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
        HashMap<String, Object> hashMap = new HashMap<>();
        HashMap<String, String> sites = new HashMap<>();
        for (int i = 0; i < titles.size(); i++) {
            if (titles.get(i).equals("") || urls.get(i).equals("")) {
                continue;
            }
            // Warning: Prior to Jelly Bean, this decoded the '+' character as '+' rather than ' '.
            sites.put(urls.get(i), titles.get(i).replace("+", " "));
        }
        hashMap.put("sites", sites);
        hashMap.put("timestamp", getTimestamp(0));
        return hashMap;
    }

}