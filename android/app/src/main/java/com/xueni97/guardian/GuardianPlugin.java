package com.xueni97.guardian;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.UserManager;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

// 守护桌面原生能力插件
// 提供：一键拨号、打开应用、设备所有者权限管理、阻止安装、隐藏应用、固定屏幕
@CapacitorPlugin(name = "GuardianPlugin")
public class GuardianPlugin extends Plugin {

    private ComponentName getAdminComponent() {
        return new ComponentName(getContext(), GuardianDeviceAdminReceiver.class);
    }

    private DevicePolicyManager getDpm() {
        return (DevicePolicyManager) getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    // ============ 一键拨号 ============
    @PluginMethod
    public void callPhone(PluginCall call) {
        String phone = call.getString("phoneNumber", "");
        if (phone.isEmpty()) {
            call.reject("电话号码为空");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            call.resolve();
        } catch (SecurityException e) {
            // 没有 CALL_PHONE 权限，降级为拨号盘
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("拨号失败: " + e.getMessage());
        }
    }

    // ============ 打开应用 ============
    @PluginMethod
    public void openApp(PluginCall call) {
        String packageName = call.getString("packageName", "");
        if (packageName.isEmpty()) {
            call.reject("包名为空");
            return;
        }
        try {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
                call.resolve();
            } else {
                call.reject("应用未安装: " + packageName);
            }
        } catch (Exception e) {
            call.reject("打开失败: " + e.getMessage());
        }
    }

    // ============ 微信自动拨号（无障碍服务方案） ============
    // 打开微信并由无障碍服务自动完成：搜索联系人 -> 点+ -> 发起视频/语音通话
    @PluginMethod
    public void startWeChatCall(PluginCall call) {
        String keyword = call.getString("keyword", "");
        String mode = call.getString("mode", "video");
        if (keyword.isEmpty()) {
            call.reject("缺少联系人微信备注名");
            return;
        }
        if (!WeChatCallService.isReady()) {
            call.reject("无障碍服务未开启，请到设置中开启");
            return;
        }
        try {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
            if (intent == null) {
                call.reject("微信未安装");
                return;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            getContext().startActivity(intent);
            // 启动无障碍状态机（等微信窗口出现后自动操作）
            WeChatCallService.startCall(keyword, "video".equals(mode));
            call.resolve();
        } catch (Exception e) {
            call.reject("拨号失败: " + e.getMessage());
        }
    }

    // ============ 检查无障碍服务是否开启 ============
    @PluginMethod
    public void isAccessibilityEnabled(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("enabled", WeChatCallService.isReady());
        call.resolve(ret);
    }

    // ============ 跳转系统无障碍设置页 ============
    @PluginMethod
    public void openAccessibilitySettings(PluginCall call) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("打开设置失败: " + e.getMessage());
        }
    }

    // ============ 检查是否为设备所有者 ============
    @PluginMethod
    public void isDeviceOwner(PluginCall call) {
        DevicePolicyManager dpm = getDpm();
        boolean isOwner = dpm.isDeviceOwnerApp(getContext().getPackageName());
        JSObject ret = new JSObject();
        ret.put("isOwner", isOwner);
        call.resolve(ret);
    }

    // ============ 设置阻止安装应用 ============
    @PluginMethod
    public void setInstallBlocked(PluginCall call) {
        boolean blocked = call.getBoolean("blocked", true);
        DevicePolicyManager dpm = getDpm();
        ComponentName admin = getAdminComponent();

        if (!dpm.isDeviceOwnerApp(getContext().getPackageName())) {
            call.reject("需要先激活设备所有者权限");
            return;
        }

        try {
            // 阻止安装未知来源
            dpm.setSecureSetting(admin, android.provider.Settings.Secure.INSTALL_NON_MARKET_APPS, blocked ? "0" : "1");

            // 阻止所有应用安装（用户限制）
            if (blocked) {
                dpm.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS);
                dpm.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
            } else {
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS);
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("设置失败: " + e.getMessage());
        }
    }

    // ============ 隐藏/显示应用 ============
    @PluginMethod
    public void setAppHidden(PluginCall call) {
        String packageName = call.getString("packageName", "");
        boolean hidden = call.getBoolean("hidden", true);
        DevicePolicyManager dpm = getDpm();
        ComponentName admin = getAdminComponent();

        if (!dpm.isDeviceOwnerApp(getContext().getPackageName())) {
            call.reject("需要先激活设备所有者权限");
            return;
        }

        try {
            dpm.setApplicationHidden(admin, packageName, hidden);
            call.resolve();
        } catch (Exception e) {
            call.reject("操作失败: " + e.getMessage());
        }
    }

    // ============ 固定屏幕模式（Kiosk） ============
    @PluginMethod
    public void setLockTask(PluginCall call) {
        boolean locked = call.getBoolean("locked", true);
        DevicePolicyManager dpm = getDpm();
        ComponentName admin = getAdminComponent();

        if (!dpm.isDeviceOwnerApp(getContext().getPackageName())) {
            call.reject("需要先激活设备所有者权限");
            return;
        }

        try {
            if (locked) {
                // 将本应用加入锁任务白名单并启动
                dpm.setLockTaskPackages(admin, new String[]{getContext().getPackageName()});
                getActivity().startLockTask();
            } else {
                getActivity().stopLockTask();
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("操作失败: " + e.getMessage());
        }
    }

    // ============ 返回系统桌面（临时退出守护桌面） ============
    @PluginMethod
    public void goSystemHome(PluginCall call) {
        try {
            // 先退出固定屏幕（如果在）
            try {
                getActivity().stopLockTask();
            } catch (Exception ignored) {}

            // 发送 HOME intent，让用户选择系统桌面
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("返回失败: " + e.getMessage());
        }
    }
}
