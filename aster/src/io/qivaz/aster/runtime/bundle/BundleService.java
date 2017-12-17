package io.qivaz.aster.runtime.bundle;

import android.app.Application;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.IBinder;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.qivaz.aster.runtime.CanNotLaunchBundleAppException;
import io.qivaz.aster.runtime.util.log.LogUtil;

public class BundleService extends Service {
    /**
     * Level for {@link #onTrimMemory(int)}: the process is nearing the end
     * of the background LRU mRegList, and if more memory isn't found soon it will
     * be killed.
     */
    static final int TRIM_MEMORY_COMPLETE = 80;
    /**
     * Level for {@link #onTrimMemory(int)}: the process is around the middle
     * of the background LRU mRegList; freeing memory can help the system keep
     * other processes running later in the mRegList for better overall performance.
     */
    static final int TRIM_MEMORY_MODERATE = 60;
    /**
     * Level for {@link #onTrimMemory(int)}: the process has gone on to the
     * LRU mRegList.  This is a good opportunity to clean up resources that can
     * efficiently and quickly be re-built if the user returns to the app.
     */
    static final int TRIM_MEMORY_BACKGROUND = 40;
    /**
     * Level for {@link #onTrimMemory(int)}: the process had been showing
     * a user interface, and is no longer doing so.  Large allocations with
     * the UI should be released at this point to allow memory to be better
     * managed.
     */
    static final int TRIM_MEMORY_UI_HIDDEN = 20;
    /**
     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
     * background process, but the device is running extremely low on memory
     * and is about to not be able to keep any background processes running.
     * Your running process should free up as many non-critical resources as it
     * can to allow that memory to be used elsewhere.  The next thing that
     * will happen after this is {@link #onLowMemory()} called to report that
     * nothing at all can be kept in the background, a situation that can start
     * to notably impact the user.
     */
    static final int TRIM_MEMORY_RUNNING_CRITICAL = 15;
    /**
     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
     * background process, but the device is running low on memory.
     * Your running process should free up unneeded resources to allow that
     * memory to be used elsewhere.
     */
    static final int TRIM_MEMORY_RUNNING_LOW = 10;
    /**
     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
     * background process, but the device is running moderately low on memory.
     * Your running process may want to release some unneeded resources for
     * use elsewhere.
     */
    static final int TRIM_MEMORY_RUNNING_MODERATE = 5;
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleService";
    private Service mTargetService;
    private BundleEntry mBundleEntry;
    private String mPackageName;
    private String mClass;
    private Intent mIntent;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug) {
            LogUtil.v(TAG, "onStartCommand(), intent=" + intent + ", flags=" + flags + ", startId=" + startId);
        }
        launchTargetService(intent);
        if (mTargetService != null) {
            mTargetService.onStartCommand(getBundleIntent(intent), flags, startId);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (debug) {
            LogUtil.v(TAG, "onBind(), intent=" + intent);
        }
        launchTargetService(intent);
        if (mTargetService != null) {
            mTargetService.onCreate();
            return mTargetService.onBind(getBundleIntent(intent));
        } else {
            return null;
        }
    }

    @Override
    public void onRebind(Intent intent) {
        if (debug) {
            LogUtil.v(TAG, "onRebind(), intent=" + intent);
        }
        super.onRebind(intent);
        if (mTargetService != null) {
            mTargetService.onRebind(getBundleIntent(intent));
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (debug) {
            LogUtil.v(TAG, "onUnbind(), intent=" + intent);
        }
        if (mTargetService != null) {
            mTargetService.onUnbind(intent);
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        if (debug) {
            LogUtil.v(TAG, "onCreate()");
        }
        if (mTargetService != null) {
            mTargetService.onCreate();
        }
    }

    @Override
    public void onDestroy() {
        if (debug) {
            LogUtil.v(TAG, "onDestroy()");
        }
        super.onDestroy();
        if (mTargetService != null) {
            mTargetService.onDestroy();
        }
    }

    @Override
    public void onTaskRemoved(Intent intent) {
        if (debug) {
            LogUtil.v(TAG, "onTaskRemoved(), intent=" + intent);
        }
        super.onTaskRemoved(intent);
        if (mTargetService != null) {
            mTargetService.onTaskRemoved(intent);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (debug) {
            LogUtil.v(TAG, "onTrimMemory(), level=" + level);
        }
        super.onTrimMemory(level);
        if (mTargetService != null) {
            mTargetService.onTrimMemory(level);
        }
    }

    @Override
    public void onLowMemory() {
        if (debug) {
            LogUtil.v(TAG, "onLowMemory()");
        }
        super.onLowMemory();
        if (mTargetService != null) {
            mTargetService.onLowMemory();
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (debug) {
            LogUtil.v(TAG, "onStart(), intent=" + intent + ", startId=" + startId);
        }
        super.onStart(intent, startId);
        if (mTargetService != null && !IntentService.class.isAssignableFrom(mTargetService.getClass())) {
            mTargetService.onStart(getBundleIntent(intent), startId);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        if (debug) {
            LogUtil.v(TAG, "onConfigurationChanged(), configuration=" + configuration);
        }
        if (mTargetService != null) {
            mTargetService.onConfigurationChanged(configuration);
        }
    }

    @Override
    public Context getApplicationContext() {
        if (debug) {
            LogUtil.v(TAG, "getApplicationContext()");
        }
        return mBundleEntry != null && mBundleEntry.mApp != null ? mBundleEntry.mApp.getApplicationContext() : super.getApplicationContext();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        if (debug) {
            LogUtil.v(TAG, "getApplicationInfo()");
        }
        return mBundleEntry != null ? mBundleEntry.createApplicationInfo() : super.getApplicationInfo();
    }

    @Override
    public Resources getResources() {
        if (debug) {
            LogUtil.v(TAG, "getResources()");
        }
        return mBundleEntry != null && mBundleEntry.mApp != null ? mBundleEntry.mApp.getResources() : super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if (debug) {
            LogUtil.v(TAG, "getAssets()");
        }
        return mBundleEntry != null && mBundleEntry.mAssetManager != null ? mBundleEntry.mAssetManager : super.getAssets();
    }

    @Override
    public ClassLoader getClassLoader() {
        if (debug) {
            LogUtil.v(TAG, "getClassLoader()");
        }
        return mBundleEntry != null && mBundleEntry.mDexClassLoader != null ? mBundleEntry.mDexClassLoader : super.getClassLoader();
    }

    @Override
    public String getPackageName() {
        if (debug) {
            LogUtil.v(TAG, "getPackageName()");
        }
        return mBundleEntry != null && mBundleEntry.mApp != null ? mBundleEntry.mApp.getPackageName() : super.getPackageName();
    }

    @Override
    public File getFilesDir() {
        if (debug) {
            LogUtil.v(TAG, "getFilesDir()");
        }
        return mBundleEntry != null && mBundleEntry.mApp != null ? mBundleEntry.mApp.getFilesDir() : super.getFilesDir();
    }

    @Override
    public File getCacheDir() {
        if (debug) {
            LogUtil.v(TAG, "getCacheDir()");
        }
        return mBundleEntry != null && mBundleEntry.mApp != null ? mBundleEntry.mApp.getCacheDir() : super.getCacheDir();
    }

    @Override
    public File getDir(String name, int mode) {
        if (debug) {
            LogUtil.v(TAG, "getDir()");
        }
        return mBundleEntry != null && mBundleEntry.mApp != null ? mBundleEntry.mApp.getDir(name, mode) : super.getDir(name, mode);
    }

    public Intent getBundleIntent(Intent intent) {
        if (intent == null) {
            LogUtil.e(TAG, "getBundleIntent(), bundle intent is null, not allowed launch BundleService without bundle intent! intent=" + intent);
            stopSelf();
            return null;
        } else {
            if (mIntent != null) {
                return mIntent;
            }

            mIntent = intent.getParcelableExtra("bundle_intent");
            if (mIntent == null || mIntent.getComponent() == null) {
                LogUtil.e(TAG, "getBundleIntent(), bundle intent is null, not allowed launch BundleService without bundle intent! intent=" + intent);
                stopSelf();
                return null;
            }
            mPackageName = mIntent.getComponent().getPackageName();
            mClass = mIntent.getComponent().getClassName();
            mBundleEntry = BundleManager.getInstance().getBundleByPackageName(mPackageName);
            if (mBundleEntry == null) {
                LogUtil.e(TAG, "getBundleIntent(), mBundleEntry is null, for package " + mPackageName + "! intent=" + intent);
                stopSelf();
                return null;
            }
            mIntent.setExtrasClassLoader(mBundleEntry.mDexClassLoader);

            return mIntent;
        }
    }

    public Service getTargetService() {
        return mTargetService;
    }

    private void launchTargetService(Intent intent) {
        if (mTargetService == null) {
            if (intent == null) {
                LogUtil.e(TAG, "launchTargetService(), intent is null");
                stopSelf();
                return;
            }

            mIntent = intent.getParcelableExtra("bundle_intent");
            if (mIntent == null || mIntent.getComponent() == null) {
                LogUtil.e(TAG, "launchTargetService(), bundle intent is null, not allowed launch BundleService without bundle intent! mIntent=" + mIntent);
                stopSelf();
                return;
            }
            mPackageName = mIntent.getComponent().getPackageName();
            mClass = mIntent.getComponent().getClassName();
            mBundleEntry = BundleManager.getInstance().getBundleByPackageName(mPackageName);
            if (mBundleEntry == null) {
                stopSelf();
                return;
            }
            mIntent.setExtrasClassLoader(mBundleEntry.mDexClassLoader);
            if (mBundleEntry.mApp == null) {
                LogUtil.w(TAG, "launchTargetService(), didn't launch bundle application in advance, start launching..");
                try {
                    mBundleEntry.launchApplication(getApplication());
                } catch (CanNotLaunchBundleAppException e) {
                    LogUtil.e(TAG, "launchTargetService(), can't launch bundle application, " + e);
                    e.printStackTrace();
                    stopSelf();
                    return;
                }
            }

            Class<?> localClass = null;
            try {
                localClass = mBundleEntry.mDexClassLoader.loadClass(mClass);
                if (IntentService.class.isAssignableFrom(localClass)) {
                    Constructor constructor = localClass.getDeclaredConstructor(new Class[]{String.class});
                    mTargetService = (Service) constructor.newInstance(new Object[]{mClass});
                } else {
                    mTargetService = (Service) localClass.newInstance();
                }
            } catch (ClassNotFoundException e) {
                LogUtil.e(TAG, "launchTargetService(), can't find class \"" + mClass + "\" in bundle " + mBundleEntry.mPackageName);
                e.printStackTrace();
                return;
            } catch (InstantiationException e) {
                LogUtil.e(TAG, "launchTargetService(), can't instantiate class \"" + mClass + "\" in bundle " + mBundleEntry.mPackageName);
                e.printStackTrace();
                return;
            } catch (IllegalAccessException e) {
                LogUtil.e(TAG, "launchTargetService(), can't access class \"" + mClass + "\" in bundle " + mBundleEntry.mPackageName);
                e.printStackTrace();
                return;
            } catch (NoSuchMethodException e) {
                LogUtil.e(TAG, "launchTargetService(), can't find such method in class \"" + mClass + "\" in bundle " + mBundleEntry.mPackageName);
                e.printStackTrace();
                return;
            } catch (InvocationTargetException e) {
                LogUtil.e(TAG, "launchTargetService(), can't invoke in class \"" + mClass + "\" in bundle " + mBundleEntry.mPackageName);
                e.printStackTrace();
                return;
            }

            attachService(mTargetService, this, mClass, mBundleEntry != null ? mBundleEntry.mApp : getApplication());
            mTargetService.onCreate();
        } else {
            LogUtil.w(TAG, "launchTargetService(), only allowed to launch once, already launched for " + mClass);
        }
    }

    private void attachService(Object object, Context baseContext, String className, Application application) {
        try {
            final Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
            final Method Service$attach = Service.class.getDeclaredMethod("attach", new Class[]{Context.class, ActivityThread, String.class, IBinder.class, Application.class, Object.class});
            Service$attach.setAccessible(true);
            Service$attach.invoke(object, new Object[]{baseContext, null, className, null, application, null});
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "attachService(), failed, " + e);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "attachService(), failed, " + e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "attachService(), failed, " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "attachService(), failed, " + e);
            e.printStackTrace();
        }
    }
}
