package io.qivaz.aster.runtime.delegate;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.ServiceManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import io.qivaz.aster.runtime.bundle.BundleEntry;
import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.bundle.BundleManager;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class ApplicationPackageManagerDelegate {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "ApplicationPackageManagerDelegate";
    private static Context mContext;

    public static void run() {
        try {
            IPackageManager iPackageManager = createPackageManager(BundleManager.getInstance().getHostApplicationContext());

            final Class<?> ApplicationPackageManager = Class.forName("android.app.ApplicationPackageManager");
            final Field ApplicationPackageManager$mPM = ApplicationPackageManager.getDeclaredField("mPM");
            ApplicationPackageManager$mPM.setAccessible(true);
            ApplicationPackageManager$mPM.set(BundleManager.getInstance().getHostApplicationContext().getPackageManager(), iPackageManager);
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "run(), " + e);
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            LogUtil.e(TAG, "run(), " + e);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "run(), " + e);
            e.printStackTrace();
        }
    }

    private static IPackageManager createPackageManager(Context context) {
        mContext = context;
        IBinder binder = ServiceManager.getService("package");
        IPackageManager pm = IPackageManager.Stub.asInterface(binder);
        return (IPackageManager) Proxy.newProxyInstance(pm.getClass().getClassLoader(), pm.getClass().getInterfaces(), new PackageManagerHandler(pm, context.getPackageManager()));
    }

    private static class PackageManagerHandler implements InvocationHandler {
        private PackageManager mPackageManager;
        private IPackageManager mDefaultPackageManager;

        public PackageManagerHandler(IPackageManager pm, PackageManager packageManager) {
            mDefaultPackageManager = pm;
            mPackageManager = packageManager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            BundleEntry bundleEntry;
            PackageInfo packageInfo;
            ComponentName componentName;
            String packageName;
            String activityName;
            int flags;

            switch (method.getName()) {
                case "getPackageInfo":
                    packageName = (String) args[0];
                    flags = (int) args[1];
                    try {
                        bundleEntry = BundleManager.getInstance().getBundleByPackageName(packageName);
                        if (bundleEntry != null && bundleEntry.mBundlePath != null) {
                            packageInfo = mPackageManager.getPackageArchiveInfo(bundleEntry.mBundlePath, flags);
                            if (packageInfo != null) {
                                packageInfo.applicationInfo = mPackageManager.getApplicationInfo(packageName, flags);
                                if (debug) {
                                    LogUtil.v(TAG, "[Proxy].getPackageInfo(), get package info for " + packageName);
                                }
                                return packageInfo;
                            }
                        }

                        // If use host package, add bundle package META data into host package info.
                        if ((flags & PackageManager.GET_META_DATA) != 0) {
                            packageInfo = (PackageInfo) method.invoke(mDefaultPackageManager, args);
                            if (packageInfo != null && packageInfo.applicationInfo != null && packageInfo.applicationInfo.metaData != null) {
                                packageInfo.applicationInfo.metaData.putAll(BundleManager.getInstance().getAllBundleMetaData());
                            }
                            if (debug) {
                                LogUtil.v(TAG, "[Default].getPackageInfo(), get package info for " + packageName + ", with bundle META data");
                            }
                            return packageInfo;
                        }
                    } catch (Exception e) {
                        LogUtil.e(TAG, "getPackageInfo(), failed to get package info for " + packageName);
                        e.printStackTrace();
                    }
                    break;
                case "getApplicationInfo":
                    packageName = (String) args[0];
                    flags = (int) args[1];
                    try {
                        bundleEntry = BundleManager.getInstance().getBundleByPackageName(packageName);
                        ApplicationInfo applicationInfo;
                        if (bundleEntry != null && (applicationInfo = bundleEntry.createApplicationInfo()) != null) {
                            if (applicationInfo != null) {
                                if (debug) {
                                    LogUtil.v(TAG, "[Proxy].getApplicationInfo(), get app info for " + packageName);
                                }
                                applicationInfo.metaData = BundleManager.getInstance().getBundleMetaData(packageName);
                                return applicationInfo;
                            }
                        }

                        // If use host application, add bundle package META data into host application info.
                        if ((flags & PackageManager.GET_META_DATA) != 0) {
                            applicationInfo = (ApplicationInfo) method.invoke(mDefaultPackageManager, args);
                            if (applicationInfo != null && applicationInfo.metaData != null) {
                                applicationInfo.metaData.putAll(BundleManager.getInstance().getAllBundleMetaData());
                            }
                            if (debug) {
                                LogUtil.v(TAG, "[Default].getApplicationInfo(), get app info for " + packageName + ", with bundle META data");
                            }
                            return applicationInfo;
                        }
                    } catch (Exception e) {
                        LogUtil.e(TAG, "getApplicationInfo(), failed to get app info for " + packageName);
                        e.printStackTrace();
                    }
                    break;
                case "getActivityInfo":
                    componentName = (ComponentName) args[0];
                    flags = (int) args[1];
                    try {
                        packageName = componentName.getPackageName();
                        activityName = componentName.getClassName();
                        bundleEntry = BundleManager.getInstance().getBundleByPackageName(packageName);
                        if (bundleEntry != null && bundleEntry.mActivityInfo != null) {
                            ActivityInfo activityInfo = bundleEntry.mActivityInfo.get(activityName);
                            if (activityInfo != null) {
                                if (debug) {
                                    LogUtil.v(TAG, "[Proxy].getActivityInfo(), get activity info for " + componentName + ", " + activityInfo);
                                }
                                return activityInfo;
                            }
                        }
                    } catch (Exception e) {
                        LogUtil.e(TAG, "getActivityInfo(), failed to get activity info for " + componentName);
                        e.printStackTrace();
                    }
                    break;
                case "resolveIntent":
                    if (debug) {
                        LogUtil.v(TAG, "[Proxy].resolveIntent(" + Arrays.toString(args) + ")");
                    }
                    break;
                case "queryIntentActivities":
                case "queryIntentServices":
                case "queryBroadcastReceivers":
                case "queryContentProviders":
                case "getServiceInfo":
                case "getReceiverInfo":
                case "getProviderInfo":
                case "resolveService":
                    break;
            }

            Object result = method.invoke(mDefaultPackageManager, args);
            if (debug) {
                LogUtil.v(TAG, "[Default]." + method.getName() + "(" + Arrays.toString(args) + "), result=" + result);
            }
            return result;
        }
    }
}
