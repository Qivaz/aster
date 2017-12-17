package io.qivaz.demo.host.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import io.qivaz.demo.host.R;


/**
 * @author Qinghua Zhang @create 2017/7/11.
 */
public class HostActivity0 extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("HostActivity0", "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host0);

        ImageButton fab = (ImageButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(HostActivity0.this, HostActivity1.class);
                startActivity(intent);
            }
        });

//        requestPermissions(new String[] {Manifest.permission.WRITE_CONTACTS},
//                2000);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        Log.e("HostActivity0", "onNewIntent()");
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart() {
        Log.e("HostActivity0", "onStart()");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.e("HostActivity0", "onRestart()");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.e("HostActivity0", "onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.e("HostActivity0", "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.e("HostActivity0", "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e("HostActivity0", "onDestroy()");
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("HostActivity0", "onActivityResult(), " + requestCode + ", " + resultCode + ", " + data);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
