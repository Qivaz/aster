package io.qivaz.aster.runtime.host;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
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

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class HostAppContext extends ContextWrapper {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "HostAppContext";
    private LayoutInflater mLayoutInflater;
    private File mFilesDir;
    private File mCacheDir;
    private File mDatabasesDir;
    private Application mHostApp;
    private Context mBaseContext;

    public HostAppContext(Application hostContext, Context baseContext) {
        super(baseContext);
        mHostApp = hostContext;
        mBaseContext = baseContext;
    }

    @Override
    public Context getBaseContext() {
        return mBaseContext;
    }

    @Override
    public Object getSystemService(String name) {
        Object service = null;
        if (Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mLayoutInflater == null) {
                mLayoutInflater = LayoutInflater.from(mBaseContext).cloneInContext(this);
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

                    private final boolean verifyClassLoader(Constructor<? extends View> constructor) {
                        final ClassLoader constructorLoader = constructor.getDeclaringClass().getClassLoader();
                        ClassLoader bundleClassLoader = HostAppContext.this.getClassLoader();
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
                                    + HostAppContext.this.getClassLoader()
                                    + " in sConstructorMap");
                        }
                        return false;
                    }
                });
            }
            service = mLayoutInflater;
        } else if (Context.NOTIFICATION_SERVICE.equals(name)) {
            service = mBaseContext.getSystemService(name);
        } else {
            service = mBaseContext.getSystemService(name);
        }
        if (debug) {
            LogUtil.v(TAG, "getSystemService(" + name + "), " + service);
        }
        return service;
    }

    @Override
    public String getPackageName() {
        String packageName = mBaseContext.getPackageName();
//            LogUtil.v(TAG, "getPackageName(), " + packageName);
        return packageName;
    }

    @Override
    public PackageManager getPackageManager() {
        PackageManager packageManager = mBaseContext.getPackageManager();
        if (debug) {
            LogUtil.v(TAG, "getPackageManager(), " + packageManager);
        }
        return packageManager;
    }

    @Override
    public Context getApplicationContext() {
        Context context = mBaseContext.getApplicationContext();
//            LogUtil.v(TAG, "getApplicationContext(), " + context);
        return context;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        ApplicationInfo applicationInfo = mBaseContext.getApplicationInfo();
//            LogUtil.v(TAG, "getApplicationInfo(), " + applicationInfo);
        return applicationInfo;
    }

    @Override
    public ClassLoader getClassLoader() {
        if (debug) {
            LogUtil.v(TAG, "getClassLoader(), " + mBaseContext.getClassLoader());
        }
        return mBaseContext.getClassLoader();
    }

    @Override
    public Resources getResources() {
        Resources resources = mBaseContext.getResources();
        if (debug) {
            LogUtil.v(TAG, "getResources(2), " + resources);
        }
        return resources;
    }

    @Override
    public AssetManager getAssets() {
        AssetManager assetManager = mBaseContext.getAssets();
        if (debug) {
            LogUtil.v(TAG, "getAssets(), " + assetManager);
        }
        return assetManager;
    }

    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = mBaseContext.getTheme();
        if (debug) {
            LogUtil.v(TAG, "getTheme(), " + theme);
        }
        return theme;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (debug) {
            LogUtil.v(TAG, "getSharedPreferences(" + name + ", " + mode + ")");
        }
        return mBaseContext.getSharedPreferences(name, mode);
    }

    @Override
    public synchronized File getDatabasePath(String name) {
        mDatabasesDir = mBaseContext.getDatabasePath(name);
        if (debug) {
            LogUtil.v(TAG, "getDatabasePath(), " + mDatabasesDir);
        }
        return mDatabasesDir;
    }

    @Override
    public File getFilesDir() {
        mFilesDir = mBaseContext.getFilesDir();
        if (debug) {
            LogUtil.v(TAG, "getFilesDir(), " + mFilesDir.getAbsolutePath());
        }
        return mFilesDir;
    }

    @Override
    public File getCacheDir() {
        mCacheDir = mBaseContext.getCacheDir();
        if (debug) {
            LogUtil.v(TAG, "getCacheDir(), " + mCacheDir.getAbsolutePath());
        }
        return mCacheDir;
    }

    @Override
    public File getDir(String name, int mode) {
        if (debug) {
            LogUtil.v(TAG, "getDir(), " + name);
        }
        return mBaseContext.getDir(name, mode);
    }

    public void attachContextImpl() {
        try {
            final Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
            final Method ActivityThread$currentActivityThread = ActivityThread.getMethod("currentActivityThread");
            ActivityThread$currentActivityThread.setAccessible(true);
            Object thread = ActivityThread$currentActivityThread.invoke(null, (Object[]) null);

            final Class<?> CompatibilityInfo = Class.forName("android.content.res.CompatibilityInfo");
            final Method CompatibilityInfo$getPackageInfoNoCheck = ActivityThread.getMethod("getPackageInfoNoCheck", ApplicationInfo.class, CompatibilityInfo);
            CompatibilityInfo$getPackageInfoNoCheck.setAccessible(true);
            Object loadedApk = CompatibilityInfo$getPackageInfoNoCheck.invoke(thread, getApplicationInfo(), null);

            final Class<?> LoadedApk = Class.forName("android.app.LoadedApk");
            final Class<?> ContextImpl = Class.forName("android.app.ContextImpl");
            final Method ContextImpl$createAppContext = ContextImpl.getDeclaredMethod("createAppContext", ActivityThread, LoadedApk);
            ContextImpl$createAppContext.setAccessible(true);
            Object contextImpl = ContextImpl$createAppContext.invoke(null, thread, loadedApk);

            //attachBaseContext((Context) contextImpl);
            final Field ContextWrapper$mBase = ContextWrapper.class.getDeclaredField("mBase");
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

//    @Override
//    public void startActivity(Intent intent) {
//        BundleManager.getInstance().startActivity(mHostApp, intent);
//        super.startActivity(intent);
//    }
}
