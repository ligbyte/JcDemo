package com.niimbot.printer.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.niimbot.jcdemo.R;
import com.niimbot.printer.adapter.ConnectAdapter;

/**
 * 设备连接
 *
 * @author zhangbin
 * 2022.03.17
 */
public class ConnectActivity extends AppCompatActivity  {
    private static final String TAG = "ConnectActivity";

    private ViewPager2 viewPager;
    private TabLayout tabs;
    private ImageView ivBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        init();
    }




    private void init() {

        viewPager = findViewById(R.id.view_pager);
        tabs = findViewById(R.id.tabs);
        ivBack = findViewById(R.id.iv_back);
        ConnectAdapter connectAdapter = new ConnectAdapter(this);
        viewPager.setAdapter(connectAdapter);
        new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            Log.d(TAG, "武汉: " + position);
            tab.setText(position == 0 ? "蓝牙" : "wifi");

        }).attach();

        ivBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}