package com.lixiao.wechat;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import android.widget.Toast;

import com.lixiao.wechat.service.WeChatService;

import java.util.List;

public class WeChatActivity extends AppCompatActivity {

    private TextView accessbilityStutas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_we_chat);
        accessbilityStutas =  (TextView)findViewById(R.id.text_accessibility_status);
        if(isAccessibilityEnable()){
            accessbilityStutas.setText("AccessibilityService已开启");
            Toast.makeText(this,"AccessibilityService已开启",Toast.LENGTH_LONG).show();
        }else{
            accessbilityStutas.setText("AccessibilityService未开启");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_we_chat, menu);
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
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean isAccessibilityEnable() {

        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> accessibilityServiceList = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        if (accessibilityServiceList != null) {

            for (AccessibilityServiceInfo serviceInfo : accessibilityServiceList) {
                if (serviceInfo.getId().contains("com.lixiao.wechat")) {
                    return true;
                }
            }
        }
        return false;
    }
}
