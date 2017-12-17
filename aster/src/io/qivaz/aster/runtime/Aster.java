package io.qivaz.aster.runtime;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.bundle.BundleManager;
import io.qivaz.aster.runtime.host.HostManager;
import io.qivaz.aster.runtime.util.BundleUtil;
import io.qivaz.aster.runtime.util.log.LogUtil;
import io.qivaz.aster.runtime.util.log.LogUtilImpl;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class Aster {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "Aster";
    private static Application mHostApp;
    private static Map<String, Object> mSharedObjs = new HashMap<>();

    public static void preInit(Application hostApp) {
        mHostApp = hostApp;
        HostManager.getInstance().preInit(hostApp);
    }

    public static void postInit(Application hostApp) {
        BundleManager.getInstance().init(hostApp);
        HostManager.getInstance().postInit(hostApp);
    }

    public static void init(Application hostApp) {
        LogUtil.setImpl(new LogUtilImpl(hostApp));

        mHostApp = hostApp;
        BundleManager.getInstance().init(hostApp);
        HostManager.getInstance().init(hostApp);
    }

    public static List<String> getInstalledBundles() {
        return BundleManager.getInstance().getInstalledBundles();
    }

    public static boolean isBundleInstalledAlias(String alias) throws NoAliasExistException {
        try {
            return BundleManager.getInstance().isBundleInstalledAlias(alias);
        } catch (NoAliasExistException e) {
            LogUtil.e(TAG, "isBundleInstalledAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        }
    }

    public static boolean isBundleInstalled(String packageName) {
        return BundleManager.getInstance().isBundleInstalled(packageName);
    }

    public static Context getHostApplicationContext() {
        return mHostApp;
    }

    public static Context getBundleApplicationContextAlias(String alias) {
        return BundleManager.getInstance().getBundleApplicationContextAlias(alias);
    }

    public static Context getBundleApplicationContext(String packageName) {
        return BundleManager.getInstance().getBundleApplicationContext(packageName);
    }

    public static String getAliasByPackageName(String packageName) {
        return BundleManager.getInstance().getAliasByPackageName(packageName);
    }

    public static String getPackageNameByAlias(String alias) {
        return BundleManager.getInstance().getPackageNameByAlias(alias);
    }

    public static void setUseMixedSP(boolean mix) {
        BundleManager.getInstance().setUseMixedSP(mix);
    }

    public static void setUseHostDelegate(boolean useHostDelegate) {
        HostManager.getInstance().setUseHostDelegate(useHostDelegate);
    }

    public static void setBundleAcceleratorEnabled(boolean enabled) {
        BundleFeature.setBundleAcceleratorEnabled(enabled);
    }

    public static Object fetchSharedObject(String key) {
        Object obj = mSharedObjs.get(key);
        mSharedObjs.remove(key);
        return obj;
    }

    public static Object removeSharedObject(String key) {
        return mSharedObjs.remove(key);
    }

    public static void putSharedObject(String key, Object val) {
        mSharedObjs.put(key, val);
    }

    public static void installAssetsBundles(Context context) {
        BundleUtil.installAssetsBundles(context);
    }

    public static void installAssetsBundles(Context context, String[] bundleFileNames) {
        BundleUtil.installAssetsBundles(context, bundleFileNames);
    }

    public static boolean installBundle(String fullApkPath, boolean deleteApkAfterSuccessfulInstall) {
        return BundleManager.getInstance().installBundle(fullApkPath, deleteApkAfterSuccessfulInstall);
    }

    public static boolean uninstallBundle(String bundleName) {
        return BundleManager.getInstance().uninstallBundle(bundleName);
    }

    public static boolean startActivity(Context context, Intent intent) {
        try {
            return BundleManager.getInstance().startActivity(context, intent);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean startActivityForResult(Context context, Intent intent, int requestCode) {
        try {
            return BundleManager.getInstance().startActivityForResult(context, intent, requestCode);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static ComponentName startService(Context context, Intent intent) {
        try {
            return BundleManager.getInstance().startService(context, intent);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean stopService(Context context, Intent intent) {
        try {
            return BundleManager.getInstance().stopService(context, intent);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean bindService(Context context, Intent intent, ServiceConnection conn, int flags) {
        try {
            return BundleManager.getInstance().bindService(context, intent, conn, flags);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void unbindService(Context context, ServiceConnection conn) {
        try {
            BundleManager.getInstance().unbindService(context, conn);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static Object constructAlias(String alias, String className, Object... args) throws ClassNotFoundException, CanNotLaunchBundleAppException, BundleNotInstalledException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoAliasExistException {
        try {
            return BundleManager.getInstance().constructAlias(alias, className, args);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "constructAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "constructAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (InstantiationException e) {
            LogUtil.e(TAG, "constructAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "constructAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "constructAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (CanNotLaunchBundleAppException e) {
            LogUtil.e(TAG, "constructAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (BundleNotInstalledException e) {
            LogUtil.e(TAG, "constructAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (NoAliasExistException e) {
            LogUtil.e(TAG, "constructAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        }
//        return null;
    }

    public static Object construct(String packageName, String className, Object... args) throws ClassNotFoundException, CanNotLaunchBundleAppException, BundleNotInstalledException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            return BundleManager.getInstance().construct(packageName, className, args);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "construct(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "construct(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (InstantiationException e) {
            LogUtil.e(TAG, "construct(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "construct(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "construct(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (CanNotLaunchBundleAppException e) {
            LogUtil.e(TAG, "construct(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (BundleNotInstalledException e) {
            LogUtil.e(TAG, "construct(), failed, " + e);
            e.printStackTrace();
            throw e;
        }
//        return null;
    }

    public static Object invoke(Object target, String methodName, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            return BundleManager.getInstance().invoke(target, methodName, args);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "invoke(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "invoke(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "invoke(), failed, " + e);
            e.printStackTrace();
            throw e;
        }
//        return null;
    }

    public static Object invokeStaticAlias(String alias, String className, String methodName, Object... args) throws ClassNotFoundException, BundleNotInstalledException, CanNotLaunchBundleAppException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoAliasExistException {
        try {
            return BundleManager.getInstance().invokeStaticAlias(alias, className, methodName, args);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "invokeStaticAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "invokeStaticAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "invokeStaticAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "invokeStaticAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (BundleNotInstalledException e) {
            LogUtil.e(TAG, "invokeStaticAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (CanNotLaunchBundleAppException e) {
            LogUtil.e(TAG, "invokeStaticAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (NoAliasExistException e) {
            LogUtil.e(TAG, "invokeStaticAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        }
//        return null;
    }

    public static Object invokeStatic(String packageName, String className, String methodName, Object... args) throws ClassNotFoundException, BundleNotInstalledException, CanNotLaunchBundleAppException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        try {
            return BundleManager.getInstance().invokeStatic(packageName, className, methodName, args);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "invokeStatic(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "invokeStatic(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "invokeStatic(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "invokeStatic(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (BundleNotInstalledException e) {
            LogUtil.e(TAG, "invokeStatic(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (CanNotLaunchBundleAppException e) {
            LogUtil.e(TAG, "invokeStatic(), failed, " + e);
            e.printStackTrace();
            throw e;
        }
//        return null;
    }

    public static void setProcedureProvider(String alias, Object target) throws NoAliasExistException {
        try {
            BundleManager.getInstance().setProcedureProvider(alias, target);
        } catch (NoAliasExistException e) {
            LogUtil.e(TAG, "setProcedureProvider(), failed, " + e);
            e.printStackTrace();
            throw e;
        }
    }

    public static void setProcedureProvider(Context context, Object target) {
        BundleManager.getInstance().setProcedureProvider(context, target);
    }

    public static Object callProcedureProviderAlias(String alias, String methodName, Object... args) throws NoAliasExistException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, BundleNotInstalledException, CanNotLaunchBundleAppException {
        try {
            return BundleManager.getInstance().callProcedureProviderAlias(alias, methodName, args);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "callProcedureProviderAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "callProcedureProviderAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "callProcedureProviderAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (InstantiationException e) {
            LogUtil.e(TAG, "callProcedureProviderAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (NoAliasExistException e) {
            LogUtil.e(TAG, "callProcedureProviderAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (BundleNotInstalledException e) {
            LogUtil.e(TAG, "callProcedureProviderAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (CanNotLaunchBundleAppException e) {
            LogUtil.e(TAG, "callProcedureProviderAlias(), failed, " + e);
            e.printStackTrace();
            throw e;
        }
    }

    public static Object callProcedureProvider(String packageName, String methodName, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, CanNotLaunchBundleAppException, BundleNotInstalledException {
        try {
            return BundleManager.getInstance().callProcedureProvider(packageName, methodName, args);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "callProcedureProvider(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "callProcedureProvider(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "callProcedureProvider(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (BundleNotInstalledException e) {
            LogUtil.e(TAG, "callProcedureProvider(), failed, " + e);
            e.printStackTrace();
            throw e;
        } catch (CanNotLaunchBundleAppException e) {
            LogUtil.e(TAG, "callProcedureProvider(), failed, " + e);
            e.printStackTrace();
            throw e;
        }
    }
}