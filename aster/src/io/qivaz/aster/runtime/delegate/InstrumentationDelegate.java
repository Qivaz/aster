package io.qivaz.aster.runtime.delegate;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.FragmentManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;

import java.lang.ref.WeakReference;

import io.qivaz.aster.runtime.bundle.BundleAccelerator;
import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.host.HostManager;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class InstrumentationDelegate extends Instrumentation {
    private static final boolean debug = BundleFeature.debug;
    private static Context mContext;
    private Instrumentation mInstrumentation;
    private WeakReference<Activity> mWeakRefCurActivity;
    private FragmentManager mFm;

    public InstrumentationDelegate(Instrumentation inst) {
        mInstrumentation = inst;
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        if (HostManager.getInstance().getUseHostDelegate()) {
            HostManager.getInstance().hookContextThemeWrapperInflater(activity);
            HostManager.getInstance().hookActivityContext(activity);
        }
        if (BundleFeature.getBundleAcceleratorEnabled()) {
            BundleAccelerator.accelerate(activity.getPackageName(), activity.getClass().getName());
        }
        mInstrumentation.callActivityOnCreate(activity, icicle);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void callActivityOnCreate(Activity activity, Bundle icicle,
                                     PersistableBundle persistentState) {
        if (HostManager.getInstance().getUseHostDelegate()) {
            HostManager.getInstance().hookContextThemeWrapperInflater(activity);
            HostManager.getInstance().hookActivityContext(activity);
        }
        if (BundleFeature.getBundleAcceleratorEnabled()) {
            BundleAccelerator.accelerate(activity.getPackageName(), activity.getClass().getName());
        }
        mInstrumentation.callActivityOnCreate(activity, icicle, persistentState);
    }

    @Override
    public Activity newActivity(Class<?> clazz, Context context,
                                IBinder token, Application application, Intent intent, ActivityInfo info,
                                CharSequence title, Activity parent, String id,
                                Object lastNonConfigurationInstance) throws InstantiationException,
            IllegalAccessException {
        Activity activity = mInstrumentation.newActivity(clazz, context, token, application, intent, info, title, parent, id, lastNonConfigurationInstance);
        return activity;
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className,
                                Intent intent)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        Activity activity = mInstrumentation.newActivity(cl, className, intent);
        return activity;
    }


    @Override
    public void callActivityOnNewIntent(Activity activity, Intent intent) {
        mInstrumentation.callActivityOnNewIntent(activity, intent);
    }

    @Override
    public void callActivityOnStart(Activity activity) {
        mInstrumentation.callActivityOnStart(activity);
    }

    @Override
    public void callActivityOnRestart(Activity activity) {
        mInstrumentation.callActivityOnRestart(activity);
    }

    @Override
    public void callActivityOnResume(Activity activity) {
        mInstrumentation.callActivityOnResume(activity);
    }

    @Override
    public void callActivityOnPause(Activity activity) {
        mInstrumentation.callActivityOnPause(activity);
    }

    @Override
    public void callActivityOnStop(Activity activity) {
        mInstrumentation.callActivityOnStop(activity);
    }

    @Override
    public void callActivityOnDestroy(Activity activity) {
        mInstrumentation.callActivityOnDestroy(activity);
    }

    @Override
    public void callActivityOnUserLeaving(Activity activity) {
        mInstrumentation.callActivityOnUserLeaving(activity);
    }

}
