package io.qivaz.aster.runtime.bundle;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import io.qivaz.aster.runtime.bundle.serialize.GsonUtil;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/19.
 */
public class BundleAccelerator {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "BundleAccelerator";
    private static Map<String, ArrayList<String>> mBundleAccelerators;
    private static LinkedList<String> mBundleAcceleratings = new LinkedList<>();

    static void register(String registrantPackage, String registrantClass, String packageName) {
        synchronized (TAG) {
            ArrayList<String> packageNames;
            String key = KEY(registrantPackage, registrantClass);
            if ((packageNames = mBundleAccelerators.get(key)) == null) {
                packageNames = new ArrayList<>();
                mBundleAccelerators.put(key, packageNames);
            }
            if (!packageNames.contains(packageName)) {
                if (debug) {
                    LogUtil.v(TAG, "register(), register accelerate bundle \"" + packageName + "\"");
                }
                packageNames.add(packageName);
                save();
            }
        }
    }

    static void unregister(String packageName) {
        synchronized (TAG) {
            Set<Map.Entry<String, ArrayList<String>>> packageNamesSet = mBundleAccelerators.entrySet();
            ArrayList<String> removedKeys = new ArrayList<>();
            for (Map.Entry<String, ArrayList<String>> entry : packageNamesSet) {
                if (entry.getValue() != null) {
                    if (entry.getValue().remove(packageName)) {
                        if (debug) {
                            LogUtil.v(TAG, "unregister(), unregister accelerate bundle \"" + packageName + "\" in " + entry.getKey());
                        }
                    } else {
                        if (debug) {
                            LogUtil.v(TAG, "unregister(), not unregister any accelerate bundle \"" + packageName + "\" in " + entry.getKey());
                        }
                    }
                }
                if (entry.getValue().isEmpty()) {
                    removedKeys.add(entry.getKey());
                }
            }
            for (String key : removedKeys) {
                if (mBundleAccelerators.remove(key) != null) {
                    if (debug) {
                        LogUtil.v(TAG, "unregister(), no bundle refer to the accelerator key \"" + key + "\", remove it!");
                    }
                }
            }
            save();
        }
    }

    private static String KEY(String registrantPackage, String registrantClass) {
        return registrantPackage + "#" + registrantClass;
    }

    public static void accelerate(final String registrantPackage, final String registrantClass) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (TAG) {
                    if (debug) {
                        LogUtil.v(TAG, "accelerate(), start accelerating bundle by \"" + KEY(registrantPackage, registrantClass) + "\"!");
                    }
                    ArrayList<String> packageNames = mBundleAccelerators.get(KEY(registrantPackage, registrantClass));
                    if (packageNames != null && packageNames.size() > 0) {
                        for (String packageName : packageNames) {
                            if (BundleManager.getInstance().getBundleByPackageName(packageName) == null
                                    && !isAccelerating(packageName)) {
                                mBundleAcceleratings.add(packageName);
                                if (debug) {
                                    LogUtil.v(TAG, "accelerate(), start accelerating bundle \"" + packageName + "\"!");
                                }
                                BundleManager.getInstance().loadBundle(packageName);
                                if (debug) {
                                    LogUtil.v(TAG, "accelerate(), finish accelerating bundle \"" + packageName + "\"!");
                                }
                                mBundleAcceleratings.remove(packageName);
                            }
                        }
                    }
                }
            }
        });
    }

    synchronized static boolean isAccelerating(String packageName) {
        return mBundleAcceleratings.contains(packageName);
    }

    static void prefetch() {
        if (!new File(BundleFeature.BUNDLE_ACC_FILE).exists()) {
            LogUtil.w(TAG, "prefetch(), no accelerated bundles!");
            mBundleAccelerators = new HashMap<>();
            return;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(BundleFeature.BUNDLE_ACC_FILE));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\r\n");
            }
            mBundleAccelerators = Collections.synchronizedMap(GsonUtil.stringToMap(sb.toString()));
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, "prefetch(), " + e);
            e.printStackTrace();
        } catch (Exception e) {
            LogUtil.e(TAG, "prefetch(), " + e);
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean save() {
        String bundleAccelerators = GsonUtil.mapToString(mBundleAccelerators);//gson.toJson(mBundleRegistry.mRegList);
        File infoFile = new File(BundleFeature.BUNDLE_ACC_FILE);
        if (infoFile.exists()) {
            if (!infoFile.delete()) {
                LogUtil.e(TAG, "save(), failed, can't delete " + infoFile.getAbsolutePath());
                return false;
            }
        }
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(BundleFeature.BUNDLE_ACC_FILE, true);
            StringBuilder sb = new StringBuilder();
            sb.append(bundleAccelerators);
            fos.write(sb.toString().getBytes());
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, "save(), e=" + e);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            LogUtil.e(TAG, "save(), e=" + e);
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
}
