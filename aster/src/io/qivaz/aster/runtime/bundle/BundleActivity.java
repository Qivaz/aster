package io.qivaz.aster.runtime.bundle;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import io.qivaz.aster.runtime.CanNotLaunchBundleAppException;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5
 */
public class BundleActivity extends Activity {
    private static final boolean debug = BundleFeature.debug;
    private final SparseArray mChildRequests = new SparseArray();
    protected String TAG = "BundleActivity";
    BundleEntry mBundleEntry;
    Theme mTheme;
    private Activity mTargetActivity;
    private String mPackageName;
    private String mClass;
    private Instrumentation mInstrumentation;
    private Intent mIntent;

//    public static Activity getContainerActivity(Activity activity) {
//        Activity container = activity != null ? activity.getParent() : null;
//        return container != null && BundleActivity.class.isAssignableFrom(container.getClass()) ? container : activity;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (debug) {
            LogUtil.v(TAG, "onCreate(), savedInstanceState=" + savedInstanceState);
        }
        if (BundleFeature.getBundleAcceleratorEnabled()) {
            BundleAccelerator.accelerate(mPackageName, mClass);
        }
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            LogUtil.e(TAG, "onCreate(), intent is null!");
            finish();
            return;
        }
        mInstrumentation = new Instrumentation();
        mIntent = intent.getParcelableExtra("bundle_intent");
        if (mIntent == null || mIntent.getComponent() == null) {
            LogUtil.e(TAG, "onCreate(), bundle intent is null, not allowed launch BundleActivity without bundle intent! mIntent=" + mIntent);
            finish();
            return;
        }
        mPackageName = mIntent.getComponent().getPackageName();
        mClass = mIntent.getComponent().getClassName();
        mBundleEntry = BundleManager.getInstance().getBundleByPackageName(mPackageName);
        if (mBundleEntry == null) {
            LogUtil.e(TAG, "onCreate(), not found bundle info. for \"" + mPackageName + "\", didn't load bundle in advance?");
            finish();
            return;
        }

        mIntent.setExtrasClassLoader(mBundleEntry.mDexClassLoader);

        if (mBundleEntry.mApp == null) {
            if (debug) {
                LogUtil.v(TAG, "onCreate(), didn't launch bundle application in advance, start launching..");
            }
            try {
                mBundleEntry.launchApplication(getApplication());
            } catch (CanNotLaunchBundleAppException e) {
                LogUtil.e(TAG, "onCreate(), can't launch bundle application, " + e);
                e.printStackTrace();
                finish();
                return;
            }
//            ActivityInfo activityInfo = mBundleEntry.mActivityInfo.get(mClass);
//            mTheme = applyTheme(this, activityInfo);
        }
        launchTargetActivity(savedInstanceState);
    }

    private Theme applyTheme(Activity activity, ActivityInfo activityInfo) {
        if (activityInfo == null) {
            return null;
        } else {
            Theme newTheme = null;
            int themeResId = activityInfo.theme != 0 ? activityInfo.theme : activityInfo.applicationInfo.theme;
            if (debug) {
                LogUtil.v(TAG, "applyTheme(), for " + activity + ", themeResId=" + themeResId);
            }
            if (themeResId != 0) {
                activity.setTheme(themeResId);
                newTheme = mBundleEntry.mResources.newTheme();
                newTheme.setTo(mBundleEntry.mApp.getTheme());

                try {
                    newTheme.applyStyle(themeResId, true);
                } catch (Exception exception) {
                    LogUtil.e(TAG, "launchTargetApplication have an exception:" + exception);
                }
            }

            return newTheme;
        }
    }

    @Override
    public Theme getTheme() {
        Theme theme = super.getTheme();
//        LogUtil.v(TAG, "getTheme(), super.getTheme()=" + super.getTheme());
        return theme;
    }

    @Override
    public PackageManager getPackageManager() {
        PackageManager packageManager = super.getPackageManager();
        if (debug) {
            LogUtil.v(TAG, "getPackageManager(), " + packageManager);
        }
        return packageManager;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (debug) {
            LogUtil.v(TAG, "onActivityResult(), requestCode=" + requestCode + ", resultCode=" + resultCode + ", intent=" + data);
        }
        if (mTargetActivity != null) {
            Activity targetActivity = mTargetActivity;
            WeakReference childRef = (WeakReference) mChildRequests.get(requestCode);
            mChildRequests.remove(requestCode);
            Activity child = childRef != null ? (Activity) childRef.get() : null;
            if (child != null && child != this) {
                targetActivity = child;
            }

            try {
                Method method = Activity.class.getDeclaredMethod("onActivityResult", new Class[]{Integer.TYPE, Integer.TYPE, Intent.class});
                method.setAccessible(true);
                if (data != null && mBundleEntry != null && mBundleEntry.mDexClassLoader != null) {
                    data.setExtrasClassLoader(mBundleEntry.mDexClassLoader);
                }

                method.invoke(targetActivity, new Object[]{Integer.valueOf(requestCode), Integer.valueOf(resultCode), data});
            } catch (Exception e) {
                LogUtil.e(TAG, "onActivityResult(), failed!" + e);
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onChildTitleChanged(Activity child, CharSequence title) {
        setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        if (debug) {
            LogUtil.v(TAG, "onConfigurationChanged(), configuration=" + configuration);
        }
        if (mTargetActivity != null) {
            mTargetActivity.onConfigurationChanged(configuration);
        }

        super.onConfigurationChanged(configuration);
    }

    @Override
    public CharSequence onCreateDescription() {
        return mTargetActivity != null ? mTargetActivity.onCreateDescription() : super.onCreateDescription();
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        boolean handle = false;
        if (mTargetActivity != null) {
            handle = mTargetActivity.onCreatePanelMenu(featureId, menu);
        }

        return handle ? true : super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public View onCreatePanelView(int featureId) {
        return mTargetActivity != null ? mTargetActivity.onCreatePanelView(featureId) : super.onCreatePanelView(featureId);
    }

    @Override
    public boolean onCreateThumbnail(Bitmap bitmap, Canvas canvas) {
        return mTargetActivity != null ? mTargetActivity.onCreateThumbnail(bitmap, canvas) : super.onCreateThumbnail(bitmap, canvas);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (debug) {
            LogUtil.v(TAG, "onNewIntent(), intent=" + intent + ", " + this);
        }
        super.onNewIntent(intent);
        if (mTargetActivity != null) {
            mInstrumentation.callActivityOnNewIntent(mTargetActivity, intent);
        }
    }

    @Override
    protected void onStart() {
        if (debug) {
            LogUtil.v(TAG, "onStart(), " + this);
        }
        super.onStart();
        if (mTargetActivity != null) {
            mInstrumentation.callActivityOnStart(mTargetActivity);
        }
    }

    @Override
    protected void onRestart() {
        if (debug) {
            LogUtil.v(TAG, "onRestart(), " + this);
        }
        super.onRestart();
        if (mTargetActivity != null) {
            mInstrumentation.callActivityOnRestart(mTargetActivity);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        if (debug) {
            LogUtil.v(TAG, "onRestoreInstanceState(), bundle=" + bundle);
        }
        super.onRestoreInstanceState(bundle);
        if (mTargetActivity != null) {
            mInstrumentation.callActivityOnRestoreInstanceState(mTargetActivity, bundle);
        }
    }

    @Override
    protected void onResume() {
        if (debug) {
            LogUtil.v(TAG, "onResume(), " + this);
        }
        super.onResume();
        if (mTargetActivity != null) {
            mInstrumentation.callActivityOnResume(mTargetActivity);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        if (debug) {
            LogUtil.v(TAG, "onSaveInstanceState(), bundle=" + bundle);
        }
        super.onSaveInstanceState(bundle);
        if (mTargetActivity != null) {
            mInstrumentation.callActivityOnSaveInstanceState(mTargetActivity, bundle);

            try {
                final Class<?> FragmentManagerImpl = Class.forName("android.app.FragmentManagerImpl");
                Method FragmentManagerImpl$noteStateNotSaved = FragmentManagerImpl.getDeclaredMethod("noteStateNotSaved", new Class[0]);
                FragmentManagerImpl$noteStateNotSaved.invoke(mTargetActivity.getFragmentManager(), new Object[0]);
            } catch (Exception e) {
                LogUtil.e(TAG, "onSaveInstanceState(), failed, " + e);
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        if (debug) {
            LogUtil.v(TAG, "onPause(), " + this);
        }
        super.onPause();
        if (mTargetActivity != null) {
            mInstrumentation.callActivityOnPause(mTargetActivity);
        }
    }

    @Override
    protected void onStop() {
        if (debug) {
            LogUtil.v(TAG, "onStop(), " + this);
        }
        super.onStop();
        if (mTargetActivity != null) {
            mInstrumentation.callActivityOnStop(mTargetActivity);
        }
    }

    @Override
    public void onDestroy() {
        if (debug) {
            LogUtil.v(TAG, "onDestroy(), " + this);
        }
        if (mTargetActivity != null) {
            Activity activity = mTargetActivity;
            mTargetActivity = null;
            try {
                mInstrumentation.callActivityOnDestroy(activity);
            } catch (Exception e) {
                LogUtil.e(TAG, "onDestroy() failed, " + e);
                e.printStackTrace();
            }
        }
        if (mInstrumentation != null) {
            mInstrumentation = null;
        }
        if (mChildRequests != null) {
            mChildRequests.clear();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        if (debug) {
            LogUtil.v(TAG, "onLowMemory(), " + this);
        }
        super.onLowMemory();
        if (mTargetActivity != null) {
            mTargetActivity.onLowMemory();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (debug) {
            LogUtil.v(TAG, "onTrimMemory(), level=" + level + ", " + this);
        }
        super.onTrimMemory(level);
        if (mTargetActivity != null) {
            mTargetActivity.onTrimMemory(level);
        }
    }

    @Override
    public String toString() {
        return TAG + " [mTargetActivity=" + mTargetActivity + "]";
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (debug) {
            LogUtil.v(TAG, "onWindowFocusChanged(), hasFocus=" + hasFocus + ", " + this);
        }
        super.onWindowFocusChanged(hasFocus);
        if (mTargetActivity != null) {
            mTargetActivity.onWindowFocusChanged(hasFocus);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (mTargetActivity != null) {
            mTargetActivity.onCreateContextMenu(menu, v, menuInfo);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return mTargetActivity != null ? mTargetActivity.onContextItemSelected(item) : super.onContextItemSelected(item);
    }

    @Override
    public void startActivityFromChild(Activity child, Intent intent, int requestCode) {
        if (debug) {
            LogUtil.v(TAG, "startActivityFromChild(), child=" + child + ", intent=" + intent + ", requestCode=" + requestCode);
        }
        Activity parent = getParent();//getContainerActivity(getParent());
        if (parent != null) {
            if (requestCode != 0) {
                mChildRequests.put(requestCode, new WeakReference(child));
            }
            parent.startActivityFromChild(this, intent, requestCode);
        } else {
            if (mTargetActivity != null) {
                try {
                    if (!BundleManager.getInstance().startActivityForResult(this, intent, requestCode)) {
                        startActivityForResult(intent, requestCode);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    super.startActivityFromChild(child, intent, requestCode);
                }
            } else {
                super.startActivityFromChild(child, intent, requestCode);
            }
        }
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        return mTargetActivity != null ? mTargetActivity.onPreparePanel(featureId, view, menu) : super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public File getFilesDir() {
        return mTargetActivity != null ? mTargetActivity.getApplicationContext().getFilesDir() : super.getFilesDir();
    }

    @Override
    public File getCacheDir() {
        return mTargetActivity != null ? mTargetActivity.getApplicationContext().getCacheDir() : super.getCacheDir();
    }

    @Override
    public File getDir(String name, int mode) {
        return mTargetActivity != null ? mTargetActivity.getApplicationContext().getDir(name, mode) : super.getDir(name, mode);
    }

    @Override
    public void finishFromChild(Activity child) {
        if (debug) {
            LogUtil.v(TAG, "finishFromChild(), child=" + child);
        }
        if (child != null) {
            try {
                final Field mResultCodeField = Activity.class.getDeclaredField("mResultCode");
                mResultCodeField.setAccessible(true);
                int resultCode = (int) mResultCodeField.get(child);
                final Field mResultDataField = Activity.class.getDeclaredField("mResultData");
                mResultDataField.setAccessible(true);
                Intent resultData = (Intent) mResultDataField.get(child);
                setResult(resultCode, resultData);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        super.finishFromChild(child);
    }

    public Activity getTargetActivity() {
        return mTargetActivity;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        LogUtil.w(TAG, "onRequestPermissionsResult(), requestCode=" + requestCode + ", permissions=" + Arrays.toString(permissions) + ", results=" + Arrays.toString(grantResults));
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initWindowDecorActionBar() {
        try {
            final Method Activity$initWindowDecorActionBar = Activity.class.getDeclaredMethod("initWindowDecorActionBar");
            Activity$initWindowDecorActionBar.setAccessible(true);
            if (debug) {
                LogUtil.v(TAG, "initWindowDecorActionBar(), " + this.getWindow());
            }
            Activity$initWindowDecorActionBar.invoke(this);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void applyChildWindowStyle(Window parentWindow, Window childWindow) {
        final Class<?> Window = Window.class;
        try {
            final Field Window$mWindowStyle = Window.getDeclaredField("mWindowStyle");
            Window$mWindowStyle.setAccessible(true);
            Object style = childWindow.getWindowStyle(); //Window$mWindowStyle.get(childWindow);
            Window$mWindowStyle.set(parentWindow, style);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void applyParentActivityMainThread(Activity parentActivity, Activity childActivity) {
        try {
            final Field Activity$mMainThread = Activity.class.getDeclaredField("mMainThread");
            Activity$mMainThread.setAccessible(true);
            Object mainThread = Activity$mMainThread.get(parentActivity);
            if (mainThread != null) {
                Activity$mMainThread.set(childActivity, mainThread);
            } else {
                LogUtil.e(TAG, "applyParentActivityMainThread(), failed, parent's window is null.");
            }
        } catch (NoSuchFieldException e) {
            LogUtil.e(TAG, "applyParentActivityMainThread(), failed, " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "applyParentActivityMainThread(), failed, " + e);
            e.printStackTrace();
        }
    }

    private void applyParentActivityWindowManager(Activity parentActivity, Activity childActivity) {
        try {
            final Field Activity$mWindowManager = Activity.class.getDeclaredField("mWindowManager");
            Activity$mWindowManager.setAccessible(true);
            Object windowManager = parentActivity.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                Activity$mWindowManager.set(childActivity, windowManager);
            } else {
                LogUtil.e(TAG, "applyParentActivityWindowManager(), failed, parent's window is null.");
            }
        } catch (NoSuchFieldException e) {
            LogUtil.e(TAG, "applyParentActivityWindowManager(), failed, " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "applyParentActivityWindowManager(), failed, " + e);
            e.printStackTrace();
        }
    }

    private void applyChildWindowDecor(Window parentWindow, Window childWindow) {
        try {
            Class<?> PhoneWindow;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PhoneWindow = Class.forName("com.android.internal.policy.PhoneWindow");
            } else {
                PhoneWindow = Class.forName("com.android.internal.policy.impl.PhoneWindow");
            }
            final Field PhoneWindow$mDecor = PhoneWindow.getDeclaredField("mDecor");
            PhoneWindow$mDecor.setAccessible(true);
            Object decorView = childWindow.peekDecorView();
            if (decorView != null && PhoneWindow.isAssignableFrom(parentWindow.getClass())) {
                parentWindow.getDecorView();
                PhoneWindow$mDecor.set(parentWindow, decorView);

                parentWindow.setAttributes(childWindow.getAttributes());
                parentWindow.setCallback(childWindow.getCallback());

//                childWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
//                ViewGroup contentParent = (ViewGroup)childWindow.findViewById(android.R.id.content); //ID_ANDROID_CONTENT
//                contentParent.setBackground(new ColorDrawable(0x00000000));
            } else {
                LogUtil.e(TAG, "applyChildWindowDecor(), failed, parent window is not inherited from PhoneWindow.");
            }
        } catch (NoSuchFieldException e) {
            LogUtil.e(TAG, "applyChildWindowDecor(), failed, " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "applyChildWindowDecor(), failed, " + e);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "applyChildWindowDecor(), failed, " + e);
            e.printStackTrace();
        }
    }

    private void setParent(Activity child, Activity parent) {
        try {
            final Field Activity$mParent = Activity.class.getDeclaredField("mParent");
            Activity$mParent.setAccessible(true);
            Activity$mParent.set(child, parent);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    protected void launchTargetActivity(Bundle bundle) {
        if (debug) {
            LogUtil.v(TAG, "launchTargetActivity(), start");
        }
        try {
            Class<?> clazz = mBundleEntry.mDexClassLoader.loadClass(mClass);
            ActivityInfo activityInfo = mBundleEntry.mActivityInfo.get(mClass);
            String title = null;
            int themeResId = 0;
            if (activityInfo != null) {
                if (activityInfo.labelRes != 0) {
                    title = mBundleEntry.mResources.getString(activityInfo.labelRes);
                } else if (activityInfo.nonLocalizedLabel != null) {
                    title = String.valueOf(activityInfo.nonLocalizedLabel);
                }
                themeResId = activityInfo.theme != 0 ? activityInfo.theme : activityInfo.applicationInfo.theme;
            }
            if (themeResId == 0) {
                themeResId = BundleFeature.getDefaultTheme();
            }

            Context baseContext = new BundleActivityContext(mBundleEntry.mApp.getBaseContext(), themeResId, this);
            mTargetActivity = mInstrumentation.newActivity(clazz,
                    baseContext,
                    new Binder(),
                    mBundleEntry.mApp,
                    mIntent,
                    activityInfo,
                    title,
                    null/*this*/, /*set as mParent*/
                    "",
                    getLastNonConfigurationInstance());
            applyParentActivityMainThread(this, mTargetActivity);
            if (mTargetActivity.getWindow() == null) {
                LogUtil.e(TAG, "launchTargetActivity(), failed to launch activity: " + mIntent);
                finish();
                return;
            }

            mTargetActivity.setTheme(themeResId);
            mTargetActivity.getWindow().setTitle(title);

            setParent(mTargetActivity, this);
            applyParentActivityWindowManager(this, mTargetActivity);
            mInstrumentation.callActivityOnCreate(mTargetActivity, bundle);
            applyChildWindowDecor(getWindow(), mTargetActivity.getWindow());
        } catch (Exception | Error e) {
            LogUtil.e(TAG, "launchTargetActivity(), start bundle activity failed! " + e);
            e.printStackTrace();
            finish();
        }
        if (debug) {
            LogUtil.v(TAG, "launchTargetActivity(), finished successfully!");
        }
    }
}
