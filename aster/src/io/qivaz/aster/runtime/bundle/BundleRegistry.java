package io.qivaz.aster.runtime.bundle;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class BundleRegistry implements Serializable {
    private static final boolean debug = BundleFeature.debug;
    private List<BundleItem> mRegList;

    public BundleRegistry() {
    }

    public List<String> getInstalledBundles() {
        if (mRegList == null) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (BundleItem item : mRegList) {
            if (!TextUtils.isEmpty(item.mPackageName)) {
                list.add(item.mPackageName);
            }
        }
        return list;
    }

    public List<BundleItem> getList() {
//        if (mRegList == null) {
//            mRegList = Collections.synchronizedList(new LinkedList<BundleItem>());
//        }
        return mRegList;
    }

    public void setList(List<BundleItem> list) {
        mRegList = list;
    }

    public void addBundle(BundleItem item) {
        if (mRegList == null) {
            mRegList = Collections.synchronizedList(new LinkedList<BundleItem>());
        }
        mRegList.add(item);
    }

    public boolean removeBundle(String bundleName) {
        return mRegList.remove(findBundle(bundleName));
    }

    public boolean removeBundle(BundleItem bundle) {
        return mRegList.remove(bundle);
    }

    public boolean isBundleAvailable(BundleItem bundleItem) {
        return bundleItem != null && bundleItem.mRegister;
    }

    public BundleItem findBundle(String bundleName) {
        if (mRegList == null || TextUtils.isEmpty(bundleName)) {
            return null;
        }
        for (BundleItem item : mRegList) {
            if (bundleName.equals(item.mPackageName)) {
                return item;
            }
        }
        return null;
    }

    public BundleItem findBundleAlias(String alias) {
        if (mRegList == null || TextUtils.isEmpty(alias)) {
            return null;
        }
        for (BundleItem item : mRegList) {
            if (alias.equals(item.mAlias)) {
                return item;
            }
        }
        return null;
    }

    public BundleItem findActivity(String activity) {
        if (mRegList == null || TextUtils.isEmpty(activity)) {
            return null;
        }
        for (BundleItem item : mRegList) {
            if (item.mActivityNameSet != null && item.mActivityNameSet.contains(activity)) {
                return item;
            }
        }
        return null;
    }

    public BundleItem findActivity(String bundleName, String activity) {
        if (mRegList == null || TextUtils.isEmpty(bundleName) || TextUtils.isEmpty(activity)) {
            return null;
        }
        for (BundleItem item : mRegList) {
            if (bundleName.equals(item.mPackageName)) {
                if (item.mActivityNameSet != null && item.mActivityNameSet.contains(activity)) {
                    return item;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public BundleItem[] findActivityAction(String action) {
        if (mRegList == null || TextUtils.isEmpty(action)) {
            return null;
        }
        List<BundleItem> l = new ArrayList<>();
        for (BundleItem item : mRegList) {
            if (item.mActivityForActionMap != null && item.mActivityForActionMap.get(action) != null) {
                l.add(item);
            }
        }
        return l.toArray(new BundleItem[l.size()]);
    }

    public BundleItem findService(String activity) {
        if (mRegList == null || TextUtils.isEmpty(activity)) {
            return null;
        }
        for (BundleItem item : mRegList) {
            if (item.mServiceNameSet != null && item.mServiceNameSet.contains(activity)) {
                return item;
            }
        }
        return null;
    }

    public BundleItem findService(String bundleName, String service) {
        if (mRegList == null || TextUtils.isEmpty(bundleName) || TextUtils.isEmpty(service)) {
            return null;
        }
        for (BundleItem item : mRegList) {
            if (bundleName.equals(item.mPackageName)) {
                if (item.mServiceNameSet != null && item.mServiceNameSet.contains(service)) {
                    return item;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public BundleItem[] findServiceAction(String action) {
        if (mRegList == null || TextUtils.isEmpty(action)) {
            return null;
        }
        List<BundleItem> l = new ArrayList<>();
        for (BundleItem item : mRegList) {
            if (item.mServiceForActionMap != null && item.mServiceForActionMap.get(action) != null) {
                l.add(item);
            }
        }
        return l.toArray(new BundleItem[l.size()]);
    }

    static public class BundleItem implements Serializable {
        public boolean mRegister;
        public String mFactor;
        public String mApkPath;
        public String mPackageName;
        public String mAlias;
        public int mVersionCode;
        public Set<String> mActivityNameSet = new HashSet<>();
        public Map<String, String> mActivityForActionMap = new HashMap<>();

        public Set<String> mServiceNameSet = new HashSet<>();
        public Map<String, String> mServiceForActionMap = new HashMap<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            sb.append(mPackageName);
            sb.append('/');
            sb.append(mVersionCode);
            sb.append('}');
            return sb.toString();
        }
    }
}
