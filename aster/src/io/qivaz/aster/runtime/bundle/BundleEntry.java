package io.qivaz.aster.runtime.bundle;

import android.app.Application;
import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;
import io.qivaz.aster.runtime.CanNotLaunchBundleAppException;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class BundleEntry {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleEntry";

    public PackageInfo mPackageInfo;
    public String mBundlePath;
    public ArrayList<ResolveInfo> mActivityFilter;
    public ArrayList<ResolveInfo> mServiceFilter;
    public ArrayList<ResolveInfo> mReceiverFilter;
    public Map<String, ActivityInfo> mActivityInfo;
    public Map<String, ServiceInfo> mServiceInfo;
    public Map<String, ActivityInfo> mReceiverInfo;
    DexClassLoader mDexClassLoader;
    Resources mResources;
    AssetManager mAssetManager;
    String mPackageName;
    //    public Map<String, ProviderInfo> mProviderFilter;

    //    ArrayList<ResolveInfo> mProviderFilter;
//    Map<String, ProviderInfo> mProviderInfo;
    ArrayList<ProviderInfo> mProviderInfo;

    Application mApp;
    ApplicationInfo mAppInfo;
    Bundle mAppMetaData;
    private Signature[] mSignatures;

    BundleEntry(String apkFullPath, PackageInfo packageInfo, DexClassLoader dexClassLoader, AssetManager assetManager, Resources resources, Bundle metaData) {
        mPackageInfo = packageInfo;
        mDexClassLoader = dexClassLoader;
        mAssetManager = assetManager;
        mResources = resources;
        mPackageName = packageInfo.packageName;
        mBundlePath = apkFullPath;
        mSignatures = packageInfo.signatures;
        mAppMetaData = metaData;

        mActivityFilter = new ArrayList<>();
        mServiceFilter = new ArrayList<>();
        mReceiverFilter = new ArrayList<>();
//        mProviderFilter = new ArrayList<>();

        mActivityInfo = new HashMap<>();
        mServiceInfo = new HashMap<>();
        mReceiverInfo = new HashMap<>();
//        mProviderInfo = new HashMap<>();
        mProviderInfo = new ArrayList<>();
    }

    public ApplicationInfo createApplicationInfo() {
        if (mAppInfo == null) {
            mAppInfo = mPackageInfo.applicationInfo;
            ApplicationInfo hostApplicationInfo;
            try {
                hostApplicationInfo = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo();
            } catch (IllegalAccessException e) {
                LogUtil.e("BundleEntry", "createApplicationInfo() failed! " + e);
                e.printStackTrace();
                return null;
            }
            mAppInfo.uid = hostApplicationInfo.uid;
            mAppInfo.processName = hostApplicationInfo.processName;
            mAppInfo.publicSourceDir = hostApplicationInfo.publicSourceDir;
//            if (mDexClassLoader instanceof BundleClassLoader) {
            mAppInfo.nativeLibraryDir = ((BundleClassLoader) mDexClassLoader).getLibPath();
//            } else {
//                mAppInfo.nativeLibraryDir = hostApplicationInfo.nativeLibraryDir;
//            }

            mAppInfo.sourceDir = mBundlePath;
            mAppInfo.publicSourceDir = mAppInfo.sourceDir;
            File dataDir = new File(BundleFeature.BUNDLE_DATA_FOLDER, mPackageInfo.packageName);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            mAppInfo.dataDir = dataDir.getAbsolutePath();
//            /** @removed */
//            mAppInfo.credentialEncryptedDataDir = mAppInfo.dataDir;
//            /** @SystemApi */
//            mAppInfo.credentialProtectedDataDir = mAppInfo.dataDir;
//            /** @removed */
//            mAppInfo.deviceEncryptedDataDir = mAppInfo.dataDir;

//            mAppInfo.deviceProtectedDataDir = mAppInfo.dataDir;
        }

        return mAppInfo;
    }

    Application launchApplication(Application application) throws CanNotLaunchBundleAppException {
        try {
            synchronized (this) {
                if (mApp != null) {
                    LogUtil.w(TAG, "launchApplication(), have already launched application for <" + mPackageName + ">");
                    return mApp;
                }

                LogUtil.w(TAG, "launchApplication(), start launch application for <" + mPackageName + ">");

                Resources.Theme newTheme;
                int appTheme;
                if (mPackageInfo.applicationInfo.theme != 0) {
                    appTheme = mPackageInfo.applicationInfo.theme;
                } else {
                    appTheme = BundleFeature.getDefaultTheme();
                }

                String className = mPackageInfo.applicationInfo.className;
                if (className == null) {
                    className = "android.app.Application";
                }
                Class<?> appClazz = mDexClassLoader.loadClass(className);
                BundleAppContext contextWrapper = new BundleAppContext(application, this, appClazz);
                contextWrapper.setTheme(appTheme);
                Application bundleApp = Instrumentation.newApplication(appClazz, contextWrapper);
                bundleApp.onCreate();
                mApp = bundleApp;
//            contextWrapper.attachContextImpl();

                BundleManager.getInstance().installReceivers(this);
//            BundleManager.getInstance().installProviders(bundleEntry);

                LogUtil.w(TAG, "launchApplication(), finished launch application <" + mPackageName + "/" + className + "> successful!");
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "launchApplication(), failed, " + e);
            e.printStackTrace();
            throw new CanNotLaunchBundleAppException("Unable to launch bundle application for " + mPackageName + ", " + e.toString());
        }

        return mApp;
    }

    @Override
    public String toString() {
        return "BundleEntry{" + mPackageInfo + ", " + mBundlePath + "}";
    }
}
