package net.mikespub.myutils;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

// See https://github.com/yasirkula/UnityNativeShare/blob/master/JAR%20Source/NativeShareContentProvider.java
public class MyFileProvider extends FileProvider {
    private static final String META_DATA_FILE_PROVIDER_PATHS = "android.support.FILE_PROVIDER_PATHS";
    private static final String TAG_EXTERNAL_FILES = "external-files-path";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PATH = "path";
    private static final String TAG = "FileProvider";
    public static HashMap<String, File> mRoots = new HashMap<String, File>();

    public MyFileProvider() {
        super();
    }

    @Override
    public boolean onCreate() {
        return super.onCreate();
    }

    @Override
    public void attachInfo(@NonNull Context context, @NonNull ProviderInfo info) {
        super.attachInfo(context, info);
        // See also https://stackoverflow.com/questions/586363/why-is-super-super-method-not-allowed-in-java
        //info.exported = true;
        //super.super.attachInfo(context, info);
    }

    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return super.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return super.getType(uri);
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return super.insert(uri, values);
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return super.update(uri, values, selection, selectionArgs);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return super.delete(uri, selection, selectionArgs);
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        return super.openFile(uri, mode);
    }

    /*
    public static Uri getUriForFile(@NonNull Context context, @NonNull String authority,
                                    @NonNull File file) {
        final PathStrategy strategy = getPathStrategy(context, authority);
        return strategy.getUriForFile(file);
    }
     */

    /*
    // https://android.googlesource.com/platform/frameworks/support/+/dcebe5a/v4/java/android/support/v4/content/FileProvider.java
     */
    public static File getFileForUri(@NonNull Context context, @NonNull String authority,
                              @NonNull Uri uri) {
        //final PathStrategy strategy = getPathStrategy(context, authority);
        //return strategy.getFileForUri(uri);
        String path = uri.getEncodedPath();
        final int splitIndex = path.indexOf('/', 1);
        final String tag = Uri.decode(path.substring(1, splitIndex));
        path = Uri.decode(path.substring(splitIndex + 1));
        final File root = getRoots(context, authority).get(tag);
        if (root == null) {
            throw new IllegalArgumentException("Unable to find configured root for " + uri);
        }
        File file = new File(root, path);
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
        }
        if (!file.getPath().startsWith(root.getPath())) {
            throw new SecurityException("Resolved path jumped beyond configured root");
        }
        return file;
    }

    // TODO: save roots by authority
    public static HashMap<String, File> getRoots(Context context, String authority) {
        if (mRoots.size() < 1) {
            try {
                addRoots(context, authority);
            } catch (Exception e) {
                Log.e(TAG, "addRoots", e);
                mRoots.put("root", context.getExternalFilesDir(null));
            }
        }
        return mRoots;
    }

    public static void addRoots(Context context, String authority) throws IOException, XmlPullParserException {
        //XmlResourceParser in = context.getResources().getXml(xmlId);  // R.xml.filepaths in app manifest [net.mikespub.mywebview]
        final ProviderInfo info = context.getPackageManager()
                .resolveContentProvider(authority, PackageManager.GET_META_DATA);
        final XmlResourceParser in = info.loadXmlMetaData(
                context.getPackageManager(), META_DATA_FILE_PROVIDER_PATHS);
        int type;
        while ((type = in.next()) != END_DOCUMENT) {
            if (type == START_TAG) {
                final String tag = in.getName();
                final String name = in.getAttributeValue(null, ATTR_NAME);
                String path = in.getAttributeValue(null, ATTR_PATH);
                Log.d(TAG, "Tag " + tag + " " + name + " " + path);
                if (name == null || path == null) {
                    continue;
                }
                if (tag.equals(TAG_EXTERNAL_FILES)) {
                    File file = new File(context.getExternalFilesDir(null), path).getCanonicalFile();
                    mRoots.put(name, file);
                    Log.d(TAG, "Adding root '" + name + "' = " + file.getAbsolutePath());
                }
            }
        }
    }
}
