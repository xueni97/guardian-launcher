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

    // ============ 打开微信指定聊天（用于微信语音/视频通话） ============
    // 微信未公开语音/视频通话 API，只能深链跳到聊天界面，由老人点一下绿色按钮
    @PluginMethod
    public void openWeChatDeepLink(PluginCall call) {
        String wxid = call.getString("wxid", "");
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage("com.tencent.mm");
            if (!wxid.isEmpty()) {
                // 尝试深链到指定聊天（部分微信版本支持）
                intent.setData(Uri.parse("weixin://dl/chat?" + wxid));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            // 深链失败，降级为打开微信主页
            try {
                Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent);
                    call.resolve();
                } else {
                    call.reject("微信未安装");
                }
            } catch (Exception e2) {
                call.reject("打开微信失败: " + e2.getMessage());
            }
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
