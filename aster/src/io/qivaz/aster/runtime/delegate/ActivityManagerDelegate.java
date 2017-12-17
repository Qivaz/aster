package io.qivaz.aster.runtime.delegate;

import android.app.Instrumentation;
import android.util.Singleton;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.bundle.BundleManager;
import io.qivaz.aster.runtime.util.log.LogUtil;


/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class ActivityManagerDelegate {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "ActivityManagerDelegate";
    private static boolean mInstrumentationSet;
    private static Instrumentation mDefaultInstrumentation;

    public static void run() {
        hookInstrument();
//        hookActivityManager();
    }

    private static void hookInstrument() {
        // Inject instrumentation, just once.
        if (!mInstrumentationSet) {
            try {
                final Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
                final Method ActivityThread$currentActivityThread = ActivityThread.getMethod("currentActivityThread");
                Object activityThread = ActivityThread$currentActivityThread.invoke(null, (Object[]) null);
                final Field ActivityThread$mInstrumentation = ActivityThread.getDeclaredField("mInstrumentation");
                ActivityThread$mInstrumentation.setAccessible(true);
                mDefaultInstrumentation = (Instrumentation) ActivityThread$mInstrumentation.get(activityThread);
                Instrumentation wrapper = new InstrumentationDelegate(mDefaultInstrumentation);
                ActivityThread$mInstrumentation.set(activityThread, wrapper);

                mInstrumentationSet = true;
            } catch (Exception e) {
                LogUtil.e(TAG, "hookInstrument(), " + e);
                e.printStackTrace();
            }
        }
    }

    private static void hookActivityManager() {
        try {
            final Class<?> ActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
            final Field ActivityManagerNative$gDefault = ActivityManagerNative.getDeclaredField("gDefault");
            ActivityManagerNative$gDefault.setAccessible(true);
            Object gDefault = ActivityManagerNative$gDefault.get(null);
            if (gDefault == null) {
                final Method ActivityManagerNative$getDefault = ActivityManagerNative.getDeclaredMethod("getDefault");
                ActivityManagerNative$getDefault.setAccessible(true);
                ActivityManagerNative$getDefault.invoke(null);

                gDefault = ActivityManagerNative$gDefault.get(null);
            }

            final Object instanceProxy;
            Object instance;
            Class<?> IActivityManager = Class.forName("android.app.IActivityManager");
            Class<?> Singleton = Class.forName("android.util.Singleton");
            if (IActivityManager.isInstance(gDefault)) {
                instanceProxy = Proxy.newProxyInstance(gDefault.getClass().getClassLoader(), gDefault.getClass().getInterfaces(), new ActivityManagerHandler(gDefault));
                ActivityManagerNative$gDefault.set(null, instanceProxy);
            } else if (Singleton.isInstance(gDefault)) {
                Field instField = gDefault.getClass().getSuperclass().getDeclaredField("mInstance");
                instField.setAccessible(true);
                instance = instField.get(gDefault);
                if (instance == null) {
                    Method get = gDefault.getClass().getDeclaredMethod("get");
                    get.invoke(gDefault);

                    instance = instField.get(gDefault);
                }

                instanceProxy = Proxy.newProxyInstance(instance.getClass().getClassLoader(), instance.getClass().getInterfaces(), new ActivityManagerHandler(instance));
                ActivityManagerNative$gDefault.set(null, new Singleton() {
                    @Override
                    protected Object create() {
                        return instanceProxy;
                    }
                });
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "hookActivityManager(), " + e);
            e.printStackTrace();
        }
    }

    private static class ActivityManagerHandler implements InvocationHandler {
        private Object mDefaultActivityManager;

        public ActivityManagerHandler(Object am) {
            mDefaultActivityManager = am;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "getIntentSender":
                    if (args.length > 1 && args[1] instanceof String) {
                        args[1] = BundleManager.getInstance().getHostApplicationContext().getPackageName();
                    }
                    break;
//                case "startActivity":
//                    Object result = BundleManager.getInstance().startActivityForResult(BundleManager.getInstance().getHostApp(), (Intent)args[2], (int)args[6]);
//                    if ((boolean)result) {
//                        return 0; //ActivityManager.START_SUCCESS
//                    }
//                    break;
                case "publishContentProviders":
                    break;
            }
            Object result = method.invoke(mDefaultActivityManager, args);
            if (debug) {
                LogUtil.v(TAG, "[Default]." + method.getName() + "(" + Arrays.toString(args) + "), result=" + result);
            }
            return result;
        }
    }
}
