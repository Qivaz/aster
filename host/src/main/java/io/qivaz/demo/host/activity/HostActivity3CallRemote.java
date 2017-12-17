package io.qivaz.demo.host.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import io.qivaz.aster.runtime.Aster;
import io.qivaz.aster.runtime.BundleNotInstalledException;
import io.qivaz.aster.runtime.CanNotLaunchBundleAppException;
import io.qivaz.aster.runtime.NoAliasExistException;
import io.qivaz.demo.host.R;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Qinghua Zhang @create 2017/7/11.
 */
public class HostActivity3CallRemote extends FragmentActivity {
    private static final String TAG = HostActivity3CallRemote.class.getSimpleName();
    private EditText sum;
    private Object remoteObj;
    private FrameLayout fl;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host3);
        sum = (EditText) findViewById(R.id.sum);
        sum.setSelection(sum.getText().length());
    }

    public void onClickConstructPluginRemoteObject(View v) {
        Log.e(TAG, "onClickConstructPluginRemoteObject(), " + v);
        try {
            remoteObj = Aster.constructAlias("plugin", "io.qivaz.plugin.PluginRemoteImpl", Integer.parseInt(sum.getText().toString()));
//        remoteObj = Aster.construct("io.qivaz.plugin", "io.qivaz.plugin.PluginRemoteImpl", Integer.parseInt(sum.getText().toString()));
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (CanNotLaunchBundleAppException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (BundleNotInstalledException e) {
            e.printStackTrace();
        } catch (NoAliasExistException e) {
            e.printStackTrace();
        }
    }

    public void onClickInvokePluginRemoteMethod1(View v) {
        Log.e(TAG, "onClickInvokePluginRemoteMethod1(), " + v);
        int result = 0;
        try {
            result = (int) Aster.invoke(remoteObj, "plus", 1);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        sum.setText(String.valueOf(result));
        sum.setSelection(sum.getText().length());
    }

    public void onClickInvokePluginRemoteMethod2(View v) {
        Log.e(TAG, "onClickInvokePluginRemoteMethod2(), " + v);
        int result = 0;
        try {
            result = (int) Aster.invoke(remoteObj, "minus", 1);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        sum.setText(String.valueOf(result));
        sum.setSelection(sum.getText().length());
    }

    public void onClickInvokePluginRemoteMethod3(View v) {
        Log.e(TAG, "onClickInvokePluginRemoteMethod3(), " + v);
        int result = 0;
        try {
            result = (int) Aster.invoke(remoteObj, "clear");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        sum.setText(String.valueOf(result));
        sum.setSelection(sum.getText().length());
    }

    public void onClickInvokePluginRemoteMethod4(View v) {
        Log.e(TAG, "onClickInvokePluginRemoteMethod4(), " + v);
        try {
            iv = (ImageView) Aster.invoke(remoteObj, "getImageView");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        fl = (FrameLayout) findViewById(R.id.container);
        if (iv != null) {
            fl.addView(iv);
        }
    }

    public void onClickInvokePluginRemoteMethod5(View v) {
        Log.e(TAG, "onClickInvokePluginRemoteMethod5(), " + v);
        try {
            Aster.invokeStaticAlias("plugin", "io.qivaz.plugin.PluginRemoteImpl", "showToast");
//        Aster.invokeStatic("io.qivaz.plugin", "io.qivaz.plugin.PluginRemoteImpl", "showToast");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (CanNotLaunchBundleAppException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (BundleNotInstalledException e) {
            e.printStackTrace();
        } catch (NoAliasExistException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (fl != null) {
            fl.removeView(iv);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("HostActivity0", "onActivityResult(), " + requestCode + ", " + resultCode + ", " + data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
