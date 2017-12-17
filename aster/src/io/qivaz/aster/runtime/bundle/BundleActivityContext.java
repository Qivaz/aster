package io.qivaz.aster.runtime.bundle;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
class BundleActivityContext extends ContextThemeWrapper {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleActivityContext";
    private BundleActivity mBundleActivity;

    public BundleActivityContext(Context base, int themeResId, BundleActivity bundleActivity) {
        super(base, themeResId);
        mBundleActivity = bundleActivity;
    }

    @Override
    public Object getSystemService(String name) {
        Object service = mBundleActivity.mBundleEntry.mApp.getSystemService(name);
        if (debug) {
            LogUtil.v(TAG, "getSystemService(" + name + "), " + service);
        }

        if (Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
            //newLayoutInflater.setPrivateFactory(activity);
            try {
                Method LayoutInflater$setPrivateFactory = LayoutInflater.class.getDeclaredMethod("setPrivateFactory", new Class[]{LayoutInflater.Factory2.class});
                LayoutInflater$setPrivateFactory.setAccessible(true);
                LayoutInflater$setPrivateFactory.invoke(service, mBundleActivity.getTargetActivity());
            } catch (NoSuchMethodException e) {
                LogUtil.e(TAG, "getSystemService(), " + e);
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                LogUtil.e(TAG, "getSystemService(), " + e);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                LogUtil.e(TAG, "getSystemService(), " + e);
                e.printStackTrace();
            }
        }/* else if (Context.WINDOW_SERVICE.equals(name)) {
            service = mBundleActivity.getSystemService(name);
        }*/
        return service;
    }

    @Override
    public String getPackageName() {
        String packageName = mBundleActivity.mBundleEntry.mApp.getPackageName();
//            LogUtil.v(TAG, "getPackageName(), " + packageName);
        return packageName;
    }

    @Override
    public PackageManager getPackageManager() {
        PackageManager packageManager = mBundleActivity.mBundleEntry.mApp.getPackageManager();
        if (debug) {
            LogUtil.v(TAG, "getPackageManager(), " + packageManager);
        }
        return packageManager;
    }

    @Override
    public Context getApplicationContext() {
        Context context = mBundleActivity.mBundleEntry.mApp.getApplicationContext();
//            LogUtil.v(TAG, "getApplicationContext(), " + context);
        return context;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return mBundleActivity.mBundleEntry.mApp.getApplicationInfo();
    }

    @Override
    public ClassLoader getClassLoader() {
        ClassLoader cl = mBundleActivity.mBundleEntry.mApp.getClassLoader();
        if (debug) {
            LogUtil.v(TAG, "getClassLoader(), " + cl);
        }
        return cl;
    }

    @Override
    public Resources getResources() {
        Resources resources = mBundleActivity.mBundleEntry.mApp.getResources();
        if (debug) {
            LogUtil.v(TAG, "getResources(), " + resources);
        }
        return resources;
    }

    @Override
    public AssetManager getAssets() {
        AssetManager assetManager = mBundleActivity.mBundleEntry.mApp.getAssets();
        if (debug) {
            LogUtil.v(TAG, "getAssets(), " + assetManager);
        }
        return assetManager;
    }

//    public Resources.Theme getTheme() {
//        Resources.Theme theme = mBundleActivity.mTheme != null ? mBundleActivity.mTheme : super.getTheme();
//
////        Resources.Theme theme = null;
////        try {
////            theme = HostManager.getInstance().getHostAppContext().getTheme();
////        } catch (IllegalAccessException e) {
////            e.printStackTrace();
////        }
//
//        LogUtil.v(TAG, "getTheme(), mTheme=" + mBundleActivity.mTheme + ", theme=" + theme);
//        return theme;
//    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferences sp = mBundleActivity.mBundleEntry.mApp.getSharedPreferences(name, mode);
        if (debug) {
            LogUtil.v(TAG, "getSharedPreferences(" + name + "), " + sp);
        }
        return sp;
    }

    @Override
    public synchronized File getDatabasePath(String name) {
        File file = mBundleActivity.mBundleEntry.mApp.getDatabasePath(name);
        if (debug) {
            LogUtil.v(TAG, "getDatabasePath(), " + file);
        }
        return file;
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        boolean result = super.bindService(service, conn, flags);
        if (!result) {
            if (debug) {
                LogUtil.v(TAG, "bindService(), NOT bound in host");
            }
            result = BundleManager.getInstance().bindServiceBundle(this, service, conn, flags);
        } else {
            if (debug) {
                LogUtil.v(TAG, "bindService(), bound in host");
            }
        }
        return result;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
//            try {
//                BundleManager.getInstance().unbindService(conn);
//            } catch (IllegalArgumentException e) {
//                try {
//                    super.unbindService(conn);
//                    LogUtil.v(TAG, "unbindService(), unbound in host activity context");
//                } catch (IllegalArgumentException ex) {
//                    LogUtil.e(TAG, "unbindService(), NOT unbound!!!");
//                    ex.printStackTrace();
//                }
//            }

        try {
            super.unbindService(conn);
            if (debug) {
                LogUtil.v(TAG, "unbindService(), unbound in host activity context");
            }
        } catch (IllegalArgumentException e) {
            LogUtil.e(TAG, "unbindService(), NOT unbound!!!");
            e.printStackTrace();
        } finally {
            BundleManager.getInstance().unbindServiceBundle(conn);
        }
    }

    @Override
    public ComponentName startService(Intent service) {
        ComponentName componentName = super.startService(service);
        if (componentName == null) {
            if (debug) {
                LogUtil.v(TAG, "startService(), NOT started in host");
            }
            componentName = BundleManager.getInstance().startServiceBundle(this, service);
        } else {
            if (debug) {
                LogUtil.v(TAG, "startService(), started in host");
            }
        }
        return componentName;
    }

    @Override
    public boolean stopService(Intent intent) {
        boolean bRet = super.stopService(intent);
        if (!bRet) {
            if (debug) {
                LogUtil.v(TAG, "stopService(), NOT stopped in host");
            }
            try {
                bRet = BundleManager.getInstance().stopService(this, intent);
            } catch (IllegalAccessException e) {
                LogUtil.e(TAG, "stopService(), stopped in host failed!");
                e.printStackTrace();
            }
        } else {
            if (debug) {
                LogUtil.v(TAG, "stopService(), stopped in host");
            }
        }
        return bRet;
    }
}
