package net.mikespub.mywebview;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class MySettingsRepository {
    // http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    private final AppCompatActivity activity;

    MySettingsRepository(AppCompatActivity activity) {
        this.activity = activity;
    }

    HashMap<String, Object> loadJsonSettings() {
        long lastUpdated = this.checkAssetFiles();
        //this.loadStringConfig();
        return this.loadJsonConfig(lastUpdated);
    }

    private String getTimestamp(long lastUpdated) {
        // https://stackoverflow.com/questions/13515168/android-time-in-iso-8601
        // works with Instant
        // Instant instant = Instant.now();
        // String timestamp = (String) instant.format(DateTimeFormatter.ISO_INSTANT);
        //ZonedDateTime zdt = ZonedDateTime.now();
        //String timestamp = zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        // https://stackoverflow.com/questions/3914404/how-to-get-current-moment-in-iso-8601-format-with-date-hour-and-minute
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String timestamp;
        if (lastUpdated > 0) {
            timestamp = df.format(new Date(lastUpdated));
        } else {
            timestamp = df.format(new Date());

        }
        // String timestamp = Instant.now().toString();
        return timestamp;
    }

    private HashMap<String, Object> loadJsonConfig(long lastUpdated) {
        String filename = "web/settings.json";
        String content = this.getJsonSettings(filename);
        HashMap<String, Object> hashMap = null;
        try {
            hashMap = new HashMap<>(MyJsonUtility.jsonToMap(content));
            Log.d("Settings", hashMap.toString());
            if (!hashMap.containsKey("timestamp")) {
                String timestamp = getTimestamp(0);
                Log.d("Timestamp New", timestamp);
                hashMap.put("timestamp", timestamp);
            } else if (lastUpdated > 0) {
                String timestamp = getTimestamp(lastUpdated);
                Log.d("Timestamp Old", timestamp);
                // String timestamp = Instant.now().toString();
                hashMap.put("timestamp", timestamp);
            }
        } catch (JSONException e) {
            Log.e("Settings", e.toString());
            //this.loadStringConfig();
        }
        return hashMap;
    }

    private long checkAssetFiles() {
        // /data/user/0/net.mikespub.mywebview/files
        // File filesdir = getFilesDir();
        // Log.d("Internal Files Dir", filesdir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files/Documents
        // File extdocsdir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        // Log.d("External Docs Dir", extdocsdir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files
        // File extfilesdir = getExternalFilesDir(null);
        // Log.d("External Files Dir", extfilesdir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files/web
        File extwebdir = new File(this.activity.getExternalFilesDir(null), "web");
        Log.d("External Web Dir", extwebdir.getAbsolutePath());
        // https://stackoverflow.com/questions/5248094/is-it-possible-to-get-last-modified-date-from-an-assets-file - using shared preferences in the end
        // See also https://stackoverflow.com/questions/37953002/mess-with-the-shared-preferences-of-android-which-function-to-use/37953072 for preferences
        long lastUpdated = 0;
        if (!extwebdir.exists()) {
            if (!extwebdir.mkdirs()) {
                Log.d("External Web Dir", Boolean.toString(extwebdir.exists()));
                return lastUpdated;
            }
            Log.d("External Web Dir", Boolean.toString(extwebdir.exists()));
        } else {
            try {
                PackageManager pm = this.activity.getPackageManager();
                PackageInfo appInfo = pm.getPackageInfo(this.activity.getPackageName(), 0);
                lastUpdated = appInfo.lastUpdateTime;
                Log.d("Web Package", String.valueOf(lastUpdated));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("Web Package", e.toString());
            }
        }
        copyAssetFiles(extwebdir, lastUpdated);
        return lastUpdated;
    }

    // https://stackoverflow.com/questions/4447477/how-to-copy-files-from-assets-folder-to-sdcard
    private void copyAssetFiles(File extwebdir, long lastUpdated) {
        AssetManager manager = this.activity.getAssets();
        String[] files;
        try {
            files = manager.list("web");
            Log.d("Web Files", Arrays.toString(files));
        } catch (IOException e) {
            Log.e("Web Files", e.toString());
            return;
        }
        for (String f: files) {
            File extfile = new File(extwebdir, f);
            if (!extfile.exists() || lastUpdated > extfile.lastModified()) {
                Log.d("Web File Missing", extfile.getAbsolutePath());
                try (InputStream in = manager.open("web/" + f); OutputStream out = new FileOutputStream(extfile)) {
                    copyFile(in, out);
                } catch (IOException e) {
                    Log.e("Web File", e.toString());
                    break;
                }
                // NOOP
                // NOOP
                Log.d("Web File Copied", extfile.getAbsolutePath());
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    // See https://stackoverflow.com/questions/3382996/how-to-unzip-files-programmatically-in-android
    public static void unzipFile(File zipFile, File targetDirectory) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(zipFile)) {
            unzipStream(inputStream, targetDirectory);
        }
    }

    static void unzipStream(InputStream inputStream, File targetDirectory) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(inputStream))) {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                // don't overwrite local settings
                String name = ze.getName();
                if (name.equals("web/settings.json")) {
                    Log.d("Web Update Unzip", name + " skip settings.json");
                    continue;
                }
                File file = new File(targetDirectory, name);
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory()) {
                    Log.d("Web Update Unzip", name + " skip directory");
                    continue;
                }
                long time = ze.getTime();
                if (file.exists() && (time > 0) && (time < file.lastModified())) {
                    Log.d("Web Update Unzip", name + " skip newer file");
                    continue;
                }
                Log.d("Web Update Unzip", name + " update");
                try (FileOutputStream fout = new FileOutputStream(file)) {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                }
                /* if time should be restored as well */
                if (time > 0) {
                    file.setLastModified(time);
                }
            }
        }
    }

    private String getJsonSettings(String filename) {
        File extwebfile = new File(this.activity.getExternalFilesDir(null), filename);
        if (extwebfile.exists()) {
            try {
                Writer writer = new StringWriter();
                try (InputStream input = new FileInputStream(extwebfile)) {
                    char[] buffer = new char[2048];
                    Reader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                }
                return writer.toString();
            } catch (IOException e) {
                Log.e("getJsonSettings", e.toString());
            }
            return null;
        }
        AssetManager manager = this.activity.getAssets();
        try {
            Writer writer = new StringWriter();
            try (InputStream input = manager.open(filename)) {
                char[] buffer = new char[2048];
                Reader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            }
            return writer.toString();
        } catch (IOException e) {
            Log.e("getJsonSettings", e.toString());
        }
        return null;
    }

    HashMap<String, Object> parseQueryParameters(Uri uri) {
        // String query = uri.getQuery();
        List<String> titles = uri.getQueryParameters("title[]");
        List<String> urls = uri.getQueryParameters("url[]");
        List<String> match0 = uri.getQueryParameters("match0[]");
        List<String> match1 = uri.getQueryParameters("match1[]");
        List<String> match2 = uri.getQueryParameters("match2[]");
        List<String> skip0 = uri.getQueryParameters("skip0[]");
        List<String> skip1 = uri.getQueryParameters("skip1[]");
        List<String> skip2 = uri.getQueryParameters("skip2[]");
        String other = uri.getQueryParameter("other");
        String remote_debug = uri.getQueryParameter("remote_debug");
        String console_log = uri.getQueryParameter("console_log");
        String js_interface = uri.getQueryParameter("js_interface");
        String context_menu = uri.getQueryParameter("context_menu");
        String not_matching = uri.getQueryParameter("not_matching");
        String update_zip = uri.getQueryParameter("update_zip");
        HashMap<String, Object> hashMap = new HashMap<>();
        List<HashMap<String, String>> sites = new ArrayList<>();
        for (int i = 0; i < titles.size(); i++) {
            if (titles.get(i).equals("") || urls.get(i).equals("")) {
                continue;
            }
            HashMap<String, String> site = new HashMap<>();
            site.put("url", urls.get(i));
            // Warning: Prior to Jelly Bean, this decoded the '+' character as '+' rather than ' '.
            site.put("title", titles.get(i).replace("+", " "));
            sites.add(site);
        }
        hashMap.put("sites", sites);
        hashMap.put("other", other);
        List<List<String>> matches = new ArrayList<>();
        for (int i = 0; i < match0.size(); i++) {
            if (match0.get(i).equals("") || match1.get(i).equals("") || match2.get(i).equals("")) {
                continue;
            }
            List<String> match = new ArrayList<>();
            match.add(match0.get(i));
            match.add(match1.get(i));
            match.add(match2.get(i));
            matches.add(match);
        }
        hashMap.put("match", matches);
        List<List<String>> skips = new ArrayList<>();
        for (int i = 0; i < skip0.size(); i++) {
            if (skip0.get(i).equals("") || skip1.get(i).equals("") || skip2.get(i).equals("")) {
                continue;
            }
            List<String> skip = new ArrayList<>();
            skip.add(skip0.get(i));
            skip.add(skip1.get(i));
            skip.add(skip2.get(i));
            skips.add(skip);
        }
        hashMap.put("skip", skips);
        if (remote_debug != null && remote_debug.equals("true")) {
            hashMap.put("remote_debug", true);
        } else {
            hashMap.put("remote_debug", false);
        }
        if (console_log != null && console_log.equals("true")) {
            hashMap.put("console_log", true);
        } else {
            hashMap.put("console_log", false);
        }
        if (js_interface != null && js_interface.equals("true")) {
            hashMap.put("js_interface", true);
        } else {
            hashMap.put("js_interface", false);
        }
        if (context_menu != null && context_menu.equals("true")) {
            hashMap.put("context_menu", true);
        } else {
            hashMap.put("context_menu", false);
        }
        if (not_matching != null && not_matching.equals("true")) {
            hashMap.put("not_matching", true);
        } else {
            hashMap.put("not_matching", false);
        }
        hashMap.put("update_zip", update_zip);
        hashMap.put("source", "updated via webview");
        hashMap.put("timestamp", getTimestamp(0));
        return hashMap;
    }

    String saveJsonSettings(HashMap<String, Object> hashMap) {
        hashMap.put("timestamp", getTimestamp(0));
        Log.d("Web Settings", hashMap.toString());
        JSONObject jsonObject=new JSONObject(hashMap);
        String jsonString = "";
        // https://stackoverflow.com/questions/16563579/jsonobject-tostring-how-not-to-escape-slashes
        try {
            jsonString = jsonObject.toString(2).replace("\\","");
            //Log.d("Web Settings", jsonString);
            File extwebfile = new File(this.activity.getExternalFilesDir(null), "web/settings.json");
            // https://stackoverflow.com/questions/11371154/outputstreamwriter-vs-filewriter/11371322
            try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(extwebfile), StandardCharsets.UTF_8)) {
                osw.write(jsonString);
            } catch (IOException e) {
                Log.e("Web Settings", e.toString());
            }
        } catch (JSONException e) {
            Log.e("Web Settings", e.toString());
        }
        return jsonString;
    }
}
