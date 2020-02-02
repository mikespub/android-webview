package net.mikespub.mywebview;

import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyAppWebViewClient extends WebViewClient {

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
}