package io.qivaz.aster.runtime.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import io.qivaz.aster.R;
import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.util.log.LogUtil;


/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class DataOverviewActivity extends Activity {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "DataOverviewActivity";
    private List<Map<String, Object>> mData;
    private Stack<String> mDir;
    private String mCurrent;
    private String mParent;
    private LinearLayout mLinearLayout;
    private ListView mList;
    private PopupMenu popupMenu;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.e(TAG, "onCreate(), " + getIntent());
        super.onCreate(savedInstanceState);
        mLinearLayout = new LinearLayout(this);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(mLinearLayout);
        init(this);
    }

    public void init(Context context) {
        String dir = getIntent().getStringExtra("path");
        if (TextUtils.isEmpty(dir)) {
            dir = "/data/data" + File.separator + context.getPackageName();
        }
        final String copyFileTo = getIntent().getStringExtra("copyFileTo");

        mDir = new Stack<>();
        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }
        mParent = mCurrent = dir;
        mData = getData(mCurrent);

        if (!TextUtils.isEmpty(copyFileTo)) {
            Button button = new Button(this);
            button.setText("复制到此");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    IOUtils.copyTo(copyFileTo, mCurrent);
                    DataOverviewActivity.this.finish();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.TOP;
            mLinearLayout.addView(button, lp);
        }

        mList = new ListView(this);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e(TAG, "onListItemClick(), " + mData.get(position).get("path"));
                if ((boolean) mData.get(position).get("isFolder")) {
                    mDir.push(mCurrent);
                    mCurrent = (String) mData.get(position).get("path");
                    mData = getData(mCurrent);
                    FileAdapter adapter = new FileAdapter(DataOverviewActivity.this);
                    mList.setAdapter(adapter);
                } else {
                    String path = (String) mData.get(position).get("path");
                    showPopupMenu(view, path);
                }
            }
        });
        FileAdapter adapter = new FileAdapter(this);
        mList.setAdapter(adapter);
        mLinearLayout.addView(mList);
    }

    private void showPopupMenu(View view, final String path) {
        popupMenu = new PopupMenu(this, view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupMenu.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        }
        menu = popupMenu.getMenu();
        menu.add(Menu.NONE, Menu.FIRST, 0, "打开");
        menu.add(Menu.NONE, Menu.FIRST + 1, 1, "复制到");
//        MenuInflater menuInflater = popupMenu.getMenuInflater();
//        menuInflater.inflate(R.menu.data_overview_popupmenu, menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case Menu.FIRST:
                        Toast.makeText(DataOverviewActivity.this, "打开" + path,
                                Toast.LENGTH_LONG).show();
                        viewTextFile(path);
                        break;
                    case Menu.FIRST + 1:
                        Toast.makeText(DataOverviewActivity.this, "复制到",
                                Toast.LENGTH_LONG).show();
                        Intent intent = new Intent();
                        intent.setClass(DataOverviewActivity.this, DataOverviewActivity.class);
                        intent.putExtra("path", "/sdcard/");
                        intent.putExtra("copyFileTo", path);
                        DataOverviewActivity.this.startActivity(intent);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void viewFile(String path) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(new File(path));
        i.setData(uri);
        i.setType("text/plain");
        startActivity(i);
    }

    private void viewTextFile(String path) {
        Intent i = new Intent();
        i.setClass(this, ViewTextActivity.class);
        i.putExtra("path", path);//BundleFeature.BUNDLE_REG_FILE
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed(), " + !mDir.empty());
        if (!mDir.empty()) {
            mCurrent = mDir.pop();
            mData = getData(mCurrent);
            FileAdapter adapter = new FileAdapter(this);
            mList.setAdapter(adapter);
        } else {
            finish();
        }
    }

    private List<Map<String, Object>> getData(String dir) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = null;
        File f = new File(dir);
        File[] files = f.listFiles();

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                map = new HashMap<String, Object>();
                map.put("name", files[i].getName());
                map.put("path", files[i].getPath());
                map.put("size", files[i].length());
                map.put("last", files[i].lastModified());
                if (files[i].isDirectory()) {
                    map.put("isFolder", true);
                } else {
                    map.put("isFolder", false);
                }
                list.add(map);
            }
            Log.v(TAG, "getData(), " + Arrays.toString(list.toArray()));

            return list;
        }
        return null;
    }

    public class FileAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public FileAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            int size = 0;
            if (mData != null) {
                size = mData.size();
            }
            return size;
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.data_overview_list_item, null); // 设置列表项的布局
                holder.img = (TextView) convertView.findViewById(R.id.img);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.date = (TextView) convertView.findViewById(R.id.date);
                holder.size = (TextView) convertView.findViewById(R.id.size);
                holder.path = (TextView) convertView.findViewById(R.id.path);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            boolean isFolder = (boolean) mData.get(position).get("isFolder");
            if (isFolder) {
                holder.img.setText("文件夹");
                holder.img.setBackgroundColor(0xFFFFD306);
                holder.size.setVisibility(View.GONE);
            } else {
                holder.img.setText("文件");
                holder.img.setBackgroundColor(0xFFD0D0D0);
                holder.size.setText(String.valueOf((long) mData.get(position).get("size")));
            }
            String format = "yyyy/MM/dd HH:MM:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);

            Date last = new Date((long) mData.get(position).get("last"));
            String time = sdf.format(last);
            holder.date.setText(time);

            holder.name.setText((String) mData.get(position).get("name"));
            holder.path.setText((String) mData.get(position).get("path"));

            Log.v(TAG, "getView(), " + mData.get(position).get("name") + ", " + mData.get(position).get("path"));

            return convertView;
        }
    }

    public final class ViewHolder {
        public TextView img;
        public TextView name;
        public TextView size;
        public TextView date;
        public TextView path;
    }

}