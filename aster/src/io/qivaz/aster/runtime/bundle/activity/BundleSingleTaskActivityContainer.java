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
public class BundleSingleTaskActivityContainer implements IContainer {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleSingleTaskActivityContainer";
    private static final int BUNLDE_ACTIVITY_CONTAINER_MAX = 20;
    private static BundleSingleTaskActivityContainer mInstance;
    private final Map<String, String> mBundleActivities = new HashMap<>();
    private final Deque<String> mBundleActivityDeque = new ArrayDeque<>();

    public static BundleSingleTaskActivityContainer getInstance() {
        if (mInstance == null) {
            synchronized (BundleSingleTaskActivityContainer.class) {
                if (mInstance == null) {
                    mInstance = new BundleSingleTaskActivityContainer();
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

    public static class BundleSingleTaskActivity extends BundleActivity {
        public BundleSingleTaskActivity() {
            super();
            TAG = "BundleSingleTaskActivity";
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


    public static class BundleActivity0 extends BundleSingleTaskActivity {
        public BundleActivity0() {
            super();
            TAG = "BundleSingleTaskActivity0";
        }
    }

    public static class BundleActivity1 extends BundleSingleTaskActivity {
        public BundleActivity1() {
            super();
            TAG = "BundleSingleTaskActivity1";
        }
    }

    public static class BundleActivity2 extends BundleSingleTaskActivity {
        public BundleActivity2() {
            super();
            TAG = "BundleSingleTaskActivity2";
        }
    }

    public static class BundleActivity3 extends BundleSingleTaskActivity {
        public BundleActivity3() {
            super();
            TAG = "BundleSingleTaskActivity3";
        }
    }

    public static class BundleActivity4 extends BundleSingleTaskActivity {
        public BundleActivity4() {
            super();
            TAG = "BundleSingleTaskActivity4";
        }
    }

    public static class BundleActivity5 extends BundleSingleTaskActivity {
        public BundleActivity5() {
            super();
            TAG = "BundleSingleTaskActivity5";
        }
    }

    public static class BundleActivity6 extends BundleSingleTaskActivity {
        public BundleActivity6() {
            super();
            TAG = "BundleSingleTaskActivity6";
        }
    }

    public static class BundleActivity7 extends BundleSingleTaskActivity {
        public BundleActivity7() {
            super();
            TAG = "BundleSingleTaskActivity7";
        }
    }

    public static class BundleActivity8 extends BundleSingleTaskActivity {
        public BundleActivity8() {
            super();
            TAG = "BundleSingleTaskActivity8";
        }
    }

    public static class BundleActivity9 extends BundleSingleTaskActivity {
        public BundleActivity9() {
            super();
            TAG = "BundleSingleTaskActivity9";
        }
    }

    public static class BundleActivity10 extends BundleSingleTaskActivity {
        public BundleActivity10() {
            super();
            TAG = "BundleSingleTaskActivity10";
        }
    }

    public static class BundleActivity11 extends BundleSingleTaskActivity {
        public BundleActivity11() {
            super();
            TAG = "BundleSingleTaskActivity11";
        }
    }

    public static class BundleActivity12 extends BundleSingleTaskActivity {
        public BundleActivity12() {
            super();
            TAG = "BundleSingleTaskActivity12";
        }
    }

    public static class BundleActivity13 extends BundleSingleTaskActivity {
        public BundleActivity13() {
            super();
            TAG = "BundleSingleTaskActivity13";
        }
    }

    public static class BundleActivity14 extends BundleSingleTaskActivity {
        public BundleActivity14() {
            super();
            TAG = "BundleSingleTaskActivity14";
        }
    }

    public static class BundleActivity15 extends BundleSingleTaskActivity {
        public BundleActivity15() {
            super();
            TAG = "BundleSingleTaskActivity15";
        }
    }

    public static class BundleActivity16 extends BundleSingleTaskActivity {
        public BundleActivity16() {
            super();
            TAG = "BundleSingleTaskActivity16";
        }
    }

    public static class BundleActivity17 extends BundleSingleTaskActivity {
        public BundleActivity17() {
            super();
            TAG = "BundleSingleTaskActivity17";
        }
    }

    public static class BundleActivity18 extends BundleSingleTaskActivity {
        public BundleActivity18() {
            super();
            TAG = "BundleSingleTaskActivity18";
        }
    }

    public static class BundleActivity19 extends BundleSingleTaskActivity {
        public BundleActivity19() {
            super();
            TAG = "BundleSingleTaskActivity19";
        }
    }
}
