package io.qivaz.aster.runtime.bundle;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class BundleClassLoader extends DexClassLoader {
    private static final boolean debug = BundleFeature.debug;
    private static final boolean debugPerf = BundleFeature.debugPerf;
    private static final String TAG = "BundleClassLoader";
    private static HashMap<String, HashMap<String, String>> mLoadedSoMap = new HashMap();
    private final Set mBundleLibPaths = new LinkedHashSet();
    private String mBundleName;
    private ClassLoader mParent;

    BundleClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent, String bundleName) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "BundleClassLoader.super(), finished");
        }
        mParent = parent;
        mBundleName = bundleName;
    }

    void addLibPath(String path) {
        if (path != null) {
            mBundleLibPaths.add(new File(path));
        }
    }

    String getLibPath() {
        Iterator iterator = mBundleLibPaths.iterator();
        if (iterator.hasNext()) {
            File file = (File) iterator.next();
            return file.getAbsolutePath();
        }
        return null;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return loadClass(className, false);
    }

    @Override
    public Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "loadClass(), start");
        }
        Class<?> clazz = loadBundleClassPrior(className, resolve);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "loadClass(), end");
        }
        return clazz;
    }

    private Class<?> loadBundleClassPrior(String className, boolean resolve) throws ClassNotFoundException {
        String from = "INVALID";
        Class<?> clazz = null;
        if (!className.startsWith(BundleFeature.ASTER_CLASS_PREFIX)) {
            try {
                clazz = super.loadClass(className, resolve);
                from = mBundleName;
            } catch (ClassNotFoundException e) {
                if (debug) {
                    LogUtil.v(TAG, "loadBundleClassPrior(" + className + ") in bundle \"" + mBundleName + "\" self failed!");
                }
            }
        } else {
            if (debug) {
                LogUtil.v(TAG, "loadBundleClassPrior(" + className + ") in bundle \"" + mBundleName + "\", ignored for Aster framework!");
            }
        }
        if (clazz == null) {
            try {
                clazz = getHostClassLoader().loadClass(className);
                from = "HOST";
            } catch (ClassNotFoundException e) {
                if (debug) {
                    LogUtil.v(TAG, "loadBundleClassPrior(" + className + ") in host failed!");
                }
            }
        }
        if (clazz != null) {
            if (debug) {
                LogUtil.v(TAG, "loadBundleClassPrior(" + className + ") successfully @ " + from);
            }
        } else {
            LogUtil.e(TAG, "loadBundleClassPrior(" + className + ") failed!");
            throw new ClassNotFoundException();
        }
        return clazz;
    }

    private Class<?> loadHostClassPrior(String className, boolean resolve) throws ClassNotFoundException {
        String from = "INVALID";
        Class<?> clazz = null;
        try {
            clazz = getHostClassLoader().loadClass(className);
            from = "HOST";
        } catch (ClassNotFoundException e) {
            if (debug) {
                LogUtil.v(TAG, "loadHostClassPrior(" + className + ") in host failed!");
            }
        }
        if (clazz == null) {
            try {
                clazz = super.loadClass(className, resolve);
                from = mBundleName;
            } catch (ClassNotFoundException e) {
                if (debug) {
                    LogUtil.v(TAG, "loadHostClassPrior(" + className + ") in bundle \"" + mBundleName + "\" self failed! (1)");
                }
            } catch (IllegalAccessError e) { //java.lang.IllegalAccessError: Class ref in pre-verified class resolved to unexpected implementation
                if (debug) {
                    LogUtil.v(TAG, "loadHostClassPrior(" + className + ") in bundle \"" + mBundleName + "\" self failed! (2)");
                }
                // dalvikvm: Class resolved by unexpected DEX: Lio/qivaz/plugin/PluginActivity1;(0x41e8d7a8):0x77cbd000 ref [Landroid/support/v7/app/AppCompatActivity;] Landroid/support/v7/app/AppCompatActivity;(0x41d2b3c0):0x76030000
                // dalvikvm: (Lio/qivaz/plugin/PluginActivity1; had used a different Landroid/support/v7/app/AppCompatActivity; during pre-verification)
                // dalvikvm: Unable to resolve superclass of Lio/qivaz/plugin/PluginActivity1; (1639)
                // dalvikvm: Link of class 'Lio/qivaz/plugin/PluginActivity1;' failed
                // BundleActivity: start plugin activity failed! java.lang.IllegalAccessError: Class ref in pre-verified class resolved to unexpected implementation
                // System.err: java.lang.IllegalAccessError: Class ref in pre-verified class resolved to unexpected implementation
                // System.err:     at dalvik.system.DexFile.defineClassNative(Native Method)
                // System.err:     at dalvik.system.DexFile.defineClass(DexFile.java:226)
                // System.err:     at dalvik.system.DexFile.loadClassBinaryName(DexFile.java:219)
                // System.err:     at dalvik.system.DexPathList.findClass(DexPathList.java:322)
                // System.err:     at dalvik.system.BaseDexClassLoader.findClass(BaseDexClassLoader.java:54)
                // System.err:     at java.lang.ClassLoader.loadClass(ClassLoader.java:497)
                // System.err:     at BundleClassLoader.loadHostClassPrior(BundleClassLoader.java:103)
                // System.err:     at BundleClassLoader.loadClass(BundleClassLoader.java:61)
                // System.err:     at BundleClassLoader.loadClass(BundleClassLoader.java:56)
                // System.err:     at BundleActivity.launchTargetActivity(BundleActivity.java:133)
                // System.err:     at BundleActivity.onCreate(BundleActivity.java:91)
                // System.err:     at android.app.Activity.performCreate(Activity.java:5302)
                // System.err:     at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1090)
                // System.err:     at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2228)
                // System.err:     at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2313)
                // System.err:     at android.app.ActivityThread.access$1100(ActivityThread.java:141)
                // System.err:     at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1238)
                // System.err:     at android.os.Handler.dispatchMessage(Handler.java:102)
                // System.err:     at android.os.Looper.loop(Looper.java:136)
                // System.err:     at android.app.ActivityThread.main(ActivityThread.java:5336)
                // System.err:     at java.lang.reflect.Method.invokeNative(Native Method)
                // System.err:     at java.lang.reflect.Method.invoke(Method.java:515)
                // System.err:     at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:871)
                // System.err:     at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:687)
                // System.err:     at dalvik.system.NativeStart.main(Native Method)
            }
        }

        if (clazz != null) {
            if (debug) {
                LogUtil.v(TAG, "loadHostClassPrior(" + className + ") successfully @ " + from);
            }
        } else {
            LogUtil.e(TAG, "loadHostClassPrior(" + className + ") failed!");
            throw new ClassNotFoundException();
        }
        return clazz;
    }

    private ClassLoader getHostClassLoader() {
        if (debug) {
            LogUtil.v(TAG, "getHostClassLoader(), mParent=" + mParent);
        }
        return mParent; //getClass().getClassLoader();
    }

    @Override
    public String findLibrary(String name) {
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "findLibrary(), start");
        }

        String path = null;
        if ((path = getCachedSo(name)) != null) {
            return path;
        }
        for (Object mBundleLibPath : mBundleLibPaths) {
            File folder = (File) mBundleLibPath;
            File soFile = new File(folder, System.mapLibraryName(name));
            if (debug) {
                LogUtil.v(TAG, "findLibrary(), So folder=" + folder + ", name=" + name);
            }
            if (debug) {
                LogUtil.v(TAG, "findLibrary(), So file=" + soFile.getAbsolutePath() + ", name=" + soFile.getName());
            }

            if (soFile.exists()) {
                path = soFile.getAbsolutePath();
                break;
            }
        }

        if (path == null && mParent instanceof BaseDexClassLoader) {
            path = ((BaseDexClassLoader) mParent).findLibrary(name);
        }
        if (path == null) {
            path = super.findLibrary(name);
        }

        if (path != null) {
            HashMap soMap = BundleClassLoader.mLoadedSoMap.get(mBundleName);
            if (soMap == null) {
                soMap = new HashMap();
            }

            soMap.put(name, path);
            BundleClassLoader.mLoadedSoMap.put(mBundleName, soMap);
        }

        if (debug) {
            LogUtil.v(TAG, "findLibrary(), name=" + name + ", return path=" + path);
        }
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "findLibrary(), end");
        }
        return path;
    }

    private String getCachedSo(String soName) {
        Iterator iterator = BundleClassLoader.mLoadedSoMap.keySet().iterator();
        while (iterator.hasNext()) {
            String bundleName = (String) iterator.next();
            if (!mBundleName.equals(bundleName)) {
                HashMap<String, String> soMap = BundleClassLoader.mLoadedSoMap.get(bundleName);
                String path = null;
                if (soMap != null && (path = soMap.get(soName)) != null) {
                    return path;
                }
            }
        }

        return null;
    }
}
