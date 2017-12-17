package io.qivaz.aster.runtime.util.log;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class DebugUtils {
    private static final String FALSE = "false";
    private static final String TRUE = "true";
    private static final String DEBUG = "debug";
    private static boolean bInit;
    private static boolean debugMode;

    /**
     * true: "true".equals(msg.toLowerCase())
     * false: (TextUtils.isEmpty(msg)
     * || "false".equals(msg.toLowerCase())
     * || other unexpected characters)
     */
    public static boolean getDebugMode(Context context) {
        if (!bInit) {
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                        PackageManager.GET_META_DATA);
                String msg = appInfo.metaData.getString(DEBUG);

                debugMode = TRUE.equals(msg.toLowerCase());
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            bInit = true;
        }

        return debugMode;
    }

    public static void setDebugMode(boolean debug) {
        bInit = true;
        debugMode = debug;
    }
}
