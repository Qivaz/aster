package io.qivaz.demo.host.service;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


/**
 * @author Qinghua Zhang @create 2017/7/11.
 */
public class HostService1 extends Service {

    private final IBinder mBinder = new Binder();

    @Override
    public void onCreate() {
        Log.e("HostService1", "onCreate()");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.e("HostService1", "onStart(), " + intent + ", " + startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("HostService1", "onBinde(), " + intent);
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("HostService1", "onStartCommand(), " + intent + ", " + flags + ", " + startId);
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        Log.e("HostService1", "onDestroy()");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e("HostService1", "onConfigurationChanged(), " + newConfig);
    }

    @Override
    public void onLowMemory() {
        Log.e("HostService1", "onLowMemory()");
    }

    @Override
    public void onTrimMemory(int level) {
        Log.e("HostService1", "onTrimMemory()");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e("HostService1", "onUnbind(), " + intent);
        return false;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.e("HostService1", "onRebind(), " + intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("HostService1", "onTaskRemoved(), " + rootIntent);
    }
}
