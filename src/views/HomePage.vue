<template>
  <div class="home-page safe-top safe-bottom">
    <!-- 顶部：大时钟 -->
    <div class="clock-section">
      <div class="clock-time">{{ currentTime }}</div>
      <div class="clock-date">{{ currentDate }}</div>
    </div>

    <!-- 联系人头像网格 -->
    <div class="contacts-section">
      <div class="section-title">常用联系人</div>
      <div class="contact-grid">
        <div
          v-for="contact in contacts"
          :key="contact.id"
          class="contact-item"
          @click="onContactClick(contact)"
        >
          <div class="avatar" :style="{ background: contact.avatar }">
            {{ contact.name.charAt(0) }}
          </div>
          <div class="contact-name">{{ contact.name }}</div>
        </div>
      </div>
    </div>

    <!-- 快捷应用区 -->
    <div class="apps-section">
      <div class="section-title">常用应用</div>
      <div class="app-grid">
        <div class="app-item" @click="openDouyinApp">
          <div class="app-icon douyin-icon">抖</div>
          <div class="app-name">抖音</div>
        </div>
        <div class="app-item" @click="openWeChatApp">
          <div class="app-icon wechat-icon">微</div>
          <div class="app-name">微信</div>
        </div>
        <div class="app-item" @click="openPhone">
          <div class="app-icon phone-icon">话</div>
          <div class="app-name">电话</div>
        </div>
        <div class="app-item" @click="openCamera">
          <div class="app-icon camera-icon">相</div>
          <div class="app-name">相机</div>
        </div>
      </div>
    </div>

    <!-- 底部：设置入口（小按钮，老人不易误触） -->
    <div class="bottom-bar">
      <div class="settings-entry" @click="goSettings">设置</div>
    </div>

    <!-- 拨号选择弹层 -->
    <van-action-sheet
      v-model:show="showCallSheet"
      :actions="callActions"
      :cancel-text="'取消'"
      close-on-click-action
      @select="onCallSelect"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { showToast } from 'vant'
import { loadContacts } from '../db/contacts'
import { callPhone, openWeChat, openDouyin, openApp } from '../utils/guardian'

const contacts = ref([])
const currentTime = ref('')
const currentDate = ref('')
const showCallSheet = ref(false)
const selectedContact = ref(null)

let timer = null

// 实时时钟
function updateClock() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  currentTime.value = `${hh}:${mm}`
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  currentDate.value = `${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
}

const callActions = computed(() => {
  if (!selectedContact.value) return []
  const actions = [
    { name: `拨打电话 ${selectedContact.value.phone}`, phone: selectedContact.value.phone }
  ]
  if (selectedContact.value.wxid) {
    actions.push({ name: '微信语音通话', wechat: true })
  } else {
    actions.push({ name: '打开微信', wechat: true })
  }
  return actions
})

function onContactClick(contact) {
  selectedContact.value = contact
  showCallSheet.value = true
}

async function onCallSelect(action) {
  if (action.phone) {
    await callPhone(action.phone)
  } else if (action.wechat) {
    const res = await openWeChat()
    if (!res.ok) showToast(res.msg || '未安装微信')
  }
}

async function openDouyinApp() {
  const res = await openDouyin()
  if (!res.ok) showToast('未安装抖音')
}

async function openWeChatApp() {
  const res = await openWeChat()
  if (!res.ok) showToast('未安装微信')
}

async function openPhone() {
  // 打开系统拨号盘
  await openApp('com.android.dialer')
}

async function openCamera() {
  await openApp('com.android.camera')
}

function goSettings() {
  // 跳转到设置页（设置页会验证密码）
  window.location.hash = '#/settings'
}

onMounted(() => {
  contacts.value = loadContacts()
  updateClock()
  timer = setInterval(updateClock, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #e3f2fd 0%, #f5f7fa 30%);
  padding: 0 16px 24px;
  box-sizing: border-box;
}

/* 时钟区 */
.clock-section {
  text-align: center;
  padding: 32px 0 16px;
}
.clock-time {
  font-size: 80px;
  font-weight: 700;
  color: #1a73e8;
  letter-spacing: 4px;
  line-height: 1.1;
}
.clock-date {
  font-size: 26px;
  color: #555;
  margin-top: 8px;
}

/* 区块标题 */
.section-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 24px 8px 16px;
}

/* 联系人网格 */
.contact-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 8px;
}
.contact-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}
.avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.contact-name {
  font-size: 22px;
  color: #1a1a1a;
  margin-top: 10px;
  font-weight: 600;
}

/* 应用网格 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 8px;
}
.app-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}
.app-icon {
  width: 68px;
  height: 68px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  color: #fff;
  font-weight: 700;
}
.douyin-icon { background: #000; }
.wechat-icon { background: #07c160; }
.phone-icon { background: #1a73e8; }
.camera-icon { background: #ff6b35; }
.app-name {
  font-size: 20px;
  color: #333;
  margin-top: 8px;
  font-weight: 500;
}

/* 底部设置入口 */
.bottom-bar {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}
.settings-entry {
  font-size: 20px;
  color: #999;
  padding: 12px 32px;
}
</style>
