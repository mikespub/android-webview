package net.mikespub.myutils;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Map;

/**
 * Download Utility Methods
 */
public class MyDownloadUtility {
    private static final String TAG = "Download";

    /*
    // From https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/provider/Downloads.java
    String COLUMN_NOTIFICATION_PACKAGE = "notificationpackage";
    String COLUMN_NOTIFICATION_CLASS = "notificationclass";
    Uri CONTENT_URI =
            Uri.parse("content://downloads/my_downloads");
    String QUERY_WHERE_CLAUSE = Impl.COLUMN_NOTIFICATION_PACKAGE + "=? AND "
        + Impl.COLUMN_NOTIFICATION_CLASS + "=?";

    public static final void removeAllDownloadsByPackage(
        Context context, String notification_package, String notification_class) {
        context.getContentResolver().delete(Impl.CONTENT_URI, QUERY_WHERE_CLAUSE,
                new String[] { notification_package, notification_class });
    }
     */

    /**
     * Show app downloads via content provider
     *
     * @param activity  current Activity context
     * @param cleanUp   clean up download content provider
     */
    public static void showMyDownloadFiles(AppCompatActivity activity, boolean cleanUp) {
        //static android.provider.Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI;
        //ALL_DOWNLOADS_CONTENT_URI
        //Uri uri = Uri.parse("content://downloads/my_downloads");
        //Uri uri = Uri.parse("content://downloads/public_downloads");
        //Uri uri = Uri.parse("content://downloads/all_downloads");
        //activity.grantUriPermission(activity.getPackageName(),uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // MediaStore.Downloads.EXTERNAL_CONTENT_URI
        // Also see https://android.googlesource.com/platform/packages/providers/DownloadProvider.git/+/refs/heads/master/src/com/android/providers/downloads/DownloadProvider.java
        MyContentUtility.showContentFiles(activity, "content://downloads/my_downloads", cleanUp);
    }

    public static List<Map<String, Object>> getMyDownloadFiles(AppCompatActivity activity) {
        return MyContentUtility.getContentItems(activity, Uri.parse("content://downloads/my_downloads"));
    }

    /**
     * Get DownloadManager service
     *
     * @param activity  current Activity context
     * @return          download manager
     */
    static DownloadManager getDownloadManager(AppCompatActivity activity) {
        return (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    /**
     * Get download status for a download id
     *
     * @param activity      current Activity context
     * @param downloadId    download id
     * @return              current status
     */
    public static int getDownloadStatus(AppCompatActivity activity, int downloadId) {
        DownloadManager mDownloadManager = getDownloadManager(activity);
        // query download status
        Cursor cursor = mDownloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
        if (cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if(status == DownloadManager.STATUS_SUCCESSFUL){
                // download is successful
                //String uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                //File file = new File(Uri.parse(uri).getPath());
                Uri uri = mDownloadManager.getUriForDownloadedFile(downloadId);
                if (uri != null) {
                    Log.d(TAG, "URI: " + uri);
                    //InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                }
            }
            else {
                // download is assumed cancelled
            }
            return status;
        }
        else {
            // download is assumed cancelled
            return DownloadManager.STATUS_FAILED;
        }
    }

}
