package net.mikespub.mywebview;

import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Repository for Settings from assets file "settings.json"
 *
 * Warning: static variables/methods cannot be overridden in Java,
 * see https://stackoverflow.com/questions/2223386/why-doesnt-java-allow-overriding-of-static-methods
 */
class MySettingsRepository extends MyJsonFileRepository {
    static final String TAG = "Settings";
    static final String fileName = "settings.json";
    static final String dirName = "web";
    // http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    //private final AppCompatActivity activity;

    /*
     * @param activity  current Activity context
     */
    //MySettingsRepository(AppCompatActivity activity) {
    //    this.activity = activity;
    //}

    /**
     * Check that the web asset files are available and load settings from settings.json
     *
     * @param activity current Activity context
     * @return  configuration
     */
    static HashMap<String, Object> loadConfiguration(AppCompatActivity activity) {
        // move this elsewhere?
        // MyAssetUtility.checkAssetFiles(activity, "local/config.json", "local");
        //loadStringConfig();
        return loadJsonFile(activity, fileName, dirName);
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
        mState.set("sites", hashMap.get("sites"));
        mState.set("other", hashMap.get("other"));
        mState.set("match", hashMap.get("match"));
        mState.set("skip", hashMap.get("skip"));
        mState.set("source", hashMap.get("source"));
        mState.set("remote_debug", hashMap.get("remote_debug"));
        mState.set("console_log", hashMap.get("console_log"));
        mState.set("js_interface", hashMap.get("js_interface"));
        mState.set("context_menu", hashMap.get("context_menu"));
        mState.set("not_matching", hashMap.get("not_matching"));
        mState.set("local_sites", hashMap.get("local_sites"));
        mState.set("update_zip", hashMap.get("update_zip"));
        mState.set("timestamp", hashMap.get("timestamp"));
        if (hashMap.containsKey("web_settings")) {
            // TODO: skip null values coming from Enum-style values in WebSettings being turned into null in json string
            mState.set("web_settings", hashMap.get("web_settings"));
        }
        // local_config are handled in MySavedStateModel for now...
        //if (hashMap.containsKey("local_config")) {
        //    mState.set("local_config", hashMap.get("local_config"));
        //}
    }

    /**
     * Get Config map from Saved State values
     *
     * @param hashMap   configuration to get
     * @param mState    Saved State Handle
     */
    static void getMapFromValues(HashMap<String, Object> hashMap, SavedStateHandle mState) {
        String source = mState.get("source");
        hashMap.put("source", source);
        List<HashMap<String, String>> sites = mState.get("sites");
        hashMap.put("sites", sites);
        String other = mState.get("other");
        hashMap.put("other", other);
        List<List<String>> match = mState.get("match");
        hashMap.put("match", match);
        List<List<String>> skip = mState.get("skip");
        hashMap.put("skip", skip);
        Boolean remote_debug = mState.get("remote_debug");
        hashMap.put("remote_debug", remote_debug);
        Boolean console_log = mState.get("console_log");
        hashMap.put("console_log", console_log);
        Boolean js_interface = mState.get("js_interface");
        hashMap.put("js_interface", js_interface);
        Boolean context_menu = mState.get("context_menu");
        hashMap.put("context_menu", context_menu);
        Boolean not_matching = mState.get("not_matching");
        hashMap.put("not_matching", not_matching);
        Boolean local_sites = mState.get("local_sites");
        hashMap.put("local_sites", local_sites);
        String update_zip = mState.get("update_zip");
        hashMap.put("update_zip", update_zip);
        // web_settings are handled in MyAppWebViewClient for now...
        // local_config are handled in MySavedStateModel for now...
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
        String local_sites = uri.getQueryParameter("local_sites");
        String update_zip = uri.getQueryParameter("update_zip");
        // web_settings are handled in MyAppWebViewClient for now...
        // local_config are handled in MySavedStateModel for now...
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
        if (local_sites != null && local_sites.equals("true")) {
            hashMap.put("local_sites", true);
        } else {
            hashMap.put("local_sites", false);
        }
        hashMap.put("update_zip", update_zip);
        // web_settings are handled in MyAppWebViewClient for now...
        // local_config are handled in MySavedStateModel for now...
        hashMap.put("source", "updated via WebView");
        hashMap.put("timestamp", getTimestamp(0));
        return hashMap;
    }

}
