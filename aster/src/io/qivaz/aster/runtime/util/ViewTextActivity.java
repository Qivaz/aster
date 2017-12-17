package io.qivaz.aster.runtime.util;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import io.qivaz.aster.runtime.bundle.BundleFeature;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class ViewTextActivity extends Activity {
    private static final boolean debug = BundleFeature.debug;
    private static final String TAG = "ViewTextActivity";
    private TextView mTextView;
    private ScrollView mScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTextView = new TextView(this);
        mScrollView = new ScrollView(this);
        mScrollView.addView(mTextView);

//        mTextView.setVerticalScrollBarEnabled(true);
//        mTextView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
//        mTextView.setMovementMethod(new ScrollingMovementMethod());

        setContentView(mScrollView);
        init();
    }

    public void init() {
        String path = getIntent().getStringExtra("path");
        mTextView.setText(getText(path));
    }

    private String getText(String fullPath) {
        StringBuilder sb = new StringBuilder();
        File file = new File(fullPath);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\r\n");
            }
            sb.delete(sb.length() - 2, sb.length());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}