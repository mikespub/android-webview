package net.mikespub.mywebview;

import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

public class MyAppWebViewClient extends WebViewClient {
    // http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    private AppCompatActivity activity;
    private WebViewAssetLoader assetLoader;

    public MyAppWebViewClient(AppCompatActivity activity) {
        this.activity = activity;
    }

    // https://stackoverflow.com/questions/41972463/android-web-view-shouldoverrideurlloading-deprecated-alternative/41973017
    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        final Uri uri = Uri.parse(url);
        // if(Uri.parse(url).getHost().endsWith("html5rocks.com")) {
        final String host = uri.getHost();
        if (host != null) {
            String[] myHostEndsArr = view.getContext().getResources().getStringArray(R.array.urlhost_endswith);
            for (String s : myHostEndsArr) {
                if (host.endsWith(s)) {
                    // skip epub files - TODO: save in media directory https://developer.android.com/training/data-storage/app-specific#media ?
                    // if (!path.endsWith(".epub") && (query == null || !query.contains("type=epub"))) {
                    //     return false;
                    // }
                    boolean isMatch = false;
                    final String path = uri.getPath();
                    if (path != null) {
                        String[] myPathNotEndsArr = view.getContext().getResources().getStringArray(R.array.urlpath_not_endswith);
                        for (String t : myPathNotEndsArr) {
                            if (path.endsWith(t)) {
                                isMatch = true;
                                break;
                            }
                        }
                        if (isMatch) {
                            continue;
                        }
                    }
                    final String query = uri.getQuery();
                    if (query != null) {
                        String[] myQueryNotContainsArr = view.getContext().getResources().getStringArray(R.array.urlquery_not_contains);
                        for (String q : myQueryNotContainsArr) {
                            if (query.contains(q)) {
                                isMatch = true;
                                break;
                            }
                        }
                        if (isMatch) {
                            continue;
                        }
                    }
                    return false;
                }
            }
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        view.getContext().startActivity(intent);
        return true;
    }

    // https://stackoverflow.com/questions/8938119/changing-html-in-a-webview-programmatically/8938191#8938191
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if("file:///android_asset/web/index.html".equals(url)){
            // this.urlCache.load("http://tutorials.jenkov.com/java/index.html");
            // view.loadUrl("javascript:addSite('replace1', 'new content 1')");
            // view.loadUrl("javascript:addSite('replace1', 'new content 1')");
        }
    }

    // https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if(url.startsWith("https://appassets.androidplatform.net/")) {
            if (this.assetLoader == null) {
                this.assetLoader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this.activity))
                    //.addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(this.activity))
                    .build();
            }
            final Uri uri = Uri.parse(url);
            return this.assetLoader.shouldInterceptRequest(uri);
        }
        return null;
    }
}