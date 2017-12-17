package io.qivaz.aster.runtime.bundle;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import io.qivaz.aster.runtime.bundle.serialize.GsonUtil;
import io.qivaz.aster.runtime.util.BundleUtil;
import io.qivaz.aster.runtime.util.IOUtils;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class BundleResolver {
    private static final boolean debug = BundleFeature.debug;
    private static final boolean debugPerf = BundleFeature.debugPerf;
    private static final String TAG = "BundleResolver";
    public static Application mHostContext;
    private static boolean force = true;
    private BundleManager mBundleManager;
    private BundleRegistry mBundleRegistry = new BundleRegistry();

    BundleResolver(BundleManager bundleManager) {
        mBundleManager = bundleManager;
    }

    void init(Application application) {
        mHostContext = application;

        File apkFolder = new File(BundleFeature.BUNDLE_ROOT_FOLDER, BundleFeature.BUNDLE_APK_FOLDER_NAME);
        if (!apkFolder.exists()) {
            apkFolder.mkdirs();
        }
        BundleFeature.BUNDLE_APK_FOLDER = apkFolder.getAbsolutePath();

        File regFolder = new File(BundleFeature.BUNDLE_ROOT_FOLDER, BundleFeature.BUNDLE_REG_FOLDER_NAME);
        if (!regFolder.exists()) {
            regFolder.mkdirs();
        }
        BundleFeature.BUNDLE_REG_FOLDER = regFolder.getAbsolutePath();
        File regFile = new File(BundleFeature.BUNDLE_REG_FOLDER, BundleFeature.BUNDLE_REG_FILE_NAME);
        BundleFeature.BUNDLE_REG_FILE = regFile.getAbsolutePath();
        File accFile = new File(BundleFeature.BUNDLE_REG_FOLDER, BundleFeature.BUNDLE_ACC_FILE_NAME);
        BundleFeature.BUNDLE_ACC_FILE = accFile.getAbsolutePath();
        File cleanFile = new File(BundleFeature.BUNDLE_REG_FOLDER, BundleFeature.BUNDLE_CLEAR_FILE_NAME);
        BundleFeature.BUNDLE_CLR_FILE = cleanFile.getAbsolutePath();

        prefetchBundleRegistrants();
        if (BundleFeature.isDalvikModeOrLowART()) {
            executeClearBundleFiles();
        }
    }

    boolean installBundle(String fullPath) {
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "installBundle(), start");
        }

        PackageInfo pi = mBundleManager.getPackageInfo(fullPath);
        if (pi == null) {
            LogUtil.e(TAG, "installBundle(), failed, not a apk file? path=" + fullPath);
            return false;
        }
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "installBundle(), got package info");
        }

        String factor = String.valueOf(System.currentTimeMillis());
        String destfullPath;
        if (BundleFeature.isDalvikModeOrLowART()) {
            destfullPath = BundleFeature.BUNDLE_APK_FOLDER + File.separator + pi.packageName + "." + factor + ".apk";
        } else {
            destfullPath = BundleFeature.BUNDLE_APK_FOLDER + File.separator + pi.packageName /*+ "_" + pi.versionCode*/ + ".apk";
        }
        BundleUtil.copyBundle(fullPath, destfullPath);
        BundleUtil.copyBundleSOLib(fullPath, pi.packageName);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "installBundle(), copied all bundle files");
        }

        if (!registerBundle(updateBundle(destfullPath, mBundleManager.loadBundle(destfullPath, true), factor))
                || !saveBundleRegistrants()) {
            LogUtil.e(TAG, "installBundle(), failed, " + pi.packageName);
            return false;
        }

        LogUtil.w(TAG, "installBundle(), install " + pi.packageName + " successfully!");
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "installBundle(), end");
        }
        return true;
    }

    boolean uninstallBundlePre(String bundleName) {
        try {
            BundleRegistry.BundleItem item = mBundleRegistry.findBundle(bundleName);
            if (item == null) {
                LogUtil.e(TAG, "uninstallBundlePre(), failed, not found installed bundle with name of " + bundleName);
                return false;
            }

            if (!unregisterBundle(item) || !saveBundleRegistrants()) {
                LogUtil.e(TAG, "uninstallBundlePre(), failed, can't unregister bundle with name of " + bundleName);
                return false;
            }
            BundleAccelerator.unregister(bundleName);
        } catch (Exception e) {
            LogUtil.e(TAG, "uninstallBundlePre(), failed, " + e);
            e.printStackTrace();
            return false;
        }
        LogUtil.w(TAG, "uninstallBundlePre(), request to uninstall " + bundleName + " successful!");
        return true;
    }

    void uninstallBundlePost() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
        try {
            for (BundleRegistry.BundleItem item : mBundleRegistry.getList()) {
                if (!item.mRegister) {
                    clearUninstalledBundle(item);
                    mBundleRegistry.removeBundle(item);
                    LogUtil.w(TAG, "uninstallBundlePost(), complete uninstall " + item.mPackageName + " successful!");
                }
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
        saveBundleRegistrants();
//            }
//        }).start();
    }

    BundleRegistry.BundleItem updateBundle(String path, BundleEntry bundleEntry, String factor/*Only for Dalvik*/) {
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "updateBundle(), start");
        }
        if (TextUtils.isEmpty(path) || bundleEntry == null || bundleEntry.mPackageInfo == null) {
            LogUtil.e(TAG, "updateBundle(), failed, null parameters, " + path + ", " + bundleEntry);
            return null;
        }

        BundleRegistry.BundleItem item = null;
        String packageName = bundleEntry.mPackageInfo.packageName;
        int versionCode = bundleEntry.mPackageInfo.versionCode;
        try {
            item = mBundleRegistry.findBundle(bundleEntry.mPackageName);
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "updateBundle(), tried to find bundle");
            }
            if (item != null) {
                if (BundleFeature.isDalvikModeOrLowART()) {
                    if (debug) {
                        LogUtil.v(TAG, "updateBundle(), must remove old registered apk, " + path + ", " + bundleEntry);
                    }
                    String[] files = new String[]{
                            BundleFeature.getBundleApkFile(packageName, item.mFactor),
                            BundleFeature.getBundleDexFile(packageName, item.mFactor),
                            BundleFeature.getBundleLibFolder(packageName)};
                    requestClearBundleFiles(files);
                }

                mBundleRegistry.removeBundle(item);
                LogUtil.w(TAG, "updateBundle(), try to override the bundle in existence, {" + packageName + ", " + versionCode + "}");
                if (debugPerf) {
                    LogUtil.w("Performance/" + TAG, "updateBundle(), requested clear old installation");
                }
            }
            item = new BundleRegistry.BundleItem();
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "updateBundle(), created BundleRegistry.BundleItem");
            }

            if (BundleFeature.isDalvikModeOrLowART()) {
                item.mFactor = factor;
            }
            item.mApkPath = path;
            item.mPackageName = packageName;
            item.mVersionCode = versionCode;

            String alias = null;
            try {
                alias = bundleEntry.mAppMetaData.getString(BundleFeature.ALIAS_NAME_KEY);
            } catch (Exception e) {
                if (debug) {
                    LogUtil.v(TAG, "updateBundle(), no bundle alias name!");
                }
            }
            item.mAlias = alias;
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "updateBundle(), got bundle alias");
            }

//            for (ActivityInfo activityInfo : bundleEntry.mActivityInfo.values()) {
//                item.mActivityNameSet.addBundle(activityInfo.name);
//            }
            item.mActivityNameSet.addAll(bundleEntry.mActivityInfo.keySet());

//            for (ServiceInfo serviceInfo : bundleEntry.mServiceInfo.values()) {
//                item.mServiceNameSet.addBundle(serviceInfo.name);
//            }
            item.mServiceNameSet.addAll(bundleEntry.mServiceInfo.keySet());
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "updateBundle(), added activity/service infos");
            }

            Iterator<ResolveInfo> resolveInfoIterator = bundleEntry.mActivityFilter.iterator();
            while (resolveInfoIterator.hasNext()) {
                ResolveInfo ri = resolveInfoIterator.next();
                if (ri.filter != null) {
                    Iterator<String> sIterator = ri.filter.actionsIterator();
                    while (sIterator.hasNext()) {
                        String act = sIterator.next();
                        item.mActivityForActionMap.put(act, ri.activityInfo.name);
                    }
                }
            }
            resolveInfoIterator = bundleEntry.mServiceFilter.iterator();
            while (resolveInfoIterator.hasNext()) {
                ResolveInfo ri = resolveInfoIterator.next();
                if (ri.filter != null) {
                    Iterator<String> sIterator = ri.filter.actionsIterator();
                    while (sIterator.hasNext()) {
                        String act = sIterator.next();
                        item.mServiceForActionMap.put(act, ri.serviceInfo.name);

                    }
                }
            }
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "updateBundle(), added activity/service filters");
            }

            mBundleRegistry.addBundle(item);
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "updateBundle(), end");
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "updateBundle(), failed, " + e);
            e.printStackTrace();
            return null;
        }

//        LogUtil.v(TAG, "updateBundle(), register " + packageName + " successfully!");
        return item;
    }

    boolean registerBundle(String bundleName) {
        BundleRegistry.BundleItem bundleItem = mBundleRegistry.findBundle(bundleName);
        if (bundleItem != null) {
            bundleItem.mRegister = true;
            return true;
        } else {
            return false;
        }
    }

    boolean unregisterBundle(String bundleName) {
        BundleRegistry.BundleItem bundleItem = mBundleRegistry.findBundle(bundleName);
        if (bundleItem != null) {
            bundleItem.mRegister = false;
            return true;
        } else {
            return false;
        }
    }

    boolean registerBundle(BundleRegistry.BundleItem bundleItem) {
        if (bundleItem != null) {
            bundleItem.mRegister = true;
            return true;
        } else {
            return false;
        }
    }

    boolean unregisterBundle(BundleRegistry.BundleItem bundleItem) {
        if (bundleItem != null) {
            bundleItem.mRegister = false;
            return true;
        } else {
            return false;
        }
    }

    void clearUninstalledBundle(BundleRegistry.BundleItem bundleItem) {
        String[] files = new String[]{
                BundleFeature.getBundleApkFile(bundleItem.mPackageName, bundleItem.mFactor),
                BundleFeature.getBundleDexFile(bundleItem.mPackageName, bundleItem.mFactor),
                BundleFeature.getBundleManifestFile(bundleItem.mPackageName),
                BundleFeature.getBundleSignatureFile(bundleItem.mPackageName),
                BundleFeature.getBundleDataFolder(bundleItem.mPackageName),
                BundleFeature.getBundleLibFolder(bundleItem.mPackageName)};
        for (String file : files) {
            File clear = new File(file);
            if (clear.exists()) {
                if (debug) {
                    LogUtil.v(TAG, "clearUninstalledBundle(), remove " + file);
                }
                IOUtils.deleteRecursive(clear);
            }
        }
    }

    void prefetchBundleRegistrants() {
        if (!new File(BundleFeature.BUNDLE_REG_FILE).exists()) {
            return;
        }
        BundleRegistry bundleRegistry = new BundleRegistry();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(BundleFeature.BUNDLE_REG_FILE));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\r\n");
            }
            bundleRegistry.setList(Collections.synchronizedList(GsonUtil.stringToList(sb.toString(), BundleRegistry.BundleItem[].class)));
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, "prefetchBundleRegistrants(), " + e);
            e.printStackTrace();
        } catch (Exception e) {
            LogUtil.e(TAG, "prefetchBundleRegistrants(), " + e);
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
        if (bundleRegistry != null) {
            mBundleRegistry = bundleRegistry;

            // Remove uninstall bundles asynchronous.
            uninstallBundlePost();
        }
    }

    private synchronized boolean saveBundleRegistrants() {
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "saveBundleRegistrants(), start");
        }

        String bundleRegistryList = GsonUtil.listToString(mBundleRegistry.getList());//gson.toJson(mBundleRegistry.mRegList);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "saveBundleRegistrants(), converted to GSON string");
        }
        File infoFile = new File(BundleFeature.BUNDLE_REG_FILE);
        if (infoFile.exists()) {
            infoFile.delete();
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "saveBundleRegistrants(), delted old file");
            }
        }
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(BundleFeature.BUNDLE_REG_FILE, true);
            StringBuilder sb = new StringBuilder();
            sb.append(bundleRegistryList);
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "saveBundleRegistrants(), before write FS");
            }
            fos.write(sb.toString().getBytes());
            if (debugPerf) {
                LogUtil.w("Performance/" + TAG, "saveBundleRegistrants(), end");
            }
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, "saveBundleRegistrants(), e=" + e);
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            LogUtil.e(TAG, "saveBundleRegistrants(), e=" + e);
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

    void executeClearBundleFiles() {
        File clearFile = new File(BundleFeature.BUNDLE_CLR_FILE);
        if (!clearFile.exists()/* || clearFile.length() == 0*/) {
            return;
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(clearFile));
            String line;
            while ((line = br.readLine()) != null) {
                File clear = new File(line);
                if (clear.exists()) {
                    IOUtils.deleteRecursive(clear);
                    if (debug) {
                        LogUtil.v(TAG, "executeClearBundleFiles(), removed " + line + " successfully!");
                    }
                } else {
                    LogUtil.w(TAG, "executeClearBundleFiles(), file not exist, line=" + line);
                }
            }
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, "executeClearBundleFiles(), " + e);
            e.printStackTrace();
        } catch (Exception e) {
            LogUtil.e(TAG, "executeClearBundleFiles(), " + e);
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
        if (clearFile.delete()) {
            if (debug) {
                LogUtil.v(TAG, "executeClearBundleFiles(), removed " + BundleFeature.BUNDLE_CLEAR_FILE_NAME + " successfully!");
            }
        } else {
            LogUtil.e(TAG, "executeClearBundleFiles(), removed " + BundleFeature.BUNDLE_CLEAR_FILE_NAME + " failed!!!");
        }
    }

    void requestClearBundleFiles(String[] files) {
        File clearFile = new File(BundleFeature.BUNDLE_CLR_FILE);
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(clearFile, true);
            StringBuilder sb = new StringBuilder();
            for (String file : files) {
                sb.append(file);
                sb.append("\r\n");
            }
            fos.write(sb.toString().getBytes());
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, "requestClearBundleFiles(), e=" + e);
            e.printStackTrace();
        } catch (Exception e) {
            LogUtil.e(TAG, "requestClearBundleFiles(), e=" + e);
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * Resolve component's info. into Intent -start-
     */

    Intent resolveInactiveBundleActivityIntent(Intent intent, boolean exact) {
        String apk = null;
        String activity = null;
        ComponentName component = intent.getComponent();
        String action = intent.getAction();

        if (component != null) {
            if (exact) {
                BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findActivity(component.getPackageName(), component.getClassName());
                if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
                    apk = bundleRegistryItem.mApkPath;
                    activity = component.getClassName();
                    if (debug) {
                        LogUtil.v(TAG, "resolveInactiveBundleActivityIntent(), [exact mode] found Activity{" + activity + "} in " + component.getPackageName());
                    }
                } else {
                    LogUtil.w(TAG, "resolveInactiveBundleActivityIntent(), [exact mode] NOT found Activity{" + component.getClassName() + "} in " + component.getPackageName());
                }
            } else {
                BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findActivity(component.getClassName());
                if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
                    apk = bundleRegistryItem.mApkPath;
                    activity = component.getClassName();
                    if (debug) {
                        LogUtil.v(TAG, "resolveInactiveBundleActivityIntent(), found Activity{" + activity + "} in " + bundleRegistryItem);
                    }
                } else {
                    LogUtil.w(TAG, "resolveInactiveBundleActivityIntent(), NOT found Activity{" + component.getClassName() + "} in all bundles!");
                }
            }
        } else if (!TextUtils.isEmpty(action)) {
            BundleRegistry.BundleItem[] bundleRegistryItems = mBundleRegistry.findActivityAction(action);
            if (bundleRegistryItems.length == 1
                    && mBundleRegistry.isBundleAvailable(bundleRegistryItems[0])) {
                apk = bundleRegistryItems[0].mApkPath;
                activity = bundleRegistryItems[0].mActivityForActionMap.get(action);
                if (debug) {
                    LogUtil.v(TAG, "resolveInactiveBundleActivityIntent(), found Action{" + action + "} provided by Activity{" + activity + "} in " + bundleRegistryItems[0]);
                }
            } else if (bundleRegistryItems.length > 1) {
                LogUtil.w(TAG, "resolveInactiveBundleActivityIntent(), found multiple provider for Action{" + action + "}: " + Arrays.toString(bundleRegistryItems));
            } else {
                LogUtil.w(TAG, "resolveInactiveBundleActivityIntent(), NOT found Action{" + action + "} in all bundles");
            }
        }
        if (apk == null || activity == null) {
            return null;
        }

        BundleEntry bundleEntry = mBundleManager.loadBundle(apk, false);
        intent.setClassName(bundleEntry.mPackageInfo.packageName, activity);
        return resolveBundleActivityInfoIntent(intent, bundleEntry);
    }

    Intent resolveInactiveBundleServiceIntent(Intent intent, boolean exact) {
        String apk = null;
        String service = null;
        ComponentName component = intent.getComponent();
        String action = intent.getAction();

        if (component != null) {
            if (exact) {
                BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findService(component.getPackageName(), component.getClassName());
                if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
                    apk = bundleRegistryItem.mApkPath;
                    service = component.getClassName();
                    if (debug) {
                        LogUtil.v(TAG, "resolveInactiveBundleServiceIntent(), [exact mode] found Service{" + service + "} in " + component.getPackageName());
                    }
                } else {
                    LogUtil.w(TAG, "resolveInactiveBundleServiceIntent(), [exact mode] NOT found Service{" + component.getClassName() + "} in " + component.getPackageName());
                }
            } else {
                BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findService(component.getClassName());
                if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
                    apk = bundleRegistryItem.mApkPath;
                    service = component.getClassName();
                    if (debug) {
                        LogUtil.v(TAG, "resolveInactiveBundleServiceIntent(), found Service{" + service + "} in " + bundleRegistryItem);
                    }
                } else {
                    LogUtil.w(TAG, "resolveInactiveBundleServiceIntent(), NOT found Service{" + component.getClassName() + "} in all bundles!");
                }
            }
        } else if (!TextUtils.isEmpty(action)) {
            BundleRegistry.BundleItem[] bundleRegistryItems = mBundleRegistry.findServiceAction(action);
            if (bundleRegistryItems.length == 1
                    && mBundleRegistry.isBundleAvailable(bundleRegistryItems[0])) {
                apk = bundleRegistryItems[0].mApkPath;
                service = bundleRegistryItems[0].mServiceForActionMap.get(action);
                if (debug) {
                    LogUtil.v(TAG, "resolveInactiveBundleServiceIntent(), found Action{" + action + "} provided by Service{" + service + "} in " + bundleRegistryItems[0]);
                }
            } else if (bundleRegistryItems.length > 1) {
                LogUtil.w(TAG, "resolveInactiveBundleServiceIntent(), found multiple provider for Action{" + action + "}: " + Arrays.toString(bundleRegistryItems));
            } else {
                LogUtil.w(TAG, "resolveInactiveBundleServiceIntent(), NOT found Action{" + action + "} in all bundles");
            }
        }
        if (apk == null || service == null) {
            return null;
        }

        BundleEntry bundleEntry = mBundleManager.loadBundle(apk, false);
        intent.setClassName(bundleEntry.mPackageInfo.packageName, service);
        return resolveBundleServiceInfoIntent(intent, bundleEntry);
    }

    public Intent resolveBundleComponentIntent(Intent intent, boolean exact) {
        if (exact) {
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(componentName.getPackageName());
                if (bundleEntry != null) {
                    intent = resolveBundleComponentIntent(intent, bundleEntry);
                    if (intent != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleComponentIntent(), resolved for {" + intent + "} in exact bundle \"" + componentName.getPackageName() + "\" components!");
                        }
                        return intent;
                    }
                }
            }
        } else {
            for (Object packageName : mBundleManager.getBundleEntities().keySet()) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
                if (bundleEntry != null) {
                    intent = resolveBundleComponentIntent(intent, bundleEntry);
                    if (intent != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleComponentIntent(), exact=" + exact + ", resolved for {" + intent + "} in bundle \"" + packageName + "\" components!");
                        }
                        return intent;
                    }
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleComponentIntent(), NOT resolved for {" + intent + "} in all active bundles' components!");
        return null;
    }

    Intent resolveBundleComponentIntent(Intent intent, BundleEntry bundleEntry) {
        if ((intent = resolveBundleActivityInfoIntent(intent, bundleEntry)) == null) {
            if ((intent = resolveBundleServiceInfoIntent(intent, bundleEntry)) == null) {
                if ((intent = resolveBundleReceiverInfoIntent(intent, bundleEntry)) == null) {
                    LogUtil.w(TAG, "resolveBundleComponentIntent(), NOT resolved for {" + intent + "} in bundle " + bundleEntry.mPackageName);
                }
            }
        }
        return intent;
    }

    Intent resolveBundleActivityInfoIntent(Intent intent, boolean exact) {
        if (exact) {
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(componentName.getPackageName());
                if (bundleEntry != null) {
                    intent = resolveBundleActivityInfoIntent(intent, bundleEntry);
                    if (intent != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleActivityInfoIntent(), resolved for {" + intent + "} in exact bundle \"" + componentName.getPackageName() + "\" activities!");
                        }
                        return intent;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleActivityInfoIntent(), NOT resolved for {" + intent + "} in exact bundle " + componentName + " activities!");
        } else {
            for (Object packageName : mBundleManager.getBundleEntities().keySet()) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
                if (bundleEntry != null) {
                    intent = resolveBundleActivityInfoIntent(intent, bundleEntry);
                    if (intent != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleActivityInfoIntent(), exact=" + exact + ", resolved for {" + intent + "} in bundle \"" + packageName + "\" activities!");
                        }
                        return intent;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleActivityInfoIntent(), NOT resolved for {" + intent + "} in all active bundles' activities!");
        }

        return null;
    }

    Intent resolveBundleActivityInfoIntent(Intent intent, BundleEntry bundleEntry) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            ActivityInfo activityInfo = bundleEntry.mActivityInfo.get(componentName.getClassName());
            if (activityInfo != null) {
                if (activityInfo.exported || force) {
                    intent.setClassName(activityInfo.packageName, activityInfo.name);
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleActivityInfoIntent(), resolved for Activity, " + intent + " to " + intent);
                    }
                    return intent;
                } else {
                    LogUtil.w(TAG, "resolveBundleActivityInfoIntent(), NOT resolved for Activity, " + intent + ", android:exported=false");
                }
            }
        }

        if (debug) {
            LogUtil.v(TAG, "resolveBundleActivityInfoIntent(), NOT resolved for Activity, " + intent);
        }
        return null;
    }

    Intent resolveBundleServiceInfoIntent(Intent intent, boolean exact) {
        if (exact) {
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(componentName.getPackageName());
                if (bundleEntry != null) {
                    intent = resolveBundleServiceInfoIntent(intent, bundleEntry);
                    if (intent != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleServiceInfoIntent(), resolved for {" + intent + "} in exact bundle \"" + componentName.getPackageName() + "\" services!");
                        }
                        return intent;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleServiceInfoIntent(), NOT resolved for {" + intent + "} in exact bundle " + componentName + " services!");
        } else {
            for (Object packageName : mBundleManager.getBundleEntities().keySet()) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
                if (bundleEntry != null) {
                    intent = resolveBundleServiceInfoIntent(intent, bundleEntry);
                    if (intent != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleServiceInfoIntent(), exact=" + exact + ", resolved for {" + intent + "} in bundle \"" + packageName + "\" services!");
                        }
                        return intent;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleServiceInfoIntent(), NOT resolved for {" + intent + "} in all active bundles' services!");
        }

        return null;
    }

    Intent resolveBundleServiceInfoIntent(Intent intent, BundleEntry bundleEntry) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            ServiceInfo serviceInfo = bundleEntry.mServiceInfo.get(componentName.getClassName());
            if (serviceInfo != null) {
                if (serviceInfo.exported) {
                    intent.setClassName(serviceInfo.packageName, serviceInfo.name);
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleServiceInfoIntent(), resolved for Service, " + intent + " to " + intent);
                    }
                    return intent;
                } else {
                    LogUtil.w(TAG, "resolveBundleServiceInfoIntent(), NOT resolved for Service, " + intent + ", android:exported=false");
                }
            }
        }

        if (debug) {
            LogUtil.v(TAG, "resolveBundleServiceInfoIntent(), NOT resolved for Service, " + intent);
        }
        return null;
    }

    Intent resolveBundleReceiverInfoIntent(Intent intent, boolean exact) {
        if (exact) {
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(componentName.getPackageName());
                if (bundleEntry != null) {
                    intent = resolveBundleReceiverInfoIntent(intent, bundleEntry);
                    if (intent != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleReceiverInfoIntent(), resolved for {" + intent + "} in exact bundle \"" + componentName.getPackageName() + "\" receivers!");
                        }
                        return intent;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleReceiverInfoIntent(), NOT resolved for {" + intent + "} in exact bundle " + componentName + " receivers!");
        } else {
            for (Object packageName : mBundleManager.getBundleEntities().keySet()) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
                if (bundleEntry != null) {
                    intent = resolveBundleReceiverInfoIntent(intent, bundleEntry);
                    if (intent != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleReceiverInfoIntent(), exact=" + exact + ", resolved for {" + intent + "} in bundle \"" + packageName + "\" receivers!");
                        }
                        return intent;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleReceiverInfoIntent(), NOT resolved for {" + intent + "} in all active bundles' receivers!");
        }

        return null;
    }

    Intent resolveBundleReceiverInfoIntent(Intent intent, BundleEntry bundleEntry) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            ActivityInfo receiverInfo = bundleEntry.mReceiverInfo.get(componentName.getClassName());
            if (receiverInfo != null) {
                if (receiverInfo.exported) {
                    intent.setClassName(receiverInfo.packageName, receiverInfo.name);
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleActivityInfoIntent(), resolved for Receiver, " + intent + " to " + intent);
                    }
                    return intent;
                } else {
                    LogUtil.w(TAG, "resolveBundleActivityInfoIntent(), NOT resolved for Receiver, " + intent + ", android:exported=false");
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleReceiverInfoIntent(), NOT resolved for Receiver, " + intent);
        return null;
    }

    Intent resolveBundleFilterIntent(Intent intent) {
        for (String packageName : mBundleManager.getBundleEntities().keySet()) {
            BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
            if (bundleEntry != null) {
                intent = resolveBundleFilterIntent(mHostContext, intent, bundleEntry);
                if (intent != null) {
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleFilterIntent(), resolved for {" + intent + "} in active bundles \"" + intent.getComponent().getPackageName() + "\" filters!");
                    }
                    return intent;
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleFilterIntent(), NOT resolved for {" + intent + "} in all active bundles' filters!");
        return null;
    }

    Intent resolveBundleFilterIntent(Context context, Intent intent, BundleEntry bundleEntry) {
        if ((intent = resolveBundleActivityFilterIntent(context, intent, bundleEntry)) == null) {
            if ((intent = resolveBundleServiceFilterIntent(context, intent, bundleEntry)) == null) {
                if ((intent = resolveBundleReceiverFilterIntent(context, intent, bundleEntry)) == null) {
                    LogUtil.w(TAG, "resolveBundleFilterIntent(), NOT resolved for {" + intent + "} in bundle " + bundleEntry.mPackageName + " filter");
                }
            }
        }
        return intent;
    }

    Intent resolveBundleActivityFilterIntent(Intent intent) {
        for (String packageName : mBundleManager.getBundleEntities().keySet()) {
            BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
            if (bundleEntry != null) {
                intent = resolveBundleActivityFilterIntent(mHostContext, intent, bundleEntry);
                if (intent != null) {
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleActivityFilterIntent(), resolved for {" + intent + "} in active bundles \"" + intent.getComponent().getPackageName() + "\" filters!");
                    }
                    return intent;
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleActivityFilterIntent(), NOT resolved for {" + intent + "} in all active bundles' filters!");
        return null;
    }

    Intent resolveBundleActivityFilterIntent(Context context, Intent intent, BundleEntry bundleEntry) {
        String action = intent.getAction();
        String type = intent.resolveType(context);

        for (ResolveInfo intentFilter : bundleEntry.mActivityFilter) {
            if (intentFilter != null && intentFilter.filter != null) {
                for (int i = 0; i < intentFilter.filter.countActions(); i++) {
                    if (intentFilter.filter.getAction(i).equals(action)) {
                        if (TextUtils.isEmpty(type)) {
                            intent.setClassName(bundleEntry.mPackageInfo.packageName, intentFilter.activityInfo.name);
                            if (debug) {
                                LogUtil.v(TAG, "resolveBundleActivityFilterIntent(1), resolved for Activity, " + intent + " to " + intent);
                            }
                            return intent;
                        }

                        for (int j = 0; j < intentFilter.filter.countDataTypes(); j++) {
                            if (type.equals(intentFilter.filter.getDataType(j))) {
                                intent.setClassName(bundleEntry.mPackageInfo.packageName, intentFilter.activityInfo.name);
                                if (debug) {
                                    LogUtil.v(TAG, "resolveBundleActivityFilterIntent(2), resolved for Activity, " + intent + " to " + intent);
                                }
                                return intent;
                            }
                        }
                    }
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleActivityFilterIntent(), NOT resolved for Activity, " + intent);
        return null;
    }

    Intent resolveBundleServiceFilterIntent(Intent intent) {
        for (String packageName : mBundleManager.getBundleEntities().keySet()) {
            BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
            if (bundleEntry != null) {
                intent = resolveBundleServiceFilterIntent(mHostContext, intent, bundleEntry);
                if (intent != null) {
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleServiceFilterIntent(), resolved for {" + intent + "} in active bundles \"" + intent.getComponent().getPackageName() + "\" filters!");
                    }
                    return intent;
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleServiceFilterIntent(), NOT resolved for {" + intent + "} in all active bundles' filters!");
        return null;
    }

    Intent resolveBundleServiceFilterIntent(Context context, Intent intent, BundleEntry bundleEntry) {
        String action = intent.getAction();
        String type = intent.resolveType(context);

        for (ResolveInfo intentFilter : bundleEntry.mServiceFilter) {
            if (intentFilter != null && intentFilter.filter != null) {
                for (int i = 0; i < intentFilter.filter.countActions(); i++) {
                    if (intentFilter.filter.getAction(i).equals(action)) {
                        if (TextUtils.isEmpty(type)) {
                            intent.setClassName(bundleEntry.mPackageInfo.packageName, intentFilter.serviceInfo.name);
                            if (debug) {
                                LogUtil.v(TAG, "resolveBundleServiceFilterIntent(1), resolved for Service, " + intent + " to " + intent);
                            }
                            return intent;
                        }

                        for (int j = 0; j < intentFilter.filter.countDataTypes(); j++) {
                            if (type.equals(intentFilter.filter.getDataType(j))) {
                                intent.setClassName(bundleEntry.mPackageInfo.packageName, intentFilter.serviceInfo.name);
                                if (debug) {
                                    LogUtil.v(TAG, "resolveBundleServiceFilterIntent(2), resolved for Service, " + intent + " to " + intent);
                                }
                                return intent;
                            }
                        }
                    }
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleServiceFilterIntent(), NOT resolved for Service, " + intent);
        return null;
    }

    Intent resolveBundleReceiverFilterIntent(Intent intent) {
        for (String packageName : mBundleManager.getBundleEntities().keySet()) {
            BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
            if (bundleEntry != null) {
                intent = resolveBundleReceiverFilterIntent(mHostContext, intent, bundleEntry);
                if (intent != null) {
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleReceiverFilterIntent(), resolved for {" + intent + "} in active bundles \"" + intent.getComponent().getPackageName() + "\" filters!");
                    }
                    return intent;
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleReceiverFilterIntent(), NOT resolved for {" + intent + "} in all active bundles' filters!");
        return null;
    }

    Intent resolveBundleReceiverFilterIntent(Context context, Intent intent, BundleEntry bundleEntry) {
        String action = intent.getAction();
        String type = intent.resolveType(context);

        for (ResolveInfo intentFilter : bundleEntry.mReceiverFilter) {
            if (intentFilter != null && intentFilter.filter != null) {
                for (int i = 0; i < intentFilter.filter.countActions(); i++) {
                    if (intentFilter.filter.getAction(i).equals(action)) {
                        if (TextUtils.isEmpty(type)) {
                            intent.setClassName(bundleEntry.mPackageInfo.packageName, intentFilter.activityInfo.name);
                            if (debug) {
                                LogUtil.v(TAG, "resolveBundleReceiverFilterIntent(1), resolved for Receiver, " + intent + " to " + intent);
                            }
                            return intent;
                        }

                        for (int j = 0; j < intentFilter.filter.countDataTypes(); j++) {
                            if (type.equals(intentFilter.filter.getDataType(j))) {
                                intent.setClassName(bundleEntry.mPackageInfo.packageName, intentFilter.activityInfo.name);
                                if (debug) {
                                    LogUtil.v(TAG, "resolveBundleReceiverFilterIntent(2), resolved for Receiver, " + intent + " to " + intent);
                                }
                                return intent;
                            }
                        }
                    }
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleReceiverFilterIntent(), NOT resolved for Receiver, " + intent);
        return null;
    }

    /*
     * Resolve component's info. into Intent -end-
     */

    /*
     * Resolve component's info. into ResolveInfo -start-
     */

    /**
     * getActivityIntentFromResolveInfo()
     *
     * @param resolveInfo Resolve Info.
     * @return Intent assembled from Resolve Info.
     */
    Intent getActivityIntentFromResolveInfo(Intent intent, ResolveInfo resolveInfo) {
        if (resolveInfo != null) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null) {
                if (activityInfo.exported || force) {
                    intent.setClassName(activityInfo.packageName, activityInfo.name);
                    if (debug) {
                        LogUtil.v(TAG, "getActivityIntentFromResolveInfo(), resolved for Activity, " + activityInfo + " to " + intent);
                    }
                    return intent;
                } else {
                    LogUtil.w(TAG, "getActivityIntentFromResolveInfo(), NOT resolved for Activity, " + activityInfo + ", android:exported=false");
                }
            }
        }

        if (debug) {
            LogUtil.v(TAG, "getActivityIntentFromResolveInfo(), NOT resolved for Activity, " + resolveInfo);
        }
        return null;
    }

    /**
     * getServiceIntentFromResolveInfo()
     *
     * @param resolveInfo Resolve Info.
     * @return Intent assembled from Resolve Info.
     */
    Intent getServiceIntentFromResolveInfo(Intent intent, ResolveInfo resolveInfo) {
        if (resolveInfo != null) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo != null) {
                if (serviceInfo.exported) {
                    intent.setClassName(serviceInfo.packageName, serviceInfo.name);
                    if (debug) {
                        LogUtil.v(TAG, "getServiceIntentFromResolveInfo(), resolved for Service, " + serviceInfo + " to " + intent);
                    }
                    return intent;
                } else {
                    LogUtil.w(TAG, "getServiceIntentFromResolveInfo(), NOT resolved for Service, " + serviceInfo + ", android:exported=false");
                }
            }
        }

        if (debug) {
            LogUtil.v(TAG, "getServiceIntentFromResolveInfo(), NOT resolved for Service, " + resolveInfo);
        }
        return null;
    }

    ResolveInfo resolveInactiveBundleActivity(Intent intent, boolean exact) {
        String apk = null;
        String activity = null;
        ComponentName component = intent.getComponent();
        String action = intent.getAction();

        if (component != null) {
            if (exact) {
                BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findActivity(component.getPackageName(), component.getClassName());
                if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
                    apk = bundleRegistryItem.mApkPath;
                    activity = component.getClassName();
                    if (debug) {
                        LogUtil.v(TAG, "resolveInactiveBundleActivity(), [exact mode] found Activity{" + activity + "} in " + component.getPackageName());
                    }
                } else {
                    LogUtil.w(TAG, "resolveInactiveBundleActivity(), [exact mode] NOT found Activity{" + component.getClassName() + "} in " + component.getPackageName());
                }
            } else {
                BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findActivity(component.getClassName());
                if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
                    apk = bundleRegistryItem.mApkPath;
                    activity = component.getClassName();
                    if (debug) {
                        LogUtil.v(TAG, "resolveInactiveBundleActivity(), found Activity{" + activity + "} in " + bundleRegistryItem);
                    }
                } else {
                    LogUtil.w(TAG, "resolveInactiveBundleActivity(), NOT found Activity{" + component.getClassName() + "} in all bundles!");
                }
            }
        } else if (!TextUtils.isEmpty(action)) {
            BundleRegistry.BundleItem[] bundleRegistryItems = mBundleRegistry.findActivityAction(action);
            if (bundleRegistryItems.length == 1
                    && mBundleRegistry.isBundleAvailable(bundleRegistryItems[0])) {
                apk = bundleRegistryItems[0].mApkPath;
                activity = bundleRegistryItems[0].mActivityForActionMap.get(action);
                if (debug) {
                    LogUtil.v(TAG, "resolveInactiveBundleActivity(), found Action{" + action + "} provided by Activity{" + activity + "} in " + bundleRegistryItems[0]);
                }
            } else if (bundleRegistryItems.length > 1) {
                LogUtil.w(TAG, "resolveInactiveBundleActivity(), found multiple provider for Action{" + action + "}: " + Arrays.toString(bundleRegistryItems));
            } else {
                LogUtil.w(TAG, "resolveInactiveBundleActivity(), NOT found Action{" + action + "} in all bundles");
            }
        }
        if (apk == null || activity == null) {
            return null;
        }

        BundleEntry bundleEntry = mBundleManager.loadBundle(apk, false);
        intent.setClassName(bundleEntry.mPackageInfo.packageName, activity);
        return resolveBundleActivityInfo(intent, bundleEntry);
    }

    ResolveInfo resolveInactiveBundleService(Intent intent, boolean exact) {
        String apk = null;
        String service = null;
        ComponentName component = intent.getComponent();
        String action = intent.getAction();

        if (component != null) {
            if (exact) {
                BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findService(component.getPackageName(), component.getClassName());
                if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
                    apk = bundleRegistryItem.mApkPath;
                    service = component.getClassName();
                    if (debug) {
                        LogUtil.v(TAG, "resolveInactiveBundleService(), [exact mode] found Service{" + service + "} in " + component.getPackageName());
                    }
                } else {
                    LogUtil.w(TAG, "resolveInactiveBundleService(), [exact mode] NOT found Service{" + component.getClassName() + "} in " + component.getPackageName());
                }
            } else {
                BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findService(component.getClassName());
                if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
                    apk = bundleRegistryItem.mApkPath;
                    service = component.getClassName();
                    if (debug) {
                        LogUtil.v(TAG, "resolveInactiveBundleService(), found Service{" + service + "} in " + bundleRegistryItem);
                    }
                } else {
                    LogUtil.w(TAG, "resolveInactiveBundleService(), NOT found Service{" + component.getClassName() + "} in all bundles!");
                }
            }
        } else if (!TextUtils.isEmpty(action)) {
            BundleRegistry.BundleItem[] bundleRegistryItems = mBundleRegistry.findServiceAction(action);
            if (bundleRegistryItems.length == 1
                    && mBundleRegistry.isBundleAvailable(bundleRegistryItems[0])) {
                apk = bundleRegistryItems[0].mApkPath;
                service = bundleRegistryItems[0].mServiceForActionMap.get(action);
                if (debug) {
                    LogUtil.v(TAG, "resolveInactiveBundleService(), found Action{" + action + "} provided by Service{" + service + "} in " + bundleRegistryItems[0]);
                }
            } else if (bundleRegistryItems.length > 1) {
                LogUtil.w(TAG, "resolveInactiveBundleService(), found multiple provider for Action{" + action + "}: " + Arrays.toString(bundleRegistryItems));
            } else {
                LogUtil.w(TAG, "resolveInactiveBundleService(), NOT found Action{" + action + "} in all bundles");
            }
        }
        if (apk == null || service == null) {
            return null;
        }

        BundleEntry bundleEntry = mBundleManager.loadBundle(apk, false);
        intent.setClassName(bundleEntry.mPackageInfo.packageName, service);
        return resolveBundleServiceInfo(intent, bundleEntry);
    }

    ResolveInfo resolveBundleComponent(Intent intent, BundleEntry bundleEntry) {
        ResolveInfo resolveInfo;
        if ((resolveInfo = resolveBundleActivityInfo(intent, bundleEntry)) == null) {
            if ((resolveInfo = resolveBundleServiceInfo(intent, bundleEntry)) == null) {
                if ((resolveInfo = resolveBundleReceiverInfo(intent, bundleEntry)) == null) {
                    LogUtil.w(TAG, "resolveBundleComponent(), NOT resolved for {" + intent + "} in bundle " + bundleEntry.mPackageName);
                }
            }
        }
        return resolveInfo;
    }

    ResolveInfo resolveBundleActivityInfo(Intent intent, boolean exact) {
        if (exact) {
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(componentName.getPackageName());
                if (bundleEntry != null) {
                    ResolveInfo resolveInfo = resolveBundleActivityInfo(intent, bundleEntry);
                    if (resolveInfo != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleActivityInfo(), resolved for {" + intent + "} in exact bundle \"" + componentName.getPackageName() + "\" activities!");
                        }
                        return resolveInfo;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleActivityInfo(), NOT resolved for {" + intent + "} in exact bundle " + componentName + " activities!");
        } else {
            for (Object packageName : mBundleManager.getBundleEntities().keySet()) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
                if (bundleEntry != null) {
                    ResolveInfo resolveInfo = resolveBundleActivityInfo(intent, bundleEntry);
                    if (resolveInfo != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleActivityInfo(), exact=" + exact + ", resolved for {" + intent + "} in bundle \"" + packageName + "\" activities!");
                        }
                        return resolveInfo;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleActivityInfo(), NOT resolved for {" + intent + "} in all active bundles' activities!");
        }

        return null;
    }

    ResolveInfo resolveBundleActivityInfo(Intent intent, BundleEntry bundleEntry) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            ActivityInfo activityInfo = bundleEntry.mActivityInfo.get(componentName.getClassName());
            if (activityInfo != null) {
                if (activityInfo.exported || force) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = activityInfo;
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleActivityInfo(), resolved for Activity, " + intent + " to " + resolveInfo);
                    }
                    return resolveInfo;
                } else {
                    LogUtil.w(TAG, "resolveBundleActivityInfo(), NOT resolved for Activity, " + intent + ", android:exported=false");
                }
            }
        }

        if (debug) {
            LogUtil.v(TAG, "resolveBundleActivityInfo(), NOT resolved for Activity, " + intent);
        }
        return null;
    }

    ResolveInfo resolveBundleServiceInfo(Intent intent, boolean exact) {
        if (exact) {
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(componentName.getPackageName());
                if (bundleEntry != null) {
                    ResolveInfo resolveInfo = resolveBundleServiceInfo(intent, bundleEntry);
                    if (resolveInfo != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleServiceInfo(), resolved for {" + intent + "} in exact bundle \"" + componentName.getPackageName() + "\" services!");
                        }
                        return resolveInfo;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleServiceInfo(), NOT resolved for {" + intent + "} in exact bundle " + componentName + " services!");
        } else {
            for (Object packageName : mBundleManager.getBundleEntities().keySet()) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
                if (bundleEntry != null) {
                    ResolveInfo resolveInfo = resolveBundleServiceInfo(intent, bundleEntry);
                    if (resolveInfo != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleServiceInfo(), exact=" + exact + ", resolved for {" + intent + "} in bundle \"" + packageName + "\" services!");
                        }
                        return resolveInfo;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleServiceInfo(), NOT resolved for {" + intent + "} in all active bundles' services!");
        }

        return null;
    }

    ResolveInfo resolveBundleServiceInfo(Intent intent, BundleEntry bundleEntry) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            ServiceInfo serviceInfo = bundleEntry.mServiceInfo.get(componentName.getClassName());
            if (serviceInfo != null) {
                if (serviceInfo.exported) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.serviceInfo = serviceInfo;
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleServiceInfo(), resolved for Service, " + intent + " to " + resolveInfo);
                    }
                    return resolveInfo;
                } else {
                    LogUtil.w(TAG, "resolveBundleServiceInfo(), NOT resolved for Service, " + intent + ", android:exported=false");
                }
            }
        }

        if (debug) {
            LogUtil.v(TAG, "resolveBundleServiceInfo(), NOT resolved for Service, " + intent);
        }
        return null;
    }

    ResolveInfo resolveBundleReceiverInfo(Intent intent, boolean exact) {
        if (exact) {
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(componentName.getPackageName());
                if (bundleEntry != null) {
                    ResolveInfo resolveInfo = resolveBundleReceiverInfo(intent, bundleEntry);
                    if (resolveInfo != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleReceiverInfo(), resolved for {" + intent + "} in exact bundle \"" + componentName.getPackageName() + "\" receivers!");
                        }
                        return resolveInfo;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleReceiverInfo(), NOT resolved for {" + intent + "} in exact bundle " + componentName + " receivers!");
        } else {
            for (Object packageName : mBundleManager.getBundleEntities().keySet()) {
                BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
                if (bundleEntry != null) {
                    ResolveInfo resolveInfo = resolveBundleReceiverInfo(intent, bundleEntry);
                    if (resolveInfo != null) {
                        if (debug) {
                            LogUtil.v(TAG, "resolveBundleReceiverInfo(), exact=" + exact + ", resolved for {" + intent + "} in bundle \"" + packageName + "\" receivers!");
                        }
                        return resolveInfo;
                    }
                }
            }
            LogUtil.w(TAG, "resolveBundleReceiverInfo(), NOT resolved for {" + intent + "} in all active bundles' receivers!");
        }

        return null;
    }

    ResolveInfo resolveBundleReceiverInfo(Intent intent, BundleEntry bundleEntry) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            ActivityInfo receiverInfo = bundleEntry.mReceiverInfo.get(componentName.getClassName());
            if (receiverInfo != null) {
                if (receiverInfo.exported) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = receiverInfo;
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleReceiverInfo(), resolved for Receiver, " + intent + " to " + resolveInfo);
                    }
                    return resolveInfo;
                } else {
                    LogUtil.w(TAG, "resolveBundleReceiverInfo(), NOT resolved for Receiver, " + intent + ", android:exported=false");
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleReceiverInfo(), NOT resolved for Receiver, " + intent);
        return null;
    }

    ResolveInfo resolveBundleActivityFilter(Intent intent) {
        for (String packageName : mBundleManager.getBundleEntities().keySet()) {
            BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
            if (bundleEntry != null) {
                ResolveInfo resolveInfo = resolveBundleActivityFilter(mHostContext, intent, bundleEntry);
                if (resolveInfo != null) {
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleActivityFilter(), resolved for {" + intent + "} in active bundles \"" + packageName + "\" filters!");
                    }
                    return resolveInfo;
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleActivityFilter(), NOT resolved for {" + intent + "} in all active bundles' filters!");
        return null;
    }

    ResolveInfo resolveBundleActivityFilter(Context context, Intent intent, BundleEntry bundleEntry) {
        String action = intent.getAction();
        String type = intent.resolveType(context);

        for (ResolveInfo intentFilter : bundleEntry.mActivityFilter) {
            if (intentFilter != null && intentFilter.filter != null) {
                for (int i = 0; i < intentFilter.filter.countActions(); i++) {
                    if (intentFilter.filter.getAction(i).equals(action)) {
                        if (TextUtils.isEmpty(type)) {
                            ResolveInfo resolveInfo = intentFilter;
                            if (debug) {
                                LogUtil.v(TAG, "resolveBundleActivityFilter(1), resolved for Activity, " + intent + " to " + resolveInfo);
                            }
                            return resolveInfo;
                        }

                        for (int j = 0; j < intentFilter.filter.countDataTypes(); j++) {
                            if (type.equals(intentFilter.filter.getDataType(j))) {
                                ResolveInfo resolveInfo = intentFilter;
                                if (debug) {
                                    LogUtil.v(TAG, "resolveBundleActivityFilter(2), resolved for Activity, " + intent + " to " + resolveInfo);
                                }
                                return resolveInfo;
                            }
                        }
                    }
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleActivityFilter(), NOT resolved for Activity, " + intent);
        return null;
    }

    ResolveInfo resolveBundleServiceFilter(Intent intent) {
        for (String packageName : mBundleManager.getBundleEntities().keySet()) {
            BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
            if (bundleEntry != null) {
                ResolveInfo resolveInfo = resolveBundleServiceFilter(mHostContext, intent, bundleEntry);
                if (resolveInfo != null) {
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleServiceFilter(), resolved for {" + intent + "} in active bundles \"" + packageName + "\" filters!");
                    }
                    return resolveInfo;
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleServiceFilter(), NOT resolved for {" + intent + "} in all active bundles' filters!");
        return null;
    }

    ResolveInfo resolveBundleServiceFilter(Context context, Intent intent, BundleEntry bundleEntry) {
        String action = intent.getAction();
        String type = intent.resolveType(context);

        for (ResolveInfo intentFilter : bundleEntry.mServiceFilter) {
            if (intentFilter != null && intentFilter.filter != null) {
                for (int i = 0; i < intentFilter.filter.countActions(); i++) {
                    if (intentFilter.filter.getAction(i).equals(action)) {
                        if (TextUtils.isEmpty(type)) {
                            ResolveInfo resolveInfo = intentFilter;
                            if (debug) {
                                LogUtil.v(TAG, "resolveBundleServiceFilter(1), resolved for Service, " + intent + " to " + resolveInfo);
                            }
                            return resolveInfo;
                        }

                        for (int j = 0; j < intentFilter.filter.countDataTypes(); j++) {
                            if (type.equals(intentFilter.filter.getDataType(j))) {
                                ResolveInfo resolveInfo = intentFilter;
                                if (debug) {
                                    LogUtil.v(TAG, "resolveBundleServiceFilter(2), resolved for Service, " + intent + " to " + resolveInfo);
                                }
                                return resolveInfo;
                            }
                        }
                    }
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleServiceFilter(), NOT resolved for Service, " + intent);
        return null;
    }

    ResolveInfo resolveBundleReceiverFilter(Intent intent) {
        for (String packageName : mBundleManager.getBundleEntities().keySet()) {
            BundleEntry bundleEntry = mBundleManager.getBundleEntities().get(packageName);
            if (bundleEntry != null) {
                ResolveInfo resolveInfo = resolveBundleReceiverFilter(mHostContext, intent, bundleEntry);
                if (resolveInfo != null) {
                    if (debug) {
                        LogUtil.v(TAG, "resolveBundleReceiverFilter(), resolved for {" + intent + "} in active bundles \"" + packageName + "\" filters!");
                    }
                    return resolveInfo;
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleReceiverFilter(), NOT resolved for {" + intent + "} in all active bundles' filters!");
        return null;
    }

    ResolveInfo resolveBundleReceiverFilter(Context context, Intent intent, BundleEntry bundleEntry) {
        String action = intent.getAction();
        String type = intent.resolveType(context);

        for (ResolveInfo intentFilter : bundleEntry.mReceiverFilter) {
            if (intentFilter != null && intentFilter.filter != null) {
                for (int i = 0; i < intentFilter.filter.countActions(); i++) {
                    if (intentFilter.filter.getAction(i).equals(action)) {
                        if (TextUtils.isEmpty(type)) {
                            ResolveInfo resolveInfo = intentFilter;
                            if (debug) {
                                LogUtil.v(TAG, "resolveBundleReceiverFilter(1), resolved for Receiver, " + intent + " to " + resolveInfo);
                            }
                            return resolveInfo;
                        }

                        for (int j = 0; j < intentFilter.filter.countDataTypes(); j++) {
                            if (type.equals(intentFilter.filter.getDataType(j))) {
                                ResolveInfo resolveInfo = intentFilter;
                                if (debug) {
                                    LogUtil.v(TAG, "resolveBundleReceiverFilter(2), resolved for Receiver, " + intent + " to " + resolveInfo);
                                }
                                return resolveInfo;
                            }
                        }
                    }
                }
            }
        }

        LogUtil.w(TAG, "resolveBundleReceiverFilter(), NOT resolved for Receiver, " + intent);
        return null;
    }

    /*
     * Resolve component's info. into ResolveInfo -end-
     */

    List<String> getInstalledBundles() {
        return mBundleRegistry.getInstalledBundles();
    }

    boolean isBundleInstalled(String packageName) {
        BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findBundle(packageName);
        if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
            if (debug) {
                LogUtil.v(TAG, "isBundleInstalled(), found Package{" + packageName + "}");
            }
            return true;
        } else {
            LogUtil.w(TAG, "isBundleInstalled(), NOT found Package{" + packageName + "}!");
            return false;
        }
    }

    BundleEntry resolveInactiveBundle(String packageName) {
        String apk = null;
        BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findBundle(packageName);
        if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
            apk = bundleRegistryItem.mApkPath;
            if (debug) {
                LogUtil.v(TAG, "resolveInactiveBundle(), found Package{" + packageName + "} at " + apk);
            }
        } else {
            LogUtil.w(TAG, "resolveInactiveBundle(), NOT found Package{" + packageName + "}!");
        }

        if (apk == null) {
            return null;
        }

        BundleEntry bundleEntry = mBundleManager.loadBundle(apk, false);
        return bundleEntry;
    }

    BundleEntry resolveInactiveBundleAlias(String alias) {
        String apk = null;
        BundleRegistry.BundleItem bundleRegistryItem = mBundleRegistry.findBundleAlias(alias);
        if (mBundleRegistry.isBundleAvailable(bundleRegistryItem)) {
            apk = bundleRegistryItem.mApkPath;
            if (debug) {
                LogUtil.v(TAG, "resolveInactiveBundle(), found Package{" + alias + "} at " + apk);
            }
        } else {
            LogUtil.w(TAG, "resolveInactiveBundleAlias(), NOT found Package{" + alias + "}!");
        }

        if (apk == null) {
            return null;
        }

        BundleEntry bundleEntry = mBundleManager.loadBundle(apk, false);
        return bundleEntry;
    }

    /**
     * getBundleRegistry()
     *
     * @return Bundle Registry
     */
    BundleRegistry getBundleRegistry() {
        return mBundleRegistry;
    }

}
