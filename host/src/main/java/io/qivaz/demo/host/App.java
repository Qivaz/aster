package io.qivaz.demo.host;

import android.app.Application;

import io.qivaz.aster.runtime.Aster;


/**
 * @author Qinghua Zhang @create 2017/7/11.
 */
public class App extends Application {
    public static Application bundleApp;

    @Override
    public void onCreate() {
        super.onCreate();
        Aster.init(this);
    }
}
