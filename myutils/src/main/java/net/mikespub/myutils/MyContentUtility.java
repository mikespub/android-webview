package net.mikespub.myutils;

import android.app.DownloadManager;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Content Utility Methods
 */
public class MyContentUtility {
    private static final String TAG = "Content";

    /**
     * Show detail of a content uri via content resolver
     *
     * @param activity  current Activity context
     * @param uri       content uri to show
     */
    /*
    Content: 0 name: _id type: 1 value: 47
    Content: 1 name: entity type: 0 value: null
    Content: 2 name: _data type: 3 value: /storage/emulated/0/Android/data/net.mikespub.mywebview/files/assets.zip
    Content: 3 name: mimetype type: 3 value: application/zip
    Content: 4 name: visibility type: 1 value: 0
    Content: 5 name: destination type: 1 value: 4
    Content: 6 name: control type: 0 value: null
    Content: 7 name: status type: 1 value: 200
    Content: 8 name: lastmod type: 1 value: 540540424
    Content: 9 name: notificationpackage type: 3 value: net.mikespub.mywebview
    Content: 10 name: notificationclass type: 0 value: null
    Content: 11 name: total_bytes type: 1 value: 6313
    Content: 12 name: current_bytes type: 1 value: 6313
    Content: 13 name: title type: 3 value: assets.zip
    Content: 14 name: description type: 3 value:
    Content: 15 name: uri type: 3 value: https://github.com/mikespub/android-webview/raw/master/app/release/updates/assets.zip
    Content: 16 name: is_visible_in_downloads_ui type: 1 value: 1
    Content: 17 name: hint type: 3 value: file:///storage/emulated/0/Android/data/net.mikespub.mywebview/files/assets.zip
    Content: 18 name: mediaprovider_uri type: 0 value: null
    Content: 19 name: deleted type: 1 value: 0
    Content: 20 name: _display_name type: 3 value: assets.zip
    Content: 21 name: _size type: 1 value: 6313
     */
    public static void showContent(AppCompatActivity activity, Uri uri) {
        Map<String, Object> cursorInfo = getContent(activity, uri);
        if (cursorInfo != null) {
            try {
                Log.d(TAG, MyJsonUtility.toJsonString(cursorInfo));
            } catch (Exception e) {
                Log.e(TAG, cursorInfo.toString(), e);
            }
        } else {
            Log.d(TAG, "No Content Found: " + uri.toString());
        }
    }

    public static Map<String, Object> getContent(AppCompatActivity activity, Uri uri) {
        Log.d(TAG, "URI: " + uri);
        Cursor cursor = activity.getContentResolver().query(uri,null,null,null,null);
        if (cursor == null) {
            return null;
        }
        Map<String, Object> cursorInfo = null;
        if (cursor.moveToFirst()) {
            cursorInfo = getCursorInfo(cursor, uri);
            if (!cursorInfo.containsKey("[content_uri]")) {
                cursorInfo.put("[content_uri]", uri.toString());
            }
        }
        cursor.close();
        return cursorInfo;
    }

    /**
     * Show columns for current cursor
     *
     * @param cursor    current cursor
     */
    private static Map<String, Object> getCursorInfo(Cursor cursor, Uri parentUri) {
        //String local_uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
        //Log.d(TAG, "Local URI: " + local_uri);
        //String[] columns = cursor.getColumnNames();
        //Log.d(TAG, "Columns: " + Arrays.toString(columns));
        //Uri contentUri = ContentUris.withAppendedId(uri, cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_ID)));
        //String id = DocumentsContract.getDocumentId(contentUri);
        //Log.d(TAG, "Current URI: " + contentUri + " Document Id: " + id);
        Map<String, Object> cursorInfo = new HashMap<>();
        for (int i=0; i < cursor.getColumnCount(); i++) {
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_NULL:
                    //Log.d(TAG, i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: null");
                    cursorInfo.put(cursor.getColumnName(i), null);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    //Log.d(TAG, i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: " + cursor.getInt(i));
                    //cursorInfo.put(cursor.getColumnName(i), cursor.getInt(i));
                    cursorInfo.put(cursor.getColumnName(i), cursor.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    //Log.d(TAG, i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: " + cursor.getFloat(i));
                    //cursorInfo.put(cursor.getColumnName(i), cursor.getFloat(i));
                    cursorInfo.put(cursor.getColumnName(i), cursor.getDouble(i));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    //Log.d(TAG, i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: " + cursor.getString(i));
                    cursorInfo.put(cursor.getColumnName(i), cursor.getString(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    //Log.d(TAG, i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: " + Arrays.toString(cursor.getBlob(i)));
                    cursorInfo.put(cursor.getColumnName(i), cursor.getBlob(i));
                    break;
                default:
                    //Log.d(TAG, i + " name: " + cursor.getColumnName(i) + " type: " + cursor.getType(i) + " value: ?");
                    cursorInfo.put(cursor.getColumnName(i), "??? - type: " + cursor.getType(i));
                    break;
            }
        }
        //Log.d(TAG, "Extras: " + cursor.getExtras().toString());
        if (cursorInfo.containsKey("[extras]")) {
            cursorInfo.put("[**extras**]", cursor.getExtras());
        } else {
            cursorInfo.put("[extras]", cursor.getExtras());
        }
        if (cursorInfo.containsKey(BaseColumns._ID) && !cursorInfo.containsKey("[content_uri]")) {
            Long cursorId = (long) cursorInfo.get(BaseColumns._ID);
            try {
                Long parseId = ContentUris.parseId(parentUri);
                if (parseId == cursorId) {
                    cursorInfo.put("[content_uri]", parentUri.toString());
                } else {
                    Log.d(TAG, cursorInfo.toString());
                }
            } catch (Exception e) {
                try {
                    Uri curUri = ContentUris.withAppendedId(parentUri, cursorId);
                    cursorInfo.put("[content_uri]", curUri.toString());
                } catch (Exception e2) {
                    Log.e(TAG, cursorInfo.toString(), e2);
                }
            }
        }
        return cursorInfo;
    }

    /**
     * Show content via content provider
     *
     * @param activity     current Activity context
     * @param contentUri   content uri to show
     * @param cleanUp      clean up content provider (for downloads)
     */
    public static void showContentFiles(AppCompatActivity activity, String contentUri, boolean cleanUp) {
        Uri uri = Uri.parse(contentUri);
        Log.d(TAG, "Uri: " + uri.toString());
        Log.d(TAG, "Authority: " + uri.getAuthority());
        Cursor cursor = activity.getContentResolver().query(uri,null,null,null,null);
        Map<String, Object> cursorInfo;
        Map<String, Long> mediaLastMods = new HashMap<>();
        Map<String, Long> mediaContentIds = new HashMap<>();
        while (cursor.moveToNext()) {
            cursorInfo = getCursorInfo(cursor, uri);
            if (cursorInfo.containsKey(DownloadManager.COLUMN_MEDIAPROVIDER_URI)) {
                //Long lastMod = (Long) cursorInfo.get(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);
                Long lastMod = (Long) cursorInfo.get("lastmod");  // Android Nougat 7.1.1 - API Level 25
                String mediaUri = (String) cursorInfo.get(DownloadManager.COLUMN_MEDIAPROVIDER_URI);
                if (mediaLastMods.containsKey(mediaUri)) {
                    Log.d(TAG, mediaUri + " duplicate " + mediaLastMods.get(mediaUri) + " != " + lastMod);
                    if (cleanUp && lastMod >= mediaLastMods.get(mediaUri)) {
                        Uri curUri = ContentUris.withAppendedId(uri, mediaContentIds.get(mediaUri));
                        /*
                        // this will actually delete (all versions of) the item from Downloads + in files!?
                        try {
                            activity.getContentResolver().delete(curUri, null, null);
                            Log.d(TAG, "Delete: " + curUri.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "Delete: " + curUri.toString(), e);
                        }
                         */
                        Log.d(TAG, "Duplicate: " + curUri);
                    }
                }
                mediaLastMods.put(mediaUri, lastMod);
                mediaContentIds.put(mediaUri, (long) cursorInfo.get(BaseColumns._ID));
            }
            if (cursorInfo.containsKey(DownloadManager.COLUMN_DESCRIPTION) && ((String) cursorInfo.get(DownloadManager.COLUMN_DESCRIPTION)).startsWith("/storage")) {
                File curFile = new File((String) cursorInfo.get(DownloadManager.COLUMN_DESCRIPTION));
                if (curFile != null && curFile.exists()) {
                    Log.d(TAG, "File: " + curFile.getAbsolutePath());
                    if (cursorInfo.containsKey(DownloadManager.COLUMN_TITLE) && !curFile.getName().equals(cursorInfo.get(DownloadManager.COLUMN_TITLE))) {
                        Log.d(TAG, "File: " + cursorInfo.get(DownloadManager.COLUMN_TITLE) + " != " + curFile.getName());
                        if (cleanUp) {
                            Uri curUri = ContentUris.withAppendedId(uri, (long) cursorInfo.get(BaseColumns._ID));
                            Log.d(TAG, "TODO: " + curUri);
                        }
                    }
                } else if (cleanUp) {
                    Log.d(TAG, "File: ?");
                    Uri curUri = ContentUris.withAppendedId(uri, (long) cursorInfo.get(BaseColumns._ID));
                    try {
                        activity.getContentResolver().delete(curUri, null, null);
                        Log.d(TAG, "Delete: " + curUri);
                    } catch (Exception e) {
                        Log.e(TAG, "Delete: " + curUri, e);
                    }
                } else {
                    Log.d(TAG, "File: ?");
                }
            } else if (cleanUp) {
                String COLUMN_NOTIFICATION_PACKAGE = "notificationpackage";
                if (cursorInfo.containsKey(COLUMN_NOTIFICATION_PACKAGE) && cursorInfo.get(COLUMN_NOTIFICATION_PACKAGE).equals(activity.getPackageName())) {
                    Uri curUri = ContentUris.withAppendedId(uri, (long) cursorInfo.get(BaseColumns._ID));
                    try {
                        activity.getContentResolver().delete(curUri, null, null);
                        Log.d(TAG, "Delete: " + curUri);
                    } catch (Exception e) {
                        Log.e(TAG, "Delete: " + curUri, e);
                    }
                } else {
                    Log.d(TAG, "Skip: " + cursorInfo.get(DownloadManager.COLUMN_TITLE));
                }
            }
            try {
                Log.d(TAG, MyJsonUtility.toJsonString(cursorInfo));
            } catch (Exception e) {
                Log.e(TAG, cursorInfo.toString(), e);
            }
        }
        Log.d(TAG, mediaLastMods.toString());
        // https://stackoverflow.com/questions/5563747/how-to-use-class-contentquerymap-to-cache-cursor-values
        /*
        ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, false, null);
        for (Map.Entry<String, ContentValues> row : mQueryMap.getRows().entrySet()) {
            Long id = Long.valueOf(row.getKey());
            Set<Map.Entry<String, Object>> data = row.getValue().valueSet();
            try {
                Log.d(TAG, MyJsonUtility.toJsonString(data));
            } catch (Exception e) {
                Log.e(TAG, data.toString(), e);
            }
        }
         */
        cursor.close();
    }

    /**
     * @param activity     current Activity context
     * @param contentUri   content uri to get items for
     * @return content items
     */
    public static List<Map<String, Object>> getContentItems(AppCompatActivity activity, Uri contentUri) {
        List<Map<String, Object>> contentItems = new ArrayList<>();
        Cursor cursor = activity.getContentResolver().query(contentUri,null,null,null,null);
        if (cursor == null) {
            return contentItems;
        }
        int numItems = cursor.getCount();
        Map<String, Object> cursorInfo;
        while (cursor.moveToNext()) {
            cursorInfo = getCursorInfo(cursor, contentUri);
            if (!cursorInfo.containsKey("[content_uri]") && numItems == 1) {
                cursorInfo.put("[content_uri]", contentUri.toString());
            }
            try {
                Log.d(TAG, MyJsonUtility.toJsonString(cursorInfo));
            } catch (Exception e) {
                Log.e(TAG, cursorInfo.toString(), e);
            }
            contentItems.add(cursorInfo);
        }
        cursor.close();
        return contentItems;
    }
}
