package net.mikespub.myutils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Environment;
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
    public static long checkAssetFiles(AppCompatActivity activity, String fileName, String dirName) {
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
        long lastUpdated = 0;
        if (fileName != null) {
            File extFile = new File(activity.getExternalFilesDir(null), fileName);
            copyAssetFile(activity, fileName, extFile, lastUpdated);
        }
        // https://stackoverflow.com/questions/5248094/is-it-possible-to-get-last-modified-date-from-an-assets-file - using shared preferences in the end
        // See also https://stackoverflow.com/questions/37953002/mess-with-the-shared-preferences-of-android-which-function-to-use/37953072 for preferences
        try {
            PackageManager pm = activity.getPackageManager();
            PackageInfo appInfo = pm.getPackageInfo(activity.getPackageName(), 0);
            lastUpdated = appInfo.lastUpdateTime;
            Log.d(TAG, "Package Updated: " + lastUpdated);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package Unknown", e);
        }
        if (dirName != null) {
            File extDir = new File(activity.getExternalFilesDir(null), dirName);
            lastUpdated = copyAssetDir(activity, dirName, extDir, lastUpdated);
        }
        // copy local asset files
        return lastUpdated;
    }

    /**
     * Copy an asset file to an external directory if needed
     *
     * @param activity  current Activity context
     * @param fileName  name of the asset file
     * @param targetFile    target file
     * @param lastUpdated   last update time needed
     */
    public static void copyAssetFile(AppCompatActivity activity, String fileName, File targetFile, long lastUpdated) {
        Log.d(TAG, "Target File: " + targetFile.getAbsolutePath());
        if (targetFile.exists() && lastUpdated <= targetFile.lastModified()) {
            Log.d(TAG, "File Exists: " + targetFile.getAbsolutePath());
            return;
        }
        File extDir = targetFile.getParentFile();
        if (!extDir.isDirectory() && !extDir.mkdirs()) {
            Log.e(TAG, "Dir Create: FAIL " + extDir.getAbsolutePath());
            return;
        }
        AssetManager manager = activity.getAssets();
        try (InputStream in = manager.open(fileName); OutputStream out = new FileOutputStream(targetFile)) {
            copyFileStream(in, out);
            Log.d(TAG, "File Copied: " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "File Error: " + targetFile.getAbsolutePath(), e);
        }
    }

    /**
     * Copy all asset files to an external directory. External files that were modified
     * after this package was last updated will not be overwritten.
     *
     * @param activity      current Activity context
     * @param dirName       directory to copy the asset files from
     * @param targetDir     target directory
     * @param lastUpdated   last update time of the current package
     * @return              last update time
     */
    // https://stackoverflow.com/questions/4447477/how-to-copy-files-from-assets-folder-to-sdcard
    public static long copyAssetDir(AppCompatActivity activity, String dirName, File targetDir, long lastUpdated) {
        Log.d(TAG, "Target Dir: " + targetDir.getAbsolutePath());
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                Log.d(TAG, "Target Dir exists: " + targetDir.exists());
                return 0;
            }
            Log.d(TAG, "Target Dir exists: " + targetDir.exists());
        } else if (!targetDir.isDirectory()) {
            Log.d(TAG, "Target Dir is File");
            return 0;
        }
        AssetManager manager = activity.getAssets();
        String[] files;
        try {
            files = manager.list(dirName);
            Log.d(TAG, "Files: " + Arrays.toString(files));
        } catch (IOException e) {
            Log.e(TAG, "Files Error", e);
            return 0;
        }
        for (String f: files) {
            File targetFile = new File(targetDir, f);
            String fileName = dirName + "/" + f;
            if (targetFile.exists()) {
                if (lastUpdated <= targetFile.lastModified()) {
                    continue;
                }
                Log.d(TAG, "File Update: " + fileName);
            } else {
                Log.d(TAG, "File Missing: " + fileName);
            }
            try (InputStream in = manager.open(fileName); OutputStream out = new FileOutputStream(targetFile)) {
                copyFileStream(in, out);
            } catch (FileNotFoundException e) {
                try {
                    if (manager.list(fileName).length > 0) {
                        if (targetFile.exists() && !targetFile.isDirectory()) {
                            targetFile.delete();
                        }
                        copyAssetDir(activity, fileName, targetFile, lastUpdated);
                    }
                } catch (IOException x){
                    Log.e(TAG, "File Error: " + targetFile.getAbsolutePath(), x);
                    return 0;
                }
            } catch (IOException e) {
                Log.e(TAG, "File Error: " + targetFile.getAbsolutePath(), e);
                return 0;
            }
            Log.d(TAG, "File Copied: " + targetFile.getAbsolutePath());
        }
        return lastUpdated;
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
        String canonicalDir = targetDirectory.getCanonicalPath();
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
                String canonicalFile = file.getCanonicalPath();
                if (!canonicalFile.startsWith(canonicalDir)) {
                    Log.d(TAG, "Unzip: " + name + " invalid filename");
                    continue;
                }
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
            template = getFilenameString(activity, fileName);
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

    /**
     * Show external files for this app
     *
     * @param activity  current Activity context
     */
    public static void showMyExternalFiles(AppCompatActivity activity, String dirName, Boolean recursive) {
        File extDir = activity.getExternalFilesDir(dirName);
        Log.d(TAG, "External Dir: " + extDir.getAbsolutePath());
        for (File file: extDir.listFiles()) {
            if (file.isDirectory()) {
                if (recursive) {
                    showMyExternalFiles(activity, dirName + "/" + file.getName(), recursive);
                } else {
                    Log.d(TAG, "Dir: " + file.getAbsolutePath());
                }
            } else {
                Log.d(TAG, "File: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Save app icon to media
     *
     * @param activity  current Activity context
     */
    public static void saveIconToMedia(AppCompatActivity activity) {
        Bitmap bitmap = null;
        try {
            PackageManager pm = activity.getPackageManager();
            Drawable icon = pm.getApplicationIcon(activity.getPackageName());
            // https://stackoverflow.com/questions/44447056/convert-adaptiveicondrawable-to-bitmap-in-android-o-preview/46018816#46018816
            if (icon instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) icon).getBitmap();
            //} else if (Build.VERSION.SDK_INT >= 26) {
            } else {
                Drawable backgroundDr = ((AdaptiveIconDrawable) icon).getBackground();
                Drawable foregroundDr = ((AdaptiveIconDrawable) icon).getForeground();

                Drawable[] drr = new Drawable[2];
                drr[0] = backgroundDr;
                drr[1] = foregroundDr;

                LayerDrawable layerDrawable = new LayerDrawable(drr);

                int width = layerDrawable.getIntrinsicWidth();
                int height = layerDrawable.getIntrinsicHeight();

                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);

                layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                layerDrawable.draw(canvas);
                //return bitmap;
            }
        } catch (Exception e) {
            Log.e(TAG, "Icon", e);
        }
        //Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
        if (bitmap != null) {
            Log.d(TAG, "Bitmap: " + bitmap);
            File extDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (!extDir.exists() && !extDir.mkdirs()) {
                Log.e(TAG, "Dir Create: FAIL " + extDir.getAbsolutePath());
                return;
            }
            File extFile = new File(extDir, "ic_launcher.png");
            try (OutputStream out = new FileOutputStream(extFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
            } catch (IOException e) {
                Log.e(TAG, "File Error: " + extFile.getAbsolutePath(), e);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Bitmap Error: " + extFile.getAbsolutePath(), e);
                return;
            }
            //insertImage(activity.getContentResolver(), extFile);
            //Uri contentUri = Uri.fromFile(extFile);
            //Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            //mediaScanIntent.setData(contentUri);
            //activity.sendBroadcast(mediaScanIntent);
            //MediaScannerConnection.scanFile(activity, new String[] {extFile.getAbsolutePath()}, new String[] {"image/png"}, new MediaScannerConnection.OnScanCompletedListener() {
            //    @Override
            //    public void onScanCompleted(String path, Uri uri) {
            //        Log.i("TAG", "Finished scanning " + path + " Uri: " + uri);
            //    }
            //});
            // https://proandroiddev.com/working-with-scoped-storage-8a7e7cafea3
            /*
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "ic_launcher.png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            //values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$bucketName/");
            Uri contentUri = MediaStore.Images.Media.getContentUri("external");
            Log.d(TAG, "Content URI: " + contentUri);
            Uri imageUri = activity.getContentResolver().insert(contentUri, values);
            Log.d(TAG, "Image URI: " + imageUri);
            try (OutputStream out = activity.getContentResolver().openOutputStream(imageUri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            } catch (Exception e) {
                Log.e(TAG, "Bitmap Error", e);
            }
             */
        } else {
            Log.d(TAG, "Bitmap: null");
        }
    }

    /*
    // https://android.googlesource.com/platform/packages/apps/DevCamera/+/refs/tags/android-7.1.1_r25/src/com/android/devcamera/MediaSaver.java
    // We use this instead of MediaStore.Images.Media.insertImage() because we want to add date metadata
    public static void insertImage(ContentResolver cr, File file) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, file.getName());
        values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
        values.put(MediaStore.Images.Media.DESCRIPTION, file.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        // Add the date meta data to ensure the image is added at the front of the gallery
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, file.lastModified());
        try {
            cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            Log.w(TAG, "Error updating media store for  " + file, e);
        }
    }
     */
}