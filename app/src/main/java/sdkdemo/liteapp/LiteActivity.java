/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package sdkdemo.liteapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;


import com.example.xie.R;


import java.util.ArrayList;

import sdkdemo.newif.DemoMainActivity;
import sdkdemo.oldif.OldDemoMainActivity;


public class LiteActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity_lite);

        Button startNewIFNormalDemo = (Button) findViewById(R.id.startNewIFNormalDemo);
        startNewIFNormalDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(LiteActivity.this, DemoMainActivity.class);
                startActivity(it);
            }
        });

        Button startNormalDemo = (Button) findViewById(R.id.startNormalDemo);
        startNormalDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(LiteActivity.this, OldDemoMainActivity.class);
                startActivity(it);
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (permissions.size() != 0) {
                requestPermissionsForM(permissions);
            }
        }
    }

    private void requestPermissionsForM(final ArrayList<String> per) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(per.toArray(new String[per.size()]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
