package net.mikespub.mywebview;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import net.mikespub.myutils.MyContentUtility;
import net.mikespub.myutils.MyDocumentUtility;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Main Activity for Android App
 */
// See also Chrome Custom Tabs https://developer.chrome.com/multidevice/android/customtabs
// and Android Browser Helper https://github.com/GoogleChrome/android-browser-helper
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // SavedStateViewModel see https://github.com/googlecodelabs/android-lifecycles/blob/master/app/src/main/java/com/example/android/lifecycles/step6_solution/SavedStateActivity.java
    // or with AndroidViewModel see https://github.com/husaynhakeem/Androidx-SavedState-Playground/blob/master/app/src/main/java/com/husaynhakeem/savedstateplayground/AndroidViewModelWithSavedState.kt
    //private MySavedStateModel mySavedStateModel;

    // https://developer.chrome.com/multidevice/webview/gettingstarted
    protected WebView myWebView;
    BroadcastReceiver onDownloadComplete;

    // https://developer.android.com/training/basics/intents/result#launch
    // GetContent creates an ActivityResultLauncher<String> to allow you to pass
    // in the mime type you'd like to allow the user to select
    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
        new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri returnUri) {
                // Handle the returned Uri
                if (!showContentUri(returnUri)) {
                    return;
                }
                readReturnUri(returnUri);
            }
        });

    // @checkme why is this a string array?
    ActivityResultLauncher<String[]> mOpenDocument = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
        new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri returnUri) {
                // Handle the returned Uri
                if (!showDocumentUri(returnUri)) {
                    return;
                }
                readReturnUri(returnUri);
            }
        });

    ActivityResultLauncher<Uri> mOpenDocumentTree = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(),
        new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri returnUri) {
                // Handle the returned Uri
                showDocumentTree(returnUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        });

    // https://developer.android.com/training/basics/intents/result#custom
    // If you do not need a custom contract, you can use the StartActivityForResult contract.
    // This is a generic contract that takes any Intent as an input and returns an ActivityResult,
    // allowing you to extract the resultCode and Intent as part of your callback
    ActivityResultLauncher<Intent> mPickForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                /*
                if (result.getResultCode() == RESULT_OK) {
                    Intent intent = result.getData();
                    // Handle the Intent
                }
                 */
                int requestCode = MyRequestHandler.REQUEST_PICK;
                int resultCode = result.getResultCode();
                Intent returnIntent = result.getData();
                // Get the file's content URI from the incoming Intent
                Uri returnUri = getActivityResultUri(requestCode, resultCode, returnIntent);
                if (returnUri == null) {
                    return;
                }
                if (!showContentUri(returnUri)) {
                    return;
                }
                readReturnUri(returnUri);
            }
        });

    ActivityResultLauncher<String> mCreateDocument = registerForActivityResult(new ActivityResultContracts.CreateDocument(),
        new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri returnUri) {
                // Handle the returned Uri
                if (!showContentUri(returnUri)) {
                    return;
                }
                readReturnUri(returnUri);
            }
        });


    /**
     * @param savedInstanceState    saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
        //this.mySavedStateModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(MySavedStateModel.class);
        //HashMap<String, Object> hashMap = mySavedStateModel.getSettings(this);
        //Log.d("State Get", hashMap.toString());

        myWebView = findViewById(R.id.activity_main_webview);
        // Enable Javascript
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // See https://ukacademe.com/MobileApplication/AndroidGUI/Android_WebView
        // webSettings.setBuiltInZoomControls(true);
        // Some other options - https://github.com/codepath/android_guides/wiki/Working-with-the-WebView
        // webSettings.setUseWideViewPort(true);
        // webSettings.setLoadWithOverviewMode(true);
        //MyDownloadUtility.showMyDownloadFiles(this, true);
        //MyContentUtility.showContent(this, Uri.parse("content://media/external/file/86"));
        //MyContentUtility.showContentFiles(this,"content://net.mikespub.mywebview.fileprovider/root/");
        //String parentDoc = this.getContentResolver().getType(Uri.parse("content://net.mikespub.mywebview.fileprovider/root/"));
        //Log.d("Parent", parentDoc);
        /*
        try {
            //JSONObject jsonObject = MyJsonUtility.mapToJson(defaultSettings);
            //JSONObject jsonObject = (JSONObject) MyJsonUtility.toJson(defaultSettings);
            //String jsonString = jsonObject.toString(2).replace("\\","");
            String jsonString = MyJsonUtility.toJsonString(defaultSettings);
            Log.d("WebView", jsonString);
        } catch (Exception e) {
            Log.e("WebView", e.toString());
        }
         */
        // myWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        // myWebView.setScrollbarFadingEnabled(false);
        //MyReflectUtility.showObject(myWebView);
        // Stop local links and redirects from opening in browser instead of WebView
        MyAppWebViewClient myWebViewClient = new MyAppWebViewClient(this);
        //MyReflectUtility.showObject(myWebViewClient);
        if (myWebViewClient.hasDebuggingEnabled()) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        // Set custom WebSettings (if any)
        //MyReflectUtility.showObject(webSettings);
        myWebViewClient.setWebSettings(webSettings);
        myWebView.setWebViewClient(myWebViewClient);
        // Add Javascript interface
        if (myWebViewClient.hasJavascriptInterface()) {
            myWebView.addJavascriptInterface(new AppJavaScriptProxy(this, myWebView), "androidAppProxy");
            Log.d("WebView", "Enable Javascript interface");
        }
        // Show console log messages - see https://developer.android.com/guide/webapps/debugging
        if (myWebViewClient.hasConsoleLog()) {
            final MainActivity myActivity = this;
            myWebView.setWebChromeClient(new WebChromeClient() {
                public boolean onConsoleMessage(ConsoleMessage cm) {
                    String message = cm.messageLevel() + " " + cm.message() + " -- From line "
                            + cm.lineNumber() + " of "
                            + cm.sourceId();
                    Log.d("WebView", message);
                    Toast toast = Toast.makeText(
                            myActivity.getApplicationContext(),
                            cm.message(),
                            Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
            });
        }
        // Support context menu for links and images in WebView
        // WebViewClient myWebViewClient = myWebView.getWebViewClient();
        //registerForContextMenu(myWebView);
        if (myWebViewClient.hasContextMenu()) {
            //Log.d("WebView", Boolean.toString(myWebView.isLongClickable()));
            myWebView.setLongClickable(true);
            // See https://github.com/AriesHoo/FastLib/blob/dev/app/src/main/java/com/aries/library/fast/demo/module/WebViewActivity.java
            myWebView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    WebView.HitTestResult hitTestResult = myWebView.getHitTestResult();
                    if (hitTestResult == null) {
                        return false;
                    }
                    int getType = hitTestResult.getType();
                    String extra = hitTestResult.getExtra();
                    if (extra == null || extra.equals("")) {
                        return false;
                    }
                    Uri uri = Uri.parse(extra);
                    Intent intent;
                    Intent chooser;
                    switch (getType) {
                        case WebView.HitTestResult.IMAGE_TYPE:
                            Log.d("WebView", "image");
                            //showDownDialog(hitTestResult.getExtra());
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                            //myWebView.getContext().startActivity(intent);
                            chooser = Intent.createChooser(intent, null);
                            myWebView.getContext().startActivity(chooser);
                            return true;
                        case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                            Log.d("WebView", "image anchor");
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                            //myWebView.getContext().startActivity(intent);
                            chooser = Intent.createChooser(intent, null);
                            myWebView.getContext().startActivity(chooser);
                            return true;
                        case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                            Log.d("WebView", "anchor");
                            //intent = new Intent(Intent.ACTION_SEND, uri);
                            // See also https://github.com/codepath/android_guides/wiki/Sharing-Content-with-Intents
                            //intent = new Intent(Intent.ACTION_SEND);
                            //intent.putExtra(Intent.EXTRA_TEXT, uri.toString());
                            //intent.putExtra(Intent.EXTRA_TITLE, "Send Me");
                            //intent.setType("text/plain");
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                            // Create intent to show chooser
                            String title = uri.toString() + "\n\nOpen with";
                            chooser = Intent.createChooser(intent, title);
                            //chooser = Intent.createChooser(intent, null);
                            //chooser.putExtra(Intent.EXTRA_TITLE, uri.toString() + "\n\nOpen with");
                            myWebView.getContext().startActivity(chooser);
                            return true;
                        default:
                            Log.d("WebView", "other " + getType);
                            break;
                    }

                    Log.d("WebView", "Type:" + hitTestResult.getType() + ";Extra:" + hitTestResult.getExtra());
                    //return true;
                    return false;
                }
            });
        }
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        if (appLinkIntent != null) {
            //Log.d("Intent", appLinkIntent.toString());
            String appLinkAction = appLinkIntent.getAction();
            // support other actions besides VIEW? (= for deep links) - https://developer.android.com/training/secure-file-sharing/share-file
            if (appLinkAction.equals(Intent.ACTION_VIEW)) {
                Uri appLinkData = appLinkIntent.getData();
                Log.d("Intent", "Action: " + appLinkAction + " - Data: " + appLinkData);
                if (appLinkData != null && appLinkData.getScheme().equals(getString(R.string.link_scheme))) {
                    String myUrl = myWebViewClient.getSiteUrlFromAppLink(appLinkData);
                    myWebView.loadUrl(myUrl);
                    return;
                }
            }
        }
        // https://stackoverflow.com/questions/36987144/preventing-webview-reload-on-rotate-android-studio/46849736#46849736
        if (savedInstanceState == null) {
            // myWebView.loadUrl("http://beta.html5test.com/");
            String myUrl = myWebViewClient.domainUrl + getString(R.string.start_uri);
            myWebView.loadUrl(myUrl);
        } else {
            // Bundle bundle = savedInstanceState.getBundle("webViewState");
            // Log.d("Web Create", bundle.toString());
            // myWebView = findViewById(R.id.activity_main_webview);
            // Stop local links and redirects from opening in browser instead of WebView
            // myWebView.setWebViewClient(new MyAppWebViewClient(this));
            // myWebView.restoreState(bundle);
        }
        // https://stackoverflow.com/questions/19365668/getting-webview-history-from-webbackforwardlist
        // https://stackoverflow.com/questions/33326833/save-state-of-webview-when-switching-activity
        // For document provider, see https://developer.android.com/guide/topics/providers/create-document-provider#queryRoots
        // and https://github.com/android/storage-samples/blob/master/StorageProvider/Application/src/main/java/com/example/android/storageprovider/MyCloudProvider.java
        /*
            getActivity().getContentResolver().notifyChange(DocumentsContract.buildRootsUri
                    (AUTHORITY), null, false);
         */
    }

    /**
     * @return  ViewModel with Saved State for Settings
     */
    // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
    public MySavedStateModel getSavedStateModel() {
        return new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(MySavedStateModel.class);
    }

    /**
     *
     */
    // Note: this is different from https://developer.android.com/guide/webapps/webview#java
    // and http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    @Override
    public void onBackPressed() {
        if(myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * @param overrideConfiguration configuration to override
     */
    // https://stackoverflow.com/questions/41025200/android-view-inflateexception-error-inflating-class-android-webkit-webview
    @Override
    public void applyOverrideConfiguration(final Configuration overrideConfiguration) {
        if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT < 25) {
            overrideConfiguration.uiMode &= ~Configuration.UI_MODE_NIGHT_MASK;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    /**
     * @param outState  instance state to save
     */
    // https://stackoverflow.com/questions/39086084/save-webview-state-on-screen-rotation
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        myWebView.saveState(outState);
        Log.d("Web Save", outState.toString());
        //Bundle bundle = new Bundle();
        //myWebView.saveState(bundle);
        //Log.d("Web Save", bundle.toString());
        //outState.putBundle("webViewState", bundle);
    }

    /**
     * @param savedInstanceState    saved instance state
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("Web Restore", savedInstanceState.toString());
        myWebView.restoreState(savedInstanceState);
        //Bundle bundle = savedInstanceState.getBundle("webViewState");
        //Log.d("Web Restore", bundle.toString());
        // myWebView.restoreState(bundle);
    }

    /**
     * Register BroadcastReceiver for Downloads
     */
    void startDownloadReceiver(BroadcastReceiver mReceiver) {
        stopDownloadReceiver();
        onDownloadComplete = mReceiver;
        Log.d("Web Create", "register receiver");
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    /**
     * Unregister BroadcastReceiver for Downloads
     */
    void stopDownloadReceiver() {
        if (onDownloadComplete != null) {
            Log.d("Web Create", "unregister receiver");
            unregisterReceiver(onDownloadComplete);
        }
    }
    /*
     * When the Activity of the app that hosts files sets a result and calls
     * finish(), this method is invoked. The returned Intent contains the
     * content URI of a selected file. The result code indicates if the
     * selection worked or not.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent returnIntent) {
        super.onActivityResult(requestCode, resultCode, returnIntent);
        // Get the file's content URI from the incoming Intent
        Uri returnUri = getActivityResultUri(requestCode, resultCode, returnIntent);
        if (returnUri == null) {
            return;
        }
        /*
        if (requestCode == MyRequestHandler.REQUEST_TREE) {
            int takeFlags = returnIntent.getFlags();
            showDocumentTree(returnUri, takeFlags);
            return;
        }
        if (requestCode == MyRequestHandler.REQUEST_OPEN) {
            if (!showDocumentUri(returnUri)) {
                return;
            }
        } else {
            if (!showContentUri(returnUri)) {
                return;
            }
        }
        readReturnUri(returnUri);
         */
    }

    private Uri getActivityResultUri(int requestCode, int resultCode, Intent returnIntent) {
        // If the selection didn't work
        if (!checkActivityResult(requestCode, resultCode, returnIntent)) {
            return null;
        }
        // Get the file's content URI from the incoming Intent
        Uri returnUri = returnIntent.getData();
        Bundle returnExtras = returnIntent.getExtras();
        if (returnUri == null) {
            // Pick text - Samsung Notes
            if (returnExtras != null) {
                Log.d("Activity Result", "Request: " + requestCode + " Result: " + resultCode + " Intent: " + returnIntent.toString() + " No Uri: " + returnIntent.toUri(0) + " Extras: " + returnExtras.toString());
            } else {
                Log.d("Activity Result", "Request: " + requestCode + " Result: " + resultCode + " Intent: " + returnIntent.toString() + " No Uri: " + returnIntent.toUri(0) + " Extras: " + returnExtras);
            }
            return null;
        }
        if (returnExtras != null) {
            Log.d("Activity Result", "Request: " + requestCode + " Result: " + resultCode + " Intent: " + returnIntent.toString() + " Uri: " + returnUri + " Extras: " + returnExtras.toString());
        } else {
            Log.d("Activity Result", "Request: " + requestCode + " Result: " + resultCode + " Intent: " + returnIntent.toString() + " Uri: " + returnUri + " Extras: " + returnExtras);
        }
        return returnUri;
    }

    private boolean checkActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        // If the selection didn't work
        if (resultCode != RESULT_OK) {
            // Exit without doing anything else
            if (returnIntent != null) {
                Log.d("Activity Result", "Request: " + requestCode + " Result: " + resultCode + " Not OK: " + returnIntent.toString());
            } else {
                Log.d("Activity Result", "Request: " + requestCode + " Result: " + resultCode + " Not OK: " + returnIntent);
            }
            return false;
        }
        if (returnIntent == null) {
            Log.d("Activity Result", "Request: " + requestCode + " Result: " + resultCode + " Intent: " + returnIntent);
            return false;
        }
        return true;
    }

    private void showDocumentTree(Uri returnUri, int takeFlags) {
        if (returnUri == null) {
            return;
        }
        // Downloads = content://com.android.providers.downloads.documents/tree/downloads
        // Virtual SD Card = content://com.android.externalstorage.documents/tree/primary%3A
        // Downloads via SD Card = content://com.android.externalstorage.documents/tree/primary%3ADownload
        if (MyDocumentUtility.checkTreeUri(returnUri)) {
            MyDocumentUtility.savePermissions(this, returnUri, takeFlags);
            MyDocumentUtility.showPermissions(this);
            MyDocumentUtility.showTreeFiles(this, returnUri);
        } else {
            Log.d("Activity Result", "Not a tree?");
        }
    }

    private boolean showDocumentUri(Uri returnUri) {
        if (returnUri == null) {
            return false;
        }
        try {
            MyDocumentUtility.showDocument(this, returnUri);
        } catch (Exception e) {
            Log.e("Activity Result", "Document Error: " + returnUri, e);
            return false;
        }
        return true;
    }

    private boolean showContentUri(Uri returnUri) {
        if (returnUri == null) {
            return false;
        }
        try {
            MyContentUtility.showContent(this, returnUri);
        } catch (Exception e) {
            Log.e("Activity Result", "Content Error: " + returnUri, e);
            return false;
        }
        return true;
    }

    /*
     * Try to open the file for "read" access using the
     * returned URI. If the file isn't found, write to the
     * error log and return.
     */
    private void readReturnUri(Uri returnUri) {
        /*
         * Get the content resolver instance for this context, and use it
         * to get a ParcelFileDescriptor for the file.
         */
        ParcelFileDescriptor inputPFD;
        try {
            inputPFD = getContentResolver().openFileDescriptor(returnUri, "r");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Result File not found.", e);
            return;
        }
        // Get a regular file descriptor for the file
        FileDescriptor fd = inputPFD.getFileDescriptor();
        try {
            FileInputStream inputStream = new FileInputStream(fd);
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Result File Error", e);
        }
        //InputStream inputStream = activity.getContentResolver().openInputStream(uri);
    }

    // See https://developer.android.com/training/permissions/requesting
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("Activity", "Request permission result for " + requestCode + " Permissions: " + Arrays.toString(permissions) + " Grant: " + Arrays.toString(grantResults));
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("Activity", "Permission granted");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Activity", "Permission denied");
                }
                return;
            }

            default:
                // other 'case' lines to check for other
                // permissions this app might request.
                Log.d("Activity", "Other Permission?");
        }
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopDownloadReceiver();
    }
}
