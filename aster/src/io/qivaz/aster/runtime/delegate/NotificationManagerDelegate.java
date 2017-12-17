package io.qivaz.aster.runtime.delegate;

import android.app.Notification;
import android.app.NotificationManager;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.os.ServiceManager;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.bundle.BundleManager;
import io.qivaz.aster.runtime.host.HostManager;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class NotificationManagerDelegate {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "NotificationManagerDelegate";

    public static void run() {
        Object iNotificationManager; //INotificationManager
        Field field;
        try {
            iNotificationManager = createNotificationManager();
            field = NotificationManager.class.getDeclaredField("sService");
            field.setAccessible(true);
            field.set(null, iNotificationManager);
            field = Toast.class.getDeclaredField("sService");
            field.setAccessible(true);
            field.set(null, iNotificationManager);
        } catch (Exception e) {
            LogUtil.e(TAG, "run(), failed, " + e);
            e.printStackTrace();
        }
    }

    private static Object createNotificationManager() {
        try {
            /*
                private static INotificationManager sService;
                static private INotificationManager getService() {
                    if (sService != null) {
                        return sService;
                    }
                    sService = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                    return sService;
                }
             */
            final Class<?> INotificationManager$Stub = Class.forName("android.app.INotificationManager$Stub");
            final Method INotificationManager$Stub$asInterface = INotificationManager$Stub.getDeclaredMethod("asInterface", new Class[]{IBinder.class});
            INotificationManager$Stub$asInterface.setAccessible(true);
            Object service = INotificationManager$Stub$asInterface.invoke(null, new Object[]{ServiceManager.getService("notification")});
            Class[] classes = service.getClass().getInterfaces();
            return Proxy.newProxyInstance(service.getClass().getClassLoader(), classes, new NotificationHandler(service));
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "createNotificationManager(), failed, " + e);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "createNotificationManager(), failed, " + e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "createNotificationManager(), failed, " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "createNotificationManager(), failed, " + e);
            e.printStackTrace();
        }
        return null;
    }

    private static class NotificationHandler implements InvocationHandler {
        private Object mDefaultNotificationManager;

        public NotificationHandler(Object notificationManager) {
            this.mDefaultNotificationManager = notificationManager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "enqueueNotificationWithTag":
                    if (args.length > 0 && args[0] instanceof String) {
                        args[0] = BundleManager.getInstance().getHostApplicationContext().getPackageName();
                    }
                    if (args.length > 4 && args[4] instanceof Notification) {
                        Notification notification = (Notification) args[4];
                        try {
                            Icon smallIcon = notification.getSmallIcon();

//                            final Field iconInt1Field = Icon.class.getDeclaredField("mInt1");
//                            iconInt1Field.setAccessible(true);
//                            int resId = (int) iconInt1Field.get(smallIcon);
//                            LogUtil.v(TAG, "enqueueNotificationWithTag(), resId=" + resId);

                            final Field iconString1Field = Icon.class.getDeclaredField("mString1");
                            iconString1Field.setAccessible(true);
                            String packageName = (String) iconString1Field.get(smallIcon);
                            if (!packageName.equals(args[0])) {
                                iconString1Field.set(smallIcon, args[0]);
                                if (debug) {
                                    LogUtil.v(TAG, "enqueueNotificationWithTag(), Icon.mString1(" + packageName + "-->" + args[0] + ")");
                                }
                            }
                        } catch (NoSuchMethodError e) {
                            LogUtil.e(TAG, "enqueueNotificationWithTag(), failed, " + e);
                        }
                    }
                    break;
                case "cancelNotificationWithTag":
                case "enqueueToast":
                case "cancelToast":
                    if (args.length > 0 && args[0] instanceof String) {
                        if (debug) {
                            LogUtil.v(TAG, method.getName() + "(), Icon.mString1(" + args[0] + "-->" + BundleManager.getInstance().getHostApplicationContext().getPackageName() + ")");
                        }
                        args[0] = HostManager.getInstance().getHostApp().getPackageName();
                    }
                    break;
            }

            Object result = method.invoke(this.mDefaultNotificationManager, args);
            if (debug) {
                LogUtil.v(TAG, "[Default]." + method.getName() + "(" + Arrays.toString(args) + "), result=" + result);
            }
            return result;
        }
    }
}
