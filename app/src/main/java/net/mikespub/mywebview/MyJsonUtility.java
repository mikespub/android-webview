package net.mikespub.mywebview;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// See https://stackoverflow.com/questions/22011200/creating-hashmap-from-a-json-string/51121461#51121461
class MyJsonUtility {

    static Map<String, Object> jsonToMap(Object json) throws JSONException {

        if(json instanceof JSONObject)
            return _jsonToMap_((JSONObject)json) ;

        else if (json instanceof String)
        {
            JSONObject jsonObject = new JSONObject((String)json) ;
            return _jsonToMap_(jsonObject) ;
        }
        return null ;
    }

    private static Map<String, Object> _jsonToMap_(JSONObject json) throws JSONException {
        Map<String, Object> retMap = new HashMap<>();

        if(json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }


    private static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }


    private static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    // https://stackoverflow.com/questions/12155800/how-to-convert-hashmap-to-json-object-in-java
    static Object toJson(Object object) throws JSONException {
        if (object instanceof Map<?, ?>) {
            return mapToJson((Map<String, Object>) object);
        } else if (object instanceof Iterable) {
            return listToJson((Iterable) object);
        }
        else {
            return object;
        }
    }

    static JSONObject mapToJson(Map<String, Object> map) throws JSONException {
        JSONObject jsonData = new JSONObject();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            //if (value instanceof Map<?, ?>) {
            //    value = mapToJson((Map<String, Object>) value);
            //}
            //jsonData.put(key, value);
            jsonData.put(key, toJson(value));
        }
        return jsonData;
    }

    static JSONArray listToJson(Iterable list) throws JSONException {
        JSONArray jsonData = new JSONArray();
        for (Object value : list) {
            jsonData.put(toJson(value));
        }
        return jsonData;
    }

    static String toJsonString(Object object) throws JSONException {
        Object jsonData = toJson(object);
        if (jsonData instanceof JSONObject) {
            return ((JSONObject) jsonData).toString(2).replace("\\","");
        } else if (jsonData instanceof JSONArray) {
            return ((JSONArray) jsonData).toString(2).replace("\\","");
        } else {
            return jsonData.toString();
        }
    }
}