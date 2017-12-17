package io.qivaz.aster.runtime.util;

import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public abstract class IOUtils {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "IOUtils";

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    public static void copy(String origFullPath, String destFullPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(new File(origFullPath));
            out = new FileOutputStream(new File(destFullPath));
            IOUtils.copyStream(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void copyTo(String origFullPath, String destPath) {
        if (TextUtils.isEmpty(origFullPath)
                || origFullPath.endsWith(File.separator)
                || TextUtils.isEmpty(destPath)) {
            return;
        }
        if (!destPath.endsWith(File.separator)) {
            destPath += File.separator;
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(new File(origFullPath));
            String fileName = origFullPath.substring(origFullPath.lastIndexOf(File.separator) + 1, origFullPath.length());
            out = new FileOutputStream(new File(destPath + fileName));
            IOUtils.copyStream(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}
