package io.qivaz.aster.runtime.bundle.service;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.bundle.BundleService;
import io.qivaz.aster.runtime.bundle.IContainer;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class BundleServiceContainer implements IContainer {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleServiceContainer";
    private static final int BUNLDE_SERVICE_MAX = 50;
    private static BundleServiceContainer mInstance;
    private final Map<String, String> mBundleServices = new HashMap<>();
    private final Deque<String> mBundleServiceDeque = new ArrayDeque<>();

    public static BundleServiceContainer getInstance() {
        if (mInstance == null) {
            synchronized (BundleServiceContainer.class) {
                if (mInstance == null) {
                    mInstance = new BundleServiceContainer();
                }
            }
        }
        return mInstance;
    }

    @Override
    public synchronized void initContainer() {
        for (int i = 0; i < BUNLDE_SERVICE_MAX; i++) {
            mBundleServiceDeque.offer(BundleFeature.BUNLDE_SERVICE_PREFIX + i);
        }
    }

    @Override
    public String peekContainer(String targetService) {
        synchronized (mBundleServices) {
            String service = mBundleServices.get(targetService);
            if (service == null) {
                if (debug) {
                    LogUtil.v(TAG, "peekContainer(), " + targetService + " not launched before");
                }
            }
            return service;
        }
    }

    @Override
    public String bindContainer(String targetService) {
        synchronized (mBundleServices) {
            String service = mBundleServiceDeque.poll();
            if (service == null) {
                LogUtil.e(TAG, "bindContainer(), failed, no vacant service container found! used " + mBundleServices.size() + " containers");
                StringBuilder sb = new StringBuilder();
                sb.append("\r\n");
                for (Object o : mBundleServices.keySet()) {
                    sb.insert(2, o);
                    sb.insert(2 + o.toString().length(), "\r\n");
                }
                LogUtil.e(TAG, "bindContainer(), mRegList all running services:" + sb.toString());
                return null;
            }
            mBundleServices.put(targetService, service);
            return service;
        }
    }

    @Override
    public boolean unbindContainer(String targetService) {
        synchronized (mBundleServices) {
            String service;
            if ((service = mBundleServices.remove(targetService)) != null) {
                mBundleServiceDeque.offer(service);
                if (debug) {
                    LogUtil.v(TAG, "unbindContainer(), recycled " + service + " successfully!");
                }
                return true;
            }
            LogUtil.e(TAG, "unbindContainer(), failed, not bound before? targetService=" + targetService);
            return false;
        }
    }

    public static class BundleContainerService extends BundleService {
        @Override
        public void onDestroy() {
            if (debug) {
                LogUtil.v(TAG, "onDestroy()");
            }
            super.onDestroy();
            if (getTargetService() != null) {
                getInstance().unbindContainer(getTargetService().getClass().getName());
            }
        }
    }

    public static class BundleService0 extends BundleContainerService {
    }

    public static class BundleService1 extends BundleContainerService {
    }

    public static class BundleService2 extends BundleContainerService {
    }

    public static class BundleService3 extends BundleContainerService {
    }

    public static class BundleService4 extends BundleContainerService {
    }

    public static class BundleService5 extends BundleContainerService {
    }

    public static class BundleService6 extends BundleContainerService {
    }

    public static class BundleService7 extends BundleContainerService {
    }

    public static class BundleService8 extends BundleContainerService {
    }

    public static class BundleService9 extends BundleContainerService {
    }

    public static class BundleService10 extends BundleContainerService {
    }

    public static class BundleService11 extends BundleContainerService {
    }

    public static class BundleService12 extends BundleContainerService {
    }

    public static class BundleService13 extends BundleContainerService {
    }

    public static class BundleService14 extends BundleContainerService {
    }

    public static class BundleService15 extends BundleContainerService {
    }

    public static class BundleService16 extends BundleContainerService {
    }

    public static class BundleService17 extends BundleContainerService {
    }

    public static class BundleService18 extends BundleContainerService {
    }

    public static class BundleService19 extends BundleContainerService {
    }

    public static class BundleService20 extends BundleContainerService {
    }

    public static class BundleService21 extends BundleContainerService {
    }

    public static class BundleService22 extends BundleContainerService {
    }

    public static class BundleService23 extends BundleContainerService {
    }

    public static class BundleService24 extends BundleContainerService {
    }

    public static class BundleService25 extends BundleContainerService {
    }

    public static class BundleService26 extends BundleContainerService {
    }

    public static class BundleService27 extends BundleContainerService {
    }

    public static class BundleService28 extends BundleContainerService {
    }

    public static class BundleService29 extends BundleContainerService {
    }

    public static class BundleService30 extends BundleContainerService {
    }

    public static class BundleService31 extends BundleContainerService {
    }

    public static class BundleService32 extends BundleContainerService {
    }

    public static class BundleService33 extends BundleContainerService {
    }

    public static class BundleService34 extends BundleContainerService {
    }

    public static class BundleService35 extends BundleContainerService {
    }

    public static class BundleService36 extends BundleContainerService {
    }

    public static class BundleService37 extends BundleContainerService {
    }

    public static class BundleService38 extends BundleContainerService {
    }

    public static class BundleService39 extends BundleContainerService {
    }

    public static class BundleService40 extends BundleContainerService {
    }

    public static class BundleService41 extends BundleContainerService {
    }

    public static class BundleService42 extends BundleContainerService {
    }

    public static class BundleService43 extends BundleContainerService {
    }

    public static class BundleService44 extends BundleContainerService {
    }

    public static class BundleService45 extends BundleContainerService {
    }

    public static class BundleService46 extends BundleContainerService {
    }

    public static class BundleService47 extends BundleContainerService {
    }

    public static class BundleService48 extends BundleContainerService {
    }

    public static class BundleService49 extends BundleContainerService {
    }

    public static class BundleService50 extends BundleContainerService {
    }

    public static class BundleService51 extends BundleContainerService {
    }

    public static class BundleService52 extends BundleContainerService {
    }

    public static class BundleService53 extends BundleContainerService {
    }

    public static class BundleService54 extends BundleContainerService {
    }

    public static class BundleService55 extends BundleContainerService {
    }

    public static class BundleService56 extends BundleContainerService {
    }

    public static class BundleService57 extends BundleContainerService {
    }

    public static class BundleService58 extends BundleContainerService {
    }

    public static class BundleService59 extends BundleContainerService {
    }

    public static class BundleService60 extends BundleContainerService {
    }

    public static class BundleService61 extends BundleContainerService {
    }

    public static class BundleService62 extends BundleContainerService {
    }

    public static class BundleService63 extends BundleContainerService {
    }

    public static class BundleService64 extends BundleContainerService {
    }

    public static class BundleService65 extends BundleContainerService {
    }

    public static class BundleService66 extends BundleContainerService {
    }

    public static class BundleService67 extends BundleContainerService {
    }

    public static class BundleService68 extends BundleContainerService {
    }

    public static class BundleService69 extends BundleContainerService {
    }

    public static class BundleService70 extends BundleContainerService {
    }

    public static class BundleService71 extends BundleContainerService {
    }

    public static class BundleService72 extends BundleContainerService {
    }

    public static class BundleService73 extends BundleContainerService {
    }

    public static class BundleService74 extends BundleContainerService {
    }

    public static class BundleService75 extends BundleContainerService {
    }

    public static class BundleService76 extends BundleContainerService {
    }

    public static class BundleService77 extends BundleContainerService {
    }

    public static class BundleService78 extends BundleContainerService {
    }

    public static class BundleService79 extends BundleContainerService {
    }

    public static class BundleService80 extends BundleContainerService {
    }

    public static class BundleService81 extends BundleContainerService {
    }

    public static class BundleService82 extends BundleContainerService {
    }

    public static class BundleService83 extends BundleContainerService {
    }

    public static class BundleService84 extends BundleContainerService {
    }

    public static class BundleService85 extends BundleContainerService {
    }

    public static class BundleService86 extends BundleContainerService {
    }

    public static class BundleService87 extends BundleContainerService {
    }

    public static class BundleService88 extends BundleContainerService {
    }

    public static class BundleService89 extends BundleContainerService {
    }

    public static class BundleService90 extends BundleContainerService {
    }

    public static class BundleService91 extends BundleContainerService {
    }

    public static class BundleService92 extends BundleContainerService {
    }

    public static class BundleService93 extends BundleContainerService {
    }

    public static class BundleService94 extends BundleContainerService {
    }

    public static class BundleService95 extends BundleContainerService {
    }

    public static class BundleService96 extends BundleContainerService {
    }

    public static class BundleService97 extends BundleContainerService {
    }

    public static class BundleService98 extends BundleContainerService {
    }

    public static class BundleService99 extends BundleContainerService {
    }
}
