package io.qivaz.aster.runtime.util;

import android.os.Build;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.util.log.LogUtil;

public final class SoLibUtils {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "SoLibUtils";

    public static String getCpuName() {
        return Build.CPU_ABI;
    }

    public static boolean copyBundleSoLib(String apkPath, String nativeLibDir, String cpuName) {
        boolean hasSoFile = false;
        File apkFile = new File(apkPath);
        if (!TextUtils.isEmpty(nativeLibDir) && apkFile.exists() && apkFile.isFile()) {
            ZipInputStream zis = null;
            try {
                zis = new ZipInputStream(new FileInputStream(apkFile));
                ZipEntry entry = null;

                while ((entry = zis.getNextEntry()) != null) {
                    String zipEntryName = entry.getName();
                    if (!entry.isDirectory() && zipEntryName.endsWith(".so")) {
                        String parentFolder = zipEntryName.substring(0, zipEntryName.lastIndexOf("/"));
                        parentFolder = parentFolder.substring(parentFolder.lastIndexOf("/") + 1);
                        if (parentFolder.equals(cpuName)) {
                            hasSoFile = true;
                            File nativeLibDirFile = new File(nativeLibDir);
                            if (!nativeLibDirFile.exists()) {
                                nativeLibDirFile.mkdirs();
                            }

                            FileOutputStream fos = null;
                            String soFileName = null;
                            try {
                                soFileName = zipEntryName.substring(zipEntryName.lastIndexOf("/") + 1);
                                File soFile = new File(nativeLibDir, soFileName);
                                if (soFile.exists()) {
                                    if (!soFile.delete()) {
                                        LogUtil.e(TAG, "copyBundleSOLib(), delete \"" + soFile.getAbsolutePath() + "\" failed!");
                                    }
                                }
                                fos = new FileOutputStream(soFile);
                                byte[] buf = new byte[1024];
                                int count;
                                while ((count = zis.read(buf)) != -1) {
                                    fos.write(buf, 0, count);
                                }

                                if (debug) {
                                    LogUtil.v(TAG, "copyBundleSOLib(), write so lib finished: " + soFileName);
                                }
                            } catch (Exception e) {
                                LogUtil.e(TAG, "copyBundleSOLib(), write so lib failed: " + soFileName + ", e=" + e);
                            } finally {
                                zis.closeEntry();
                                if (fos != null) {
                                    fos.close();
                                }

                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "copyBundleSOLib(), failed, e=" + e);
            } finally {
                if (zis != null) {
                    try {
                        zis.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            LogUtil.e(TAG, "copyBundleSOLib(), failed, apkPath=" + apkPath + ", nativeLibDir=" + nativeLibDir + ", cpuName=" + cpuName);
        }
        return hasSoFile;
    }

    private static Set<String> getAllSoFolder(String dexPath) {
        Set<String> folderSet = new HashSet<>();
        File file = new File(dexPath);
        if (file.exists() && file.isFile()) {
            ZipInputStream zin = null;

            try {
                zin = new ZipInputStream(new FileInputStream(file));
                ZipEntry entry = null;

                while ((entry = zin.getNextEntry()) != null) {
                    String zipEntryName = entry.getName();
                    if (!entry.isDirectory()) {
                        if (zipEntryName.endsWith(".so")) {
                            String parentFolder = zipEntryName.substring(0, zipEntryName.lastIndexOf("/"));
                            parentFolder = parentFolder.substring(parentFolder.lastIndexOf("/") + 1);
                            folderSet.add(parentFolder);
                        }

                        zin.closeEntry();
                    }
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "getAllSoFolder(), failed, e=" + e);
            } finally {
                if (zin != null) {
                    try {
                        zin.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return folderSet;
        } else {
            return folderSet;
        }
    }

    private static String getCpuPrefix(String name) {
        String[] strs = name.split("-");
        String prefix = name;
        if (strs.length > 0) {
            prefix = strs[0];
        }

        return prefix;
    }

    public static String getSupposedCpuName(String dexPath, String cpuName) {
        Set<String> folderNameList = getAllSoFolder(dexPath);
        String cpuPrefix = getCpuPrefix(cpuName);
        for (Object aFolderNameList : folderNameList) {
            String folderName = (String) aFolderNameList;
            String folderPrefix = getCpuPrefix(folderName);
            if (cpuPrefix.equals(folderPrefix)) {
                return folderName;
            }
        }

        LogUtil.e(TAG, "getSupposedCpuName(), not match SO with cpu name, " + cpuName);
        return cpuName;
    }
}
