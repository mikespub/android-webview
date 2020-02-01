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
        String myHostEnds = view.getContext().getResources().getString(R.string.urlhost_endswith);
        // if(Uri.parse(url).getHost().endsWith("html5rocks.com")) {
        final String host = uri.getHost();
        if(host != null && host.endsWith(myHostEnds)) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        view.getContext().startActivity(intent);
        return true;
    }
}