package io.qivaz.aster.runtime.delegate;

import android.app.Activity;
import android.view.Window;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.util.log.LogUtil;


/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class WindowCallbackDelegate {
    private static final boolean debug = BundleFeature.debug;
    private static boolean mCallbackSet;
    private static Field mCallbackField;

    public static void run(Activity activity, Window window) {
        try {
            Class<?> winClass = Window.class;
            if (!mCallbackSet) {
                mCallbackField = winClass.getDeclaredField("mCallback");
                mCallbackField.setAccessible(true);

                mCallbackSet = true;
            }
            Object cb = mCallbackField.get(window);
            Object windowCallbackProxy = Proxy.newProxyInstance(winClass.getClassLoader(),
                    new Class[]{Window.Callback.class}, new WindowCallbackInvocationHandler(activity, cb));
            mCallbackField.set(window, windowCallbackProxy);
        } catch (NoSuchFieldException e) {
            LogUtil.e("WindowCallbackDelegate", "WindowCallbackDelegate.run(), " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            LogUtil.e("WindowCallbackDelegate", "WindowCallbackDelegate.run(), " + e);
            e.printStackTrace();
        } catch (Exception e) {
            LogUtil.e("WindowCallbackDelegate", "WindowCallbackDelegate.run(), " + e);
            e.printStackTrace();
        }
    }

    private static class WindowCallbackInvocationHandler implements InvocationHandler {
        private Window.Callback mCb;
        private WeakReference<Activity> mWeakRefActivity;

        public WindowCallbackInvocationHandler(Activity activity, Object cb) {
            mWeakRefActivity = new WeakReference<>(activity);
            mCb = (Window.Callback) cb;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object obj = method.invoke(mCb, args);
            LogUtil.e("WindowCallbackDelegate", "WindowCallbackDelegate..invoke(), " + method.getName() + "(" + Arrays.toString(args) + "), return(" + obj + "), " + mWeakRefActivity.get());
            return obj;
        }
    }

}
