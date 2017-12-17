package io.qivaz.demo.plugin;

import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * @author Qinghua Zhang @create 2017/7/25.
 */
public class PluginRemoteImpl {
    private static final String TAG = PluginRemoteImpl.class.getSimpleName();
    private static int sum;

    static {
        sum = 10;
    }

    public PluginRemoteImpl(int num) {
        sum = num;
        Log.e(TAG, "PluginRemoteImpl$<init>(), sum=" + sum);
    }

    public static void showToast() {
        Toast.makeText(App.getInstance(), "I'M a TOAST\r\nNice to meet you!", Toast.LENGTH_LONG).show();
    }

    public int plus(int val) {
        sum += val;
        Log.e(TAG, "PluginRemoteImpl$plus(), sum=" + sum);
        return sum;
    }

    public int minus(int val) {
        sum -= val;
        Log.e(TAG, "PluginRemoteImpl$minus(), sum=" + sum);
        return sum;
    }

    public int clear() {
        sum = 0;
        Log.e(TAG, "PluginRemoteImpl$clear(), sum=" + sum);
        return sum;
    }

    public ImageView getImageView() {
        ImageView iv = new ImageView(App.getInstance());
        iv.setImageResource(R.drawable.ic_filter_vintage_black_48dp);
        return iv;
    }
}
