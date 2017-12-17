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
public class BundleSingleInstanceActivityContainer implements IContainer {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleSingleInstanceActivityContainer";
    private static final int BUNLDE_ACTIVITY_CONTAINER_MAX = 20;
    private static BundleSingleInstanceActivityContainer mInstance;
    private final Map<String, String> mBundleActivities = new HashMap<>();
    private final Deque<String> mBundleActivityDeque = new ArrayDeque<>();

    public static BundleSingleInstanceActivityContainer getInstance() {
        if (mInstance == null) {
            synchronized (BundleSingleInstanceActivityContainer.class) {
                if (mInstance == null) {
                    mInstance = new BundleSingleInstanceActivityContainer();
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

    public static class BundleSingleInstanceActivity extends BundleActivity {
        public BundleSingleInstanceActivity() {
            super();
            TAG = "BundleSingleInstanceActivity";
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


    public static class BundleActivity0 extends BundleSingleInstanceActivity {
        public BundleActivity0() {
            super();
            TAG = "BundleSingleInstanceActivity0";
        }
    }

    public static class BundleActivity1 extends BundleSingleInstanceActivity {
        public BundleActivity1() {
            super();
            TAG = "BundleSingleInstanceActivity1";
        }
    }

    public static class BundleActivity2 extends BundleSingleInstanceActivity {
        public BundleActivity2() {
            super();
            TAG = "BundleSingleInstanceActivity2";
        }
    }

    public static class BundleActivity3 extends BundleSingleInstanceActivity {
        public BundleActivity3() {
            super();
            TAG = "BundleSingleInstanceActivity3";
        }
    }

    public static class BundleActivity4 extends BundleSingleInstanceActivity {
        public BundleActivity4() {
            super();
            TAG = "BundleSingleInstanceActivity4";
        }
    }

    public static class BundleActivity5 extends BundleSingleInstanceActivity {
        public BundleActivity5() {
            super();
            TAG = "BundleSingleInstanceActivity5";
        }
    }

    public static class BundleActivity6 extends BundleSingleInstanceActivity {
        public BundleActivity6() {
            super();
            TAG = "BundleSingleInstanceActivity6";
        }
    }

    public static class BundleActivity7 extends BundleSingleInstanceActivity {
        public BundleActivity7() {
            super();
            TAG = "BundleSingleInstanceActivity7";
        }
    }

    public static class BundleActivity8 extends BundleSingleInstanceActivity {
        public BundleActivity8() {
            super();
            TAG = "BundleSingleInstanceActivity8";
        }
    }

    public static class BundleActivity9 extends BundleSingleInstanceActivity {
        public BundleActivity9() {
            super();
            TAG = "BundleSingleInstanceActivity9";
        }
    }

    public static class BundleActivity10 extends BundleSingleInstanceActivity {
        public BundleActivity10() {
            super();
            TAG = "BundleSingleInstanceActivity10";
        }
    }

    public static class BundleActivity11 extends BundleSingleInstanceActivity {
        public BundleActivity11() {
            super();
            TAG = "BundleSingleInstanceActivity11";
        }
    }

    public static class BundleActivity12 extends BundleSingleInstanceActivity {
        public BundleActivity12() {
            super();
            TAG = "BundleSingleInstanceActivity12";
        }
    }

    public static class BundleActivity13 extends BundleSingleInstanceActivity {
        public BundleActivity13() {
            super();
            TAG = "BundleSingleInstanceActivity13";
        }
    }

    public static class BundleActivity14 extends BundleSingleInstanceActivity {
        public BundleActivity14() {
            super();
            TAG = "BundleSingleInstanceActivity14";
        }
    }

    public static class BundleActivity15 extends BundleSingleInstanceActivity {
        public BundleActivity15() {
            super();
            TAG = "BundleSingleInstanceActivity15";
        }
    }

    public static class BundleActivity16 extends BundleSingleInstanceActivity {
        public BundleActivity16() {
            super();
            TAG = "BundleSingleInstanceActivity16";
        }
    }

    public static class BundleActivity17 extends BundleSingleInstanceActivity {
        public BundleActivity17() {
            super();
            TAG = "BundleSingleInstanceActivity17";
        }
    }

    public static class BundleActivity18 extends BundleSingleInstanceActivity {
        public BundleActivity18() {
            super();
            TAG = "BundleSingleInstanceActivity18";
        }
    }

    public static class BundleActivity19 extends BundleSingleInstanceActivity {
        public BundleActivity19() {
            super();
            TAG = "BundleSingleInstanceActivity19";
        }
    }
}
