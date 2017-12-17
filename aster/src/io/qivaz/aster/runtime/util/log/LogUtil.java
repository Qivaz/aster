package io.qivaz.aster.runtime.util.log;

import android.util.Log;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class LogUtil {
    private static final String ASTER_PREFIX = "ASTER/";

    private static LogInterface impl = new LogInterface() {
        @Override
        public void v(String tag, String msg) {
            Log.v(tag, msg);
        }

        @Override
        public void d(String tag, String msg) {
            Log.d(tag, msg);
        }

        @Override
        public void i(String tag, String msg) {
            Log.i(tag, msg);
        }

        @Override
        public void w(String tag, String msg) {
            Log.w(tag, msg);
        }

        @Override
        public void e(String tag, String msg) {
            Log.e(tag, msg);
        }
    };

    public static void setImpl(LogInterface log) {
        impl = log;
    }

    public static void v(String tag, String msg) {
        impl.v(ASTER_PREFIX + tag, msg);
    }

    public static void d(String tag, String msg) {
        impl.d(ASTER_PREFIX + tag, msg);
    }

    public static void i(String tag, String msg) {
        impl.i(ASTER_PREFIX + tag, msg);
    }

    public static void w(String tag, String msg) {
        impl.w(ASTER_PREFIX + tag, msg);
    }

    public static void e(String tag, String msg) {
        impl.e(ASTER_PREFIX + tag, msg);
    }

    public interface LogInterface {
        void v(String tag, String msg);

        void d(String tag, String msg);

        void i(String tag, String msg);

        void w(String tag, String msg);

        void e(String tag, String msg);
    }
}
