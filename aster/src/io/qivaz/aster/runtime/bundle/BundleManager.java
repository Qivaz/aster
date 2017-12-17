package io.qivaz.aster.runtime.bundle;


import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.qivaz.aster.runtime.BundleNotInstalledException;
import io.qivaz.aster.runtime.CanNotLaunchBundleAppException;
import io.qivaz.aster.runtime.NoAliasExistException;
import io.qivaz.aster.runtime.bundle.activity.BundleMultipleActivity;
import io.qivaz.aster.runtime.bundle.activity.BundleSingleInstanceActivityContainer;
import io.qivaz.aster.runtime.bundle.activity.BundleSingleTaskActivityContainer;
import io.qivaz.aster.runtime.bundle.activity.BundleSingleTopActivityContainer;
import io.qivaz.aster.runtime.bundle.service.BundleServiceContainer;
import io.qivaz.aster.runtime.host.HostManager;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class BundleManager {
    private static final boolean debug = BundleFeature.debug;
    private static final boolean debugPerf = BundleFeature.debugPerf;
    private static final String TAG = "BundleManager";
    private static BundleManager mInstance;
    final Map<String, Object/*SharedPreferencesImpl*/> mSharedPrefs = new HashMap<>();
    private final Map<String, BundleEntry> mBundleEntries = Collections.synchronizedMap(new HashMap<String, BundleEntry>());
    private final Map<String, /*Class<?>*/Object> mProcedureProviders = Collections.synchronizedMap(new HashMap<String, /*Class<?>*/Object>());
    private Application mHostApp;
    private Handler uiHandler;
    private BundleResolver mBundleResolver;
    private boolean mUseMixedSP = true;
    private Map<ServiceConnection, String> mBoundServiceMap = new HashMap<>();
    private IContainer serviceContainer = BundleServiceContainer.getInstance();
    private IContainer singleTopActivityContainer = BundleSingleTopActivityContainer.getInstance();
    private IContainer singleTaskActivityContainer = BundleSingleTaskActivityContainer.getInstance();
    private IContainer singleInstanceActivityContainer = BundleSingleInstanceActivityContainer.getInstance();

    private BundlePackageParser mBundlePackageParser;

    private BundleManager() {
        mBundleResolver = new BundleResolver(this);
        try {
            mBundlePackageParser = new BundlePackageParser();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static BundleManager getInstance() {
        if (mInstance == null) {
            synchronized (BundleManager.class) {
                if (mInstance == null) {
                    mInstance = new BundleManager();
                }
            }
        }
        return mInstance;
    }

    public Context getHostApplicationContext() throws IllegalAccessException {
        if (mHostApp == null) {
            throw new IllegalAccessException("Must call BundleManager.init() before use BundleManager.getHostApplicationContext()!");
        }
        return mHostApp;
    }

    public Context getBundleApplicationContextAlias(String alias) {
        String packageName;
        if (BundleFeature.HOST_ALIAS_NAME.equals(alias)) {
            return null;
        } else {
            packageName = getPackageNameByAlias(alias);
            if (packageName == null) {
                LogUtil.e(TAG, "getBundleApplicationContextAlias(), failed, no installed bundle matches alias of " + alias);
                return null;
            }
        }
        return getBundleApplicationContext(packageName);
    }

    public Context getBundleApplicationContext(String packageName) {
        if (!mHostApp.getPackageName().equals(packageName)) {
            try {
                BundleEntry bundleEntry = mBundleEntries.get(packageName);
                if (bundleEntry == null) {
                    bundleEntry = launchBundle(packageName);
                }
                if (bundleEntry.mApp == null) {
                    bundleEntry.launchApplication(mHostApp);
                }
                return bundleEntry.mApp;
            } catch (CanNotLaunchBundleAppException e) {
                LogUtil.e(TAG, "getBundleApplicationContext(" + packageName + "), " + e);
                e.printStackTrace();
            } catch (BundleNotInstalledException e) {
                LogUtil.e(TAG, "getBundleApplicationContext(" + packageName + "), " + e);
                e.printStackTrace();
            }
            return null;
        } else {
            LogUtil.e(TAG, "getBundleApplicationContext(), should not use host package name:" + packageName);
            return null;
        }
    }

    public void init(Application application) {
        mHostApp = application;
        uiHandler = new Handler();

        File rootFolder = new File(mHostApp.getApplicationInfo().dataDir, BundleFeature.BUNDLE_ROOT_FOLDER_NAME);
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }
        BundleFeature.BUNDLE_ROOT_FOLDER = rootFolder.getAbsolutePath();
        File dexFolder = new File(BundleFeature.BUNDLE_ROOT_FOLDER, BundleFeature.BUNDLE_DEX_FOLDER_NAME);
        if (!dexFolder.exists()) {
            dexFolder.mkdirs();
        }
        BundleFeature.BUNDLE_DEX_FOLDER = dexFolder.getAbsolutePath();
        File dataFolder = new File(BundleFeature.BUNDLE_ROOT_FOLDER, BundleFeature.BUNDLE_DATA_FOLDER_NAME);
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        BundleFeature.BUNDLE_DATA_FOLDER = dataFolder.getAbsolutePath();
        BundleFeature.BUNDLE_NATIVELIB_FOLDER = BundleFeature.BUNDLE_DATA_FOLDER;

        File manifestFolder = new File(BundleFeature.BUNDLE_ROOT_FOLDER, BundleFeature.BUNDLE_MANIFEST_FOLDER_NAME);
        if (!manifestFolder.exists()) {
            manifestFolder.mkdirs();
        }
        BundleFeature.BUNDLE_MANIFEST_FOLDER = manifestFolder.getAbsolutePath();

        File tempFolder = new File(BundleFeature.BUNDLE_ROOT_FOLDER, BundleFeature.BUNDLE_TEMP_FOLDER_NAME);
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }
        BundleFeature.BUNDLE_TEMP_FOLDER = tempFolder.getAbsolutePath();

        mBundleResolver.init(mHostApp);
        BundleAccelerator.prefetch();

        singleTopActivityContainer.initContainer();
        singleTaskActivityContainer.initContainer();
        singleInstanceActivityContainer.initContainer();

        serviceContainer.initContainer();
    }

    public boolean installBundle(String fullPath, boolean delete) {
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "installBundle(), start, delete=" + delete);
        }
        if (!mBundleResolver.installBundle(fullPath)) {
            LogUtil.e(TAG, "installBundle(), failed, " + fullPath);
            return false;
        }
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "installBundle(), installed");
        }
        if (delete) {
            if (!(new File(fullPath).delete())) {
                LogUtil.e(TAG, "installBundle(), failed, can't remove the apk file " + fullPath);
                return false;
            }
        }
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "installBundle(), end");
        }
        return true;
    }

    public boolean uninstallBundle(String bundleName) {
        releaseBundle(bundleName);
        return mBundleResolver.uninstallBundlePre(bundleName);
    }

//    private void registerBundle(String path, BundleEntry bundleEntry, String factor/*Only for Dalvik*/) {
//        mBundleResolver.registerBundle(path, bundleEntry, factor);
//    }
//
//    private void unregisterBundle(String bundleName) {
//        mBundleResolver.unregisterBundle(bundleName);
//    }
//
//    void prefetchBundle() {
//        mBundleResolver.prefetchBundleRegistrants();
//    }

    public List<String> getInstalledBundles() {
        return mBundleResolver.getInstalledBundles();
    }

    public boolean isBundleInstalledAlias(String alias) throws NoAliasExistException {
        String packageName;
        if (BundleFeature.HOST_ALIAS_NAME.equals(alias)) {
            packageName = mHostApp.getPackageName();
        } else {
            packageName = getPackageNameByAlias(alias);
            if (packageName == null) {
                LogUtil.e(TAG, "isBundleInstalledAlias(), failed, no installed bundle matches alias of " + alias);
                throw new NoAliasExistException("No alias of " + alias + " exist");
            }
        }
        return isBundleInstalled(packageName);
    }

    public boolean isBundleInstalled(String packageName) {
        return mBundleResolver.isBundleInstalled(packageName);
    }

    BundleEntry launchBundle(String packageName) throws BundleNotInstalledException {
        BundleEntry bundleEntry = mBundleResolver.resolveInactiveBundle(packageName);
        if (bundleEntry == null) {
            throw new BundleNotInstalledException("No bundle found with package name " + packageName);
        }
//        if (bundleEntry.mApp == null) {
//            LogUtil.v(TAG, "launchBundle(), didn't launch bundle application in advance, start launching..");
//            bundleEntry.launchApplication(mHostApp, bundleEntry);
//        }
        return bundleEntry;
    }

    BundleEntry launchBundleAlias(String alias) throws BundleNotInstalledException {
        BundleEntry bundleEntry = mBundleResolver.resolveInactiveBundleAlias(alias);
        if (bundleEntry == null) {
            throw new BundleNotInstalledException("No bundle found with alias name " + alias);
        }
//        if (bundleEntry.mApp == null) {
//            LogUtil.v(TAG, "launchBundleAlias(), didn't launch bundle application in advance, start launching..");
//            bundleEntry.launchApplication(mHostApp, bundleEntry);
//        }
        return bundleEntry;
    }

    public BundleRegistry getBundleRegistry() {
        return mBundleResolver.getBundleRegistry();
    }

    Class<?> getPrimaryType(Class<?> clazz) {
        if (Boolean.class.isAssignableFrom(clazz)) {
            clazz = Boolean.TYPE;
        } else if (Character.class.isAssignableFrom(clazz)) {
            clazz = Character.TYPE;
        } else if (Byte.class.isAssignableFrom(clazz)) {
            clazz = Byte.TYPE;
        } else if (Short.class.isAssignableFrom(clazz)) {
            clazz = Short.TYPE;
        } else if (Integer.class.isAssignableFrom(clazz)) {
            clazz = Integer.TYPE;
        } else if (Long.class.isAssignableFrom(clazz)) {
            clazz = Long.TYPE;
        } else if (Float.class.isAssignableFrom(clazz)) {
            clazz = Float.TYPE;
        } else if (Double.class.isAssignableFrom(clazz)) {
            clazz = Double.TYPE;
        }
        return clazz;
    }

    /**
     * Uses the constructor represented by this {@code Constructor} object to
     * create and initialize a new instance of the constructor's
     * declaring class, with the specified initialization parameters.
     * Individual parameters are automatically unwrapped to match
     * primitive formal parameters, and both primitive and reference
     * parameters are subject to method invocation conversions as necessary.
     *
     * @param args array of objects to be passed as arguments to
     *             the constructor call; values of primitive types are wrapped in
     *             a wrapper object of the appropriate type (e.g. a {@code float}
     *             in a {@link java.lang.Float Float})
     * @return a new object created by calling the constructor
     * this object represents
     * @throws BundleNotInstalledException    if the bundle is not installed in advance.
     * @throws CanNotLaunchBundleAppException if the bundle application launch failed.
     * @throws ClassNotFoundException         if the class name not exist.
     * @throws NoAliasExistException          if no alias exist.
     */
    public Object constructAlias(String alias, String className, Object... args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException, BundleNotInstalledException, CanNotLaunchBundleAppException, NoAliasExistException {
        String packageName;
        if (BundleFeature.HOST_ALIAS_NAME.equals(alias)) {
            packageName = mHostApp.getPackageName();
        } else {
            packageName = getPackageNameByAlias(alias);
            if (packageName == null) {
                LogUtil.e(TAG, "constructAlias(), failed, no installed bundle matches alias of " + alias);
                throw new NoAliasExistException("No alias of " + alias + " exist");
            }
        }
        return construct(packageName, className, args);
    }

    /**
     * Uses the constructor represented by this {@code Constructor} object to
     * create and initialize a new instance of the constructor's
     * declaring class, with the specified initialization parameters.
     * Individual parameters are automatically unwrapped to match
     * primitive formal parameters, and both primitive and reference
     * parameters are subject to method invocation conversions as necessary.
     *
     * @param args array of objects to be passed as arguments to
     *             the constructor call; values of primitive types are wrapped in
     *             a wrapper object of the appropriate type (e.g. a {@code float}
     *             in a {@link java.lang.Float Float})
     * @return a new object created by calling the constructor
     * this object represents
     * @throws BundleNotInstalledException    if the bundle is not installed in advance.
     * @throws CanNotLaunchBundleAppException if the bundle application launch failed.
     * @throws ClassNotFoundException         if the class name not exist.
     */
    public Object construct(String packageName, String className, Object... args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException, CanNotLaunchBundleAppException, BundleNotInstalledException {
        Class<?> clazz;
        if (mHostApp.getPackageName().equals(packageName)) {
            clazz = mHostApp.getClassLoader().loadClass(className);
        } else {
            BundleEntry bundleEntry = mBundleEntries.get(packageName);
            if (bundleEntry == null) {
                bundleEntry = launchBundle(packageName);
            }
            if (bundleEntry.mApp == null) {
                bundleEntry.launchApplication(mHostApp);
            }
            clazz = bundleEntry.mDexClassLoader.loadClass(className);
        }
        return construct(clazz, args);
    }

    Object construct(Class<?> cls, Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        ArrayList<Class<?>> paraList = new ArrayList<>();
        for (Object arg : args) {
            Class<?> clazz = getPrimaryType(arg.getClass());
            paraList.add(clazz);
        }
        Constructor<?> constructor = cls.getDeclaredConstructor(paraList.toArray(new Class<?>[0]));
        Object obj = constructor.newInstance(args);
        return obj;
    }

    public Object invoke(Object target, String methodName, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ArrayList<Class<?>> paraList = new ArrayList<>();
        for (Object arg : args) {
            Class<?> clazz = getPrimaryType(arg.getClass());
            paraList.add(clazz);
        }
        Class<?> cls = target.getClass();
        Method method = cls.getDeclaredMethod(methodName, paraList.toArray(new Class<?>[0]));
        Object result = method.invoke(target, args);
        return result;
    }

    public Object invoke(Object target, Method method, Object... args) throws InvocationTargetException, IllegalAccessException {
        Object result = method.invoke(target, args);
        return result;
    }

    public Object invokeStaticAlias(String alias, String className, String methodName, Object... args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, BundleNotInstalledException, CanNotLaunchBundleAppException, NoAliasExistException {
        String packageName;
        if (BundleFeature.HOST_ALIAS_NAME.equals(alias)) {
            packageName = mHostApp.getPackageName();
        } else {
            packageName = getPackageNameByAlias(alias);
            if (packageName == null) {
                LogUtil.e(TAG, "invokeStaticAlias(), failed, no installed bundle matches alias of " + alias);
                throw new NoAliasExistException("No alias of " + alias + " exist");
            }
        }
        return invokeStatic(packageName, className, methodName, args);
    }

    public Object invokeStatic(String packageName, String className, String methodName, Object... args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, BundleNotInstalledException, CanNotLaunchBundleAppException {
        Class<?> clazz;
        if (mHostApp.getPackageName().equals(packageName)) {
            clazz = mHostApp.getClassLoader().loadClass(className);
        } else {
            BundleEntry bundleEntry = mBundleEntries.get(packageName);
            if (bundleEntry == null) {
                bundleEntry = launchBundle(packageName);
            }
            if (bundleEntry.mApp == null) {
                bundleEntry.launchApplication(mHostApp);
            }
            clazz = bundleEntry.mDexClassLoader.loadClass(className);
        }
        return invokeStatic(clazz, methodName, args);
    }

    Object invokeStatic(Class<?> cls, String methodName, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ArrayList<Class<?>> paraList = new ArrayList<>();
        for (Object arg : args) {
            Class<?> clazz = getPrimaryType(arg.getClass());
            paraList.add(clazz);
        }
        Method method = cls.getDeclaredMethod(methodName, paraList.toArray(new Class<?>[0]));
        Object result = method.invoke(null, args);
        return result;
    }

    public void setProcedureProvider(String alias, Object target) throws NoAliasExistException {
        String packageName;
        if (BundleFeature.HOST_ALIAS_NAME.equals(alias)) {
            packageName = mHostApp.getPackageName();
        } else {
            packageName = getPackageNameByAlias(alias);
            if (packageName == null) {
                LogUtil.e(TAG, "invokeStaticAlias(), failed, no installed bundle matches alias of " + alias);
                throw new NoAliasExistException("No alias of " + alias + " exist");
            }
        }
        mProcedureProviders.put(packageName, target);
    }

    public void setProcedureProvider(Context context, Object target) {
        String packageName = context.getPackageName();
        mProcedureProviders.put(packageName, target);
    }

    public Object callProcedureProvider(String packageName, String methodName, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, BundleNotInstalledException, CanNotLaunchBundleAppException {
        if (!mHostApp.getPackageName().equals(packageName)) {
            BundleEntry bundleEntry = mBundleEntries.get(packageName);
            if (bundleEntry == null) {
                bundleEntry = launchBundle(packageName);
            }
            if (bundleEntry.mApp == null) {
                bundleEntry.launchApplication(mHostApp);
            }
        }

        IllegalArgumentException ex = null;
        Object target = mProcedureProviders.get(packageName);
        if (target == null) {
            throw new BundleNotInstalledException("Target is null. Not installed bundle, or not invoked setProcedureProvider() in advance?");
        }
        Method[] methods = target.getClass().getDeclaredMethods();
        Method method = null;
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                method = m;
                try {
                    return invoke(target, method, args);
                } catch (IllegalArgumentException e) {
                    ex = e;
                    if (debug) {
                        LogUtil.v(TAG, "callProcedureProvider(), " + e);
                    }
//                    e.printStackTrace();
                }
            }
        }

        if (ex == null) {
            throw new NoSuchMethodException("\"" + methodName + "\" not exist!");
        } else {
            throw ex;
        }
    }

    public Object callProcedureProviderAlias(String alias, String methodName, Object... args) throws NoAliasExistException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, BundleNotInstalledException, CanNotLaunchBundleAppException {
        String packageName;
        if (BundleFeature.HOST_ALIAS_NAME.equals(alias)) {
            packageName = mHostApp.getPackageName();
        } else {
            packageName = getPackageNameByAlias(alias);
            if (packageName == null) {
                LogUtil.e(TAG, "invokeStaticAlias(), failed, no installed bundle matches alias of " + alias);
                throw new NoAliasExistException("No alias of " + alias + " exist");
            }
        }
        return callProcedureProvider(packageName, methodName, args);
    }

    private void installProvider(Context context, List<ProviderInfo> providers) {
        try {
            final Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
            final Method ActivityThread$currentActivityThread = ActivityThread.getMethod("currentActivityThread");
            Object activityThread = ActivityThread$currentActivityThread.invoke(null, (Object[]) null);
            final Method ActivityThread$installProvider = ActivityThread.getDeclaredMethod("installProvider",
                    new Class[]{Context.class,
                            Class.forName("android.app.IActivityManager$ContentProviderHolder"),
                            ProviderInfo.class,
                            Boolean.TYPE,
                            Boolean.TYPE,
                            Boolean.TYPE});
            ActivityThread$installProvider.setAccessible(true);

            Object result = null;
            for (ProviderInfo cpi : providers) {
                result = ActivityThread$installProvider.invoke(activityThread, context, null, cpi, true, true, true);
                if (debug) {
                    LogUtil.v(TAG, "installProvider(), " + result);
                }
            }
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "installProvider(), failed, " + e);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "installProvider(), failed, " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "installProvider(), failed, " + e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "installProvider(), failed, " + e);
            e.printStackTrace();
        }
    }

    private void installContentProviders(Context context, List<ProviderInfo> providers) {
        try {
            final Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
            final Method ActivityThread$currentActivityThread = ActivityThread.getMethod("currentActivityThread");
            Object activityThread = ActivityThread$currentActivityThread.invoke(null, (Object[]) null);
            final Method ActivityThread$installContentProviders = ActivityThread.getDeclaredMethod("installContentProviders", new Class[]{Context.class, List.class});
            ActivityThread$installContentProviders.setAccessible(true);
            ActivityThread$installContentProviders.invoke(activityThread, context, providers);

            if (debug) {
                LogUtil.v(TAG, "installContentProviders(), " + activityThread);
            }
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "installContentProviders(), failed, " + e);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "installContentProviders(), failed, " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "installContentProviders(), failed, " + e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "installContentProviders(), failed, " + e);
            e.printStackTrace();
        }
    }

    void installProviders(BundleEntry bundleEntry) {
        List<ProviderInfo> providerInfoList = bundleEntry.mProviderInfo;
        if (providerInfoList != null && providerInfoList.size() > 0) {
//            installContentProviders(bundleEntry.mApp, providerInfoList);
            installProvider(bundleEntry.mApp, providerInfoList);
        }
    }

    void installReceivers(BundleEntry bundleEntry) {
        List<ResolveInfo> resolveInfoList = bundleEntry.mReceiverFilter;
        if (resolveInfoList != null) {
            for (ResolveInfo resolveInfo : resolveInfoList) {
                try {
                    Class cls = bundleEntry.mDexClassLoader.loadClass(resolveInfo.activityInfo.name);
                    BroadcastReceiver broadcastReceiver = (BroadcastReceiver) cls.newInstance();
                    if (resolveInfo.filter != null) {
                        mHostApp.registerReceiver(broadcastReceiver, resolveInfo.filter);
                    }
                } catch (Exception e) {
                    LogUtil.e(TAG, "installReceivers(" + bundleEntry + "), failed to register receiver, " + e);
                }
            }
        }
    }

    PackageInfo getPackageInfo(String bundlePath) {
        File apkFile = new File(bundlePath);
        if (!apkFile.isFile()) {
            LogUtil.e(TAG, "bundle file not exists: " + apkFile);
            return null;
        } else {
            try {
                PackageInfo packageInfo = getHostApplicationContext().getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(),
                        PackageManager.GET_META_DATA
                                | PackageManager.GET_ACTIVITIES
                                | PackageManager.GET_SERVICES
                                | PackageManager.GET_PROVIDERS
                                | PackageManager.GET_RECEIVERS);
                if (packageInfo != null) {
                    return packageInfo;
                }

                LogUtil.e(TAG, "packageInfo is null when load bundle in apk loader: " + apkFile);
            } catch (Exception exception) {
                LogUtil.e(TAG, "getPackageInfo have an exception : " + exception);
            }

            return null;
        }
    }

    public String getAliasByPackageName(String packageName) {
        BundleRegistry.BundleItem item = mBundleResolver.getBundleRegistry().findBundle(packageName);
        if (item != null && !TextUtils.isEmpty(item.mAlias)) {
            return item.mAlias;
        } else {
            return null;
        }
    }

    public String getPackageNameByAlias(String alias) {
        BundleRegistry.BundleItem item = mBundleResolver.getBundleRegistry().findBundleAlias(alias);
        if (item != null && !TextUtils.isEmpty(item.mPackageName)) {
            return item.mPackageName;
        } else {
            return null;
        }
    }

    boolean releaseBundleAlias(String alias) {
        BundleRegistry.BundleItem item = mBundleResolver.getBundleRegistry().findBundleAlias(alias);
        if (item != null) {
            return releaseBundle(item.mPackageName);
        } else {
            LogUtil.e(TAG, "releaseBundleAlias(" + alias + ") failed, not load the bundle before?");
            return false;
        }
    }

    boolean releaseBundle(String packageName) {
        BundleEntry bundleEntry = mBundleEntries.remove(packageName);
        if (bundleEntry != null) {
            return true;
        } else {
            LogUtil.e(TAG, "releaseBundle(" + packageName + ") failed, not load the bundle before?");
            return false;
        }
    }

    BundleEntry loadBundleAlias(String alias) {
        BundleRegistry.BundleItem item = mBundleResolver.getBundleRegistry().findBundleAlias(alias);
        if (item != null) {
            return loadBundle(item.mApkPath, false);
        } else {
            LogUtil.e(TAG, "loadBundleAlias(" + alias + ") failed, not install the bundle before?");
            return null;
        }
    }

    BundleEntry loadBundle(String bundleName) {
        BundleRegistry.BundleItem item = mBundleResolver.getBundleRegistry().findBundle(bundleName);
        if (item != null) {
            return loadBundle(item.mApkPath, false);
        } else {
            LogUtil.e(TAG, "loadBundle(" + bundleName + ") failed, not install the bundle before?");
            return null;
        }
    }

    BundleEntry loadBundle(String apkFullPath, boolean override) {
        BundleEntry bundleEntry = null;
        File apkFile = new File(apkFullPath);
        if (!apkFile.isFile()) {
            LogUtil.e(TAG, "loadBundle(), bundle file not exists: " + apkFile);
            return null;
        } else {
            LogUtil.w(TAG, "loadBundle(), start load Bundle <" + apkFullPath + ">");
            synchronized (mBundleEntries) {
                if (debugPerf) {
                    LogUtil.w("Performance/" + TAG, "loadBundle(), start, " + apkFullPath);
                }
                try {
                    Object packageObj = mBundlePackageParser.parseBundle(apkFullPath, override);
                    if (debugPerf) {
                        LogUtil.w("Performance/" + TAG, "loadBundle(), parsed bundle");
                    }
                    if (packageObj == null) {
                        return null;
                    }
                    PackageInfo packageInfo = mBundlePackageParser.getPackageInfo(packageObj);
                    if (debugPerf) {
                        LogUtil.w("Performance/" + TAG, "loadBundle(), got package info, " + packageInfo.packageName);
                    }

                    if (override) {
                        String signatures = fetchBundleSignatures(packageInfo);
                        if (!TextUtils.isEmpty(signatures)) {
                            String newSignatures = getBundleSignaturesString(packageInfo);
                            if (!signatures.equals(newSignatures)) {
                                LogUtil.e(TAG, "Not allowed to install the package [packageName:" + packageInfo.packageName + ", versionCode:" + packageInfo.versionCode + "], whose signature is different with previous installed package!");
                                throw new SecurityException("Not allowed to install the package [packageName:" + packageInfo.packageName + ", versionCode:" + packageInfo.versionCode + "], whose signature is different with previous installed package!");
                            }
                        } else {
                            saveBundleSignatures(packageInfo, getBundleSignaturesString(packageInfo));
                        }
                    }
                    if (debugPerf) {
                        LogUtil.w("Performance/" + TAG, "loadBundle(), parsed signature");
                    }

                    bundleEntry = mBundleEntries.get(packageInfo.packageName);
                    if (bundleEntry != null && !override) {
                        LogUtil.w(TAG, "loadBundle(), finished load bundle, already loaded " + bundleEntry.mPackageInfo.packageName + " before!");
                        return bundleEntry;
                    }
                    bundleEntry = mBundlePackageParser.createBundleEntry(apkFullPath, packageObj, packageInfo);
                    if (debugPerf) {
                        LogUtil.w("Performance/" + TAG, "loadBundle(), created bundle entry for " + packageInfo.packageName);
                    }
                    mBundleEntries.put(packageInfo.packageName, bundleEntry);

                    LogUtil.w(TAG, "loadBundle(), finished load bundle " + apkFullPath + "<" + bundleEntry.mPackageInfo.packageName + "/" + bundleEntry.mPackageInfo.versionCode + "/" + bundleEntry.mPackageInfo.versionName + "> successful!");

                    if (!override) {
                        try {
                            if (debugPerf) {
                                LogUtil.w("Performance/" + TAG, "loadBundle(), start to launch Application for " + packageInfo.packageName);
                            }
                            bundleEntry.launchApplication(mHostApp);
                            if (debugPerf) {
                                LogUtil.w("Performance/" + TAG, "loadBundle(), launched Application for " + packageInfo.packageName);
                            }
                        } catch (CanNotLaunchBundleAppException e) {
                            LogUtil.e(TAG, "launchApplication(1), can't launch bundle application, " + e);
                            e.printStackTrace();
                        }
                    } else {
                        final BundleEntry finalBundleEntry = bundleEntry;
                        uiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (debugPerf) {
                                        LogUtil.w("Performance/" + TAG, "loadBundle()..run(), start to launch Application for " + finalBundleEntry.mPackageName);
                                    }
                                    finalBundleEntry.launchApplication(mHostApp);
                                    if (debugPerf) {
                                        LogUtil.w("Performance/" + TAG, "loadBundle()..run(), launched Application for " + finalBundleEntry.mPackageName);
                                    }
                                } catch (CanNotLaunchBundleAppException e) {
                                    LogUtil.e(TAG, "launchApplication(2), can't launch bundle application, " + e);
                                    e.printStackTrace();
                                }
                            }
                        }, 0);
                    }
                } catch (Exception | Error e) {
                    LogUtil.e(TAG, "loadBundle(), failed to load apk, " + e);
                    e.printStackTrace();
                } finally {
                }

                if (debugPerf) {
                    LogUtil.w("Performance/" + TAG, "loadBundle(), end");
                }
                return bundleEntry;
            }
        }
    }

    ClassLoader getClassLoader(String apkFileName, String bundlePackageName) throws IllegalAccessException {
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "getClassLoader(), start");
        }
        BundleClassLoader loader = null;
        boolean useSo = false;
        String nativeLibPath = null;
        File apkFile = new File(apkFileName);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "getClassLoader(), created File object");
        }
        try {
            nativeLibPath = BundleFeature.getBundleLibFolder(bundlePackageName);
            File nativeLibFolder = new File(nativeLibPath);

//            File[] files = nativeLibFolder.listFiles();
//            if (files != null && files.length > 0) {
//                useSo = true;
//            }
            if (nativeLibFolder.exists()) {
                useSo = true;
            }
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "getClassLoader(), created SO File object");
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "getClassLoader(" + bundlePackageName + "), failed, " + e);
            e.printStackTrace();
        }
        if (debug) {
            LogUtil.v(TAG, "getClassLoader(), useSo=" + useSo);
        }

        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "getClassLoader(), start creating BundleClassLoader");
        }
        loader = new BundleClassLoader(apkFile.getAbsolutePath(), BundleFeature.BUNDLE_DEX_FOLDER, BundleFeature.getBundleLibFolder(bundlePackageName), HostManager.getInstance().getHostClassLoader()/*ClassLoader.getSystemClassLoader()*/, bundlePackageName);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "getClassLoader(), created BundleClassLoader object");
        }
        loader.addLibPath(nativeLibPath);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "getClassLoader(), end");
        }
        return loader;
    }

    AssetManager getAsset(File apkFile) {
        AssetManager assetmanager = null;
        Method method = null;
        try {
            assetmanager = AssetManager.class.newInstance();
            method = AssetManager.class.getDeclaredMethod("addAssetPath", new Class[]{String.class});
            method.setAccessible(true);
            method.invoke(assetmanager, apkFile.getAbsolutePath());
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "getAsset(" + apkFile + "), failed, " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            LogUtil.e(TAG, "getAsset(" + apkFile + "), failed, " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "getAsset(" + apkFile + "), failed, " + e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "getAsset(" + apkFile + "), failed, " + e);
            e.printStackTrace();
        }

        return assetmanager;
    }

    Resources getResources(AssetManager assetManager) throws IllegalAccessException {
        final Resources hostResources = getHostApplicationContext().getResources();
        Resources resources = new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration()) {
            @Override
            public void getValue(String name, TypedValue outValue, boolean resolveRefs)
                    throws NotFoundException {
                super.getValue(name, outValue, resolveRefs);
                if (debug) {
                    LogUtil.v(TAG, "getResources().1.getValue(" + name + ", " + outValue + ", " + resolveRefs + ")");
                }
            }

            @Override
            public void getValue(int id, TypedValue outValue, boolean resolveRefs) {
                super.getValue(id, outValue, resolveRefs);
                if (debug) {
                    LogUtil.v(TAG, "getResources().2.getValue(" + id + ", " + outValue + ", " + resolveRefs + ")");
                }
            }

            @Override
            public int getIdentifier(String name, String defType, String defPackage) {
                int ret = super.getIdentifier(name, defType, defPackage);
                if (debug) {
                    LogUtil.v(TAG, "getResources().<bundle>.getIdentifier(" + name + ", " + defType + ", " + defPackage + "), return " + ret);
                }
                if (ret == 0) {
                    ret = hostResources.getIdentifier(name, defType, defPackage);
                }
                if (debug) {
                    LogUtil.v(TAG, "getResources().<host>.getIdentifier(" + name + ", " + defType + ", " + defPackage + "), return " + ret);
                }
                return ret;
            }

        };

        if (!compareLocale(hostResources.getConfiguration().locale, resources.getConfiguration().locale)) {
            String lang = hostResources.getConfiguration().locale.getLanguage();
            String country = hostResources.getConfiguration().locale.getCountry();
            String variant = hostResources.getConfiguration().locale.getVariant();
            updateResource(resources, new Locale(lang, country, variant));
        }

        return resources;
    }

    private boolean compareLocale(Locale former, Locale latter) {
        return former.getLanguage().equals(latter.getLanguage())
                && former.getCountry().equals(latter.getCountry())
                && former.getVariant().equals(latter.getVariant());
    }

    private void updateResource(Resources resources, Locale locale) {
        if (resources != null && locale != null) {
            DisplayMetrics dm = resources.getDisplayMetrics();
            Configuration conf = resources.getConfiguration();
            conf.locale = locale;
            resources.updateConfiguration(conf, dm);
        }
    }

    private String getBundleSignaturesFileName(PackageInfo packageInfo) {
        return packageInfo.packageName /*+ "_" + packageInfo.versionCode*/ + ".cert";
    }

    private String fetchBundleSignatures(PackageInfo packageInfo) {
        String signaturesFileName = getBundleSignaturesFileName(packageInfo);
        File signaturesFile = new File(BundleFeature.BUNDLE_MANIFEST_FOLDER, signaturesFileName);
        return readFile(signaturesFile);
    }

    private void saveBundleSignatures(PackageInfo packageInfo, String signatures) {
        String signaturesFileName = getBundleSignaturesFileName(packageInfo);
        File signaturesFile = new File(BundleFeature.BUNDLE_MANIFEST_FOLDER, signaturesFileName);
        if (signaturesFile.exists()) {
            if (signaturesFile.delete()) {
                LogUtil.e(TAG, "saveBundleManifest(), remove " + signaturesFile.getAbsolutePath() + " failed!");
                return;
            }
        }
        writeFile(signaturesFile, signatures);
    }

    private String getBundleSignaturesString(PackageInfo packageInfo) {
        Signature[] signatures = packageInfo.signatures;
        if (signatures == null) {
            LogUtil.e(TAG, "getBundleSignaturesString(), signatures is null");
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Signature signature : signatures) {
            sb.append(signature.toCharsString());
        }
        return sb.toString();
    }

    private String readFile(File file) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\r\n");
            }
            sb.delete(sb.length() - 2, sb.length());
            return sb.toString();
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, "readFile(" + file.getAbsolutePath() + "), " + e);
            e.printStackTrace();
        } catch (IOException e) {
            LogUtil.e(TAG, "readFile(" + file.getAbsolutePath() + "), " + e);
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void writeFile(File file, String str) {
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(file, true);
            fos.write(str.getBytes());
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, "writeFile(" + file.getAbsolutePath() + "), " + e);
            e.printStackTrace();
        } catch (IOException e) {
            LogUtil.e(TAG, "writeFile(" + file.getAbsolutePath() + "), " + e);
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    Map<String, BundleEntry> getBundleEntities() {
        return mBundleEntries;
    }

    public boolean getUseMixedSP() {
        return mUseMixedSP;
    }

    public void setUseMixedSP(boolean mix) {
        mUseMixedSP = mix;
    }

    public BundleEntry getBundleByPackageName(String packageName) {
        BundleEntry bundleEntry = mBundleEntries.get(packageName);
        if (bundleEntry == null) {
            LogUtil.e(TAG, "getBundleByPackageName(), return null for " + packageName);
        }
        return bundleEntry;
    }

    private String getFullClassName(String packageName, String className) {
        if (className.startsWith(".")) {
            className = packageName + className;
        }
        return className;
    }

    private String getFullClassName(ComponentName componentName) {
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();
        if (className.startsWith(".")) {
            className = packageName + className;
        }
        return className;
    }

    public Bundle getAllBundleMetaData() {
        Bundle bundle = new Bundle();
        Iterator iterator = mBundleEntries.values().iterator();

        while (iterator.hasNext()) {
            BundleEntry bundleEntry = (BundleEntry) iterator.next();
            if (bundleEntry != null && bundleEntry.mAppInfo != null && bundleEntry.mAppInfo.metaData != null) {
                bundle.putAll(bundleEntry.mAppInfo.metaData);
            }
        }
        return bundle;
    }

    public Bundle getBundleMetaData(String bundleName) {
        Bundle bundle = new Bundle();
        BundleEntry bundleEntry = mBundleEntries.get(bundleName);
        if (bundleEntry != null && bundleEntry.mAppInfo != null && bundleEntry.mAppInfo.metaData != null) {
            bundle.putAll(bundleEntry.mAppInfo.metaData);
        }
        return bundle;
    }

    public boolean startActivity(Context context, Intent intent) throws IllegalAccessException {
        return startActivityForResult(context, intent, -1);
    }

    public boolean startActivityForResult(Context context, Intent intent, int requestCode) throws IllegalAccessException {
        boolean ret = false;
        if (!(ret = startActivityForResultHost(context, intent, requestCode))) {
            ret = startActivityForResultBundle(context, intent, requestCode);
        }
        return ret;
    }

    boolean startActivityForResultHost(Context context, Intent intent, int requestCode) throws IllegalAccessException {
        PackageManager packageManager = getHostApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
        if (resolveInfo != null) {
            if (debug) {
                LogUtil.v(TAG, "startActivityForResultHost(), start activity in host, intent=" + intent);
            }

//            // We're stronger than default FW behavior, check if we should do this!
//            if (true) {
//                String clazz = getFullClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
//                LogUtil.v(TAG, "startActivityForResultHost(), start activity for host, clazz=" + clazz);
//                intent.setClassName(resolveInfo.activityInfo.packageName, clazz);
//            }

            try {
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, requestCode);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } catch (ActivityNotFoundException e) {
                if (debug) {
                    LogUtil.v(TAG, "startActivityForResultHost(), start activity in host failed, intent=" + intent);
                }
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            if (debug) {
                LogUtil.v(TAG, "startActivityForResultHost(), can't start activity in host, intent=" + intent);
            }
        }
        return false;
    }

    boolean startActivityForResultBundle(Context context, Intent intent, int requestCode) {
//        Intent resolvedIntent;
//        if ((resolvedIntent = mBundleResolver.resolveBundleActivityInfoIntent(intent, true)) == null) {
//            if ((resolvedIntent = mBundleResolver.resolveBundleActivityFilterIntent(intent)) == null) {
//                resolvedIntent = mBundleResolver.resolveInactiveBundleActivityIntent(intent, true);
//            }
//        }
//
//        if (resolvedIntent == null || resolvedIntent.getComponent() == null) {
//            LogUtil.e(TAG, "startActivityForResultBundle(), failed, not resolved! intent=" + intent);
//            return false;
//        }
//        LogUtil.v(TAG, "startActivityForResultBundle(), resolved intent:" + resolvedIntent + ", original intent:" + intent);

        ResolveInfo resolveInfo;
        if ((resolveInfo = mBundleResolver.resolveBundleActivityInfo(intent, true)) == null) {
            if ((resolveInfo = mBundleResolver.resolveBundleActivityFilter(intent)) == null) {
                resolveInfo = mBundleResolver.resolveInactiveBundleActivity(intent, true);
            }
        }
        if (resolveInfo == null) {
            LogUtil.e(TAG, "startActivityForResultBundle(), failed, not resolved! resolveInfo is null, intent=" + intent);
            return false;
        }

        Intent resolvedIntent = mBundleResolver.getActivityIntentFromResolveInfo(intent, resolveInfo);
        if (resolvedIntent == null || resolvedIntent.getComponent() == null) {
            LogUtil.e(TAG, "startActivityForResultBundle(), failed, not resolved! intent=" + intent);
            return false;
        }
        if (debug) {
            LogUtil.v(TAG, "startActivityForResultBundle(), resolved intent:" + resolvedIntent + ", original intent:" + intent);
        }

        try {
            ComponentName stubComponentName;
            if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_MULTIPLE) {
                stubComponentName = new ComponentName(getHostApplicationContext(), BundleMultipleActivity.class);
            } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {
                String targetActivity = getFullClassName(resolvedIntent.getComponent());
                String containerActivity;
                if ((containerActivity = singleTopActivityContainer.peekContainer(targetActivity)) == null) {
                    containerActivity = singleTopActivityContainer.bindContainer(targetActivity);
                } else {
                    if (debug) {
                        LogUtil.v(TAG, "startActivityForResultBundle(), already launched before, " + targetActivity + " @ " + containerActivity);
                    }
                }
                stubComponentName = new ComponentName(getHostApplicationContext(), containerActivity);
            } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
                String targetActivity = getFullClassName(resolvedIntent.getComponent());
                String containerActivity;
                if ((containerActivity = singleTaskActivityContainer.peekContainer(targetActivity)) == null) {
                    containerActivity = singleTaskActivityContainer.bindContainer(targetActivity);
                } else {
                    if (debug) {
                        LogUtil.v(TAG, "startActivityForResultBundle(), already launched before, " + targetActivity + " @ " + containerActivity);
                    }
                }
                stubComponentName = new ComponentName(getHostApplicationContext(), containerActivity);
            } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
                String targetActivity = getFullClassName(resolvedIntent.getComponent());
                String containerActivity;
                if ((containerActivity = singleInstanceActivityContainer.peekContainer(targetActivity)) == null) {
                    containerActivity = singleInstanceActivityContainer.bindContainer(targetActivity);
                } else {
                    if (debug) {
                        LogUtil.v(TAG, "startActivityForResultBundle(), already launched before, " + targetActivity + " @ " + containerActivity);
                    }
                }
                stubComponentName = new ComponentName(getHostApplicationContext(), containerActivity);
            } else {
                stubComponentName = new ComponentName(getHostApplicationContext(), BundleMultipleActivity.class);
            }
            Intent stubIntent = new Intent();
            stubIntent.putExtra("bundle_intent", resolvedIntent);
            stubIntent.setComponent(stubComponentName);
            stubIntent.setFlags(resolvedIntent.getFlags());
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(stubIntent, requestCode);
                BundleAccelerator.register(context.getPackageName(),
                        context.getClass().getName(),
                        resolvedIntent.getComponent().getPackageName());
            } else {
                stubIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(stubIntent);
            }
            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, "startActivityForResultBundle(), " + e);
            e.printStackTrace();
            return false;
        }
    }

    public ComponentName startService(Context context, Intent intent) throws IllegalAccessException {
        ComponentName bRet;
        if ((bRet = startServiceHost(context, intent)) == null) {
            bRet = startServiceBundle(context, intent);
        }
        return bRet;
    }

    ComponentName startServiceHost(Context context, Intent intent) throws IllegalAccessException {
        PackageManager packageManager = getHostApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveService(intent, 0);
        if (resolveInfo != null) {
            if (debug) {
                LogUtil.v(TAG, "startServiceHost(), start service for host, intent=" + intent);
            }
            return context.startService(intent);
        }
        return null;
    }

    ComponentName startServiceBundle(Context context, Intent intent) {
//        Intent resolvedIntent;
//        if ((resolvedIntent = mBundleResolver.resolveBundleServiceInfoIntent(intent, true)) == null) {
//            if ((resolvedIntent = mBundleResolver.resolveBundleServiceFilterIntent(intent)) == null) {
//                resolvedIntent = mBundleResolver.resolveInactiveBundleServiceIntent(intent, true);
//            }
//        }
//
//        if (resolvedIntent == null || resolvedIntent.getComponent() == null) {
//            LogUtil.e(TAG, "startServiceBundle(), failed, not resolved! intent=" + intent);
//            return null;
//        }
//        LogUtil.v(TAG, "startServiceBundle(), resolved intent:" + resolvedIntent + ", original intent:" + intent);

        ResolveInfo resolveInfo;
        if ((resolveInfo = mBundleResolver.resolveBundleServiceInfo(intent, true)) == null) {
            if ((resolveInfo = mBundleResolver.resolveBundleServiceFilter(intent)) == null) {
                resolveInfo = mBundleResolver.resolveInactiveBundleService(intent, true);
            }
        }
        if (resolveInfo == null) {
            LogUtil.e(TAG, "startServiceBundle(), failed, not resolved! resolveInfo is null, intent=" + intent);
            return null;
        }

        Intent resolvedIntent = mBundleResolver.getServiceIntentFromResolveInfo(intent, resolveInfo);
        if (resolvedIntent == null || resolvedIntent.getComponent() == null) {
            LogUtil.e(TAG, "startServiceBundle(), failed, not resolved! intent=" + intent);
            return null;
        }
        if (debug) {
            LogUtil.v(TAG, "startServiceBundle(), resolved intent:" + resolvedIntent + ", original intent:" + intent);
        }

        try {
            String targetService = getFullClassName(resolvedIntent.getComponent());
            String containerService;
            if ((containerService = serviceContainer.peekContainer(targetService)) == null) {
                containerService = serviceContainer.bindContainer(targetService);
            } else {
                LogUtil.w(TAG, "startServiceBundle(), already launched before, " + targetService + " @ " + containerService);
            }
            if (debug) {
                LogUtil.v(TAG, "startServiceBundle(), resolved service container:" + targetService + " @ " + containerService);
            }
            ComponentName stubComponentName = new ComponentName(getHostApplicationContext(), containerService);
            Intent stubIntent = new Intent();
            stubIntent.putExtra("bundle_intent", resolvedIntent);
            stubIntent.setComponent(stubComponentName);

            return context.startService(stubIntent);
        } catch (Exception e) {
            LogUtil.e(TAG, "startServiceBundle(), " + e);
            e.printStackTrace();
            return null;
        }
    }

    public boolean stopService(Context context, Intent intent) throws IllegalAccessException {
        boolean bRet;
        if (!(bRet = stopServiceHost(context, intent))) {
            bRet = stopServiceBundle(context, intent);
        }
        return bRet;
    }

    boolean stopServiceHost(Context context, Intent intent) throws IllegalAccessException {
        PackageManager packageManager = getHostApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveService(intent, 0);
        if (resolveInfo != null) {
            if (debug) {
                LogUtil.v(TAG, "stopServiceHost(), stop service for host, intent=" + intent);
            }
            return getHostApplicationContext().stopService(intent);
        }
        return false;
    }

    boolean stopServiceBundle(Context context, Intent intent) {
        Intent resolvedIntent = intent;
//        if ((resolvedIntent = mBundleResolver.resolveBundleServiceInfoIntent(intent, false)) == null) {
//            if ((resolvedIntent = mBundleResolver.resolveBundleServiceFilterIntent(intent)) == null) {
//                resolvedIntent = mBundleResolver.resolveInactiveBundleServiceIntent(intent);
//            }
//        }
//
//        if (resolvedIntent == null || resolvedIntent.getComponent() == null) {
//            LogUtil.e(TAG, "stopServiceBundle(), failed, not resolved! intent=" + intent);
//            return false;
//        }
//        LogUtil.v(TAG, "stopServiceBundle(), resolved intent:" + resolvedIntent + ", original intent:" + intent);

        try {
            String targetService = getFullClassName(resolvedIntent.getComponent());
            String containerService;
            if ((containerService = serviceContainer.peekContainer(targetService)) == null) {
                LogUtil.e(TAG, "stopServiceBundle(), NOT launched before, " + targetService);
                return false;
            }
            if (debug) {
                LogUtil.v(TAG, "stopServiceBundle(), resolved service container:" + targetService + "@" + containerService);
            }

            ComponentName stubComponentName = new ComponentName(getHostApplicationContext(), containerService);
            Intent stubIntent = new Intent();
            stubIntent.setComponent(stubComponentName);

            boolean bRet = context.stopService(stubIntent);
            if (!bRet) {
                LogUtil.e(TAG, "stopServiceBundle(), failed");
            }
            return bRet;
        } catch (Exception e) {
            LogUtil.e(TAG, "stopServiceBundle(), " + e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean bindService(Context context, Intent intent, ServiceConnection conn, int flags) throws IllegalAccessException {
        boolean bRet;
        if (!(bRet = bindServiceHost(context, intent, conn, flags))) {
            bRet = bindServiceBundle(context, intent, conn, flags);
        }
        return bRet;
    }

    boolean bindServiceHost(Context context, Intent intent, ServiceConnection conn, int flags) throws IllegalAccessException {
        PackageManager packageManager = getHostApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveService(intent, 0);
        if (resolveInfo != null) {
            if (debug) {
                LogUtil.v(TAG, "bindServiceHost(), start service for host, intent=" + intent);
            }
            return context.bindService(intent, conn, flags);
        }
        return false;
    }

    boolean bindServiceBundle(Context context, Intent intent, ServiceConnection conn, int flags) {
//        Intent resolvedIntent;
//        if ((resolvedIntent = mBundleResolver.resolveBundleServiceInfoIntent(intent, true)) == null) {
//            if ((resolvedIntent = mBundleResolver.resolveBundleServiceFilterIntent(intent)) == null) {
//                resolvedIntent = mBundleResolver.resolveInactiveBundleServiceIntent(intent, true);
//            }
//        }
//
//        if (resolvedIntent == null || resolvedIntent.getComponent() == null) {
//            LogUtil.e(TAG, "bindServiceBundle(), failed, not resolved! intent=" + intent);
//            return false;
//        }
//        LogUtil.v(TAG, "bindServiceBundle(), resolved intent:" + resolvedIntent + ", original intent:" + intent);

        ResolveInfo resolveInfo;
        if ((resolveInfo = mBundleResolver.resolveBundleServiceInfo(intent, true)) == null) {
            if ((resolveInfo = mBundleResolver.resolveBundleServiceFilter(intent)) == null) {
                resolveInfo = mBundleResolver.resolveInactiveBundleService(intent, true);
            }
        }
        if (resolveInfo == null) {
            LogUtil.e(TAG, "bindServiceBundle(), failed, not resolved! resolveInfo is null, intent=" + intent);
            return false;
        }

        Intent resolvedIntent = mBundleResolver.getServiceIntentFromResolveInfo(intent, resolveInfo);
        if (resolvedIntent == null || resolvedIntent.getComponent() == null) {
            LogUtil.e(TAG, "bindServiceBundle(), failed, not resolved! intent=" + intent);
            return false;
        }
        if (debug) {
            LogUtil.v(TAG, "bindServiceBundle(), resolved intent:" + resolvedIntent + ", original intent:" + intent);
        }

        try {
            String targetService = getFullClassName(resolvedIntent.getComponent());
            String containerService;
            if ((containerService = serviceContainer.peekContainer(targetService)) == null) {
                containerService = serviceContainer.bindContainer(targetService);
            } else {
                LogUtil.w(TAG, "bindServiceBundle(), already launched before, " + targetService + "@" + containerService);
            }
            if (debug) {
                LogUtil.v(TAG, "bindServiceBundle(), resolved service container:" + targetService + "@" + containerService);
            }
            ComponentName stubComponentName = new ComponentName(getHostApplicationContext(), containerService);
            Intent stubIntent = new Intent();
            stubIntent.putExtra("bundle_intent", resolvedIntent);
            stubIntent.setComponent(stubComponentName);

            boolean bRet = context.bindService(stubIntent, conn, flags);//Context.BIND_AUTO_CREATE
            if (bRet) {
                mBoundServiceMap.put(conn, resolvedIntent.getComponent().getPackageName());
            } else {
                LogUtil.e(TAG, "bindServiceBundle(), failed!!!");
            }
            return bRet;
        } catch (Exception e) {
            LogUtil.e(TAG, "bindServiceBundle(), " + e);
            e.printStackTrace();
            return false;
        }
    }

    void unbindServiceBundle(ServiceConnection conn) {
        if (mBoundServiceMap.remove(conn) != null) {
            return;
        }
        LogUtil.e(TAG, "unbindServiceBundle(), NOT bound with connection " + conn);
    }

    public void unbindService(Context context, ServiceConnection conn) throws IllegalAccessException {
        String packageName = mBoundServiceMap.get(conn);
        if (!TextUtils.isEmpty(packageName)) {
            mBoundServiceMap.remove(conn);
        }

        if (context != null) {
            context.unbindService(conn);
            if (debug) {
                LogUtil.v(TAG, "unbindService(" + conn + "), unbound in " + context.getPackageName() + ", " + context);
            }
            return;
        }

        if (!TextUtils.isEmpty(packageName)) {
            BundleEntry bundleEntry = getBundleEntities().get(packageName);
            if (bundleEntry != null) {
                try {
                    bundleEntry.mApp.unbindService(conn);
                    if (debug) {
                        LogUtil.v(TAG, "unbindService(" + conn + "), unbound in bundle " + packageName);
                    }
                    return;
                } catch (IllegalArgumentException e) {
                    if (debug) {
                        LogUtil.v(TAG, "unbindService(" + conn + "), NOT unbound in bundle " + packageName);
                    }
                    e.printStackTrace();
                }
            }
        }

        try {
            getHostApplicationContext().unbindService(conn);
            if (debug) {
                LogUtil.v(TAG, "unbindService(" + conn + "), unbound in host application");
            }
            return;
        } catch (IllegalArgumentException e) {
            if (debug) {
                LogUtil.v(TAG, "unbindService(" + conn + "), NOT unbound host application");
            }
            e.printStackTrace();
            throw e;
        }
    }
}