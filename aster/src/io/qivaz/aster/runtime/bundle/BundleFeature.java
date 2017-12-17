package io.qivaz.aster.runtime.bundle;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.qivaz.aster.runtime.bundle.activity.BundleSingleInstanceActivityContainer;
import io.qivaz.aster.runtime.bundle.activity.BundleSingleTaskActivityContainer;
import io.qivaz.aster.runtime.bundle.activity.BundleSingleTopActivityContainer;
import io.qivaz.aster.runtime.bundle.service.BundleServiceContainer;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/6.
 */
public class BundleFeature {
    public static final boolean debug = false;
    public static final boolean debugPerf = true;

    public static final String HOST_ALIAS_NAME = "@HOST";

    public static final String BUNLDE_SERVICE_PREFIX = BundleServiceContainer.class.getName() + "$BundleService";
    public static final String BUNLDE_ACTIVITY_TOP_PREFIX = BundleSingleTopActivityContainer.class.getName() + "$BundleActivity";
    public static final String BUNLDE_ACTIVITY_TASK_PREFIX = BundleSingleTaskActivityContainer.class.getName() + "$BundleActivity";
    public static final String BUNLDE_ACTIVITY_INSTANCE_PREFIX = BundleSingleInstanceActivityContainer.class.getName() + "$BundleActivity";

    public static final int RUNTIME_DVK = 1;
    public static final int RUNTIME_ART = 2;

    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_PORTAL = "android.intent.action.PORTAL";
    public static final String ALIAS_NAME_KEY = "io.qivaz.aster.ALIAS_NAME";
    public static final String ASTER_CLASS_PREFIX = "io.qivaz.aster";
    public static final String BUNDLE_ROOT_FOLDER_NAME = ".bundle";
    public static final String BUNDLE_DEX_FOLDER_NAME = "dex";
    public static final String BUNDLE_DATA_FOLDER_NAME = "data";
    public static final String BUNDLE_APK_FOLDER_NAME = "apk";
    public static final String BUNDLE_MANIFEST_FOLDER_NAME = "manifest";
    public static final String BUNDLE_REG_FOLDER_NAME = "registry";
    public static final String BUNDLE_TEMP_FOLDER_NAME = ".tmp";
    public static final String BUNDLE_REG_FILE_NAME = "bundles.reg";
    public static final String BUNDLE_ACC_FILE_NAME = "bundles.acc";
    public static final String BUNDLE_CLEAR_FILE_NAME = "bundles.clr";
    public static final String BUNDLE_ASSET_FOLDER_NAME = "apk";
    private static final String TAG = "BundleFeature";
    public static String BUNDLE_ROOT_FOLDER;
    public static String BUNDLE_DEX_FOLDER;
    public static String BUNDLE_DATA_FOLDER;
    public static String BUNDLE_NATIVELIB_FOLDER;
    public static String BUNDLE_APK_FOLDER;
    public static String BUNDLE_MANIFEST_FOLDER;
    public static String BUNDLE_REG_FOLDER;
    public static String BUNDLE_REG_FILE;
    public static String BUNDLE_ACC_FILE;
    public static String BUNDLE_CLR_FILE;
    public static String BUNDLE_TEMP_FOLDER;

    private static boolean bundleAcceleratorEnabled = true;
    private static int mRuntime = -1;

    public static synchronized String getBundleDataFolder(String bundlePackageName) {
        return BundleFeature.BUNDLE_DATA_FOLDER + File.separator + bundlePackageName;
    }

    public static synchronized String getBundleLibFolder(String bundlePackageName) {
        return BundleFeature.BUNDLE_NATIVELIB_FOLDER + File.separator + bundlePackageName + File.separator + "lib";
    }

    public static synchronized String getBundleApkFile(String bundlePackageName, String factor) {
        if (isDalvikModeOrLowART()) {
            return BundleFeature.BUNDLE_APK_FOLDER + File.separator + bundlePackageName + "." + factor + ".apk";
        } else {
            return BundleFeature.BUNDLE_APK_FOLDER + File.separator + bundlePackageName + ".apk";
        }
    }

    public static synchronized String getBundleDexFile(String bundlePackageName, String factor) {
        if (isDalvikModeOrLowART()) {
            return BundleFeature.BUNDLE_DEX_FOLDER + File.separator + bundlePackageName + "." + factor + ".dex";
        } else {
            return BundleFeature.BUNDLE_DEX_FOLDER + File.separator + bundlePackageName + ".dex";
        }
    }

    public static synchronized String getBundleManifestFile(String bundlePackageName) {
        return BundleFeature.BUNDLE_MANIFEST_FOLDER + File.separator + bundlePackageName + ".xml";
    }

    public static synchronized String getBundleSignatureFile(String bundlePackageName) {
        return BundleFeature.BUNDLE_MANIFEST_FOLDER + File.separator + bundlePackageName + ".cert";
    }

    public static boolean getBundleAcceleratorEnabled() {
        return bundleAcceleratorEnabled;
    }

    public static void setBundleAcceleratorEnabled(boolean enabled) {
        bundleAcceleratorEnabled = enabled;
    }

    public static boolean isDalvikModeOrLowART() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
    }

    public static boolean isDalvikMode() {
        return RUNTIME_DVK == getRuntime();
    }

    private static int getRuntime() {
        if (mRuntime != -1) {
            return mRuntime;
        }
        try {
            final String version = getRuntimeVersion();
            if (!TextUtils.isEmpty(version)) {
                int number = Integer.parseInt(version.substring(0, 1));
                if (number >= 2) {
                    mRuntime = RUNTIME_ART;
                } else {
                    mRuntime = RUNTIME_DVK;
                }
            } else {
                final String LIB_DALVIK = "libdvm.so";
                final String LIB_ART = "libart.so";
                final String LIB_ART_D = "libartd.so";

                String value = getRuntimeLib();
                if (LIB_DALVIK.equals(value)) {
                    //Dalvik
                    mRuntime = RUNTIME_DVK;
                } else if (LIB_ART.equals(value)) {
                    //ART
                    mRuntime = RUNTIME_ART;
                } else if (LIB_ART_D.equals(value)) {
                    //ART debug build
                    mRuntime = RUNTIME_ART;
                } else {
                    // WTF?
                    mRuntime = RUNTIME_DVK;
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "getRuntime(), " + e);
            e.printStackTrace();
            mRuntime = RUNTIME_DVK;
        }
        return mRuntime;
    }

    private static String getRuntimeVersion() {
        final String version = System.getProperty("java.vm.version");
        LogUtil.w(TAG, "getRuntimeVersion(), java.vm.version=" + version);
        return version;
    }

    private static String getRuntimeLib() {
        final String SELECT_RUNTIME_PROPERTY = "persist.sys.dalvik.vm.lib";
        final String SELECT_RUNTIME_PROPERTY2 = "persist.sys.dalvik.vm.lib.2";
        try {
            Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
            try {
                Method SystemProperties$get = SystemProperties.getMethod("get", String.class, String.class);
                try {
                    String value = (String) SystemProperties$get.invoke(SystemProperties, SELECT_RUNTIME_PROPERTY, "NONE");
                    if ("NONE".equals(value)) {
                        LogUtil.e(TAG, "getRuntimeLib(), No property of " + SELECT_RUNTIME_PROPERTY);
                        value = (String) SystemProperties$get.invoke(SystemProperties, SELECT_RUNTIME_PROPERTY2, "Dalvik");
                    }
                    LogUtil.w(TAG, "getRuntimeLib(), vm.lib= " + value);
                    return value;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } catch (NoSuchMethodException e) {
                LogUtil.e(TAG, "getRuntimeLib(), SystemProperties.get() method is not found, " + e);
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "getRuntimeLib(), SystemProperties class is not found, " + e);
        }
        return null;
    }

    @SuppressLint("ObsoleteSdkInt")
    public static int getDefaultTheme() {
        int appTheme;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // 21 ~ 
            appTheme = 0x01030224; // Theme.Material
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) { // 14 ~ 20
            appTheme = 0x01030128; // Theme.DeviceDefault
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { // 11 ~ 13
            appTheme = 0x0103006b; // Theme.Holo
        } else {
            appTheme = 0x01030005; // Theme
        }
        return appTheme;
    }
}
