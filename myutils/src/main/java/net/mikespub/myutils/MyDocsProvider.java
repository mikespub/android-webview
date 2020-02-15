package net.mikespub.myutils;

import android.content.Context;
import android.content.IntentSender;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;

// See https://developer.android.com/guide/topics/providers/create-document-provider#queryRoots
// and https://github.com/android/storage-samples/blob/master/StorageProvider/Application/src/main/java/com/example/android/storageprovider/MyCloudProvider.java
public class MyDocsProvider extends DocumentsProvider {
    private static final String TAG = "MyDocsProvider";

    public MyDocsProvider() {
        super();
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "onCreate");
        //mBaseDir = getContext().getFilesDir();
        //writeDummyFilesToStorage();
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        return super.isChildDocument(parentDocumentId, documentId);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        return super.createDocument(parentDocumentId, mimeType, displayName);
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        return super.renameDocument(documentId, displayName);
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        super.deleteDocument(documentId);
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        return super.copyDocument(sourceDocumentId, targetParentDocumentId);
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        return super.moveDocument(sourceDocumentId, sourceParentDocumentId, targetParentDocumentId);
    }

    @Override
    public void removeDocument(String documentId, String parentDocumentId) throws FileNotFoundException {
        super.removeDocument(documentId, parentDocumentId);
    }

    @Override
    public DocumentsContract.Path findDocumentPath(@Nullable String parentDocumentId, String childDocumentId) throws FileNotFoundException {
        return super.findDocumentPath(parentDocumentId, childDocumentId);
    }

    @Override
    public IntentSender createWebLinkIntent(String documentId, @Nullable Bundle options) throws FileNotFoundException {
        return super.createWebLinkIntent(documentId, options);
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        return null;
    }

    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection) throws FileNotFoundException {
        return super.queryRecentDocuments(rootId, projection);
    }

    @Nullable
    @Override
    public Cursor queryRecentDocuments(@NonNull String rootId, @Nullable String[] projection, @Nullable Bundle queryArgs, @Nullable CancellationSignal signal) throws FileNotFoundException {
        return super.queryRecentDocuments(rootId, projection, queryArgs, signal);
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        return null;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        return null;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, @Nullable String[] projection, @Nullable Bundle queryArgs) throws FileNotFoundException {
        return super.queryChildDocuments(parentDocumentId, projection, queryArgs);
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
        return super.querySearchDocuments(rootId, query, projection);
    }

    @Nullable
    @Override
    public Cursor querySearchDocuments(@NonNull String rootId, @Nullable String[] projection, @NonNull Bundle queryArgs) throws FileNotFoundException {
        return super.querySearchDocuments(rootId, projection, queryArgs);
    }

    @Override
    public void ejectRoot(String rootId) {
        super.ejectRoot(rootId);
    }

    @Nullable
    @Override
    public Bundle getDocumentMetadata(@NonNull String documentId) throws FileNotFoundException {
        return super.getDocumentMetadata(documentId);
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        return super.getDocumentType(documentId);
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        return null;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        return super.openDocumentThumbnail(documentId, sizeHint, signal);
    }

    @Override
    public AssetFileDescriptor openTypedDocument(String documentId, String mimeTypeFilter, Bundle opts, CancellationSignal signal) throws FileNotFoundException {
        return super.openTypedDocument(documentId, mimeTypeFilter, opts, signal);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        return super.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    @Override
    public Uri canonicalize(Uri uri) {
        return super.canonicalize(uri);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        return super.call(method, arg, extras);
    }

    @Override
    public String[] getDocumentStreamTypes(String documentId, String mimeTypeFilter) {
        return super.getDocumentStreamTypes(documentId, mimeTypeFilter);
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        return super.getStreamTypes(uri, mimeTypeFilter);
    }
}
