package net.mikespub.mywebview;

import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// https://proandroiddev.com/when-to-load-data-in-viewmodels-ad9616940da7
public class MySavedStateModel extends ViewModel {
    private final SavedStateHandle mState;

    public MySavedStateModel(SavedStateHandle savedStateHandle) {
        mState = savedStateHandle;
        Log.d("Saved State", mState.toString());
    }

    Object getValue(String key) {
        return mState.get(key);
    }

    void setValue(String key, Object value) {
        mState.set(key, value);
    }

    HashMap<String, Object>getSettings(AppCompatActivity activity) {
        HashMap<String, Object> hashMap = new HashMap<>();
        String source = (String) getValue("source");
        if (source == null) {
            MySettingsRepository repo = new MySettingsRepository(activity);
            hashMap = repo.loadJsonSettings();
            setValuesFromMap(hashMap);
            return hashMap;
        }
        hashMap.put("source", source);
        List<HashMap<String, String>> sites = (ArrayList) getValue("sites");
        hashMap.put("sites", sites);
        String other = (String) getValue("other");
        hashMap.put("other", other);
        List<List<String>> match = (ArrayList) getValue("match");
        hashMap.put("match", match);
        List<List<String>> skip = (ArrayList) getValue("skip");
        hashMap.put("skip", skip);
        Boolean console_log = (Boolean) getValue("console_log");
        hashMap.put("console_log", console_log);
        Boolean js_interface = (Boolean) getValue("js_interface");
        hashMap.put("js_interface", js_interface);
        Boolean context_menu = (Boolean) getValue("context_menu");
        hashMap.put("context_menu", context_menu);
        return hashMap;
    }

    HashMap<String, Object> parseQuery(AppCompatActivity activity, Uri uri) {
        MySettingsRepository repo = new MySettingsRepository(activity);
        return repo.parseQueryParameters(uri);
    }

    private void setValuesFromMap(HashMap<String, Object> hashMap) {
        setValue("sites", hashMap.get("sites"));
        setValue("other", hashMap.get("other"));
        setValue("match", hashMap.get("match"));
        setValue("skip", hashMap.get("skip"));
        setValue("source", hashMap.get("source"));
        setValue("console_log", hashMap.get("console_log"));
        setValue("js_interface", hashMap.get("js_interface"));
        setValue("context_menu", hashMap.get("context_menu"));
        setValue("timestamp", hashMap.get("timestamp"));
    }

    String setSettings(AppCompatActivity activity, HashMap<String, Object> hashMap) {
        setValuesFromMap(hashMap);
        MySettingsRepository repo = new MySettingsRepository(activity);
        //setValue("timestamp", repo.getTimestamp(0));
        return repo.saveJsonSettings(hashMap);
    }
}
