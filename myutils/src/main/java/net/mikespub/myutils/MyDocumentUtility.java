package net.mikespub.myutils;

import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Document Utility Methods
 */
public class MyDocumentUtility {
    private static final String TAG = "Document";

    // Downloads = content://com.android.providers.downloads.documents/tree/downloads
    // Virtual SD Card = content://com.android.externalstorage.documents/tree/primary%3A
    // Downloads via SD Card = content://com.android.externalstorage.documents/tree/primary%3ADownload

    public static void savePermissions(AppCompatActivity activity, Uri returnUri, int takeFlags) {
        if (checkTreeUri(returnUri)) {
            //        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // activity.getContentResolver().takePersistableUriPermission(returnUri, takeFlags);
            if ((takeFlags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0) {
                activity.getContentResolver().takePersistableUriPermission(returnUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                activity.getContentResolver().takePersistableUriPermission(returnUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        } else {
            Log.d(TAG, "Not a tree Uri: " + returnUri);
        }
    }

    public static void showPermissions(AppCompatActivity activity) {
        /*
        Permissions IN: UriPermission {uri=content://com.android.providers.downloads.documents/tree/downloads, modeFlags=3, persistedTime=1581805900986}
        Permissions IN: UriPermission {uri=content://com.android.externalstorage.documents/tree/primary%3ADownload, modeFlags=3, persistedTime=1581806457493}
        Permissions IN: UriPermission {uri=content://com.android.externalstorage.documents/tree/primary%3A, modeFlags=3, persistedTime=1581806397717}
        Permissions IN: UriPermission {uri=content://com.android.externalstorage.documents/tree/primary%3APictures, modeFlags=3, persistedTime=1581808051284}
         */
        List<UriPermission> inPerms = activity.getContentResolver().getPersistedUriPermissions();
        for (UriPermission perm: inPerms) {
            Log.d(TAG,"Permissions IN: " + perm.toString());
        }
        List<UriPermission> outPerms = activity.getContentResolver().getOutgoingPersistedUriPermissions();
        for (UriPermission perm: outPerms) {
            Log.d(TAG, "Permissions OUT: " + perm.toString());
        }
    }

    public static List<Uri> getPermittedTreeUris(AppCompatActivity activity) {
        List<Uri> treeUris = new ArrayList<>();
        List<UriPermission> inPerms = activity.getContentResolver().getPersistedUriPermissions();
        for (UriPermission perm: inPerms) {
            Uri treeUri = perm.getUri();
            if (checkTreeUri(treeUri)) {
                treeUris.add(treeUri);
            } else {
                Log.d(TAG, "Not a tree Uri: " + treeUri);
            }
        }
        return treeUris;
    }

    public static boolean checkTreeUri(Uri treeUri) {
        if (Build.VERSION.SDK_INT >= 24) {
            return DocumentsContract.isTreeUri(treeUri);
        } else if (Build.VERSION.SDK_INT >= 21) {
            try {
                String treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
                return !treeDocumentId.isEmpty();
            } catch (Exception e) {
                return false;
            }
        } else return treeUri.getPath().startsWith("/tree/");
    }

    public static void showTreeFiles(AppCompatActivity activity, Uri treeUri) {
        List<Map<String, Object>> treeItems = getTreeContentItems(activity, treeUri);
        for (Map<String, Object> cursorInfo: treeItems) {
            try {
                Log.d(TAG, MyJsonUtility.toJsonString(cursorInfo));
            } catch (Exception e) {
                Log.e(TAG, cursorInfo.toString(), e);
            }
        }
        List<DocumentFile> treeFiles = getTreeDocumentFiles(activity, treeUri);
        for (DocumentFile file: treeFiles) {
            Uri fileUri = file.getUri();
            String fileId = DocumentsContract.getDocumentId(fileUri);
            //if (Build.VERSION.SDK_INT >= 21) {
            //    fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, fileId);
            //}
            Log.d(TAG, "File: " + file.getName() + " id: " + fileId + " size: " + file.length() + " uri: " + fileUri + " doc: " + file.toString());
        }
    }

    public static List<Map<String, Object>> getTreeContentItems(AppCompatActivity activity, Uri treeUri) {
        if (Build.VERSION.SDK_INT >= 21) {
            String treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
            Log.d(TAG, "Tree Id: " + treeDocumentId + " Uri: " + treeUri.toString());
            Uri documentUri;
            Uri childrenUri;
            try {
                String documentId = DocumentsContract.getDocumentId(treeUri);
                Log.d(TAG, "Document Id: " + documentId);
                //documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, Uri.encode(documentId, ":"));
                //childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, Uri.encode(documentId, ":"));
                documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
                childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
            } catch (Exception e) {
                Log.d(TAG, "Document Id: null");
                documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId);
                childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId);
            }
            Log.d(TAG, "Document Uri: " + documentUri.toString());
            MyContentUtility.showContent(activity, documentUri);
            Log.d(TAG, "Children Uri: " + childrenUri.toString());
            List<Map<String, Object>> treeItems = MyContentUtility.getContentItems(activity, childrenUri);
            for (Map<String, Object> cursorInfo: treeItems) {
                if (cursorInfo.containsKey(DocumentsContract.Document.COLUMN_DOCUMENT_ID)) {
                    String childId = (String) cursorInfo.get(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                    cursorInfo.put("[document_uri]", DocumentsContract.buildDocumentUriUsingTree(treeUri, childId));
                }
            }
            return treeItems;
        } else {
            Log.d(TAG, "Tree Uri: " + treeUri.toString());
            return null;
        }
    }

    public static void showDocument(AppCompatActivity activity, Uri uri) {
        Map<String, Object> cursorInfo = getTreeContent(activity, uri);
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

    public static Map<String, Object> getTreeContent(AppCompatActivity activity, Uri contentUri) {
        Map<String, Object> cursorInfo = MyContentUtility.getContent(activity, contentUri);
        if (cursorInfo != null) {
            if (cursorInfo.containsKey(DocumentsContract.Document.COLUMN_MIME_TYPE)) {
                String type = (String) cursorInfo.get(DocumentsContract.Document.COLUMN_MIME_TYPE);
                if (type != null && type.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                    //MyDocumentUtility.showTreeFiles(activity, contentUri);
                    try {
                        cursorInfo.put("[children]", getTreeContentItems(activity, contentUri));
                    } catch (Exception e) {
                        Log.e(TAG, "Children: " + contentUri.toString(), e);
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                Uri mediaUri = MediaStore.getMediaUri(activity, contentUri);
                Log.d(TAG, "Media Uri: " + mediaUri.toString());
                cursorInfo.put("[media_uri]", mediaUri.toString());
            } catch (Exception e) {
                Log.d(TAG, "No equivalent Media Uri");
            }
        }
        return cursorInfo;
    }

    public static List<DocumentFile> getTreeDocumentFiles(AppCompatActivity activity, Uri treeUri) {
        // https://developer.android.com/reference/androidx/documentfile/provider/DocumentFile?hl=en
        DocumentFile treeDir = DocumentFile.fromTreeUri(activity, treeUri);
        List<DocumentFile> treeFiles = new ArrayList<>();
        // List all existing files inside picked directory
        for (DocumentFile file : treeDir.listFiles()) {
            // do something extra with file?
            treeFiles.add(file);
        }
        return treeFiles;
    }
}
