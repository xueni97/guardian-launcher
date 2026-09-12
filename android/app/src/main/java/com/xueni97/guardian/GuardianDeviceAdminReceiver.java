package com.xueni97.guardian;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

// 设备管理员接收器
// 激活设备所有者后，可通过 DevicePolicyManager 控制应用安装、隐藏应用等
public class GuardianDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
    }
}
