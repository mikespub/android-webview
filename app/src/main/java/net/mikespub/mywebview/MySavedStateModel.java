package net.mikespub.mywebview;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;

/**
 * ViewModel with Saved State for Settings
 */
// https://proandroiddev.com/when-to-load-data-in-viewmodels-ad9616940da7
public class MySavedStateModel extends ViewModel {
    private final SavedStateHandle mState;

    /**
     * @param savedStateHandle  Saved State Handle
     */
    public MySavedStateModel(SavedStateHandle savedStateHandle) {
        mState = savedStateHandle;
        Log.d("Saved State", mState.toString());
    }

    /**
     * Get value from Saved State
     *
     * @param key   key name of the value to get
     * @return      value to get
     */
    Object getValue(String key) {
        return mState.get(key);
    }

    /**
     * Set value to Saved State
     *
     * @param key   key name of the value to set
     * @param value value to set
     */
    void setValue(String key, Object value) {
        mState.set(key, value);
    }

    /**
     * Get current Settings map from Saved State or Repository
     *
     * @param activity  current Activity context
     * @return          configuration settings
     */
    HashMap<String, Object>getSettings(AppCompatActivity activity) {
        HashMap<String, Object> hashMap = new HashMap<>();
        String source = (String) getValue("source");
        if (source == null) {
            //MySettingsRepository repo = new MySettingsRepository(activity);
            hashMap = MySettingsRepository.loadJsonSettings(activity);
            MySettingsRepository.setValuesFromMap(hashMap, mState);
        } else {
            MySettingsRepository.getMapFromValues(hashMap, mState);
        }
        return hashMap;
    }

    /**
     * Set current Settings map to Saved State and Repository
     *
     * @param activity  current Activity context
     * @param hashMap   configuration settings to set
     * @return          json string with the new settings
     */
    String setSettings(AppCompatActivity activity, HashMap<String, Object> hashMap) {
        MySettingsRepository.setValuesFromMap(hashMap, mState);
        //MySettingsRepository repo = new MySettingsRepository(activity);
        //setValue("timestamp", repo.getTimestamp(0));
        return MySettingsRepository.saveJsonSettings(activity, hashMap);
    }

}
