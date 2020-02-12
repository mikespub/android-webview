package net.mikespub.myutils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON Utility Methods
 */
// See https://stackoverflow.com/questions/22011200/creating-hashmap-from-a-json-string/51121461#51121461
public class MyJsonUtility {

    /**
     * Convert JSON string or object to map
     *
     * @param json  string or object to convert to map
     * @return      converted map
     * @throws JSONException    trouble converting
     */
    public static Map<String, Object> jsonToMap(Object json) throws JSONException {

        if(json instanceof JSONObject)
            return _jsonToMap_((JSONObject)json) ;

        else if (json instanceof String)
        {
            JSONObject jsonObject = new JSONObject((String)json) ;
            return _jsonToMap_(jsonObject) ;
        }
        return null ;
    }

    /**
     * @param json  JSON object to convert to map
     * @return      converted map
     * @throws JSONException    trouble converting
     */
    private static Map<String, Object> _jsonToMap_(JSONObject json) throws JSONException {
        Map<String, Object> retMap = new HashMap<>();

        if(json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }


    /**
     * @param object    JSON object to convert to map
     * @return          converted map
     * @throws JSONException    trouble converting
     */
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


    /**
     * @param array JSON array to convert to list
     * @return      converted list
     * @throws JSONException    trouble converting
     */
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

    /**
     * Convert object to JSON object or array
     *
     * @param object    object to convert to JSON
     * @return          converted JSON object
     * @throws JSONException    trouble converting
     */
    // https://stackoverflow.com/questions/12155800/how-to-convert-hashmap-to-json-object-in-java
    public static Object toJson(Object object) throws JSONException {
        if (object instanceof Map<?, ?>) {
            return mapToJson((Map<String, Object>) object);
        } else if (object instanceof Set<?>) {
            return setToJson((Set<Map.Entry<String, Object>>) object);
        } else if (object instanceof Iterable) {
            return listToJson((Iterable) object);
        }
        else {
            return object;
        }
    }

    /**
     * Convert map to JSON object
     *
     * @param map   map to convert to JSON object
     * @return      converted JSON object
     * @throws JSONException    trouble converting
     */
    private static JSONObject mapToJson(Map<String, Object> map) throws JSONException {
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

    /**
     * Convert set to JSON object - like ContentValues from ContentQueryMap
     *
     * @param set   set to convert to JSON object
     * @return      converted JSON object
     * @throws JSONException    trouble converting
     */
    // https://stackoverflow.com/questions/16108734/convert-setmap-entryk-v-to-hashmapk-v
    private static JSONObject setToJson(Set<Map.Entry<String, Object>> set) throws JSONException {
        JSONObject jsonData = new JSONObject();
        for (Map.Entry<String, Object> entry : set) {
            jsonData.put(entry.getKey(), toJson(entry.getValue()));
        }
        return jsonData;
    }

    /**
     * Convert list to JSON array
     *
     * @param list  list to convert to JSON array
     * @return      converted JSON array
     * @throws JSONException    trouble converting
     */
    private static JSONArray listToJson(Iterable list) throws JSONException {
        JSONArray jsonData = new JSONArray();
        for (Object value : list) {
            jsonData.put(toJson(value));
        }
        return jsonData;
    }

    /**
     * Convert object to JSON string
     *
     * @param object    object to convert to JSON string
     * @return          converted JSON string
     * @throws JSONException    trouble converting
     */
    public static String toJsonString(Object object) throws JSONException {
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