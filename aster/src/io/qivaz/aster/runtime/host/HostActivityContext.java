package io.qivaz.aster.runtime.host;

import android.app.Activity;
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
import java.util.HashMap;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
class HostActivityContext extends ContextWrapper {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "HostActivityContext";
    private LayoutInflater mLayoutInflater;
    private Activity mActivity;
    private Context mBaseContext;

    public HostActivityContext(Activity activity, Context baseContext) {
        super(baseContext);
        mActivity = activity;
        mBaseContext = baseContext;
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
                        ClassLoader bundleClassLoader = HostActivityContext.this.getClassLoader();
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
                                    + HostActivityContext.this.getClassLoader()
                                    + " in sConstructorMap");
                        }
                        return false;
                    }
                });
            }
            service = mLayoutInflater;
        } else if (Context.NOTIFICATION_SERVICE.equals(name)) {
            service = super.getSystemService(name);
        } else {
            service = super.getSystemService(name);
        }
        if (debug) {
            LogUtil.v(TAG, "getSystemService(" + name + "), " + service);
        }
        return service;
    }

    @Override
    public String getPackageName() {
        String packageName = mBaseContext.getPackageName();
//            if (debug) LogUtil.v(TAG, "getPackageName(), " + packageName);
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
        return mBaseContext.getApplicationInfo();
    }

    @Override
    public ClassLoader getClassLoader() {
        ClassLoader cl = mBaseContext.getClassLoader();
        if (debug) {
            LogUtil.v(TAG, "getClassLoader(), " + cl);
        }
        return cl;
    }

    @Override
    public Resources getResources() {
        Resources resources = mBaseContext.getResources();
        if (debug) {
            LogUtil.v(TAG, "getResources(), " + resources);
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
            LogUtil.v(TAG, "getTheme(), theme=" + theme);
        }
        return theme;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferences sp = mBaseContext.getSharedPreferences(name, mode);
        if (debug) {
            LogUtil.v(TAG, "getSharedPreferences(" + name + "), " + sp);
        }
        return sp;
    }

    @Override
    public synchronized File getDatabasePath(String name) {
        File file = mBaseContext.getDatabasePath(name);
        if (debug) {
            LogUtil.v(TAG, "getDatabasePath(), " + file);
        }
        return file;
    }

//    @Override
//    public void startActivity(Intent intent) {
//        BundleManager.getInstance().startActivity(mActivity, intent);
//        super.startActivity(intent);
//    }

}
