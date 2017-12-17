package io.qivaz.aster.runtime.delegate;

import android.content.pm.IPackageManager;
import android.os.IBinder;
import android.os.ServiceManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class GlobalPackageManagerDelegate {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "GlobalPackageManagerDelegate";

    public static void run() {
        try {
            IPackageManager iPackageManager = createPackageManager();

            final Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
            final Method ActivityThread$currentActivityThread = ActivityThread.getMethod("currentActivityThread");
            ActivityThread$currentActivityThread.setAccessible(true);
            Object thread = ActivityThread$currentActivityThread.invoke(null, (Object[]) null);
            final Method ActivityThread$getPackageManager = ActivityThread.getMethod("getPackageManager");
            ActivityThread$getPackageManager.setAccessible(true);
            ActivityThread$getPackageManager.invoke(thread);
            final Field ActivityThread$sPackageManager = ActivityThread.getDeclaredField("sPackageManager");
            ActivityThread$sPackageManager.setAccessible(true);
            ActivityThread$sPackageManager.set(null, iPackageManager);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static IPackageManager createPackageManager() {
        IBinder binder = ServiceManager.getService("package");
        IPackageManager pm = IPackageManager.Stub.asInterface(binder);
        return (IPackageManager) Proxy.newProxyInstance(pm.getClass().getClassLoader(), pm.getClass().getInterfaces(), new PackageManagerHandler(pm));
    }

    private static class PackageManagerHandler implements InvocationHandler {
        private IPackageManager mDefaultPackageManager;

        public PackageManagerHandler(IPackageManager pm) {
            mDefaultPackageManager = pm;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "getPackageInfo":
                case "getApplicationInfo":
                case "getActivityInfo":
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
