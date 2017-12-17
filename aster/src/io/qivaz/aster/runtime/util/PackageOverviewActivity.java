package io.qivaz.aster.runtime.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import io.qivaz.aster.runtime.bundle.BundleFeature;
import io.qivaz.aster.runtime.bundle.BundleManager;
import io.qivaz.aster.runtime.bundle.BundleRegistry;
import io.qivaz.aster.runtime.util.log.LogUtil;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class PackageOverviewActivity extends Activity {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "PackageOverviewActivity";
    private BundleManager mBundleManager;
    private ExpandableListView mExpandableListView;
    private ArrayList<BundleRegistry.BundleItem> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExpandableListView = new ExpandableListView(this);
        setContentView(mExpandableListView);
        init(this);
    }

    public void init(Context context) {
        list = new ArrayList(BundleManager.getInstance().getBundleRegistry().getList());
        mExpandableListView.setAdapter(new PackageAdapter(this));
        mExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPos, int childPos, long id) {
                LogUtil.e(TAG, "onChildClick(), parent=" + parent + ", v=" + v + ", groupPos=" + groupPos + ", childPos=" + childPos + ", id=" + id);
                String packageName = (String) list.get(groupPos).mPackageName;
                String activity = (String) list.get(groupPos).mActivityNameSet.toArray()[childPos];
                Intent intent = new Intent();
                intent.setClassName(packageName, activity);
                try {
                    BundleManager.getInstance().startActivity(PackageOverviewActivity.this, intent);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        LogUtil.e(TAG, "onBackPressed()");
        super.onBackPressed();
//        finish();
    }

    public class PackageAdapter extends BaseExpandableListAdapter {
        public PackageAdapter(Context context) {
        }

        @Override
        public int getGroupCount() {
            LogUtil.e(TAG, "getGroupCount(), " + list.size());
            return list.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            LogUtil.e(TAG, "getChildrenCount(), " + list.get(groupPosition).mActivityNameSet.size());
            return list.get(groupPosition).mActivityNameSet.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            LogUtil.e(TAG, "getGroup(), " + list.get(groupPosition));
            return list.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            LogUtil.e(TAG, "getChild(), " + list.get(groupPosition).mActivityNameSet.toArray()[childPosition]);
            return list.get(groupPosition).mActivityNameSet.toArray()[childPosition];
        }

        @Override
        public long getGroupId(int groupPosition) {
            LogUtil.e(TAG, "getGroupId(), " + groupPosition);
            return 0;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            LogUtil.e(TAG, "getChildId(), " + groupPosition + ", " + childPosition);
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView tv;
            LinearLayout ll;
            if (convertView == null) {
                ll = new LinearLayout(PackageOverviewActivity.this);
                ll.setOrientation(LinearLayout.HORIZONTAL);
                ll.setMinimumHeight(145);

                tv = new TextView(PackageOverviewActivity.this);
                tv.setTag(ll);
                ll.setPadding(85, 0, 0, 0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.gravity = Gravity.CENTER_VERTICAL;
                ll.addView(tv, lp);
                convertView = ll;
            } else {
                ll = (LinearLayout) convertView;
                tv = (TextView) ll.findViewWithTag(ll);
            }
            BundleRegistry.BundleItem item = list.get(groupPosition);
            tv.setText(item.mPackageName + "(" + item.mVersionCode + ")");
            convertView.setBackgroundColor(Color.GRAY);
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            TextView tv;
            LinearLayout ll;
            if (convertView == null) {
                ll = new LinearLayout(PackageOverviewActivity.this);
                ll.setOrientation(LinearLayout.HORIZONTAL);
                ll.setMinimumHeight(145);

                tv = new TextView(PackageOverviewActivity.this);
                tv.setTag(ll);
                ll.setPadding(100, 0, 0, 0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.gravity = Gravity.CENTER_VERTICAL;
                ll.addView(tv, lp);
                convertView = ll;
            } else {
                ll = (LinearLayout) convertView;
                tv = (TextView) ll.findViewWithTag(ll);
            }
            String activity = (String) list.get(groupPosition).mActivityNameSet.toArray()[childPosition];
            tv.setText(activity);
            convertView.setBackgroundColor(Color.LTGRAY);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}