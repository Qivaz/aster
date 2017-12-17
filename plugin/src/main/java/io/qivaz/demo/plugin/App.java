package io.qivaz.demo.plugin;

import android.app.Application;

/**
 * Created by @author:Qinghua Zhang on 2017/7/26.
 */
public class App extends Application {
    private static Application mApp;

    public static Application getInstance() {
        return mApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
    }
}
