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

Note: steps 10-12 are new for release v1.15

1. Press on HTML5 Test link or select in dropdown list

Verify that the app goes to the site via its own WebView and not by opening the standard browser

2. Press Back on your phone or tablet to return to the first page

3. Type in another URL like https://www.google.com/ in the Other: box and press the Go button

Verify that the app does NOT go to that site and tells you the link does not match.
This default behavior can be changed in Advanced Options to open the regular browser instead

4. Click on the Update Settings link to allow another site

Verify that you get to the Update Settings page with all the input fields

5. Add https://www.google.com/ in the Other: field, and add the following in the empty Match fields:

host  equals  www.google.com

6. Press the Update Settings button to submit the changes

Verify that your changes are shown in the result page (json format)

7. Press the My WebView link or Back until you get to the first page

Verify that your new website is in the Other: field, and the host match is there

8. Press the Go button to go to the site you specified above

Verify that now the app goes to that site without telling the link does not match or trying to open the standard browser

9. Close the app and open it again

Verify that your new settings are still there

10. Click on the Update Settings link and specify a .zip file in Update Assets from trusted site

For testing, you could download a bootstrap template from https://bootstrapmade.com/ and put it on your site,
e.g. https://owncloud.mikespub.net/eBusiness.zip

If the .zip file contains a homepage at eBusiness/index.html, also change the Other: field to
https://appassets.androidplatform.net/assets/eBusiness/index.html (or http://localhost/assets/eBusiness/index.html)
to go to the local site in your app after the update.

11. Press the Update Settings button to submit the changes

Verify that your changes are shown in the result page (json format)

12. Press the My WebView link or Back until you get to the first page

Verify that your local website is in the Other: field, and press Go to test it.
