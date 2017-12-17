package io.qivaz.demo.plugin;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * @author Qinghua Zhang @create 2017/6/22.
 */
public class PluginService3 extends Service {

    private final IBinder mBinder = new Binder();

    @Override
    public void onCreate() {
        Log.e("PluginService3", "onCreate()");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.e("PluginService3", "onStart(), " + intent + ", " + startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("PluginService3", "onBinde(), " + intent);
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("PluginService3", "onStartCommand(), " + intent + ", " + flags + ", " + startId);
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        Log.e("PluginService3", "onDestroy()");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e("PluginService3", "onConfigurationChanged(), " + newConfig);
    }

    @Override
    public void onLowMemory() {
        Log.e("PluginService3", "onLowMemory()");
    }

    @Override
    public void onTrimMemory(int level) {
        Log.e("PluginService3", "onTrimMemory()");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e("PluginService3", "onUnbind(), " + intent);
        return false;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.e("PluginService3", "onRebind(), " + intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("PluginService3", "onTaskRemoved(), " + rootIntent);
    }

}
