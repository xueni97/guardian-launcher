<template>
  <div class="contacts-page">
    <van-nav-bar
      title="联系人管理"
      left-arrow
      fixed
      safe-area-inset-top
      placeholder
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
          <div v-if="c.photo" class="row-avatar photo">
            <img :src="c.photo" alt="" />
          </div>
          <div v-else class="row-avatar" :style="{ background: c.avatar }">
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

        <!-- 照片上传（大头贴） -->
        <div class="photo-upload">
          <div class="photo-preview" @click="triggerPhotoPick">
            <img v-if="editing.photo" :src="editing.photo" alt="头像" />
            <div v-else class="photo-placeholder">
              <span class="photo-plus">+</span>
              <span class="photo-hint">上传照片</span>
            </div>
          </div>
          <div v-if="editing.photo" class="photo-remove" @click="editing.photo = ''">删除照片</div>
          <input
            ref="photoInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="onPhotoChange"
          />
        </div>

        <van-cell-group inset>
          <van-field v-model="editing.name" label="姓名" placeholder="请输入姓名" />
          <van-field v-model="editing.phone" label="电话" placeholder="请输入电话号码" type="tel" />
          <van-field v-model="editing.relation" label="关系" placeholder="如：儿子、女儿" />
          <van-field v-model="editing.wxname" label="微信备注名" placeholder="微信里的备注或昵称，用于自动拨号" />
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
const photoInput = ref(null)
const editing = reactive({ id: '', name: '', phone: '', relation: '', wxname: '', avatar: '#1a73e8', photo: '' })

const colorList = ['#1a73e8', '#e91e63', '#43a047', '#fb8c00', '#8e24aa', '#00acc1', '#5d4037', '#546e7a']

function refresh() {
  contacts.value = loadContacts()
}

function editContact(c) {
  Object.assign(editing, c)
  showEdit.value = true
}

// ============ 照片上传：压缩裁剪成 240x240 正方形 ============
function triggerPhotoPick() {
  photoInput.value && photoInput.value.click()
}

function onPhotoChange(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  compressImage(file).then(base64 => {
    editing.photo = base64
  })
  // 清空 input，允许重复选择同一张图
  e.target.value = ''
}

function compressImage(file) {
  return new Promise(resolve => {
    const reader = new FileReader()
    reader.onload = ev => {
      const img = new Image()
      img.onload = () => {
        const size = 240
        const canvas = document.createElement('canvas')
        canvas.width = size
        canvas.height = size
        const ctx = canvas.getContext('2d')
        // 居中裁剪成正方形
        const min = Math.min(img.width, img.height)
        ctx.drawImage(
          img,
          (img.width - min) / 2,
          (img.height - min) / 2,
          min,
          min,
          0,
          0,
          size,
          size
        )
        resolve(canvas.toDataURL('image/jpeg', 0.85))
      }
      img.src = ev.target.result
    }
    reader.readAsDataURL(file)
  })
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
  Object.assign(editing, { id: '', name: '', phone: '', relation: '', wxname: '', avatar: '#1a73e8', photo: '' })
  refresh()
  showToast('已保存')
}

onMounted(refresh)
</script>

<style scoped>
.contacts-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: env(safe-area-inset-bottom);
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
  overflow: hidden;
}
.row-avatar.photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
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
  overflow-y: auto;
}
.edit-form h3 {
  font-size: 28px;
  margin: 0 0 16px;
}

/* 照片上传区 */
.photo-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16px;
}
.photo-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 3px dashed #c8ccd4;
}
.photo-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.photo-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #999;
}
.photo-plus {
  font-size: 44px;
  line-height: 1;
}
.photo-hint {
  font-size: 18px;
  margin-top: 4px;
}
.photo-remove {
  font-size: 18px;
  color: #e53935;
  margin-top: 8px;
  padding: 4px 12px;
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
