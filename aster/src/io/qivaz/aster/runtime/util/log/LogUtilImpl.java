package io.qivaz.aster.runtime.util.log;

import android.content.Context;
import android.util.Log;

import io.qivaz.aster.runtime.bundle.BundleFeature;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class LogUtilImpl implements LogUtil.LogInterface {
    private Context mContext;

    public LogUtilImpl(Context context) {
        mContext = context;
        LogFile.getInstance().init(context);
        LogFile.getInstance().delFile();
    }

    @Override
    public void v(String tag, String msg) {
        if (DebugUtils.getDebugMode(mContext)) { // Debug only
            if (BundleFeature.debug) {
                consoleLog("w", tag, msg);
            } else {
                consoleLog("v", tag, msg);
            }
        }
        LogFile.getInstance().writeFile("v", tag, msg);
    }

    @Override
    public void d(String tag, String msg) {
        if (DebugUtils.getDebugMode(mContext)) { // Debug only
            consoleLog("d", tag, msg);
        }
        LogFile.getInstance().writeFile("d", tag, msg);
    }

    @Override
    public void i(String tag, String msg) {
        if (DebugUtils.getDebugMode(mContext)) { // Debug only
            consoleLog("i", tag, msg);
        }
        LogFile.getInstance().writeFile("i", tag, msg);
    }

    @Override
    public void w(String tag, String msg) {
        if (DebugUtils.getDebugMode(mContext)) { // Debug only
            consoleLog("w", tag, msg);
        }
        LogFile.getInstance().writeFile("w", tag, msg);
    }

    @Override
    public void e(String tag, String msg) {
        if (DebugUtils.getDebugMode(mContext)) { // Debug only
            consoleLog("e", tag, msg);
        }
        LogFile.getInstance().writeFile("e", tag, msg);
    }

    private void consoleLog(String type, String tag, String text) {
        if (text != null) {
            for (int start = 0; start < text.length(); start += 2048) {
                int end = text.length() > start + 2048 ? start + 2048 : text.length();
                switch (type) {
                    case "v":
                        Log.v(tag, text.substring(start, end));
                        break;
                    case "d":
                        Log.d(tag, text.substring(start, end));
                        break;
                    case "i":
                        Log.i(tag, text.substring(start, end));
                        break;
                    case "w":
                        Log.w(tag, text.substring(start, end));
                        break;
                    case "e":
                        Log.e(tag, text.substring(start, end));
                        break;
                    default:
                        break;
                }
            }
        }
    }

}
