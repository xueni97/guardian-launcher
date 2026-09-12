package com.xueni97.guardian;

import com.getcapacitor.BridgeActivity;

// 守护桌面主 Activity
// 注册自定义插件 GuardianPlugin
public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        registerPlugin(GuardianPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
