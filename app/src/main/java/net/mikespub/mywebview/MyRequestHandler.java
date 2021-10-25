package net.mikespub.mywebview;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.webkit.WebViewAssetLoader;

import net.mikespub.myutils.MyAssetUtility;
import net.mikespub.myutils.MyContentUtility;
import net.mikespub.myutils.MyDocumentUtility;
import net.mikespub.myutils.MyDownloadUtility;
import net.mikespub.myutils.MyJsonUtility;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * See also old stuff at https://android.googlesource.com/platform/packages/apps/Browser/+/refs/heads/master/src/com/android/browser/homepages/RequestHandler.java
 */
class MyRequestHandler {
    private static final String TAG = "Request";
    static final String[] linkNames = {"sites", "files", "web", "local", "root", "content", "media", "document", "refresh"};
    static final String[] mediaNames = {"files", "images", "audio", "video", "downloads"};
    static final String[] intentNames = {"intent", "view", "send", "pick", "get", "open", "tree", "create", "edit", "delete"};
    static final int REQUEST_PICK = 1;
    static final int REQUEST_GET = 2;
    static final int REQUEST_OPEN = 3;
    static final int REQUEST_TREE = 4;
    static final int REQUEST_CREATE = 5;
    private final MyAppWebViewClient webViewClient;
    private final MainActivity activity;
    private final String domainUrl;
    private final String providerAuthority;
    private WebViewAssetLoader assetLoader;

    MyRequestHandler(MyAppWebViewClient webViewClient) {
        this.webViewClient = webViewClient;
        this.activity = webViewClient.activity;
        this.domainUrl = webViewClient.domainUrl;
        PackageManager pm = activity.getPackageManager();
        String authority = null;
        try {
            PackageInfo appInfo = pm.getPackageInfo(activity.getPackageName(), PackageManager.GET_PROVIDERS);
            if (appInfo.providers != null && appInfo.providers.length > 0) {
                authority = appInfo.providers[0].authority;
            }
            //Log.d("Package", Arrays.toString(appInfo.providers));
        } catch (Exception e) {
            Log.e(TAG, "Package Name not found", e);
        }
        if (authority == null) {
            authority = activity.getPackageName() + ".fileprovider";
        }
        this.providerAuthority = authority;
        Log.d(TAG, "Provider Authority: " + authority);
    }

    /**
     * Intercept a particular request and handle it ourselves
     *
     * @param view  current WebView context
     * @param uri   uri to handle
     * @return      WebResourceResponse or null
     */
    // https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
    WebResourceResponse handleRequest(WebView view, Uri uri) {
        //String path = uri.getPath();
        String path = uri.getEncodedPath();
        Log.d(TAG, "Path: " + path);
        // ByteArrayInputStream str = new ByteArrayInputStream(message.getBytes());
        // return new WebResourceResponse("text/plain", "utf-8", str);
        // InputStream localStream = assetMgr.open(path);
        // return new WebResourceResponse((url.contains(".js") ? "text/javascript" : "text/css"), "UTF-8", localStream);
        // Note: we could also have used the Javascript interface, but then this might be available for all sites
        // See also UriMatcher https://developer.android.com/reference/android/content/UriMatcher.html
        if (path.equals("/assets/web/fake_post.jsp")) {
            return handleUpdateSettings(uri);
        } else if (path.equals("/assets/local/get_config.jsp")) {
            return handleGetLocalConfig(uri);
        } else if (path.equals("/assets/local/download.jsp")) {
            return handleDownloadBundle(uri);
        } else if (path.equals("/assets/local/extract.jsp")) {
            return handleExtractBundle(uri);
        } else if (path.equals("/assets/local/delete.jsp")) {
            return handleDeleteBundle(uri);
        } else if (path.equals("/assets/local/cleanup.jsp")) {
            return handleCleanUpDownloads(uri);
        } else if (path.equals("/assets/local/404.jsp")) {
            return handleFileNotFound(uri);
        } else if (path.startsWith("/assets/")) {
            return handleAssetFileRequest(uri);
        } else if (webViewClient.hasLocalSites() && path.startsWith("/sites/")) {
        //} else if (path.startsWith("/sites/")) {
            // handle local sites if not already under /assets/...
            return handleLocalSiteRequest(uri);
        } else if (path.startsWith("/content/")) {
            return handleContentRequest(uri);
        } else if (path.startsWith("/media/")) {
            return handleMediaRequest(uri);
        } else if (path.startsWith("/document/")) {
            return handleTreeRequest(uri);
        } else if (path.startsWith("/refresh/")) {
            MyLocalConfigRepository.refreshDemoSite(activity);
            return handleLocalSiteRequest(Uri.parse(domainUrl + "sites/demo/test.html"));
        } else if (getIntentPrefixFromUri(uri) != null) {
            // should be handled in shouldOverrideUrlLoading already or not?
            return handleIntentRequest(view, uri);
        }
        if (this.assetLoader == null) {
            this.assetLoader = new WebViewAssetLoader.Builder()
                    //.setDomain(domainName)
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(activity))
                    //.addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(activity))
                    .build();
        }
        return this.assetLoader.shouldInterceptRequest(uri);
    }
    WebResourceResponse handleAssetFileRequest(Uri uri) {
        String fileName = uri.getPath().substring("/assets/".length());
        return handleFileRequest(null, fileName);
    }

    WebResourceResponse handleLocalSiteRequest(Uri uri) {
        String fileName = uri.getPath().substring("/sites/".length());
        if (fileName.isEmpty()) {
            // show sites index
            return handleFileRequest(null, "local/index.html");
        }
        return handleFileRequest(Environment.DIRECTORY_DOCUMENTS, fileName);
    }

    WebResourceResponse handleFileRequest(@Nullable String dirName, @NonNull String fileName) {
        File extFile = new File(activity.getExternalFilesDir(dirName), fileName);
        if (fileName.endsWith("/") && extFile.exists() && extFile.isDirectory()) {
            Log.d(TAG, "File Request " + extFile + " is directory - trying with index.html");
            if (fileName.startsWith("local/")) {
                fileName += "sites.html";
            } else {
                fileName += "index.html";
            }
            extFile = new File(activity.getExternalFilesDir(dirName), fileName);
        }
        if (!extFile.exists() || extFile.isDirectory()) {
            Log.d(TAG, "File Request " + extFile + " exists: " + extFile.exists());
            return handleFileNotFound(Uri.parse(fileName));
        }
        String type = getMimeType(extFile.getName());
        if (type.equals("TODO")) {
            Log.d(TAG, "File Request " + extFile + " type: " + type);
            return handleFileNotFound(Uri.parse(fileName));
        }
        try {
            InputStream targetStream = new FileInputStream(extFile);
            Log.d(TAG, "File Request " + extFile + " mimetype: " + type);
            if (type.startsWith("image/") || type.startsWith("font/")) {
                return new WebResourceResponse(type, null, targetStream);
            } else {
                return new WebResourceResponse(type, "UTF-8", targetStream);
            }
        } catch (Exception e) {
            Log.e(TAG, "File Request " + extFile.getAbsolutePath(), e);
        }
        return handleFileNotFound(Uri.parse(fileName));
    }

    boolean checkExternalStoragePermission() {
        // See https://developer.android.com/training/permissions/requesting
        // https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
        if (ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission: Requesting read access to external storage");
            // Permission is not granted
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
            return false;
        } else {
            return true;
        }
    }

    WebResourceResponse createResultResponse(String templateName, String output) {
        // use template file for response here
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("output", output);
        String message = MyAssetUtility.getTemplateFile(activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    WebResourceResponse handleContentRequest(Uri uri) {
        String contentName = uri.getPath().substring("/content/".length());
        if (contentName.isEmpty()) {
            // show demo test page
            return handleFileRequest(Environment.DIRECTORY_DOCUMENTS, "demo/test.html");
        }
        Uri contentUri = Uri.parse("content://" + contentName);
        List<Map<String, Object>> contentItems;
        try {
            contentItems = MyContentUtility.getContentItems(activity, contentUri);
        } catch (Exception e) {
            Log.e(TAG, "Content Uri: " + contentUri.toString(), e);
            // use template file for response here
            return createResultResponse("local/result.html", "Uri: " + contentUri.toString() + "\nException: " + e.toString());
        }
        String output;
        if (contentItems != null) {
            try {
                output = MyJsonUtility.toJsonString(contentItems);
            } catch (Exception e) {
                output = contentItems.toString();
            }
        } else {
            output = "No Content Found: " + contentName.replace("<", "&lt;");
        }
        // use template file for response here
        return createResultResponse("local/result.html", output);
    }

    // files/47 - /media/ part is already removed here
    // or images/media/49
    Uri getMediaUriFromName(String mediaName) {
        //https://stackoverflow.com/questions/1128723/how-do-i-determine-whether-an-array-contains-a-particular-value-in-java
        final List<String> mediaList = Arrays.asList(mediaNames);
        String[] parts = mediaName.split("/", 3);
        if (!mediaList.contains(parts[0])) {
            Log.d(TAG, "Media Name: " + mediaName + " Parts: " + Arrays.toString(parts));
            return null;
        }
        Uri mediaUri;
        switch (parts[0]) {
            case "files":
                mediaUri = MediaStore.Files.getContentUri("external");
                break;
            case "images":
                mediaUri = MediaStore.Images.Media.getContentUri("external");
                break;
            case "audio":
                mediaUri = MediaStore.Audio.Media.getContentUri("external");
                break;
            case "video":
                mediaUri = MediaStore.Video.Media.getContentUri("external");
                break;
            case "downloads":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mediaUri = MediaStore.Downloads.getContentUri("external");
                } else {
                    mediaUri = null;
                }
                break;
            default:
                mediaUri = null;
        }
        if (mediaUri != null && parts.length > 1 && !parts[parts.length - 1].isEmpty()) {
            // handle images/media/49 too
            /*
            if (parts.length > 2) {
                Uri.Builder builder = mediaUri.buildUpon();
                for (int i = 1; i < parts.length - 1; i++) {
                    String part = parts[i];
                    builder.appendPath(part);
                }
                mediaUri = builder.build();
            }
             */
            mediaUri = ContentUris.withAppendedId(mediaUri, Long.valueOf(parts[parts.length - 1]));
        }
        Log.d(TAG, "Media Name: " + mediaName + " Parts: " + Arrays.toString(parts) + " Uri: " + mediaUri);
        return mediaUri;
    }

    WebResourceResponse handleMediaRequest(Uri uri) {
        String mediaName = uri.getPath().substring("/media/".length());
        if (mediaName.isEmpty()) {
            // requires android.permission.READ_EXTERNAL_STORAGE, or grantUriPermission()
            // See https://developer.android.com/training/permissions/requesting
            // https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
            // use template file for response here
            StringBuilder builder = new StringBuilder();
            builder.append("<ul>");
            for (String name: mediaNames) {
                builder.append("<li><a href=\"/media/" + name + "/\">" + name + "</a></li>");
            }
            builder.append("</ul>");
            if (!checkExternalStoragePermission()) {
                builder.append("No Permission");
            } else {
                builder.append("OK Permission");
            }
            //Uri contentUri = MediaStore.Files.getContentUri("external");
            //Log.d(TAG, "Media Uri: " + contentUri.toString());
            //MyContentUtility.showContentFiles(activity, contentUri.toString(), false);
            // show demo test page
            //return handleFileRequest(Environment.DIRECTORY_DOCUMENTS, "demo/test.html");
            return createResultResponse("local/result.html", builder.toString());
        }
        Uri mediaUri = getMediaUriFromName(mediaName);
        if (mediaUri == null) {
            return createResultResponse("local/result.html", "No Media Uri for: " + mediaName.replace("<", "&lt;"));
        }
        // what's the right Uri for media?
        //Uri contentUri = Uri.parse("content://" + mediaName);
        //Uri contentUri = MediaStore.getDocumentUri(activity, mediaUri); // API level 29
        List<Map<String, Object>> contentItems;
        try {
            contentItems = MyContentUtility.getContentItems(activity, mediaUri);
        } catch (Exception e) {
            Log.e(TAG, "Media Uri: " + mediaUri.toString(), e);
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    // if we already opened this media via OPEN_DOCUMENT before...
                    Uri documentUri = MediaStore.getDocumentUri(activity, mediaUri);
                    Log.d(TAG, "Document Uri: " + documentUri.toString());
                    contentItems = MyContentUtility.getContentItems(activity, documentUri);
                    String output;
                    if (contentItems != null) {
                        try {
                            output = MyJsonUtility.toJsonString(contentItems);
                        } catch (Exception g) {
                            Log.e(TAG, "Document Uri: " + documentUri.toString(), g);
                            output = contentItems.toString();
                        }
                    } else {
                        output = "No Media Found: " + mediaName.replace("<", "&lt;") + "\nMedia Uri: " + mediaUri.toString() + "\nDocument Uri: " + documentUri.toString();
                    }
                    // use template file for response here
                    return createResultResponse("local/result.html", output);
                } catch (Exception f) {
                    Log.e(TAG, "Document Uri Failed", f);
                }
            }
            // use template file for response here
            return createResultResponse("local/result.html", "Uri: " + mediaUri.toString() + "\nException: " + e.toString());
        }
        String output;
        if (contentItems != null) {
            try {
                output = MyJsonUtility.toJsonString(contentItems);
            } catch (Exception e) {
                output = contentItems.toString();
            }
        } else {
            output = "No Media Found: " + mediaName.replace("<", "&lt;");
        }
        // use template file for response here
        return createResultResponse("local/result.html", output);
    }

    WebResourceResponse handleTreeRequest(Uri uri) {
        //String treeName = uri.getPath().substring("/document/".length());
        String treeName = uri.getEncodedPath().substring("/document/".length());
        if (treeName.isEmpty()) {
            // use template file for response here
            List<Uri> treeUris = MyDocumentUtility.getPermittedTreeUris(activity);
            StringBuilder builder = new StringBuilder();
            builder.append("Available Trees:");
            builder.append("<ul>");
            for (Uri treeUri: treeUris) {
                builder.append("<li><a href=\"/document/" + treeUri.getEncodedAuthority() + treeUri.getEncodedPath() + "\">" + treeUri.getLastPathSegment() + "</a> [" + treeUri.getAuthority() + "]</li>");
            }
            builder.append("</ul>");
            return createResultResponse("local/result.html", builder.toString());
        }
        Uri contentUri = Uri.parse("content://" + treeName);
        if (DocumentsContract.isDocumentUri(activity, contentUri)) {
            Log.d(TAG, "Document Uri: " + contentUri.toString());
        } else if (MyDocumentUtility.checkTreeUri(contentUri)) {
            Log.d(TAG, "Tree Uri: " + contentUri.toString());
            StringBuilder builder = new StringBuilder();
            builder.append("Tree: " + contentUri.getLastPathSegment() + "\n");
            builder.append("Authority: " + contentUri.getAuthority() + "\n");
            builder.append("Uri: " + contentUri.toString() + "\n");
            builder.append("Available Documents:");
            builder.append("<ul>");
            if (Build.VERSION.SDK_INT >= 21) {
                //MyDocumentUtility.showTreeFiles(activity, contentUri);
                List<Map<String, Object>> treeItems = MyDocumentUtility.getTreeContentItems(activity, contentUri);
                for (Map<String, Object> childInfo: treeItems) {
                    Uri fileUri = (Uri) childInfo.get("[uri]");
                    String fileId = DocumentsContract.getDocumentId(fileUri);
                    String fileName = (String) childInfo.get(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                    if (Build.VERSION.SDK_INT >= 21) {
                        //Uri newUri = DocumentsContract.buildDocumentUriUsingTree(contentUri, fileId);
                        Uri newUri = fileUri;
                        builder.append("<li><a href=\"/document/" + newUri.getEncodedAuthority() + newUri.getEncodedPath() + "\">" + fileName + "</a>: " + fileId + " New: " + newUri.toString() + "</li>");
                    } else {
                        builder.append("<li><a href=\"/document/" + fileUri.getEncodedAuthority() + fileUri.getEncodedPath() + "\">" + fileName + "</a>: " + fileId + " Old: " + fileUri.toString() + "</li>");
                    }
                }
            } else {
                List<DocumentFile> treeFiles = MyDocumentUtility.getTreeDocumentFiles(activity, contentUri);
                for (DocumentFile file: treeFiles) {
                    Uri fileUri = file.getUri();
                    String fileId = DocumentsContract.getDocumentId(fileUri);
                    if (Build.VERSION.SDK_INT >= 21) {
                        //Uri newUri = DocumentsContract.buildDocumentUriUsingTree(contentUri, fileId);
                        Uri newUri = fileUri;
                        builder.append("<li><a href=\"/document/" + newUri.getEncodedAuthority() + newUri.getEncodedPath() + "\">" + file.getName() + "</a>: " + fileId + " New: " + newUri.toString() + "</li>");
                    } else {
                        builder.append("<li><a href=\"/document/" + fileUri.getEncodedAuthority() + fileUri.getEncodedPath() + "\">" + file.getName() + "</a>: " + fileId + " Old: " + fileUri.toString() + "</li>");
                    }
                }
            }
            builder.append("</ul>");
            return createResultResponse("local/result.html", builder.toString());
        } else {
            Log.d(TAG, "Other Uri: " + contentUri.toString());
        }
        /*
        List<Map<String, Object>> contentItems;
        try {
            contentItems = MyContentUtility.getContentItems(activity, contentUri);
        } catch (Exception e) {
            Log.e(TAG, "Content Uri: " + contentUri.toString(), e);
            // use template file for response here
            return createResultResponse("local/result.html", "Uri: " + contentUri.toString() + "\nException: " + e.toString());
        }
        String output;
        if (contentItems != null) {
            try {
                output = MyJsonUtility.toJsonString(contentItems);
            } catch (Exception e) {
                output = contentItems.toString();
            }
        } else {
            output = "No Tree Found: " + treeName.replace("<", "&lt;");
        }
         */
        Map<String, Object> cursorInfo = MyDocumentUtility.getTreeContent(activity, contentUri);
        String output;
        if (cursorInfo != null) {
            if (cursorInfo.containsKey("[children]")) {
                // list children instead of content
                StringBuilder builder = new StringBuilder();
                builder.append("Dir: " + contentUri.getLastPathSegment() + "\n");
                builder.append("Authority: " + contentUri.getAuthority() + "\n");
                builder.append("Uri: " + contentUri.toString() + "\n");
                builder.append("Available Documents:");
                builder.append("<ul>");
                for (Map<String, Object> childInfo: (List<Map<String, Object>>) cursorInfo.get("[children]")) {
                    Uri fileUri = (Uri) childInfo.get("[uri]");
                    String fileId = DocumentsContract.getDocumentId(fileUri);
                    String fileName = (String) childInfo.get(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                    if (Build.VERSION.SDK_INT >= 21) {
                        //Uri newUri = DocumentsContract.buildDocumentUriUsingTree(contentUri, fileId);
                        Uri newUri = fileUri;
                        builder.append("<li><a href=\"/document/" + newUri.getEncodedAuthority() + newUri.getEncodedPath() + "\">" + fileName + "</a>: " + fileId + " New: " + newUri.toString() + "</li>");
                    } else {
                        builder.append("<li><a href=\"/document/" + fileUri.getEncodedAuthority() + fileUri.getEncodedPath() + "\">" + fileName + "</a>: " + fileId + " Old: " + fileUri.toString() + "</li>");
                    }
                }
                builder.append("</ul>");
                try {
                    builder.append(MyJsonUtility.toJsonString(cursorInfo));
                } catch (Exception e) {
                    builder.append(cursorInfo.toString());
                }
                output = builder.toString();
            } else {
                try {
                    output = MyJsonUtility.toJsonString(cursorInfo);
                } catch (Exception e) {
                    output = cursorInfo.toString();
                }
            }
        } else {
            output = "No Document Found: " + contentUri.toString();
        }
        // use template file for response here
        return createResultResponse("local/result.html", output);
    }

    WebResourceResponse handleIntentRequest(WebView view, Uri uri) {
        Boolean isHandled = handleIntentUri(view, uri);
        String output = "Intent: " + uri.toString() + "\nHandled: " + isHandled;
        // use template file for response here
        return createResultResponse("local/result.html", output);
    }

    String getIntentPrefixFromUri(Uri uri) {
        //https://stackoverflow.com/questions/1128723/how-do-i-determine-whether-an-array-contains-a-particular-value-in-java
        final List<String> intentList = Arrays.asList(intentNames);
        String[] parts = uri.getPath().split("/", 3);
        if (intentList.contains(parts[1])) {
            return parts[1];
        }
        return null;
    }

    File getExternalFileFromPath(String path) {
        if (path.startsWith("/assets/")) {
            String fileName = path.substring("/assets/".length());
            return new File(activity.getExternalFilesDir(null), fileName);
        } else if (path.startsWith("/sites/")) {
            String dirName = Environment.DIRECTORY_DOCUMENTS;
            String fileName = path.substring("/sites/".length());
            return new File(activity.getExternalFilesDir(dirName), fileName);
        } else if (path.startsWith("/files/")) {
            String dirName = Environment.DIRECTORY_DOWNLOADS;
            String fileName = path.substring("/files/".length());
            return new File(activity.getExternalFilesDir(dirName), fileName);
        }
        return null;
    }

    Uri getContentUriFromPath(String path) {
        File extFile = getExternalFileFromPath(path);
        if (extFile == null) {
            Log.d(TAG, "Content File Not Found: " + path);
            return null;
        }
        Uri contentUri;
        try {
            contentUri = FileProvider.getUriForFile(activity, providerAuthority, extFile);
            MyContentUtility.showContent(activity, contentUri);
        } catch (Exception e) {
            Log.e(TAG, "Content getUriForFile: " + extFile.getAbsolutePath(), e);
            contentUri = Uri.fromFile(extFile);
        }
        return contentUri;
    }

    Boolean handleIntentUri(WebView view, Uri uri) {
        Log.d(TAG, "Intent: " + uri.toString());
        String prefix = getIntentPrefixFromUri(uri);
        if (prefix == null) {
            return false;
        }
        String path = uri.getPath().substring(prefix.length() + 1);
        Intent intent;
        Uri contentUri;
        String type;
        Uri initialUri;
        // See also http://www.openintents.org/intentsregistry/
        switch (prefix) {
            // IntentRequest: /view/sites/demo/index.html
            case "view":
                contentUri = getContentUriFromPath(path);
                if (contentUri == null) {
                    return false;
                }
                //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(extFile));
                intent = new Intent(Intent.ACTION_VIEW);
                //intent.setDataAndType(Uri.fromFile(extFile), getMimeType(extFile.getName()));
                intent.setDataAndType(contentUri, getMimeType(path));
                // Grant temporary read permission to the content URI
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                //view.getContext().startActivity(intent);
                if (intent.resolveActivity(activity.getPackageManager()) == null) {
                    Log.d(TAG, "Intent No activity for " + intent.toString());
                    intent.setDataAndType(contentUri, "text/*");
                }
                // Create intent to show chooser
                String title = uri.toString() + "\n\nOpen with";
                Intent chooser = Intent.createChooser(intent, title);
                try {
                    view.getContext().startActivity(chooser);
                }  catch (ActivityNotFoundException e) {
                    Log.d(TAG, "Intent Start activity failed for " + intent.toString());
                    return false;
                }
                break;
            // IntentRequest: /pick/<type>
            case "pick":
                intent = new Intent(Intent.ACTION_PICK);
                type = "";
                if (path.equals("/")) {
                    type = "*/*";
                } else if (path.indexOf("/", 1) < 0) {
                    type = path.substring(1) + "/*";
                } else if (path.startsWith("//")) {
                    // { action=android.app.action.PICK data=content://com.google.provider.NotePad/notes }
                    path = "content:" + path;
                    intent.setData(Uri.parse(path));
                    Log.d(TAG, "Intent Pick Data: " + path);
                } else {
                    type = path.substring(1);
                }
                if (!type.isEmpty()) {
                    intent.setType(type);
                    Log.d(TAG, "Intent Pick Type: " + type);
                }
                //intent.addCategory(Intent.CATEGORY_OPENABLE);
                if (intent.resolveActivity(activity.getPackageManager()) == null) {
                    Log.d(TAG, "Intent No activity for " + intent.toString());
                    return false;
                }
                //activity.startActivityForResult(intent, REQUEST_PICK);
                activity.mPickForResult.launch(intent);
                break;
            // IntentRequest: /get/<type>
            case "get":
                //intent = new Intent(Intent.ACTION_GET_CONTENT);
                type = "";
                if (path.equals("/")) {
                    type = "*/*";
                } else if (path.indexOf("/", 1) < 0) {
                    type = path.substring(1) + "/*";
                } else if (path.startsWith("//")) {
                    path = "content:" + path;
                    //intent.setData(Uri.parse(path));
                    Log.d(TAG, "Intent Get Data: " + path);
                } else {
                    // { action=android.app.action.GET_CONTENT type=vnd.android.cursor.item/vnd.google.note }
                    type = path.substring(1);
                }
                //if (!type.isEmpty()) {
                //    intent.setType(type);
                //    Log.d(TAG, "Intent Get Type: " + type);
                //}
                //intent.addCategory(Intent.CATEGORY_OPENABLE);
                //if (intent.resolveActivity(activity.getPackageManager()) == null) {
                //    Log.d(TAG, "Intent No activity for " + intent.toString());
                //    return false;
                //}
                //activity.startActivityForResult(intent, REQUEST_GET);
                activity.mGetContent.launch(type);
                break;
            // IntentRequest: /open/<type>
            case "open":
                //intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                //intent.addCategory(Intent.CATEGORY_OPENABLE);
                //initialUri = Uri.parse("content://downloads/my_downloads");
                if (path.equals("/")) {
                    type = "*/*";
                } else if (path.indexOf("/", 1) < 0) {
                    type = path.substring(1) + "/*";
                //} else if (path.startsWith("//")) {
                //    path = "content:" + path;
                //    intent.setData(Uri.parse(path));
                //    Log.d(TAG, "Intent Get Data: " + path);
                } else {
                    // { action=android.app.action.OPEN_DOCUMENT type=text/html }
                    type = path.substring(1);
                }
                //if (!type.isEmpty()) {
                //    intent.setType(type);
                //    Log.d(TAG, "Intent Open Type: " + type);
                //}
                //intent.setType("application/*");
                //intent.putExtra("android.provider.extra.INITIAL_URI", initialUri); // android.net.Uri
                //if (intent.resolveActivity(activity.getPackageManager()) == null) {
                //    Log.d(TAG, "Intent No activity for " + intent.toString());
                //    return false;
                //}
                //activity.startActivityForResult(intent, REQUEST_OPEN);
                // @checkme why is this a string array?
                String[] typeArray = new String[] { type };
                activity.mOpenDocument.launch(typeArray);
                break;
            // IntentRequest: /tree/sites/demo/index.html
            case "tree":
                //if (Build.VERSION.SDK_INT < 21) {
                //    return false;
                //}
                //intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                initialUri = Uri.parse("content://downloads/my_downloads");
                //intent.putExtra("android.provider.extra.INITIAL_URI", initialUri); // android.net.Uri
                //if (Build.VERSION.SDK_INT >= 19) {
                //    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                //}
                //if (intent.resolveActivity(activity.getPackageManager()) == null) {
                //    Log.d(TAG, "Intent No activity for " + intent.toString());
                //    return false;
                //}
                //activity.startActivityForResult(intent, REQUEST_TREE);
                activity.mOpenDocumentTree.launch(initialUri);
                break;
            // IntentRequest: /create/readme.txt
            case "create":
                //intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                //if (intent.resolveActivity(activity.getPackageManager()) == null) {
                //    Log.d(TAG, "Intent No activity for " + intent.toString());
                //    return false;
                //}
                //activity.startActivityForResult(intent, REQUEST_CREATE);
                if (path.equals("/")) {
                    path = "README.txt";
                } else if (path.indexOf(".", 1) < 0) {
                    path = path.substring(1) + ".txt";
                } else {
                    // { action=android.app.action.CREATE_DOCUMENT path=index.html }
                    path = path.substring(1);
                }
                activity.mCreateDocument.launch(path);
                break;
            // IntentRequest: /edit/sites/demo/index.html
            case "edit":
                contentUri = getContentUriFromPath(path);
                if (contentUri == null) {
                    return false;
                }
                intent = new Intent(Intent.ACTION_EDIT);
                intent.setDataAndType(contentUri, getMimeType(path));
                // Grant temporary read permission to the content URI
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                if (intent.resolveActivity(activity.getPackageManager()) == null) {
                    return false;
                }
                activity.startActivity(intent);
                break;
            case "delete":
                intent = new Intent(Intent.ACTION_DELETE);
                //intent.setData(dataUri); // Data to be deleted.
                if (intent.resolveActivity(activity.getPackageManager()) == null) {
                    Log.d(TAG, "Intent No activity for " + intent.toString());
                    return false;
                }
                activity.startActivity(intent);
                break;
            // IntentRequest: /send/sites/demo/index.html
            case "send":
            default:
                contentUri = getContentUriFromPath(path);
                if (contentUri == null) {
                    return false;
                }
                intent = new Intent(Intent.ACTION_SEND);
                intent.setDataAndType(contentUri, getMimeType(path));
                // Grant temporary read permission to the content URI
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                if (intent.resolveActivity(activity.getPackageManager()) == null) {
                    Log.d(TAG, "Intent No activity for " + intent.toString());
                    return false;
                }
                view.getContext().startActivity(intent);
                // Create intent to show chooser
                //String title = uri.toString() + "\n\nOpen with";
                //Intent chooser = Intent.createChooser(intent, title);
                //view.getContext().startActivity(chooser);
                break;
        }
        return true;
    }

    // move back to MyAppWebViewClient?
    WebResourceResponse handleGetLocalConfig(Uri uri) {
        String message;
        try {
            Map<String, Object> localConfig = webViewClient.getLocalConfig();
            HashMap<String, String> localSites = (HashMap<String, String>) localConfig.get("sites");
            if (MyLocalConfigRepository.updateLocalSites(activity, localSites)) {
                localConfig.put("sites", localSites);
            }
            localConfig.put("bundles", MyLocalConfigRepository.findAvailableBundles(activity));
            message = MyJsonUtility.toJsonString(localConfig);
        } catch (Exception e) {
            Log.e(TAG, "Local Config", e);
            message = e.toString();
        }
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("application/json", "UTF-8", targetStream);
    }

    WebResourceResponse handleDownloadBundle(Uri uri) {
        String updateZip = uri.getQueryParameter("update_zip");
        String extract = uri.getQueryParameter("extract");
        Log.d(TAG, "Download Bundle URL: " + updateZip + " Extract: " + extract);
        if (updateZip != null && updateZip.startsWith("http")) {
            // start download request - https://medium.com/@trionkidnapper/android-webview-downloading-images-f0ec21ac75d2
            Uri updateUri = Uri.parse(updateZip);
            if (updateUri != null && URLUtil.isHttpsUrl(updateUri.toString())) {
                requestUriDownload(updateUri, extract != null && extract.equals("true"));
            }
        }
        // use template file for response here
        String templateName = "local/result.html";
        Map<String, String> valuesMap = new HashMap<>();
        Map<String, String> output = new HashMap<>();
        output.put("update_zip", updateZip);
        output.put("extract", extract);
        try {
            valuesMap.put("output", MyJsonUtility.toJsonString(output));
        } catch (Exception e) {
            valuesMap.put("output", e.toString());
        }
        String message = MyAssetUtility.getTemplateFile(activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    WebResourceResponse handleExtractBundle(Uri uri) {
        String bundle = uri.getQueryParameter("bundle");
        File extFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), bundle);
        Log.d(TAG, "Extract: " + extFile.getAbsolutePath());
        try {
            FileInputStream inputStream = new FileInputStream(extFile);
            File targetDirectory = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            String[] skipNames = { MySettingsRepository.fileName };
            MyAssetUtility.unzipStream(inputStream, targetDirectory, skipNames);
            if (inputStream != null)
                inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Extract", e);
        }
        String output = "Extracted: " + extFile.getName();
        // use template file for response here
        return createResultResponse("local/result.html", output);
    }

    WebResourceResponse handleDeleteBundle(Uri uri) {
        String bundle = uri.getQueryParameter("bundle");
        File extFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), bundle);
        Log.d(TAG, "Delete: " + extFile.getAbsolutePath());
        try {
            // do we need to find the equivalent Download file first?
            extFile.delete();
        } catch (Exception e) {
            Log.e(TAG, "Delete", e);
        }
        String output = "Deleted: " + extFile.getName();
        // use template file for response here
        return createResultResponse("local/result.html", output);
    }

    WebResourceResponse handleCleanUpDownloads(Uri uri) {
        try {
            MyDownloadUtility.showMyDownloadFiles(activity, true);
        } catch (Exception e) {
            Log.e(TAG, "Cleanup", e);
        }
        String output = "Cleaned up...";
        // use template file for response here
        return createResultResponse("local/result.html", output);
    }

    WebResourceResponse handleFileNotFound(Uri uri) {
        // from unknown deep links, see getSiteUrlFromAppLink()
        String link = uri.getQueryParameter("link");
        // use template file for response here
        String templateName = "local/result.html";
        Map<String, String> valuesMap = new HashMap<>();
        if (link != null) {
            valuesMap.put("output", "File not found:" + link.replace("<", "&lt;"));
        } else {
            valuesMap.put("output", "File not found:" + uri.getPath().replace("<", "&lt;"));
        }
        String message = MyAssetUtility.getTemplateFile(activity, templateName, valuesMap);
        ByteArrayInputStream targetStream = new ByteArrayInputStream(message.getBytes());
        if (Build.VERSION.SDK_INT >= 21) {
            return new WebResourceResponse("text/html", "UTF-8", 404, "Not Found", null, targetStream);
        }
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
    }

    // move back to MyAppWebViewClient?
    WebResourceResponse handleUpdateSettings(Uri uri) {
        // String query = uri.getQuery();
        HashMap<String, Object> hashMap = MySettingsRepository.parseQueryParameters(uri);
        // add custom web settings here?
        hashMap.put("web_settings", webViewClient.myCustomWebSettings);
        // add local config here?
        hashMap.put("local_config", webViewClient.getLocalConfig());
        String output = webViewClient.mySavedStateModel.setSettings(activity, hashMap);
        webViewClient.loadSettings();
        String updateZip = webViewClient.getUpdateZip();
        webViewClient.mDownloadId = -1;
        if (updateZip != null && updateZip.startsWith("http")) {
            // start download request - https://medium.com/@trionkidnapper/android-webview-downloading-images-f0ec21ac75d2
            Uri updateUri = Uri.parse(updateZip);
            if (updateUri != null && URLUtil.isHttpsUrl(updateUri.toString())) {
                requestUriDownload(updateUri, true);
            }
        }
        // use template file for response here
        return createResultResponse("web/result.html", output);
    }

    // move back to MyAppWebViewClient?
    long requestUriDownload(Uri updateUri, boolean extract) {
        String updateName = URLUtil.guessFileName(updateUri.toString(), null, null);
        File mDownloadFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), updateName);
        if (mDownloadFile.exists()) {
            Log.d(TAG, "Download: " + mDownloadFile.getAbsolutePath() + " exists");
            String lastModified = String.valueOf(mDownloadFile.lastModified() / 1000);
            File renameFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), updateName.replace(".zip", "." + lastModified + ".zip"));
            mDownloadFile.renameTo(renameFile);
            // update corresponding Downloads entry as well via getContentResolver.update()
        } else {
            Log.d(TAG, "Download: " + mDownloadFile.getAbsolutePath() + " does not exist");
        }
        DownloadManager.Request request = new DownloadManager.Request(updateUri);
        request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, updateName);
        // not easy to verify final path from DownloadManager in onDownloadComplete - use description to preset
        request.setDescription(mDownloadFile.getAbsolutePath());
        try {
            webViewClient.mDownloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            webViewClient.mDownloadExtract = extract;
            webViewClient.mDownloadId = webViewClient.mDownloadManager.enqueue(request);
            Log.d(TAG, "Download Enqueue: " + webViewClient.mDownloadId);
        } catch (Exception e) {
            Log.e(TAG, "Download", e);
        }
        return webViewClient.mDownloadId;
    }

    static String getMimeType(String fileName) {
        String type;
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (extension != null) {
            extension = extension.toLowerCase();
        }
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        // https://www.iana.org/assignments/media-types/media-types.xhtml
        if (extension != null && mime.hasExtension(extension)) {
            type = mime.getMimeTypeFromExtension(extension);
        } else if (fileName.endsWith(".json")) {
            type = "application/json";
        } else if (fileName.endsWith(".js")) {
            type = "application/javascript";
        } else if (fileName.endsWith(".ttf")) {
            //type = "application/x-font-ttf";
            type = "font/ttf";
        } else if (fileName.endsWith(".woff")) {
            //type = "application/font-woff";
            type = "font/woff";
        } else if (fileName.endsWith(".woff2")) {
            type = "font/woff2";
        } else if (fileName.endsWith(".svg")) {
            type = "image/svg+xml";
        } else {
            type = "TODO";
            Log.d(TAG, "File: " + fileName + " extension: " + extension + " mimetype: " + type);
        }
        return type;
    }
}
