package io.qivaz.demo.host.activity;

import android.app.Dialog;
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
import android.widget.Button;
import android.widget.EditText;

import io.qivaz.aster.runtime.Aster;
import io.qivaz.demo.host.R;
import io.qivaz.demo.host.receiver.HostReceiver2;

/**
 * @author Qinghua Zhang @create 2017/7/11.
 */
public class HostMainActivity extends FragmentActivity {
    private static final String TAG = HostMainActivity.class.getSimpleName();
    BroadcastReceiver receiver = new HostReceiver2();
    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "ServiceConnection.onServiceConnected(), " + name);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "ServiceConnection.onServiceDisconnected(), " + name);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_main);
    }

    public void onClickInstallAssetApks(View v) {
        Log.e(TAG, "onClickInstallAssetApks(), " + v);
        Aster.installAssetsBundles(this/*, new String[]{"BaiduWenku.apk", "plugin-debug.apk", "Aster.apk"}*/);
    }

    public void onClickInstallApk(View v) {
        Log.e(TAG, "onClickInstallApk(), " + v);

        final Dialog dialog = new Dialog(this, R.style.DialogStyle2);
        dialog.setContentView(R.layout.dialog_install);
        final EditText path = (EditText) dialog.findViewById(R.id.editText);
        Button ok = (Button) dialog.findViewById(R.id.ok);
        Button cancel = (Button) dialog.findViewById(R.id.cancel);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String apk = path.getText().toString();
                Aster.installBundle(apk, true);
                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void onClickUninstallApk(View v) {
        Log.e(TAG, "onClickUninstallApk(), " + v);
        final Dialog dialog = new Dialog(this, R.style.DialogStyle2);
        dialog.setContentView(R.layout.dialog_uninstall);
        final EditText path = (EditText) dialog.findViewById(R.id.editText);
        Button ok = (Button) dialog.findViewById(R.id.ok);
        Button cancel = (Button) dialog.findViewById(R.id.cancel);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bundleName = path.getText().toString();
                Aster.uninstallBundle(bundleName);
                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void onClickStartHostActivity0(View v) {
        Log.e(TAG, "onClickStartHostActivity0(), " + v);
        Intent intent = new Intent();
        intent.setClass(this, HostActivity0.class);
        startActivity(intent);
    }

    public void onClickStartHostActivity1(View v) {
        Log.e(TAG, "onClickStartHostActivity1(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.host", "HostActivity1");
        Aster.startActivity(this, intent);
    }

    public void onClickStartHostActivity2(View v) {
        Log.e(TAG, "onClickStartHostActivity2(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.host", "HostActivity2");
        Aster.startActivity(this, intent);
    }

    public void onClickStartPluginActivity0(View v) {
        Log.e(TAG, "onClickStartPluginActivity0(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "io.qivaz.plugin.PluginActivity0");
        Aster.startActivity(this, intent);
    }

    public void onClickStartPluginActivity1(View v) {
        Log.e(TAG, "onClickStartPluginActivity1(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "io.qivaz.plugin.PluginActivity1");
        Aster.startActivity(this, intent);
    }

    public void onClickStartPluginActivity2(View v) {
        Log.e(TAG, "onClickStartPluginActivity2(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "io.qivaz.plugin.PluginActivity2");
        Aster.startActivity(this, intent);
    }

    public void onClickStartPluginActivity4Action(View v) {
        Log.e(TAG, "onClickStartPluginActivity4Action(), " + v);
        Intent intent = new Intent();
        intent.setAction("io.qivaz.plugin.test");
        Aster.startActivity(this, intent);
    }

    public void onClickStartService0(View v) {
        Log.e(TAG, "onClickStartService0(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "io.qivaz.plugin.PluginService0");
        Aster.startService(this, intent);
    }

    public void onClickStopService0(View v) {
        Log.e(TAG, "onClickStopService0(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "io.qivaz.plugin.PluginService0");
        Aster.stopService(this, intent);
    }

    public void onClickBindService1(View v) {
        Log.e(TAG, "onClickBindService1(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "io.qivaz.plugin.PluginService1");
        Aster.bindService(this, intent, sc, Context.BIND_AUTO_CREATE);
    }

    public void onClickUnbindService1(View v) {
        Log.e(TAG, "onClickUnbindService1(), " + v);
        Aster.unbindService(this, sc);
    }

    public void onClickRegisterReceiver(View v) {
        Log.e(TAG, "onClickRegisterReceiver(), " + v);
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(receiver, intentFilter);
    }

    public void onClickUnregisterReceiver(View v) {
        Log.e(TAG, "onClickUnregisterReceiver(), " + v);
        unregisterReceiver(receiver);
    }

    public void onClickStartPluginMain(View v) {
        Log.e(TAG, "onClickStartPluginMain(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.plugin", "io.qivaz.plugin.PluginMainActivity");
        Aster.startActivity(this, intent);
    }

    public void onClickStartCallPluginRemoteImpl(View v) {
        Log.e(TAG, "onClickStartPluginMain(), " + v);
        Intent intent = new Intent();
        intent.setClassName("io.qivaz.host", "HostActivity3CallRemote");
        startActivity(intent);
    }
}
