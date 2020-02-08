package net.mikespub.mywebview;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

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
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class MyAssetUtility {
    private static final String TAG = "Asset";

    static long checkAssetFiles(AppCompatActivity activity) {
        // /data/user/0/net.mikespub.mywebview/files
        // File filesDir = getFilesDir();
        // Log.d("Internal Files Dir", filesDir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files/Documents
        // File extDocsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        // Log.d("External Docs Dir", extDocsDir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files
        // File extFilesDir = getExternalFilesDir(null);
        // Log.d("External Files Dir", extFilesDir.getAbsolutePath());
        // /storage/emulated/0/Android/data/net.mikespub.mywebview/files/web
        File extWebDir = new File(activity.getExternalFilesDir(null), "web");
        Log.d("External Web Dir", extWebDir.getAbsolutePath());
        // https://stackoverflow.com/questions/5248094/is-it-possible-to-get-last-modified-date-from-an-assets-file - using shared preferences in the end
        // See also https://stackoverflow.com/questions/37953002/mess-with-the-shared-preferences-of-android-which-function-to-use/37953072 for preferences
        long lastUpdated = 0;
        if (!extWebDir.exists()) {
            if (!extWebDir.mkdirs()) {
                Log.d("External Web Dir", Boolean.toString(extWebDir.exists()));
                return lastUpdated;
            }
            Log.d("External Web Dir", Boolean.toString(extWebDir.exists()));
        } else {
            try {
                PackageManager pm = activity.getPackageManager();
                PackageInfo appInfo = pm.getPackageInfo(activity.getPackageName(), 0);
                lastUpdated = appInfo.lastUpdateTime;
                Log.d("Web Package", String.valueOf(lastUpdated));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("Web Package", e.toString());
            }
        }
        copyAssetFiles(activity, extWebDir, lastUpdated);
        return lastUpdated;
    }

    // https://stackoverflow.com/questions/4447477/how-to-copy-files-from-assets-folder-to-sdcard
    static void copyAssetFiles(AppCompatActivity activity, File extWebDir, long lastUpdated) {
        AssetManager manager = activity.getAssets();
        String[] files;
        try {
            files = manager.list("web");
            Log.d("Web Files", Arrays.toString(files));
        } catch (IOException e) {
            Log.e("Web Files", e.toString());
            return;
        }
        for (String f: files) {
            File extFile = new File(extWebDir, f);
            if (!extFile.exists() || lastUpdated > extFile.lastModified()) {
                Log.d("Web File Missing", extFile.getAbsolutePath());
                try (InputStream in = manager.open("web/" + f); OutputStream out = new FileOutputStream(extFile)) {
                    copyFile(in, out);
                } catch (IOException e) {
                    Log.e("Web File", e.toString());
                    break;
                }
                // NOOP
                // NOOP
                Log.d("Web File Copied", extFile.getAbsolutePath());
            }
        }
    }

    static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    // See https://stackoverflow.com/questions/3382996/how-to-unzip-files-programmatically-in-android
    static void unzipFile(File zipFile, File targetDirectory) throws IOException {
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
                if (name.equals(MySettingsRepository.fileName)) {
                    Log.d("Web Update Unzip", name + " skip settings");
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

    static String getFilenameString(AppCompatActivity activity, String fileName) throws IOException {
        File extWebFile = new File(activity.getExternalFilesDir(null), fileName);
        String result = "";
        if (extWebFile.exists()) {
            try (InputStream input = new FileInputStream(extWebFile)) {
                try {
                    result = getInputstreamString(input);
                } catch (IOException e) {
                    Log.e("getFilenameString", "External: " + e.toString());
                }
            }
        } else {
            AssetManager manager = activity.getAssets();
            try (InputStream input = manager.open(fileName)) {
                try {
                    result = getInputstreamString(input);
                } catch (IOException e) {
                    Log.e("getFilenameString", "Asset: " + e.toString());
                }
            }
        }
        return result;
    }

    static String getInputstreamString(InputStream inputStream) throws IOException {
        Writer writer = new StringWriter();
        char[] buffer = new char[8192];
        Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        int n;
        while ((n = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, n);
        }
        return writer.toString();
    }

    static void saveFilenameString(AppCompatActivity activity, String fileName, String content) {
        File extWebFile = new File(activity.getExternalFilesDir(null), fileName);
        // https://stackoverflow.com/questions/11371154/outputstreamwriter-vs-filewriter/11371322
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(extWebFile), StandardCharsets.UTF_8)) {
            osw.write(content);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
}
