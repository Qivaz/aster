package io.qivaz.aster.runtime.host;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class HostClassLoader extends ClassLoader {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "HostClassLoader";
    private ClassLoader mDelegatedClassLoader;

    public HostClassLoader(ClassLoader parent) {
        mDelegatedClassLoader = parent;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (debug) {
            LogUtil.v(TAG, "loadClass(), name=" + name);
        }
        Class<?> clazz = mDelegatedClassLoader.loadClass(name);
        return clazz;
    }

}
