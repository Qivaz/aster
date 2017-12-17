package io.qivaz.demo.plugin;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import io.qivaz.aster.runtime.Aster;


/**
 * @author Qinghua Zhang @create 2017/7/11.
 */
public class PluginActivity0 extends FragmentActivity {
    boolean bNotif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin0);

        ImageButton fab = (ImageButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notification();
                toast();
            }
        });
    }

    private void notification() {
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!bNotif) {
            PendingIntent contentIntent = PendingIntent.getActivity(
                    this, 0, new Intent(this, PluginActivity1.class), 0);

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_menu_gallery)
                            .setContentTitle("My notification")
                            .setContentText("Hello World!")
                            .setContentIntent(contentIntent);
            //mBuilder.setSmallIcon(this.getResources().getIdentifier("ic_menu_gallery", "drawable", this.getPackageName()));
            mBuilder.setSmallIcon(Aster.getHostApplicationContext().getResources().getIdentifier("ic_menu_gallery", "drawable", Aster.getHostApplicationContext().getPackageName()));
            Log.e("PluginActivity0", "setSmallIcon() ===> " + Aster.getHostApplicationContext());
            Notification notification = mBuilder.build();
            mNotifyMgr.notify(1000, notification);

            bNotif = true;
        } else {
            mNotifyMgr.cancel(1000);

            bNotif = false;
        }
    }

    private void toast() {
        Toast.makeText(this, R.string.toast_string, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_plugin, menu);
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
