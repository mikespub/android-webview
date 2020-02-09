package net.mikespub.mywebview;

import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ViewModel with Saved State for Settings
 */
// https://proandroiddev.com/when-to-load-data-in-viewmodels-ad9616940da7
public class MySavedStateModel extends ViewModel {
    private final SavedStateHandle mState;

    /**
     * @param savedStateHandle  saved state handle
     */
    public MySavedStateModel(SavedStateHandle savedStateHandle) {
        mState = savedStateHandle;
        Log.d("Saved State", mState.toString());
    }

    /**
     * @param key   key name of the value to get
     * @return      value to get
     */
    Object getValue(String key) {
        return mState.get(key);
    }

    /**
     * @param key   key name of the value to set
     * @param value value to set
     */
    void setValue(String key, Object value) {
        mState.set(key, value);
    }

    /**
     * @param activity  current Activity context
     * @return          configuration settings
     */
    HashMap<String, Object>getSettings(AppCompatActivity activity) {
        HashMap<String, Object> hashMap = new HashMap<>();
        String source = (String) getValue("source");
        if (source == null) {
            MySettingsRepository repo = new MySettingsRepository(activity);
            hashMap = repo.loadJsonSettings();
            setValuesFromMap(hashMap);
        } else {
            getMapFromValues(hashMap);
        }
        return hashMap;
    }

    /**
     * @param activity  current Activity context
     * @param uri       query uri to parse the configuration settings from
     * @return          configuration settings parsed
     */
    HashMap<String, Object> parseQuery(AppCompatActivity activity, Uri uri) {
        MySettingsRepository repo = new MySettingsRepository(activity);
        return MySettingsRepository.parseQueryParameters(uri);
    }

    /**
     * @param hashMap   configuration settings to set
     */
    private void setValuesFromMap(HashMap<String, Object> hashMap) {
        setValue("sites", hashMap.get("sites"));
        setValue("other", hashMap.get("other"));
        setValue("match", hashMap.get("match"));
        setValue("skip", hashMap.get("skip"));
        setValue("source", hashMap.get("source"));
        setValue("remote_debug", hashMap.get("remote_debug"));
        setValue("console_log", hashMap.get("console_log"));
        setValue("js_interface", hashMap.get("js_interface"));
        setValue("context_menu", hashMap.get("context_menu"));
        setValue("not_matching", hashMap.get("not_matching"));
        setValue("update_zip", hashMap.get("update_zip"));
        setValue("timestamp", hashMap.get("timestamp"));
        if (hashMap.containsKey("web_settings")) {
            // TODO: skip null values coming from Enum-style values in WebSettings being turned into null in json string
            setValue("web_settings", hashMap.get("web_settings"));
        }
    }

    /**
     * @param hashMap   configuration settings to get
     */
    private void getMapFromValues(HashMap<String, Object> hashMap) {
        String source = (String) getValue("source");
        hashMap.put("source", source);
        List<HashMap<String, String>> sites = (ArrayList) getValue("sites");
        hashMap.put("sites", sites);
        String other = (String) getValue("other");
        hashMap.put("other", other);
        List<List<String>> match = (ArrayList) getValue("match");
        hashMap.put("match", match);
        List<List<String>> skip = (ArrayList) getValue("skip");
        hashMap.put("skip", skip);
        Boolean remote_debug = (Boolean) getValue("remote_debug");
        hashMap.put("remote_debug", remote_debug);
        Boolean console_log = (Boolean) getValue("console_log");
        hashMap.put("console_log", console_log);
        Boolean js_interface = (Boolean) getValue("js_interface");
        hashMap.put("js_interface", js_interface);
        Boolean context_menu = (Boolean) getValue("context_menu");
        hashMap.put("context_menu", context_menu);
        Boolean not_matching = (Boolean) getValue("not_matching");
        hashMap.put("not_matching", not_matching);
        String update_zip = (String) getValue("update_zip");
        hashMap.put("update_zip", update_zip);
    }

    /**
     * @param activity  current Activity context
     * @param hashMap   configuration settings to set
     * @return          json string with the new settings
     */
    String setSettings(AppCompatActivity activity, HashMap<String, Object> hashMap) {
        setValuesFromMap(hashMap);
        MySettingsRepository repo = new MySettingsRepository(activity);
        //setValue("timestamp", repo.getTimestamp(0));
        return repo.saveJsonSettings(hashMap);
    }

}
