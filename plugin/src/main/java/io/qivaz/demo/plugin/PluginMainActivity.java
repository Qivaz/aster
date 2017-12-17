package io.qivaz.demo.plugin;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import io.qivaz.aster.runtime.Aster;

/**
 * @author Qinghua Zhang @create 2017/7/11.
 */

public class PluginMainActivity extends FragmentActivity {
    private static final String TAG = PluginMainActivity.class.getSimpleName();
    BroadcastReceiver receiver = new PluginReceiver2();
    private ServiceConnection hostServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "hostServiceConnection.onServiceConnected(), " + name);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "hostServiceConnection.onServiceDisconnected(), " + name);
        }
    };
    private ServiceConnection bundleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "bundleServiceConnection.onServiceConnected(), " + name);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "bundleServiceConnection.onServiceDisconnected(), " + name);
        }
    };

    public static ProgressDialog showProgress(Context context, String title,
                                              String content, boolean indeterminate, boolean cancelable) {
        ProgressDialog dialog = null;
        try {
            dialog = ProgressDialog.show(context, title, null,
                    indeterminate, cancelable);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setContentView(R.layout.loading_dialog);
        } catch (Exception e) {
            Log.e(TAG, "showProgress(), " + e);
            e.printStackTrace();
        }
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_main);
    }

    public void onClickStartHostActivity0(View v) {
        Log.e(TAG, "onClickStartHostActivity0(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.host", "io.qivaz.host.activity.HostActivity0");
        Aster.startActivity(this, intent);
    }

    public void onClickStartPluginActivity0(View v) {
        Log.e(TAG, "onClickStartPluginActivity0(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "PluginActivity0");
        Aster.startActivity(this, intent);
    }

    public void onClickStartPluginActivity1(View v) {
        Log.e(TAG, "onClickStartPluginActivity1(), " + v);
        Intent intent = new Intent();
        intent.setClass(this, PluginActivity1.class);
        startActivity(intent);
    }

    public void onClickStartPluginActivityTranslucent(View v) {
        Log.e(TAG, "onClickStartPluginActivityTranslucent(), " + v);
        Intent intent = new Intent();
        intent.setClass(this, PluginActivityTranslucent.class);
        startActivity(intent);
    }

    public void onClickStartHostActivity4Action(View v) {
        Log.e(TAG, "onClickStartHostActivity4Action(), " + v);
        Intent intent = new Intent();
        intent.setAction("io.qivaz.host.test");

        //The same as Aster.startActivity(this, intent);
        startActivity(intent);
    }

    public void onClickStartHostService0(View v) {
        Log.e(TAG, "onClickStartHostService0(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.host", "io.qivaz.host.service.HostService0");
        Aster.startService(this, intent);
    }

    public void onClickStopHostService0(View v) {
        Log.e(TAG, "onClickStopHostService0(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.host", "io.qivaz.host.service.HostService0");
        Aster.stopService(this, intent);
    }

    public void onClickBindHostService1(View v) {
        Log.e(TAG, "onClickBindHostService1(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.host", "io.qivaz.host.service.HostService1");
        Aster.bindService(this, intent, hostServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onClickUnbindHostService1(View v) {
        Log.e(TAG, "onClickUnbindHostService1(), " + v);
        Aster.unbindService(this, hostServiceConnection);
    }

    public void onClickStartBundleService0(View v) {
        Log.e(TAG, "onClickStartBundleService0(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "PluginService0");
        startService(intent);
    }

    public void onClickStopBundleService0(View v) {
        Log.e(TAG, "onClickStopBundleService0(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "PluginService0");
        stopService(intent);
    }

    public void onClickBindBundleService1(View v) {
        Log.e(TAG, "onClickBindBundleService1(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "PluginService1");
        bindService(intent, bundleServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onClickUnbindBundleService1(View v) {
        Log.e(TAG, "onClickUnbindBundleService1(), " + v);
        unbindService(bundleServiceConnection);
    }

    public void onClickRegisterPluginReceiver2(View v) {
        Log.e(TAG, "onClickRegisterPluginReceiver2(), " + v);
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(receiver, intentFilter);
    }

    public void onClickUnregisterPluginReceiver2(View v) {
        Log.e(TAG, "onClickUnregisterPluginReceiver2(), " + v);
        unregisterReceiver(receiver);
    }

    public void onClickShowPluginProgressDialog(View v) {
        Log.e(TAG, "onClickShowPluginProgressDialog(), " + v);
        showProgress(this, "hello", "World", true, true);
    }

//    @Override
//    public Object getSystemService(String name) {
//        Object service;
//        if (getParent() != null) {
//            service = getParent().getSystemService(name);
//            Log.e(TAG, "getSystemService(" + name + "),1 " + service);
//        } else {
//            service = super.getSystemService(name);
//            Log.e(TAG, "getSystemService(" + name + "),2 " + service);
//        }
//        return service;
//    }
}
