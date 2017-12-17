package io.qivaz.aster.runtime.bundle;

import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/7/6.
 */
public class BundlePackageParser {
    private static final boolean debug = BundleFeature.debug;
    private static final boolean debugPerf = BundleFeature.debugPerf;
    private static final String TAG = "BundlePackageParser";
    private static Object packageParser;
    private final Class<?> PackageParser;
    private final Class<?> PackageParser$Package;
    private final Class<?> PackageParser$Component;
    private final Class<?> PackageParser$Activity;
    private final Class<?> PackageParser$Service;
    private final Class<?> PackageParser$Provider;
    private final Field PackageParser$Component_intents;
    private final Field PackageParser$Package_activities;
    private final Field PackageParser$Package_services;
    private final Field PackageParser$Package_receivers;
    private final Field PackageParser$Package_providers;
    private final Field PackageParser$Package_applicationInfo;
    private final Field PackageParser$Package_packageName;
    private final Field PackageParser$Package_mVersionCode;
    private final Field PackageParser$Package_mVersionName;
    private final Field PackageParser$Package_mSignatures;
    private final Field PackageParser$Package_mAppMetaData;
    private final Field PackageParser$Package_baseCodePath;
    private final Constructor<?> PackageParser_constructor;
    private final Method PackageParser_parsePackage;
    private final Method PackageParser_collectCertificates;
    private AssetManager assetManager;
    private Resources resources;

    private ArrayList<Object> activitys; //ArrayList<PackageParser.Activity>
    private ArrayList<Object> services; //ArrayList<PackageParser.Service>
    private ArrayList<Object> providers; //ArrayList<PackageParser.Provider>
    private ArrayList<Object> receivers; //ArrayList<PackageParser.Activity>


    public BundlePackageParser() throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        PackageParser = Class.forName("android.content.pm.PackageParser");
        PackageParser$Package = Class.forName("android.content.pm.PackageParser$Package");

        PackageParser$Component = Class.forName("android.content.pm.PackageParser$Component");
        PackageParser$Activity = Class.forName("android.content.pm.PackageParser$Activity");
        PackageParser$Service = Class.forName("android.content.pm.PackageParser$Service");
        PackageParser$Provider = Class.forName("android.content.pm.PackageParser$Provider");

        PackageParser$Component_intents = PackageParser$Component.getDeclaredField("intents");
        PackageParser$Package_activities = PackageParser$Package.getDeclaredField("activities");
        PackageParser$Package_services = PackageParser$Package.getDeclaredField("services");
        PackageParser$Package_receivers = PackageParser$Package.getDeclaredField("receivers");
        PackageParser$Package_providers = PackageParser$Package.getDeclaredField("providers");
        PackageParser$Package_applicationInfo = PackageParser$Package.getDeclaredField("applicationInfo");
        PackageParser$Package_packageName = PackageParser$Package.getDeclaredField("packageName");
        PackageParser$Package_mVersionCode = PackageParser$Package.getDeclaredField("mVersionCode");
        PackageParser$Package_mVersionName = PackageParser$Package.getDeclaredField("mVersionName");
        PackageParser$Package_mSignatures = PackageParser$Package.getDeclaredField("mSignatures");
        PackageParser$Package_mAppMetaData = PackageParser$Package.getDeclaredField("mAppMetaData");
        PackageParser$Component_intents.setAccessible(true);
        PackageParser$Package_activities.setAccessible(true);
        PackageParser$Package_services.setAccessible(true);
        PackageParser$Package_receivers.setAccessible(true);
        PackageParser$Package_providers.setAccessible(true);
        PackageParser$Package_applicationInfo.setAccessible(true);
        PackageParser$Package_packageName.setAccessible(true);
        PackageParser$Package_mVersionCode.setAccessible(true);
        PackageParser$Package_mVersionName.setAccessible(true);
        PackageParser$Package_mSignatures.setAccessible(true);
        PackageParser$Package_mAppMetaData.setAccessible(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PackageParser_constructor = PackageParser.getDeclaredConstructor();
        } else {
            PackageParser_constructor = PackageParser.getDeclaredConstructor(String.class);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PackageParser_parsePackage = PackageParser.getDeclaredMethod("parseBaseApk", Resources.class, android.content.res.XmlResourceParser.class, Integer.TYPE, String[].class);
            PackageParser_parsePackage.setAccessible(true);
        } else {
            PackageParser_parsePackage = PackageParser.getDeclaredMethod("parsePackage", Resources.class, android.content.res.XmlResourceParser.class, Integer.TYPE, String[].class);
            PackageParser_parsePackage.setAccessible(true);
        }

        //public void collectCertificates(Package pkg, int flags)
        PackageParser_collectCertificates = PackageParser.getDeclaredMethod("collectCertificates", PackageParser$Package, Integer.TYPE);
        PackageParser_parsePackage.setAccessible(true);
        // Above LL, collectCertificates() uses baseCodePath to parse the Certificates.
        // So, we should set it properly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PackageParser$Package_baseCodePath = PackageParser$Package.getDeclaredField("baseCodePath");
            PackageParser$Package_baseCodePath.setAccessible(true);
        } else {
            PackageParser$Package_baseCodePath = null;
        }
    }


    private Object parsePackage(Object packageParserObj, Resources resources, android.content.res.XmlResourceParser parser) {
        final String[] outError = new String[1];
        try {
            return PackageParser_parsePackage.invoke(packageParserObj, resources, parser, 0, outError);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * parseBundle()
     *
     * @param apkFullPath apk path
     * @return PackageParser.Package object
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    Object parseBundle(String apkFullPath, boolean override) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, IOException {
        Object packageObj = null;
        if (packageParser == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                packageParser = PackageParser_constructor.newInstance();
            } else {
                packageParser = PackageParser_constructor.newInstance(apkFullPath);
            }
        }

        assetManager = AssetManager.class.newInstance();
        Method method = AssetManager.class.getDeclaredMethod("addAssetPath", new Class[]{String.class});
        method.setAccessible(true);
        int cookie = (int) method.invoke(assetManager, apkFullPath);

        Resources hostResources = BundleManager.getInstance().getHostApplicationContext().getResources();
        resources = new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
        android.content.res.XmlResourceParser parser = assetManager.openXmlResourceParser(cookie, "AndroidManifest.xml");
        packageObj = parsePackage(packageParser, resources, parser);

        if (packageObj == null) {
            return null;
        }
        if (override) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                PackageParser$Package_baseCodePath.set(packageObj, apkFullPath);
            }
            getCertificates(packageParser, packageObj);
        }
        return packageObj;
    }

    PackageInfo getPackageInfo(Object packageObj) throws IllegalAccessException {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = (String) PackageParser$Package_packageName.get(packageObj);
        packageInfo.versionCode = (int) PackageParser$Package_mVersionCode.get(packageObj);
        packageInfo.versionName = (String) PackageParser$Package_mVersionName.get(packageObj);
        packageInfo.signatures = (Signature[]) PackageParser$Package_mSignatures.get(packageObj);
        return packageInfo;
    }

    Bundle getAppMetaData(Object packageObj) {
        Bundle bundle = null;
        try {
            bundle = (Bundle) PackageParser$Package_mAppMetaData.get(packageObj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return bundle;
    }

    ApplicationInfo getApplicationInfo(Object packageObj) throws IllegalAccessException {
//        PackageParser$Package_packageName.set(packageObj, BundleManager.getInstance().getHostApp().getPackageName());
        ApplicationInfo applicationInfo = (ApplicationInfo) PackageParser$Package_applicationInfo.get(packageObj);
//        applicationInfo.name = BundleManager.getInstance().getHostApp().getApplicationInfo().name;
//        applicationInfo.className = BundleManager.getInstance().getHostApp().getApplicationInfo().className;
        applicationInfo.taskAffinity = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().taskAffinity;
        applicationInfo.permission = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().permission;
        applicationInfo.processName = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().processName;
//        applicationInfo.theme = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().theme;
        applicationInfo.flags = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().flags;
        applicationInfo.uiOptions = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().uiOptions;
        applicationInfo.backupAgentName = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().backupAgentName;
        applicationInfo.descriptionRes = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().descriptionRes;
        applicationInfo.targetSdkVersion = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().targetSdkVersion;
        applicationInfo.compatibleWidthLimitDp = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().compatibleWidthLimitDp;
        applicationInfo.uid = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().uid;
        applicationInfo.largestWidthLimitDp = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().largestWidthLimitDp;
        applicationInfo.enabled = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().enabled;
        applicationInfo.requiresSmallestWidthDp = BundleManager.getInstance().getHostApplicationContext().getApplicationInfo().requiresSmallestWidthDp;
//        applicationInfo.packageName = BundleManager.getInstance().getHostApp().getApplicationInfo().packageName;
        return applicationInfo;
    }

    void getCertificates(Object packageParser, Object packageObj) throws InvocationTargetException, IllegalAccessException {
        PackageParser_collectCertificates.invoke(packageParser, packageObj, 0);
    }

    BundleEntry createBundleEntry(String apkFullPath, Object packageObj, PackageInfo packageInfo) throws IllegalAccessException {
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "createBundleEntry(), start");
        }
        ApplicationInfo applicationInfo = getApplicationInfo(packageObj);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "createBundleEntry(), got application info.");
        }
        packageInfo.applicationInfo = applicationInfo;
        ClassLoader loader = BundleManager.getInstance().getClassLoader(apkFullPath, packageInfo.packageName);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "createBundleEntry(), got ClassLoader");
        }
        Bundle bundle = getAppMetaData(packageObj);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "createBundleEntry(), got Meta Data");
        }
        BundleEntry bundleEntry = new BundleEntry(apkFullPath, packageInfo, (DexClassLoader) loader, assetManager, resources, bundle);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "createBundleEntry(), got BundleEntry object");
        }

        activitys = (ArrayList<Object>) PackageParser$Package_activities.get(packageObj); //ArrayList<PackageParser.Activity>
        services = (ArrayList<Object>) PackageParser$Package_services.get(packageObj); //ArrayList<PackageParser.Service>
        providers = (ArrayList<Object>) PackageParser$Package_providers.get(packageObj); //ArrayList<PackageParser.Provider>
        receivers = (ArrayList<Object>) PackageParser$Package_receivers.get(packageObj); //ArrayList<PackageParser.Activity>
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "createBundleEntry(), got components' info.");
        }

        addActivity(bundleEntry);
        addService(bundleEntry);
        addProvider(bundleEntry);
        addReceiver(bundleEntry);
        if (debugPerf) {
            LogUtil.w("Performance/" + TAG, "createBundleEntry(), end");
        }

        return bundleEntry;
    }

    private void addActivity(BundleEntry bundleEntry) {
        if (activitys != null) {
            for (Object activity : activitys) {
                try {
                    ActivityInfo activityInfo = (ActivityInfo) activity.getClass().getField("info").get(activity);
//                    if (activityInfo.targetActivity != null) {
//                        activityInfo.taskAffinity = BundleManager.getInstance().getHostApp().getApplicationInfo().taskAffinity;
//                    }
//                    activityInfo.processName = TextUtils.isEmpty(activityInfo.processName) ? BundleManager.getInstance().getHostApp().getPackageName() : activityInfo.processName;
//                    activityInfo.packageName = BundleManager.getInstance().getHostApp().getApplicationInfo().packageName;
                    bundleEntry.mActivityInfo.put(activityInfo.name, activityInfo);
                    ArrayList<IntentFilter> intents = (ArrayList<IntentFilter>) PackageParser$Component_intents.get(activity);
                    for (IntentFilter intent : intents) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.filter = intent;
                        resolveInfo.activityInfo = activityInfo;
                        if (bundleEntry.mActivityFilter == null) {
                            bundleEntry.mActivityFilter = new ArrayList<>();
                        }
                        bundleEntry.mActivityFilter.add(resolveInfo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addService(BundleEntry bundleEntry) {
        if (services != null) {
            for (Object service : services) {
                try {
                    ServiceInfo serviceInfo = (ServiceInfo) service.getClass().getField("info").get(service);
//                    serviceInfo.processName = TextUtils.isEmpty(serviceInfo.processName) ? BundleManager.getInstance().getHostApp().getPackageName() : serviceInfo.processName;
//                    serviceInfo.packageName = BundleManager.getInstance().getHostApp().getApplicationInfo().packageName;
                    bundleEntry.mServiceInfo.put(serviceInfo.name, serviceInfo);

                    ArrayList<IntentFilter> intents = (ArrayList<IntentFilter>) PackageParser$Component_intents.get(service);
                    for (IntentFilter intent : intents) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.filter = intent;
                        resolveInfo.serviceInfo = serviceInfo;
                        if (bundleEntry.mServiceFilter == null) {
                            bundleEntry.mServiceFilter = new ArrayList<>();
                        }
                        bundleEntry.mServiceFilter.add(resolveInfo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addProvider(BundleEntry bundleEntry) {
        if (providers != null) {
            for (Object provider : providers) {
                try {
                    ProviderInfo providerInfo = (ProviderInfo) provider.getClass().getField("info").get(provider);
                    bundleEntry.mProviderInfo.add(providerInfo);
////                    providerInfo.processName = TextUtils.isEmpty(providerInfo.processName) ? BundleManager.getInstance().getHostApp().getPackageName() : providerInfo.processName;
////                    providerInfo.packageName = BundleManager.getInstance().getHostApp().getApplicationInfo().packageName;
//                    bundleEntry.mProviderInfo.put(providerInfo.name, providerInfo);
//
//                    ArrayList<IntentFilter> intents = (ArrayList<IntentFilter>) PackageParser$Component_intents.get(provider);
//                    for (IntentFilter intent : intents) {
//                        ResolveInfo resolveInfo = new ResolveInfo();
//                        resolveInfo.filter = intent;
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                            resolveInfo.providerInfo = providerInfo;
//                        }
//                        if (bundleEntry.mProviderFilter == null) {
//                            bundleEntry.mProviderFilter = new ArrayList<>();
//                        }
//                        bundleEntry.mProviderFilter.add(resolveInfo);
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addReceiver(BundleEntry bundleEntry) {
        if (receivers != null) {
            for (Object receiver : receivers) {
                try {
                    ActivityInfo activityInfo = (ActivityInfo) receiver.getClass().getField("info").get(receiver);
//                    activityInfo.processName = TextUtils.isEmpty(activityInfo.processName) ? BundleManager.getInstance().getHostApp().getPackageName() : activityInfo.processName;
//                    activityInfo.packageName = BundleManager.getInstance().getHostApp().getApplicationInfo().packageName;
                    bundleEntry.mReceiverInfo.put(activityInfo.name, activityInfo);

                    ArrayList<IntentFilter> intents = (ArrayList<IntentFilter>) PackageParser$Component_intents.get(receiver);
                    for (IntentFilter intent : intents) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.activityInfo = activityInfo;
                        resolveInfo.filter = intent;
                        if (bundleEntry.mReceiverFilter == null) {
                            bundleEntry.mReceiverFilter = new ArrayList<>();
                        }
                        bundleEntry.mReceiverFilter.add(resolveInfo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
