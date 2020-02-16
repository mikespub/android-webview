package net.mikespub.myutils;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.FileNotFoundException;

// See https://github.com/yasirkula/UnityNativeShare/blob/master/JAR%20Source/NativeShareContentProvider.java
public class MyFileProvider extends FileProvider {
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

}
