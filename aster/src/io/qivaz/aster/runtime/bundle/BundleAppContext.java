package io.qivaz.aster.runtime.bundle;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class BundleAppContext extends ContextWrapper {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleAppContext";
    private LayoutInflater mLayoutInflater;
    private File mFilesDir;
    private File mCacheDir;
    private File mDatabasesDir;
    private BundleEntry mBundleEntry;
    private Application mApplication;
    private Class<?> mAppClazz;
    private Resources.Theme mNewTheme;

    public BundleAppContext(Application application, BundleEntry bundleEntry, Class<?> appClazz) {
        super(application);
        mApplication = application;
        mBundleEntry = bundleEntry;
        mAppClazz = appClazz;
        mNewTheme = bundleEntry.mResources.newTheme();
    }

    @Override
    public Object getSystemService(String name) {
        Object service = null;
        switch (name) {
            case Context.LAYOUT_INFLATER_SERVICE:
                if (mLayoutInflater == null) {
                    mLayoutInflater = LayoutInflater.from(mApplication.getBaseContext()).cloneInContext(this);
                    final LayoutInflater.Factory factory = mLayoutInflater.getFactory();
                    mLayoutInflater.setFactory(new LayoutInflater.Factory() {
                        @Override
                        public View onCreateView(String name, Context context, AttributeSet attrs) {
                            checkClassLoader(name);
                            if (factory != null) {
                                return factory.onCreateView(name, context, attrs);
                            }
                            return null;
                        }

                        /**
                         * Added for LayoutInflater on API level below API-24.
                         * Above API-24, Framework can check itself.
                         *
                         * @param viewName View's name to inflate
                         */
                        private void checkClassLoader(String viewName) {
                            try {
                                Field field = LayoutInflater.class.getDeclaredField("sConstructorMap");
                                field.setAccessible(true);
                                HashMap map = (HashMap) field.get(mLayoutInflater);
                                Constructor constructor = (Constructor) map.get(viewName);
                                if (constructor != null && !verifyClassLoader(constructor)) {
                                    LogUtil.w(TAG, "checkClassLoader(), remove " + viewName + " @ " + constructor.getDeclaringClass().getClassLoader() + " in sConstructorMap");
                                    map.remove(viewName);
                                }
                            } catch (IllegalAccessException e1) {
                                e1.printStackTrace();
                            } catch (NoSuchFieldException e1) {
                                e1.printStackTrace();
                            }
                        }

                        private boolean verifyClassLoader(Constructor<? extends View> constructor) {
                            final ClassLoader constructorLoader = constructor.getDeclaringClass().getClassLoader();
                            ClassLoader bundleClassLoader = BundleAppContext.this.getClassLoader();
                            do {
                                if (constructorLoader == bundleClassLoader) {
                                    if (debug) {
                                        LogUtil.v(TAG, "verifyClassLoader(), verify "
                                                + constructor.getDeclaringClass()
                                                + " in the same constructor "
                                                + constructor.getDeclaringClass().getClassLoader() + " in sConstructorMap");
                                    }
                                    return true;
                                }
                                bundleClassLoader = bundleClassLoader.getParent();
                            } while (bundleClassLoader != null);
                            if (debug) {
                                LogUtil.v(TAG, "verifyClassLoader(), verify " + constructor.getDeclaringClass() + " with different constructors: "
                                        + constructor.getDeclaringClass().getClassLoader()
                                        + " <vs> "
                                        + BundleAppContext.this.getClassLoader()
                                        + " in sConstructorMap");
                            }
                            return false;
                        }
                    });
                }
                service = mLayoutInflater;
                break;
            case Context.NOTIFICATION_SERVICE:
            default:
                service = super.getSystemService(name);
                break;
        }
        if (debug) {
            LogUtil.v(TAG, "getSystemService(" + name + "), " + service);
        }
        return service;
    }

    @Override
    public String getPackageName() {
        String packageName = mBundleEntry.mPackageInfo.packageName;
//            LogUtil.v(TAG, "getPackageName(), " + packageName);
        return packageName;
    }

    @Override
    public PackageManager getPackageManager() {
        PackageManager packageManager = null;
        try {
            packageManager = BundleManager.getInstance().getHostApplicationContext().getApplicationContext().getPackageManager();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (debug) {
            LogUtil.v(TAG, "getPackageManager(), " + packageManager);
        }
        return packageManager;
    }

    @Override
    public Context getApplicationContext() {
        Context context = mBundleEntry.mApp != null ? mBundleEntry.mApp : this;
//            LogUtil.v(TAG, "getApplicationContext(), " + context);
        return context;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        if (mBundleEntry.mAppInfo == null) {
            mBundleEntry.mAppInfo = mBundleEntry.createApplicationInfo();
        }
//            LogUtil.v(TAG, "getApplicationInfo(), " + mBundleEntry.mAppInfo);
        return mBundleEntry.mAppInfo;
    }

    @Override
    public ClassLoader getClassLoader() {
        ClassLoader cl = mBundleEntry.mDexClassLoader;
        if (debug) {
            LogUtil.v(TAG, "getClassLoader(), " + cl);
        }
        return cl;
    }

    @Override
    public Resources getResources() {
        if (mBundleEntry != null && mBundleEntry.mResources != null) {
            if (debug) {
                LogUtil.v(TAG, "getResources(1), " + mBundleEntry.mResources);
            }
            return mBundleEntry.mResources;
        } else {
            Resources resources = super.getResources();
            if (debug) {
                LogUtil.v(TAG, "getResources(2), " + resources);
            }
            return resources;
        }
    }

    @Override
    public AssetManager getAssets() {
        if (debug) {
            LogUtil.v(TAG, "getAssets(), " + mBundleEntry.mAssetManager);
        }
        return mBundleEntry.mAssetManager;
    }


    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = mNewTheme != null ? mNewTheme : super.getTheme();
        if (debug) {
            LogUtil.v(TAG, "getTheme(), " + theme);
        }
        return theme;
    }

    @Override
    public void setTheme(int resid) {
        if (debug) {
            LogUtil.v(TAG, "setTheme(), " + resid);
        }
        if (mNewTheme != null) {
            mNewTheme.applyStyle(resid, true);
        }
        super.setTheme(resid);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (debug) {
            LogUtil.v(TAG, "getSharedPreferences(" + name + ", " + mode + "), mixed flag=" + BundleManager.getInstance().getUseMixedSP());
        }
        if (BundleManager.getInstance().getUseMixedSP()) {
            return super.getSharedPreferences(name, mode);
        } else {
            try {
                Object sharedPref;
                synchronized (BundleManager.getInstance().mSharedPrefs) {
                    Class<?> SharedPreferencesImpl = Class.forName("android.app.SharedPreferencesImpl");
                    sharedPref = BundleManager.getInstance().mSharedPrefs.get(name);
                    if (sharedPref == null) {
                        if (mBundleEntry.mAppInfo == null) {
                            mBundleEntry.mAppInfo = mBundleEntry.createApplicationInfo();
                        }

                        File base = new File(mBundleEntry.mAppInfo.dataDir, "shared_prefs");
                        File prefsFile = new File(base, name + ".xml");
                        final Constructor SharedPreferencesImpl$Constructor = SharedPreferencesImpl.getDeclaredConstructor(File.class, Integer.TYPE);
                        SharedPreferencesImpl$Constructor.setAccessible(true);
                        sharedPref = SharedPreferencesImpl$Constructor.newInstance(prefsFile, mode);
                        BundleManager.getInstance().mSharedPrefs.put(name, sharedPref);
                        return (SharedPreferences) sharedPref;
                    }
                }

                if ((mode & MODE_MULTI_PROCESS) != 0 || getApplicationInfo().targetSdkVersion < 11) {
                    try {
                        Method method = mAppClazz.getDeclaredMethod("startReloadIfChangedUnexpectedly");
                        method.setAccessible(true);
                        method.invoke(sharedPref);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return (SharedPreferences) sharedPref;
            } catch (Exception e) {
                LogUtil.e(TAG, "getSharedPreferences(), have to use default SharedPreferences, " + e);
                e.printStackTrace();
                return super.getSharedPreferences(name, mode);
            }
        }
    }

    @Override
    public synchronized File getDatabasePath(String name) {
        mDatabasesDir = super.getDatabasePath(name);
        if (debug) {
            LogUtil.v(TAG, "getDatabasePath(), " + mDatabasesDir);
        }
        return mDatabasesDir;
    }

    @Override
    public File getFilesDir() {
        if (mFilesDir == null) {
            mFilesDir = new File(mBundleEntry.createApplicationInfo().dataDir, "files");
        }

        if (!mFilesDir.exists()) {
            if (!mFilesDir.mkdirs()) {
                LogUtil.e(TAG, "getFilesDir(), Unable to create files directory " + mFilesDir.getPath());
                return null;
            }
        }
        if (debug) {
            LogUtil.v(TAG, "getFilesDir(), " + mFilesDir.getAbsolutePath());
        }
        return mFilesDir;
    }

    @Override
    public File getCacheDir() {
        if (mCacheDir == null) {
            mCacheDir = new File(mBundleEntry.createApplicationInfo().dataDir, "cache");
        }

        if (!mCacheDir.exists()) {
            if (!mCacheDir.mkdirs()) {
                LogUtil.e(TAG, "getCacheDir(), Unable to create cache directory " + mCacheDir.getAbsolutePath());
                return null;
            }
        }
        if (debug) {
            LogUtil.v(TAG, "getCacheDir(), " + mCacheDir.getAbsolutePath());
        }
        return mCacheDir;
    }

    @Override
    public File getDir(String name, int mode) {
        name = "app_" + name;
        File file = new File(mBundleEntry.createApplicationInfo().dataDir, name);
        if (!file.exists()) {
            if (!file.mkdir()) {
                LogUtil.e(TAG, "getDir(), Unable to create app directory " + file.getAbsolutePath());
                return null;
            }
        }
        if (debug) {
            LogUtil.v(TAG, "getCacheDir(), " + file.getAbsolutePath());
        }
        return file;
    }

    @Override
    public void startActivity(Intent intent) {
        if (debug) {
            LogUtil.v(TAG, "startActivity(), " + intent);
        }
        try {
            if (!BundleManager.getInstance().startActivity(BundleManager.getInstance().getHostApplicationContext(), intent)) {
                super.startActivity(intent);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void startActivity(Intent intent, Bundle options) {
//        super.startActivity(intent, options);
//    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        if (debug) {
            LogUtil.v(TAG, "bindService(), " + service);
        }
        boolean result = BundleManager.getInstance().bindServiceBundle(mApplication, service, conn, flags);
        return !result ? super.bindService(service, conn, flags) : result;
    }

    @Override
    public ComponentName startService(Intent service) {
        if (debug) {
            LogUtil.v(TAG, "startService(), " + service);
        }
        ComponentName componentName = BundleManager.getInstance().startServiceBundle(mApplication, service);
        return componentName == null ? super.startService(service) : componentName;
    }

    public void attachContextImpl() {
        try {
            final Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
            final Method ActivityThread$currentActivityThread = ActivityThread.getMethod("currentActivityThread");
            ActivityThread$currentActivityThread.setAccessible(true);
            Object thread = ActivityThread$currentActivityThread.invoke(null, (Object[]) null);

            final Class<?> CompatibilityInfo = Class.forName("android.content.res.CompatibilityInfo");
            final Method ActivityThread$getPackageInfoNoCheck = ActivityThread.getMethod("getPackageInfoNoCheck", ApplicationInfo.class, CompatibilityInfo);
            ActivityThread$getPackageInfoNoCheck.setAccessible(true);
            Object loadedApk = ActivityThread$getPackageInfoNoCheck.invoke(thread, BundleManager.getInstance().getHostApplicationContext().getApplicationInfo(), null);

            final Class<?> LoadedApk = Class.forName("android.app.LoadedApk");
            final Class<?> ContextImpl = Class.forName("android.app.ContextImpl");
            final Method ContextImpl$createAppContext = ContextImpl.getDeclaredMethod("createAppContext", ActivityThread, LoadedApk);
            ContextImpl$createAppContext.setAccessible(true);
            Object contextImpl = ContextImpl$createAppContext.invoke(null, thread, loadedApk);

            //attachBaseContext((Context) contextImpl);
            Field ContextWrapper$mBase = ContextWrapper.class.getDeclaredField("mBase");
            ContextWrapper$mBase.setAccessible(true);
            ContextWrapper$mBase.set(this, contextImpl);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
