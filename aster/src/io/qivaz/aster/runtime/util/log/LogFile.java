package io.qivaz.aster.runtime.util.log;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Qinghua Zhang @create 2017/3/24.
 */
final class LogFile {
    private static final int LOG_FILES_KEEP_DAYS = 7; //Include today
    private static String logPath;
    private static LogFile instance = new LogFile();
    static private boolean printAllLevel = true;
    private SimpleDateFormat logFileNameDateFormat;
    private SimpleDateFormat logDateFormat;
    private String logFile;
    private String logFileNameSuffix;

    private HandlerThread asyncHandlerThread = new HandlerThread("AsterLogger");
    private Handler asyncHandler;
    private boolean init = false;

    private LogFile() {
    }

    protected static LogFile getInstance() {
        return instance;
    }

    public static void setPrintAllLevel(boolean on) {
        printAllLevel = on;
    }

    protected synchronized void init(Context context) {
        if (!init) {
            logFileNameDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            logDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
            asyncHandlerThread.start();
            asyncHandler = new Handler(asyncHandlerThread.getLooper());
            logPath = Environment.getExternalStorageDirectory() + "/aster/" + context.getPackageName() + "/log";
            logFileNameSuffix = ".log";

            logFile = logFileNameDateFormat.format(new Date()) + logFileNameSuffix;
            init = true;
        }
    }

    protected void writeFile(final String type, final String tag, String msg) {
        writeFile(android.os.Process.myTid(), type, tag, msg);
    }

    protected void writeFile(final int thread, final String type, final String tag, final String msg) {
        if (init) {
            if (logFileNameDateFormat != null) {
                if ("w".equals(type) || "e".equals(type) || printAllLevel) {
                    asyncHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            writeFileAsync(thread, type, tag, msg);
                        }
                    });
                }
            }
        }
    }

    private void writeFileAsync(int thread, String type, String tag, String msg) {
        FileOutputStream outStream = null;
        try {
            Date date = new Date();
            String msgPrefix = logDateFormat.format(date) + " " + android.os.Process.myPid() + "/" + thread + " " + type.toUpperCase() + "/" + tag + ": ";
            File dir = new File(logPath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    return;
                }
            }
            File file = new File(logPath, logFile);
            outStream = new FileOutputStream(file, true);

            final StringBuilder sb = new StringBuilder();
            sb.append(msgPrefix);
            sb.append(msg);
            sb.append("\r\n");

            outStream.write(sb.toString().getBytes("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void delFile() {
        asyncHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    List fileNames = getExpiredFileNameList();
                    File dir = new File(logPath);
                    if (!dir.isDirectory() || dir.length() < 1L) {
                        return;
                    }

                    File[] files = dir.listFiles();
                    int length = files.length;

                    for (int i = 0; i < length; ++i) {
                        File file = files[i];
                        if (!fileNames.contains(file.getName())) {
                            file.delete();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private List<String> getExpiredFileNameList() {
        try {
            Date now = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            List<String> fileNames = new ArrayList<>();

            for (int i = 0; i < LOG_FILES_KEEP_DAYS; ++i) {
                fileNames.add(logFileNameDateFormat.format(calendar.getTime()) + logFileNameSuffix);
                calendar.add(Calendar.DATE, -1);
            }
            return fileNames;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
