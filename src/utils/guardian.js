// 守护桌面原生能力桥接 - 通过 Capacitor 自定义插件调用原生
// 插件原生代码在 android/app/src/main/java/com/xueni97/guardian/GuardianPlugin.java

import { registerPlugin } from '@capacitor/core'

// 注册自定义插件
const GuardianPlugin = registerPlugin('GuardianPlugin', {
  web: () => import('./web').then(m => new m.GuardianWeb())
})

// 拨打电话（直接呼叫，需 CALL_PHONE 权限）
export async function callPhone(phoneNumber) {
  try {
    await GuardianPlugin.callPhone({ phoneNumber })
    return { ok: true }
  } catch (e) {
    // Web 端降级：用 tel: 链接
    window.location.href = `tel:${phoneNumber}`
    return { ok: true, fallback: true }
  }
}

// 打开微信
export async function openWeChat() {
  try {
    await GuardianPlugin.openApp({ packageName: 'com.tencent.mm' })
    return { ok: true }
  } catch (e) {
    return { ok: false, msg: '未安装微信' }
  }
}

// 打开抖音
export async function openDouyin() {
  try {
    await GuardianPlugin.openApp({ packageName: 'com.ss.android.ugc.aweme' })
    return { ok: true }
  } catch (e) {
    return { ok: false, msg: '未安装抖音' }
  }
}

// 打开任意应用
export async function openApp(packageName) {
  try {
    await GuardianPlugin.openApp({ packageName })
    return { ok: true }
  } catch (e) {
    return { ok: false, msg: '应用未安装' }
  }
}

// 检查是否为设备所有者（Device Owner）
export async function isDeviceOwner() {
  try {
    const res = await GuardianPlugin.isDeviceOwner()
    return res.isOwner
  } catch (e) {
    return false
  }
}

// 设置是否阻止安装应用（需设备所有者权限）
export async function setInstallBlocked(blocked) {
  try {
    await GuardianPlugin.setInstallBlocked({ blocked })
    return { ok: true }
  } catch (e) {
    return { ok: false, msg: e.message }
  }
}

// 隐藏/显示应用（需设备所有者权限）
export async function setAppHidden(packageName, hidden) {
  try {
    await GuardianPlugin.setAppHidden({ packageName, hidden })
    return { ok: true }
  } catch (e) {
    return { ok: false, msg: e.message }
  }
}

// 进入/退出固定屏幕模式（Kiosk，需设备所有者权限）
export async function setLockTask(locked) {
  try {
    await GuardianPlugin.setLockTask({ locked })
    return { ok: true }
  } catch (e) {
    return { ok: false, msg: e.message }
  }
}

// 返回系统桌面（临时退出守护桌面）
export async function goSystemHome() {
  try {
    await GuardianPlugin.goSystemHome()
    return { ok: true }
  } catch (e) {
    return { ok: false }
  }
}
