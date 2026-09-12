// Web 端占位实现 - 真正实现运行在 Android 原生
export class GuardianWeb {
  async callPhone({ phoneNumber }) {
    window.location.href = `tel:${phoneNumber}`
    return {}
  }
  async openApp() {
    return { ok: false, msg: 'Web 环境不支持' }
  }
  async startWeChatCall() {
    return { ok: false, msg: 'Web 环境不支持' }
  }
  async isAccessibilityEnabled() {
    return { enabled: false }
  }
  async openAccessibilitySettings() {
    return { ok: false }
  }
  async isDeviceOwner() {
    return { isOwner: false }
  }
  async setInstallBlocked() {
    return { ok: false, msg: 'Web 环境不支持' }
  }
  async setAppHidden() {
    return { ok: false, msg: 'Web 环境不支持' }
  }
  async setLockTask() {
    return { ok: false, msg: 'Web 环境不支持' }
  }
  async goSystemHome() {
    return { ok: false }
  }
}
