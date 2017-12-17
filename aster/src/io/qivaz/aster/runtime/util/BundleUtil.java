package io.qivaz.aster.runtime.util;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.bundle.BundleManager;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/6.
 */
public class BundleUtil {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleUtil";

    public static void installAssetsBundles(Context context) {
        generateAndInstallAssetsBundles(context, null, BundleFeature.BUNDLE_TEMP_FOLDER, true);
    }

    public static void installAssetsBundles(Context context, String[] bundleFileNames) {
        generateAndInstallAssetsBundles(context, bundleFileNames, BundleFeature.BUNDLE_TEMP_FOLDER, true);
    }

    public static void generateAndInstallAssetsBundles(Context context, String[] bundleFileNames, String destPath, boolean deleteApkAfterInstalled) {
        AssetManager assetManager = context.getAssets();
        String[] bundleFiles = null;
        try {
            bundleFiles = assetManager.list(BundleFeature.BUNDLE_ASSET_FOLDER_NAME);
        } catch (IOException e) {
            LogUtil.e(TAG, "generateAndInstallAssetsBundles(), failed, e=" + e);
        }

        if (bundleFiles != null) {
            for (String bundleName : bundleFiles) {
                if (!bundleName.endsWith(".apk")
                        || (bundleFileNames != null
                        && Arrays.binarySearch(bundleFileNames, bundleName) < 0)) {
                    LogUtil.e(TAG, "generateAndInstallAssetsBundles(), excluded \"" + bundleName + "\"");
                    continue;
                }
                generateBundleFromAssets(context, destPath, bundleName);
                String fullPath = destPath + File.separator + bundleName;
                if (!BundleManager.getInstance().installBundle(fullPath, deleteApkAfterInstalled)) {
                    LogUtil.e(TAG, "generateAndInstallAssetsBundles(), install \"" + fullPath + "\" failed");
                }
            }
        }
    }

    public static void generateAssetsBundles(Context application, String destPath) {
        AssetManager assetManager = application.getAssets();
        String[] bundleFiles = null;
        try {
            bundleFiles = assetManager.list(BundleFeature.BUNDLE_ASSET_FOLDER_NAME);
        } catch (IOException e) {
            LogUtil.e(TAG, "generateAssetsBundles(), failed, e=" + e);
        }

        if (bundleFiles != null) {
            ArrayList list = new ArrayList();
            String[] astring = bundleFiles;
            list.add(astring);
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                String name = (String) iterator.next();
                generateBundleFromAssets(application, destPath, name);
            }
        }
    }

    public static void generateBundleFromAssets(Context application, String path, String bundleName) {
        InputStream in = null;
        File file = null;
        OutputStream out = null;
        try {
            in = application.getAssets().open(BundleFeature.BUNDLE_ASSET_FOLDER_NAME + File.separator + bundleName);

            file = new File(path + File.separator + bundleName);
            out = new FileOutputStream(file);
            IOUtils.copyStream(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void copyBundle(String origFullPath, String destPath, String bundleName) {
        if (!origFullPath.endsWith(".apk")) {
            return;
        }
        if (!destPath.endsWith(File.separator)) {
            destPath += File.separator;
        }
        IOUtils.copy(origFullPath, destPath + bundleName);
    }

    public static void copyBundle(String origFullPath, String destFullPath) {
        if (!origFullPath.endsWith(".apk")) {
            return;
        }
        if (!destFullPath.endsWith(".apk")) {
            return;
        }
        File destFile = new File(destFullPath);
        if (destFile.exists()) {
            destFile.delete();
        }
        IOUtils.copy(origFullPath, destFullPath);
    }

    public static String copyBundleSOLib(String bundlePath, String packageName) {
        String cpuName = SoLibUtils.getCpuName();
        cpuName = SoLibUtils.getSupposedCpuName(bundlePath, cpuName);
        String targetNativeLibPath = BundleFeature.getBundleLibFolder(packageName);
        File targetNativeLibPathFile = new File(targetNativeLibPath);
        if (targetNativeLibPathFile.exists()) {
            if (!targetNativeLibPathFile.delete()) {
                LogUtil.e(TAG, "copyBundleSOLib(), delete1 \"" + targetNativeLibPath + "\" failed!");
            }
        }
        if (!targetNativeLibPathFile.mkdirs()) {
            LogUtil.e(TAG, "copyBundleSOLib(), mkdirs \"" + targetNativeLibPath + "\" failed!");
        }
        boolean hasSo = SoLibUtils.copyBundleSoLib(bundlePath, targetNativeLibPath, cpuName);
        if (!hasSo) {
            if (!targetNativeLibPathFile.delete()) {
                LogUtil.e(TAG, "copyBundleSOLib(), delete2 \"" + targetNativeLibPath + "\" failed!");
            }
        }
        if (debug) {
            LogUtil.v(TAG, "copyBundleSOLib(), hasSo=" + hasSo + ", bundlePath=" + bundlePath);
        }
        return hasSo ? targetNativeLibPath : "";
    }
}
