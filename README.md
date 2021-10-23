# Android WebView

Experiment with Android WebView based on various tutorials:

- https://developer.chrome.com/multidevice/webview/gettingstarted
- http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
- https://ukacademe.com/MobileApplication/AndroidGUI/Android_WebView
- https://developer.android.com/guide/webapps
- ...

## Development

Use Android Studio to import this Git repository

Adapt [assets/web/](app/src/main/assets/web/) and [res/values/strings.xml](app/src/main/res/values/strings.xml) for predefined websites, allowed hosts...

## Testing

Check the [latest release](https://github.com/mikespub/android-webview/releases) for [app-release.apk](app/release/app-release.apk)

Note: steps 10-12 are updated for release v1.16

<img src="https://github.com/mikespub/android-webview/raw/master/app/screenshots/index.png" width="200">

1. Press on HTML5 Test Site link or select in dropdown list

<img src="https://github.com/mikespub/android-webview/raw/master/app/screenshots/html5test.png" width="200">

Verify that the app goes to the site via its own WebView and not by opening the standard browser

2. Press Back on your phone or tablet to return to the first page

3. Type in another URL like https://www.google.com/ in the Other: box and press the Go button

Verify that the app does NOT go to that site and tells you the link does not match.
This default behavior can be changed in Advanced Options to open the regular browser instead

4. Click on the Update Settings link to allow another site

<img src="https://github.com/mikespub/android-webview/raw/master/app/screenshots/update.png" width="200">

Verify that you get to the Update Settings page with all the input fields

5. Add https://www.google.com/ in the Other: field, and add the following in the empty Match fields:

host  equals  www.google.com

6. Press the Update Settings button to submit the changes

<img src="https://github.com/mikespub/android-webview/raw/master/app/screenshots/settings.png" width="200">

Verify that your changes are shown in the result page (json format)

7. Press the My WebView link or Back until you get to the first page

Verify that your new website is in the Other: field, and the host match is there

8. Press the Go button to go to the site you specified above

<img src="https://github.com/mikespub/android-webview/raw/master/app/screenshots/diskstation.png" width="200">

Verify that now the app goes to that site without telling the link does not match or trying to open the standard browser

9. Close the app and open it again

Verify that your new settings are still there

10. Click on the Local Websites link and specify a .zip file in Download Bundle from trusted site (v1.16+)

For testing, you could download a bootstrap template from https://bootstrapmade.com/ and put it on your site,
e.g. https://owncloud.mikespub.net/Moderna.zip

<img src="https://github.com/mikespub/android-webview/raw/master/app/screenshots/site_bundle.png" width="200">

11. Press the Download Bundle button to download the site bundle and extract it (v1.16+)

Verify that your changes are shown in the result page (json format)

12. Press the Local Websites link or Back until you get to the Local Websites page (v1.16+)

Verify that your local website is in the Site List now, and click the link to test it.

<img src="https://github.com/mikespub/android-webview/raw/master/app/screenshots/local_site.png" width="200">

## Testing without Manage Local Websites

If you don't use Local Websites much and you unchecked the option to manage Local Websites separately, you can also:

10. Click on the Update Settings link and specify a .zip file in Update Assets from trusted site (v1.15+)

If the .zip file contains a homepage at Moderna/index.html, also change the Other: field to
https://appassets.androidplatform.net/sites/Moderna/index.html (or http://localhost/sites/Moderna/index.html)
to go to the local site in your app after the update.

<img src="https://github.com/mikespub/android-webview/raw/master/app/screenshots/update_assets.png" width="200">

11. Press the Update Settings button to submit the changes (v1.15+)

Verify that your changes are shown in the result page (json format)

12. Press the My WebView link or Back until you get to the first page (v1.15+)

Verify that your local website is in the Other: field, and press Go to test it.

## Try out Deep Links

See [Deep Links](https://github.mikespub.net/android-webview/deeplinks.html)
