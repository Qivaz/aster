package io.qivaz.demo.host.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import io.qivaz.aster.runtime.Aster;
import io.qivaz.demo.host.R;

/**
 * @author Qinghua Zhang @create 2017/7/11.
 */
public class HostActivity2 extends FragmentActivity {
    private RelativeLayout rl;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("HostActivity2", "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host2);

        rl = (RelativeLayout) findViewById(R.id.container);
        iv = (ImageView) Aster.fetchSharedObject("image_view");
        if (iv != null) {
            rl.addView(iv);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        Log.e("HostActivity2", "onNewIntent()");
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart() {
        Log.e("HostActivity2", "onStart()");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.e("HostActivity2", "onRestart()");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.e("HostActivity2", "onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.e("HostActivity2", "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.e("HostActivity2", "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e("HostActivity2", "onDestroy()");

        if (rl != null) {
            rl.removeView(iv);
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
