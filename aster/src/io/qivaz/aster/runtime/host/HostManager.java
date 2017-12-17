package io.qivaz.aster.runtime.host;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.delegate.ActivityManagerDelegate;
import io.qivaz.aster.runtime.delegate.ApplicationPackageManagerDelegate;
import io.qivaz.aster.runtime.delegate.NotificationManagerDelegate;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class HostManager {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "HostManager";
    private static HostManager mInstance;
    private static boolean useHostDelegate = true;
    private Application mHostApp;
    private Context mBaseContext;
    private Context mHostAppContext;
    private ClassLoader mHostClassLoader;

    public static HostManager getInstance() {
        if (mInstance == null) {
            synchronized (HostManager.class) {
                if (mInstance == null) {
                    mInstance = new HostManager();
                }
            }
        }
        return mInstance;
    }

    public boolean getUseHostDelegate() {
        return useHostDelegate;
    }

    public void setUseHostDelegate(boolean use) {
        useHostDelegate = use;
    }

    public Context getHostApp() throws IllegalAccessException {
        if (mHostApp == null) {
            throw new IllegalAccessException("Must call HostManager.getHostApp() before use HostManager instance!");
        }
        return mHostApp;
    }

    public Context getHostAppContext() throws IllegalAccessException {
        if (mHostAppContext == null) {
            throw new IllegalAccessException("Must call HostManager.getHostAppContext() before use HostManager instance!");
        }
        return mHostAppContext;
    }

    public ClassLoader getHostClassLoader() throws IllegalAccessException {
        if (mHostClassLoader == null) {
            throw new IllegalAccessException("Must call HostManager.init() before use HostManager instance!");
        }
        return mHostClassLoader;
    }

    public void preInit(Application application) {
        mHostApp = application;
        mBaseContext = application.getBaseContext();

        ApplicationPackageManagerDelegate.run();
//        GlobalPackageManagerDelegate.run();
        ActivityManagerDelegate.run();
        NotificationManagerDelegate.run();
    }

    public void postInit(Application application) {
        hookLoadedApkClassLoader();

//        hookApplicationContext();
    }

    public void init(Application application) {
        mHostApp = application;
        mBaseContext = application.getBaseContext();
        hookLoadedApkClassLoader();

//        hookApplicationContext();

        ApplicationPackageManagerDelegate.run();
//        GlobalPackageManagerDelegate.run();
        ActivityManagerDelegate.run();
        NotificationManagerDelegate.run();
    }

    private void hookApplicationContext() {
        try {
            final Field mBaseField = ContextWrapper.class.getDeclaredField("mBase");
            mBaseField.setAccessible(true);
            mBaseContext = mHostApp.getBaseContext();
            mHostAppContext = new HostAppContext(mHostApp, mBaseContext);
            mBaseField.set(mHostApp, mHostAppContext);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void hookActivityContext(Activity activity) {
        try {
            final Field mBaseField = ContextWrapper.class.getDeclaredField("mBase");
            mBaseField.setAccessible(true);
            Context baseContext = activity.getBaseContext();
            Context hostContext = new HostActivityContext(activity, baseContext);
            mBaseField.set(activity, hostContext);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Activity's Inflater was created very early when attach()->makeNewWindow()->PhoneWindow($init)
     * LayoutInflater.from()->ContextThemeWrapper.getSystemService()->ContextThemeWrapper.mInflater
     * So, we replace it when Activity.onCreate()
     *
     * @param activity
     */
    public void hookContextThemeWrapperInflater(Activity activity) {
        try {
            if (debug) {
                LogUtil.v(TAG, "hookContextThemeWrapperInflater(), before, " + activity.getBaseContext());
            }

            final Field mInflaterField = android.view.ContextThemeWrapper.class.getDeclaredField("mInflater");
            mInflaterField.setAccessible(true);
            final LayoutInflater layoutInflater = (LayoutInflater) mInflaterField.get(activity);
            final LayoutInflater newLayoutInflater = LayoutInflater.from(mBaseContext).cloneInContext(activity);
            final LayoutInflater.Factory factory = layoutInflater.getFactory();

            //newLayoutInflater.setPrivateFactory(activity);
            try {
                Method LayoutInflater$setPrivateFactory = LayoutInflater.class.getDeclaredMethod("setPrivateFactory", new Class[]{LayoutInflater.Factory2.class});
                LayoutInflater$setPrivateFactory.setAccessible(true);
                LayoutInflater$setPrivateFactory.invoke(newLayoutInflater, activity);
            } catch (NoSuchMethodException e) {
                LogUtil.e(TAG, "hookContextThemeWrapperInflater(), " + e);
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                LogUtil.e(TAG, "hookContextThemeWrapperInflater(), " + e);
                e.printStackTrace();
            }

            newLayoutInflater.setFactory(new LayoutInflater.Factory() {
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
                        HashMap map = (HashMap) field.get(newLayoutInflater);
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
                    ClassLoader bundleClassLoader = mHostApp.getClassLoader();
                    do {
                        if (constructorLoader == bundleClassLoader) {
                            LogUtil.v(TAG, "verifyClassLoader(), verify "
                                    + constructor.getDeclaringClass()
                                    + " in the same constructor "
                                    + constructor.getDeclaringClass().getClassLoader() + " in sConstructorMap");
                            return true;
                        }
                        bundleClassLoader = bundleClassLoader.getParent();
                    } while (bundleClassLoader != null);
                    if (debug) {
                        LogUtil.v(TAG, "verifyClassLoader(), verify " + constructor.getDeclaringClass() + " with different constructors: "
                                + constructor.getDeclaringClass().getClassLoader()
                                + " <vs> "
                                + mHostApp.getClassLoader()
                                + " in sConstructorMap");
                    }
                    return false;
                }
            });
            mInflaterField.set(activity, newLayoutInflater);
            if (debug) {
                LogUtil.v(TAG, "hookContextThemeWrapperInflater(), after, " + activity.getBaseContext());
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void hookActivityWindowInflater(Activity activity) {
        try {
            Window window = activity.getWindow();
            final Class<?> PhoneWindow = Class.forName("com.android.internal.policy.impl.PhoneWindow");
            if (PhoneWindow.isAssignableFrom(window.getClass())) {
                final Field PhoneWindow$mLayoutInflater = PhoneWindow.getDeclaredField("mLayoutInflater");
                PhoneWindow$mLayoutInflater.setAccessible(true);
                Object layoutInflater = PhoneWindow$mLayoutInflater.get(window);

                final LayoutInflater newLayoutInflater = LayoutInflater.from(mBaseContext).cloneInContext(mHostApp);
                final LayoutInflater.Factory factory = newLayoutInflater.getFactory();
                newLayoutInflater.setFactory(new LayoutInflater.Factory() {
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
                            HashMap map = (HashMap) field.get(newLayoutInflater);
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
                        ClassLoader bundleClassLoader = mHostApp.getClassLoader();
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
                                    + mHostApp.getClassLoader()
                                    + " in sConstructorMap");
                        }
                        return false;
                    }
                });
                PhoneWindow$mLayoutInflater.set(window, newLayoutInflater);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void hookLoadedApkClassLoader() {
        try {
            final Class<?> ContextImpl = Class.forName("android.app.ContextImpl");
            final Field ContextImpl$mPackageInfo = ContextImpl.getDeclaredField("mPackageInfo");
            ContextImpl$mPackageInfo.setAccessible(true);
            Object loadedApk = ContextImpl$mPackageInfo.get(mHostApp.getBaseContext());
            final Class<?> LoadedApk = Class.forName("android.app.LoadedApk");
            final Method LoadedApk$getClassLoader = LoadedApk.getDeclaredMethod("getClassLoader");
            LoadedApk$getClassLoader.setAccessible(true);
            ClassLoader oldClassLoader = (ClassLoader) LoadedApk$getClassLoader.invoke(loadedApk);
            ClassLoader hostClassLoader = new HostClassLoader(oldClassLoader);
            final Field LoadedApk$mClassLoader = LoadedApk.getDeclaredField("mClassLoader");
            LoadedApk$mClassLoader.setAccessible(true);
            LoadedApk$mClassLoader.set(loadedApk, hostClassLoader);

            mHostClassLoader = hostClassLoader;
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

    private Resources getHostResourcesWrapper() {
        final Resources hostResources = mHostApp.getResources();
        Resources resources = null;
        Object resourcesImpl = null;
        try {
            // NOTICE: should continue to hook for API level below API-24
            Field mResourcesImplFiled = Resources.class.getDeclaredField("mResourcesImpl");
            mResourcesImplFiled.setAccessible(true);
            resourcesImpl = mResourcesImplFiled.get(hostResources); //ResourcesImpl

            resources = new Resources(hostResources.getAssets(), hostResources.getDisplayMetrics(), hostResources.getConfiguration()) {
                @Override
                public void getValue(String name, TypedValue outValue, boolean resolveRefs)
                        throws NotFoundException {
                    super.getValue(name, outValue, resolveRefs);
                    if (debug) {
                        LogUtil.v(TAG, "getHostResourcesWrapper().1.getValue(" + name + ", " + outValue + ", " + resolveRefs + ")");
                    }
                }

                @Override
                public void getValue(int id, TypedValue outValue, boolean resolveRefs) {
                    super.getValue(id, outValue, resolveRefs);
                    if (debug) {
                        LogUtil.v(TAG, "getHostResourcesWrapper().2.getValue(" + id + ", " + outValue + ", " + resolveRefs + ")");
                    }
                }

                @Override
                public int getIdentifier(String name, String defType, String defPackage) {
                    int ret = super.getIdentifier(name, defType, defPackage);
                    if (debug) {
                        LogUtil.v(TAG, "getHostResourcesWrapper().<host>.getIdentifier(" + name + ", " + defType + ", " + defPackage + "), return " + ret);
                    }
                    return ret;
                }
            };
            mResourcesImplFiled.set(resources, resourcesImpl);
            return resources;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (resourcesImpl == null) {
            try {
                Field mAssetsFiled = Resources.class.getDeclaredField("mAssets");
                mAssetsFiled.setAccessible(true);
                AssetManager assets = (AssetManager) mAssetsFiled.get(hostResources);

                Field mMetricsField = Resources.class.getDeclaredField("mMetrics");
                mMetricsField.setAccessible(true);
                DisplayMetrics metrics = (DisplayMetrics) mMetricsField.get(hostResources);

                Field mTmpConfigField = Resources.class.getDeclaredField("mTmpConfig");
                mTmpConfigField.setAccessible(true);
                Configuration tmpConfig = (Configuration) mTmpConfigField.get(hostResources);

                Field mCompatibilityInfoField = Resources.class.getDeclaredField("mCompatibilityInfo");
                mCompatibilityInfoField.setAccessible(true);
                Object compatibilityInfo = mCompatibilityInfoField.get(hostResources);

//            final Class<?> compatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
//            final Constructor<?> constructor = Resources.class.getConstructor(AssetManager.class, DisplayMetrics.class, Configuration.class, compatibilityInfoClass);
//            constructor.setAccessible(true);
//            resources = (Resources) constructor.newInstance(assets, metrics, tmpConfig, compatibilityInfo);
                resources = new Resources(assets, metrics, tmpConfig) {
                    @Override
                    public void getValue(String name, TypedValue outValue, boolean resolveRefs)
                            throws NotFoundException {
                        super.getValue(name, outValue, resolveRefs);
                        if (debug) {
                            LogUtil.v(TAG, "getHostResourcesWrapper().1.getValue(" + name + ", " + outValue + ", " + resolveRefs + ")");
                        }
                    }

                    @Override
                    public void getValue(int id, TypedValue outValue, boolean resolveRefs) {
                        super.getValue(id, outValue, resolveRefs);
                        if (debug) {
                            LogUtil.v(TAG, "getHostResourcesWrapper().2.getValue(" + id + ", " + outValue + ", " + resolveRefs + ")");
                        }
                    }

                    @Override
                    public int getIdentifier(String name, String defType, String defPackage) {
                        int ret = super.getIdentifier(name, defType, defPackage);
                        if (debug) {
                            LogUtil.v(TAG, "getHostResourcesWrapper().<host>.getIdentifier(" + name + ", " + defType + ", " + defPackage + "), return " + ret);
                        }
                        return ret;
                    }
                };
                mCompatibilityInfoField.set(resources, compatibilityInfo);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return resources;
    }

    private void hookHostResources() {
        Resources resources = getHostResourcesWrapper();
        try {
            final Class<?> ContextImpl = Class.forName("android.app.ContextImpl");
            Field ContextImpl$mResources = ContextImpl.getDeclaredField("mResources");
            ContextImpl$mResources.setAccessible(true);

            Context contextImpl = mHostApp.getBaseContext();
            ContextImpl$mResources.set(contextImpl, resources);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void hookHostResourcesManager() {
//        Resources resources = getHostResourcesWrapper();
//        try {
//            final Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
//            final Method getInstanceMethod = resourcesManagerClass.getDeclaredMethod("getInstance");
//            getInstanceMethod.setAccessible(true);
//            Object resourceManager = getInstanceMethod.invoke(null);
//            final Field mActiveResourcesFiled = resourcesManagerClass.getDeclaredField("mActiveResources");
//            mActiveResourcesFiled.setAccessible(true);
//            ArrayMap<Object, WeakReference<Resources>> activeResources =
//                    (ArrayMap<Object, WeakReference<Resources>>) mActiveResourcesFiled.get(resourceManager);
//
////            final Class<?> resourceKeyClass = Class.forName("android.content.res.ResourcesKey");
////            final Constructor constructor = resourceKeyClass.getConstructor(String.class, Integer.TYPE, Configuration.class, Float.TYPE);
////             //public ResourcesKey(String resDir, int displayId, Configuration overrideConfiguration, float scale)
////            constructor.newInstance()
//            LogUtil.e(TAG, "hookHostResourcesManager(), " + Arrays.toString(activeResources.values().toArray()));
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
    }

    private void hookApplicationPackageManager() {
        //ApplicationPackageManager
    }
}