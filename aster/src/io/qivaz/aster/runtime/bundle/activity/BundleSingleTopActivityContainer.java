package io.qivaz.aster.runtime.bundle.activity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import io.qivaz.aster.runtime.bundle.BundleActivity;
import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.bundle.IContainer;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class BundleSingleTopActivityContainer implements IContainer {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleSingleTopActivityContainer";
    private static final int BUNLDE_ACTIVITY_CONTAINER_MAX = 20;
    private static BundleSingleTopActivityContainer mInstance;
    private final Map<String, String> mBundleActivities = new HashMap<>();
    private final Deque<String> mBundleActivityDeque = new ArrayDeque<>();

    public static BundleSingleTopActivityContainer getInstance() {
        if (mInstance == null) {
            synchronized (BundleSingleTopActivityContainer.class) {
                if (mInstance == null) {
                    mInstance = new BundleSingleTopActivityContainer();
                }
            }
        }
        return mInstance;
    }

    @Override
    public synchronized void initContainer() {
        for (int i = 0; i < BUNLDE_ACTIVITY_CONTAINER_MAX; i++) {
            mBundleActivityDeque.offer(BundleFeature.BUNLDE_ACTIVITY_TOP_PREFIX + i);
        }
    }

    @Override
    public String peekContainer(String targetActivity) {
        synchronized (mBundleActivities) {
            String activity = mBundleActivities.get(targetActivity);
            if (activity == null) {
                if (debug) {
                    LogUtil.v(TAG, "peekContainer(), " + targetActivity + " not launched before");
                }
            }
            return activity;
        }
    }

    @Override
    public String bindContainer(String targetActivity) {
        synchronized (mBundleActivities) {
            String activity = mBundleActivityDeque.poll();
            if (activity == null) {
                LogUtil.e(TAG, "bindContainer(), failed, no vacant activity container found! used " + mBundleActivities.size() + " containers");
                StringBuilder sb = new StringBuilder();
                sb.append("\r\n");
                for (Object o : mBundleActivities.keySet()) {
                    sb.insert(2, o);
                    sb.insert(2 + o.toString().length(), "\r\n");
                }
                LogUtil.e(TAG, "bindContainer(), mRegList all running activities:" + sb.toString());
                return null;
            }
            mBundleActivities.put(targetActivity, activity);
            return activity;
        }
    }

    @Override
    public boolean unbindContainer(String targetActivity) {
        synchronized (mBundleActivities) {
            String activity;
            if ((activity = mBundleActivities.remove(targetActivity)) != null) {
                mBundleActivityDeque.offer(activity);
                if (debug) {
                    LogUtil.v(TAG, "unbindContainer(), recycled " + activity + " successfully!");
                }
                return true;
            }
            LogUtil.e(TAG, "unbindContainer(), failed, not bound before? targetActivity=" + targetActivity);
            return false;
        }
    }

    public static class BundleSingleTopActivity extends BundleActivity {
        public BundleSingleTopActivity() {
            super();
            TAG = "BundleSingleTopActivity";
        }

        @Override
        public void onDestroy() {
            if (debug) {
                LogUtil.v(TAG, "onDestroy()");
            }
            super.onDestroy();
            if (getTargetActivity() != null) {
                getInstance().unbindContainer(getTargetActivity().getClass().getName());
            }
        }
    }


    public static class BundleActivity0 extends BundleSingleTopActivity {
        public BundleActivity0() {
            super();
            TAG = "BundleSingleTopActivity0";
        }
    }

    public static class BundleActivity1 extends BundleSingleTopActivity {
        public BundleActivity1() {
            super();
            TAG = "BundleSingleTopActivity1";
        }
    }

    public static class BundleActivity2 extends BundleSingleTopActivity {
        public BundleActivity2() {
            super();
            TAG = "BundleSingleTopActivity2";
        }
    }

    public static class BundleActivity3 extends BundleSingleTopActivity {
        public BundleActivity3() {
            super();
            TAG = "BundleSingleTopActivity3";
        }
    }

    public static class BundleActivity4 extends BundleSingleTopActivity {
        public BundleActivity4() {
            super();
            TAG = "BundleSingleTopActivity4";
        }
    }

    public static class BundleActivity5 extends BundleSingleTopActivity {
        public BundleActivity5() {
            super();
            TAG = "BundleSingleTopActivity5";
        }
    }

    public static class BundleActivity6 extends BundleSingleTopActivity {
        public BundleActivity6() {
            super();
            TAG = "BundleSingleTopActivity6";
        }
    }

    public static class BundleActivity7 extends BundleSingleTopActivity {
        public BundleActivity7() {
            super();
            TAG = "BundleSingleTopActivity7";
        }
    }

    public static class BundleActivity8 extends BundleSingleTopActivity {
        public BundleActivity8() {
            super();
            TAG = "BundleSingleTopActivity8";
        }
    }

    public static class BundleActivity9 extends BundleSingleTopActivity {
        public BundleActivity9() {
            super();
            TAG = "BundleSingleTopActivity9";
        }
    }

    public static class BundleActivity10 extends BundleSingleTopActivity {
        public BundleActivity10() {
            super();
            TAG = "BundleSingleTopActivity10";
        }
    }

    public static class BundleActivity11 extends BundleSingleTopActivity {
        public BundleActivity11() {
            super();
            TAG = "BundleSingleTopActivity11";
        }
    }

    public static class BundleActivity12 extends BundleSingleTopActivity {
        public BundleActivity12() {
            super();
            TAG = "BundleSingleTopActivity12";
        }
    }

    public static class BundleActivity13 extends BundleSingleTopActivity {
        public BundleActivity13() {
            super();
            TAG = "BundleSingleTopActivity13";
        }
    }

    public static class BundleActivity14 extends BundleSingleTopActivity {
        public BundleActivity14() {
            super();
            TAG = "BundleSingleTopActivity14";
        }
    }

    public static class BundleActivity15 extends BundleSingleTopActivity {
        public BundleActivity15() {
            super();
            TAG = "BundleSingleTopActivity15";
        }
    }

    public static class BundleActivity16 extends BundleSingleTopActivity {
        public BundleActivity16() {
            super();
            TAG = "BundleSingleTopActivity16";
        }
    }

    public static class BundleActivity17 extends BundleSingleTopActivity {
        public BundleActivity17() {
            super();
            TAG = "BundleSingleTopActivity17";
        }
    }

    public static class BundleActivity18 extends BundleSingleTopActivity {
        public BundleActivity18() {
            super();
            TAG = "BundleSingleTopActivity18";
        }
    }

    public static class BundleActivity19 extends BundleSingleTopActivity {
        public BundleActivity19() {
            super();
            TAG = "BundleSingleTopActivity19";
        }
    }
}
