<template>
  <div class="settings-page">
    <van-nav-bar
      title="设置"
      left-arrow
      fixed
      safe-area-inset-top
      placeholder
      @click-left="$router.push('/')"
    />

    <!-- 密码验证 -->
    <div v-if="!unlocked" class="password-box">
      <h3>请输入管理密码</h3>
      <van-field
        v-model="password"
        type="password"
        placeholder="请输入密码"
        center
        maxlength="6"
      />
      <van-button type="primary" block class="big-btn" @click="verifyPassword">
        确认
      </van-button>
      <p class="hint">默认密码：1234</p>
    </div>

    <!-- 设置内容 -->
    <div v-else class="settings-content">
      <!-- 联系人管理 -->
      <van-cell-group inset title="联系人">
        <van-cell
          title="管理联系人"
          is-link
          @click="$router.push('/contacts')"
        />
      </van-cell-group>

      <!-- 微信自动拨号 -->
      <van-cell-group inset title="微信自动拨号">
        <van-cell
          title="无障碍服务"
          :value="accessibilityOn ? '已开启' : '未开启'"
          is-link
          @click="goAccessibility"
        >
          <template #label>
            <span class="cell-tip">开启后，点联系人即可自动拨打微信视频/语音通话</span>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 桌面显示 -->
      <van-cell-group inset title="桌面显示">
        <van-cell title="联系人显示模式" center>
          <template #label>
            <span class="cell-tip">照片模式需先给联系人上传大头贴</span>
          </template>
          <template #right-icon>
            <van-switch
              :model-value="settings.displayMode === 'photo'"
              size="28px"
              @update:model-value="onDisplayModeChange"
            />
          </template>
          <template #value>
            {{ settings.displayMode === 'photo' ? '大头贴照片' : '名字' }}
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 守护功能 -->
      <van-cell-group inset title="守护功能">
        <van-cell title="阻止安装新应用" center>
          <template #right-icon>
            <van-switch v-model="settings.blockInstall" @change="onBlockInstallChange" />
          </template>
        </van-cell>
        <van-cell title="固定屏幕模式（Kiosk）" center>
          <template #right-icon>
            <van-switch v-model="settings.lockMode" @change="onLockModeChange" />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 设备所有者状态 -->
      <van-cell-group inset title="设备所有者权限">
        <van-cell title="当前状态" :value="isOwner ? '已激活' : '未激活'" />
        <van-cell
          v-if="!isOwner"
          title="激活设备所有者"
          label="需通过 ADB 命令激活"
          is-link
          @click="showAdbHelp = true"
        />
      </van-cell-group>

      <!-- 快捷操作 -->
      <van-cell-group inset title="快捷操作">
        <van-cell title="返回系统桌面" is-link @click="goSystemHome" />
        <van-cell title="隐藏应用商店" is-link @click="hideAppStores" />
      </van-cell-group>

      <!-- 密码修改 -->
      <van-cell-group inset title="密码">
        <van-field
          v-model="newPassword"
          label="新密码"
          type="password"
          maxlength="6"
          placeholder="6位数字"
        />
        <van-button type="primary" block class="big-btn" @click="changePassword">
          修改密码
        </van-button>
      </van-cell-group>

      <div class="about">
        <p>守护桌面 v1.0.0</p>
        <p>让老人用得安心，家人放心</p>
      </div>
    </div>

    <!-- ADB 激活帮助弹层 -->
    <van-popup v-model:show="showAdbHelp" position="bottom" round style="height: 70%">
      <div class="adb-help">
        <h3>激活设备所有者</h3>
        <p class="tip">激活后可彻底阻止应用安装、隐藏应用。激活步骤：</p>
        <ol>
          <li>手机连接电脑，打开 USB 调试</li>
          <li>电脑执行命令：</li>
        </ol>
        <div class="code-box">
          adb shell dpm set-device-owner com.xueni97.guardian/.GuardianDeviceAdminReceiver
        </div>
        <p class="warn">注意：激活设备所有者前需移除所有手机账户（如小米账号等），且无法撤销，恢复出厂设置才可解除。</p>
        <van-button block type="primary" class="big-btn" @click="showAdbHelp = false">我知道了</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { showToast } from 'vant'
import { loadSettings, saveSettings } from '../db/contacts'
import {
  isDeviceOwner,
  setInstallBlocked,
  setLockTask,
  setAppHidden,
  goSystemHome as goSystemHomeNative,
  isAccessibilityEnabled,
  openAccessibilitySettings
} from '../utils/guardian'

const unlocked = ref(false)
const password = ref('')
const newPassword = ref('')
const isOwner = ref(false)
const showAdbHelp = ref(false)
const accessibilityOn = ref(false)
const settings = reactive(loadSettings())

async function checkOwner() {
  isOwner.value = await isDeviceOwner()
  accessibilityOn.value = await isAccessibilityEnabled()
}

// 跳转系统无障碍设置开启服务
async function goAccessibility() {
  if (accessibilityOn.value) {
    showToast('无障碍服务已开启')
    return
  }
  await openAccessibilitySettings()
  showToast('请找到「守护桌面」并开启')
}

function verifyPassword() {
  if (password.value === settings.password) {
    unlocked.value = true
  } else {
    showToast('密码错误')
  }
}

// 桌面联系人显示模式切换（名字/照片）
function onDisplayModeChange(val) {
  settings.displayMode = val ? 'photo' : 'name'
  saveSettings({ ...settings })
  showToast(val ? '已切换为大头贴照片' : '已切换为名字')
}

async function onBlockInstallChange(val) {
  if (val) {
    const res = await setInstallBlocked(true)
    if (!res.ok) {
      showToast('需要先激活设备所有者权限')
      settings.blockInstall = false
      return
    }
  } else {
    await setInstallBlocked(false)
  }
  saveSettings({ ...settings })
  showToast(val ? '已开启阻止安装' : '已关闭阻止安装')
}

async function onLockModeChange(val) {
  const res = await setLockTask(val)
  if (!res.ok) {
    showToast('需要先激活设备所有者权限')
    settings.lockMode = false
    return
  }
  saveSettings({ ...settings })
}

async function hideAppStores() {
  // 隐藏常见应用商店
  const stores = [
    'com.android.vending',       // Google Play
    'com.xiaomi.market',         // 小米应用商店
    'com.huawei.appmarket',      // 华为应用市场
    'com.oppo.market',           // OPPO 软件商店
    'com.bbk.appstore',          // vivo 应用商店
    'com.heytap.market'          // realme/一加
  ]
  let success = 0
  for (const pkg of stores) {
    const res = await setAppHidden(pkg, true)
    if (res.ok) success++
  }
  showToast(`已隐藏 ${success} 个应用商店`)
}

async function goSystemHome() {
  await goSystemHomeNative()
}

function changePassword() {
  if (newPassword.value.length < 4) {
    showToast('密码至少4位')
    return
  }
  settings.password = newPassword.value
  saveSettings({ ...settings })
  newPassword.value = ''
  showToast('密码已修改')
}

onMounted(() => {
  checkOwner()
})
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: env(safe-area-inset-bottom);
}
.cell-tip {
  font-size: 16px;
  color: #999;
}
.password-box {
  padding: 48px 24px;
  text-align: center;
}
.password-box h3 {
  font-size: 28px;
  margin: 0 0 24px;
}
.hint {
  color: #999;
  font-size: 18px;
  margin-top: 16px;
}
.settings-content {
  padding-bottom: 32px;
}
.about {
  text-align: center;
  color: #999;
  font-size: 18px;
  margin-top: 40px;
}
.adb-help {
  padding: 24px;
}
.adb-help h3 {
  font-size: 26px;
  margin: 0 0 16px;
}
.adb-help .tip {
  font-size: 20px;
  line-height: 1.6;
}
.adb-help ol {
  font-size: 20px;
  line-height: 2;
}
.code-box {
  background: #1e1e1e;
  color: #4ec9b0;
  padding: 16px;
  border-radius: 8px;
  font-family: monospace;
  font-size: 16px;
  word-break: break-all;
  margin: 12px 0;
  -webkit-user-select: text;
  user-select: text;
}
.warn {
  color: #e53935;
  font-size: 18px;
  margin: 16px 0;
}
</style>
