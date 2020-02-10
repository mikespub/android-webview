package net.mikespub.myutils;

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
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Asset Utility Methods
 */
public class MyAssetUtility {
    private static final String TAG = "Asset";

    /**
     * Check that all web asset files have been copied to the external web directory,
     * and copy/update them if needed.
     *
     * @param activity  current Activity context
     * @return          last update time of the current package
     */
    public static long checkAssetFiles(AppCompatActivity activity, String fileName) {
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
        copyAssetFile(activity, fileName, 0);
        File extWebDir = new File(activity.getExternalFilesDir(null), "web");
        Log.d(TAG, "External Dir: " + extWebDir.getAbsolutePath());
        // https://stackoverflow.com/questions/5248094/is-it-possible-to-get-last-modified-date-from-an-assets-file - using shared preferences in the end
        // See also https://stackoverflow.com/questions/37953002/mess-with-the-shared-preferences-of-android-which-function-to-use/37953072 for preferences
        long lastUpdated = 0;
        if (!extWebDir.exists()) {
            if (!extWebDir.mkdirs()) {
                Log.d(TAG, "External Dir exists: " + extWebDir.exists());
                return lastUpdated;
            }
            Log.d(TAG, "External Dir exists: " + extWebDir.exists());
        } else {
            try {
                PackageManager pm = activity.getPackageManager();
                PackageInfo appInfo = pm.getPackageInfo(activity.getPackageName(), 0);
                lastUpdated = appInfo.lastUpdateTime;
                Log.d(TAG, "Package Updated: " + lastUpdated);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package Unknown", e);
            }
        }
        copyWebAssetFiles(activity, extWebDir, lastUpdated);
        // copy _local asset files
        return lastUpdated;
    }

    /**
     * Copy an asset file to an external directory if needed
     *
     * @param activity  current Activity context
     * @param fileName  name of the asset file
     * @param lastUpdated   last update time needed
     */
    static void copyAssetFile(AppCompatActivity activity, String fileName, long lastUpdated) {
        File extFile = new File(activity.getExternalFilesDir(null), fileName);
        Log.d(TAG, "External File: " + fileName);
        if (extFile.exists() && lastUpdated <= extFile.lastModified()) {
            Log.d(TAG, "File Exists: " + extFile.getAbsolutePath());
            return;
        }
        File extDir = extFile.getParentFile();
        if (!extDir.isDirectory() && !extDir.mkdirs()) {
            Log.e(TAG, "Dir Create: FAIL " + extDir.getAbsolutePath());
            return;
        }
        AssetManager manager = activity.getAssets();
        try (InputStream in = manager.open(fileName); OutputStream out = new FileOutputStream(extFile)) {
            copyFileStream(in, out);
            Log.d(TAG, "File Copied: " + extFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "File Error: " + extFile.getAbsolutePath(), e);
        }
    }

    /**
     * Copy all web asset files to an external web directory. External web files that were modified
     * after this package was last updated will not be overwritten.
     *
     * @param activity      current Activity context
     * @param extWebDir     external web directory to copy the asset files to
     * @param lastUpdated   last update time of the current package
     */
    // https://stackoverflow.com/questions/4447477/how-to-copy-files-from-assets-folder-to-sdcard
    static void copyWebAssetFiles(AppCompatActivity activity, File extWebDir, long lastUpdated) {
        AssetManager manager = activity.getAssets();
        String[] files;
        try {
            files = manager.list("web");
            Log.d(TAG, "Files: " + Arrays.toString(files));
        } catch (IOException e) {
            Log.e(TAG, "Files Error", e);
            return;
        }
        for (String f: files) {
            File extFile = new File(extWebDir, f);
            if (extFile.exists() && lastUpdated <= extFile.lastModified()) {
                continue;
            }
            Log.d(TAG, "File Missing:" + extFile.getAbsolutePath());
            try (InputStream in = manager.open("web/" + f); OutputStream out = new FileOutputStream(extFile)) {
                copyFileStream(in, out);
            } catch (IOException e) {
                Log.e(TAG, "File Error:" + extFile.getAbsolutePath(), e);
                break;
            }
            Log.d(TAG, "File Copied:" + extFile.getAbsolutePath());
        }
    }

    /**
     * @param in    input stream
     * @param out   output stream
     * @throws IOException  trouble with IO
     */
    private static void copyFileStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    /**
     * @param zipFile           zip file to extract from
     * @param targetDirectory   target directory
     * @throws IOException      trouble with IO
     */
    // See https://stackoverflow.com/questions/3382996/how-to-unzip-files-programmatically-in-android
    public static void unzipFile(File zipFile, File targetDirectory, String[] skipNames) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(zipFile)) {
            unzipStream(inputStream, targetDirectory, skipNames);
        }
    }

    /**
     * @param inputStream       input stream of zip file
     * @param targetDirectory   target directory
     * @throws IOException      trouble with IO
     */
    public static void unzipStream(InputStream inputStream, File targetDirectory, String[] skipNames) throws IOException {
        //https://stackoverflow.com/questions/1128723/how-do-i-determine-whether-an-array-contains-a-particular-value-in-java
        List<String> skipList = Arrays.asList(skipNames);
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(inputStream))) {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                // don't overwrite local settings
                String name = ze.getName();
                if (skipList.contains(name)) {
                    Log.d(TAG, "Unzip: " + name + " skip names");
                    continue;
                }
                File file = new File(targetDirectory, name);
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory()) {
                    Log.d(TAG, "Unzip: " + name + " skip directory");
                    continue;
                }
                long time = ze.getTime();
                if (file.exists() && (time > 0) && (time <= file.lastModified())) {
                    Log.d(TAG, "Unzip: " + name + " skip current file");
                    continue;
                }
                Log.d(TAG, "Unzip: " + name + " updated");
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

    /**
     * @param activity  current Activity context
     * @param fileName  name of the external file to get
     * @return          string content of the file
     * @throws IOException  trouble with IO
     */
    public static String getFilenameString(AppCompatActivity activity, String fileName) throws IOException {
        File extWebFile = new File(activity.getExternalFilesDir(null), fileName);
        String result = "";
        if (extWebFile.exists()) {
            try (InputStream input = new FileInputStream(extWebFile)) {
                try {
                    result = getInputstreamString(input);
                } catch (IOException e) {
                    Log.e(TAG, "Get External File: " + extWebFile.getAbsolutePath(), e);
                }
            }
        } else {
            AssetManager manager = activity.getAssets();
            try (InputStream input = manager.open(fileName)) {
                try {
                    result = getInputstreamString(input);
                } catch (IOException e) {
                    Log.e(TAG, "Get Asset File: " + fileName, e);
                }
            }
        }
        return result;
    }

    /**
     * @param inputStream   input stream to read content from
     * @return              string content of the input stream
     * @throws IOException  trouble with IO
     */
    private static String getInputstreamString(InputStream inputStream) throws IOException {
        Writer writer = new StringWriter();
        char[] buffer = new char[8192];
        Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        int n;
        while ((n = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, n);
        }
        return writer.toString();
    }

    /**
     * @param activity  current Activity context
     * @param fileName  name of the external file to save to
     * @param content   string content to save
     */
    public static void saveFilenameString(AppCompatActivity activity, String fileName, String content) {
        File extWebFile = new File(activity.getExternalFilesDir(null), fileName);
        // https://stackoverflow.com/questions/11371154/outputstreamwriter-vs-filewriter/11371322
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(extWebFile), StandardCharsets.UTF_8)) {
            osw.write(content);
        } catch (IOException e) {
            Log.e(TAG, "Save External File: " + extWebFile.getAbsolutePath(), e);
        }
    }

    /**
     * Use a template file with values to replace and return its content
     *
     * @param activity  current Activity context
     * @param fileName  name of the external template file to use
     * @param valuesMap values to replace in the template file
     * @return          content of the template with replaced values
     */
    public static String getTemplateFile(AppCompatActivity activity, String fileName, Map<String, String> valuesMap) {
        String template;
        try {
            template = MyAssetUtility.getFilenameString(activity, fileName);
        } catch (IOException e) {
            template = e.toString();
        }
        if (valuesMap == null) {
            return template;
        }
        //StringSubstitutor sub = new StringSubstitutor(valuesMap);
        for (String key: valuesMap.keySet()) {
            template = template.replace("${" + key + "}", valuesMap.get(key));
        }
        return template;
    }
}
