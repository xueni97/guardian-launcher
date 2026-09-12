<template>
  <div class="contacts-page safe-top safe-bottom">
    <van-nav-bar
      title="联系人管理"
      left-arrow
      @click-left="$router.push('/')"
    />
    <div class="content">
      <van-button
        type="primary"
        block
        class="big-btn"
        @click="showEdit = true"
      >
        + 添加联系人
      </van-button>

      <div class="contact-list">
        <div v-for="c in contacts" :key="c.id" class="contact-row">
          <div class="row-avatar" :style="{ background: c.avatar }">
            {{ c.name.charAt(0) }}
          </div>
          <div class="row-info">
            <div class="row-name">{{ c.name }}</div>
            <div class="row-phone">{{ c.phone }}</div>
            <div class="row-relation" v-if="c.relation">{{ c.relation }}</div>
          </div>
          <div class="row-actions">
            <van-button size="small" type="primary" @click="editContact(c)">编辑</van-button>
            <van-button size="small" type="danger" @click="removeContact(c.id)">删除</van-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑弹层 -->
    <van-popup v-model:show="showEdit" position="bottom" round style="height: 80%">
      <div class="edit-form">
        <h3>{{ editing.id ? '编辑联系人' : '添加联系人' }}</h3>
        <van-cell-group inset>
          <van-field v-model="editing.name" label="姓名" placeholder="请输入姓名" />
          <van-field v-model="editing.phone" label="电话" placeholder="请输入电话号码" type="tel" />
          <van-field v-model="editing.relation" label="关系" placeholder="如：儿子、女儿" />
          <van-field v-model="editing.wxid" label="微信号" placeholder="选填，用于微信直达" />
        </van-cell-group>

        <div class="color-picker">
          <div class="color-label">头像颜色</div>
          <div class="colors">
            <div
              v-for="c in colorList"
              :key="c"
              class="color-dot"
              :style="{ background: c }"
              :class="{ active: editing.avatar === c }"
              @click="editing.avatar = c"
            />
          </div>
        </div>

        <div class="form-actions">
          <van-button block type="primary" class="big-btn" @click="saveContact">保存</van-button>
          <van-button block class="big-btn" @click="showEdit = false">取消</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { loadContacts, addContact, updateContact, deleteContact } from '../db/contacts'

const contacts = ref([])
const showEdit = ref(false)
const editing = reactive({ id: '', name: '', phone: '', relation: '', wxid: '', avatar: '#1a73e8' })

const colorList = ['#1a73e8', '#e91e63', '#43a047', '#fb8c00', '#8e24aa', '#00acc1', '#5d4037', '#546e7a']

function refresh() {
  contacts.value = loadContacts()
}

function editContact(c) {
  Object.assign(editing, c)
  showEdit.value = true
}

async function removeContact(id) {
  try {
    await showConfirmDialog({ title: '确认删除该联系人？' })
    deleteContact(id)
    refresh()
    showToast('已删除')
  } catch (e) {}
}

function saveContact() {
  if (!editing.name || !editing.phone) {
    showToast('请填写姓名和电话')
    return
  }
  if (editing.id) {
    updateContact(editing.id, { ...editing })
  } else {
    addContact({ ...editing })
  }
  showEdit.value = false
  Object.assign(editing, { id: '', name: '', phone: '', relation: '', wxid: '', avatar: '#1a73e8' })
  refresh()
  showToast('已保存')
}

onMounted(refresh)
</script>

<style scoped>
.contacts-page {
  min-height: 100vh;
  background: #f5f7fa;
}
.content {
  padding: 16px;
}
.contact-list {
  margin-top: 16px;
}
.contact-row {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  gap: 16px;
}
.row-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}
.row-info {
  flex: 1;
  min-width: 0;
}
.row-name {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}
.row-phone {
  font-size: 20px;
  color: #555;
  margin-top: 4px;
}
.row-relation {
  font-size: 16px;
  color: #999;
  margin-top: 2px;
}
.row-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.edit-form {
  padding: 24px 16px;
}
.edit-form h3 {
  font-size: 28px;
  margin: 0 0 16px;
}
.color-picker {
  padding: 16px;
}
.color-label {
  font-size: 20px;
  margin-bottom: 12px;
}
.colors {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.color-dot {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 3px solid transparent;
}
.color-dot.active {
  border-color: #1a73e8;
  transform: scale(1.1);
}
.form-actions {
  padding: 0 16px;
  margin-top: 16px;
}
</style>
