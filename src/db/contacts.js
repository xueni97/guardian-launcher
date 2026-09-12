// 联系人本地存储 - 用 localStorage（老人手机无需复杂数据库）
// 联系人结构: { id, name, phone, wxname(微信备注名), avatar(color), photo(base64), relation }

const STORAGE_KEY = 'guardian_contacts'

// 默认联系人（演示用，可在设置页编辑）
const DEFAULT_CONTACTS = [
  { id: '1', name: '儿子', phone: '13800138000', wxname: '', avatar: '#1a73e8', relation: '儿子' },
  { id: '2', name: '女儿', phone: '13800138001', wxname: '', avatar: '#e91e63', relation: '女儿' },
  { id: '3', name: '孙子', phone: '13800138002', wxname: '', avatar: '#43a047', relation: '孙子' }
]

export function loadContacts() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(DEFAULT_CONTACTS))
      return DEFAULT_CONTACTS
    }
    return JSON.parse(raw)
  } catch (e) {
    return DEFAULT_CONTACTS
  }
}

export function saveContacts(contacts) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(contacts))
}

export function addContact(contact) {
  const list = loadContacts()
  contact.id = Date.now().toString()
  list.push(contact)
  saveContacts(list)
  return list
}

export function updateContact(id, updates) {
  const list = loadContacts()
  const idx = list.findIndex(c => c.id === id)
  if (idx >= 0) {
    list[idx] = { ...list[idx], ...updates }
    saveContacts(list)
  }
  return list
}

export function deleteContact(id) {
  const list = loadContacts().filter(c => c.id !== id)
  saveContacts(list)
  return list
}

// 设置存储
const SETTINGS_KEY = 'guardian_settings'
const DEFAULT_SETTINGS = {
  password: '1234',          // 进入设置/联系人管理的密码
  fontScale: 'large',        // 字体大小: normal / large / xlarge
  blockInstall: true,        // 阻止安装新应用
  allowDouyin: true,         // 允许抖音
  lockMode: false,           // 固定屏幕模式（需设备所有者权限）
  displayMode: 'name'        // 桌面联系人显示模式: name(名字) / photo(大头贴照片)
}

export function loadSettings() {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY)
    if (!raw) return DEFAULT_SETTINGS
    return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) }
  } catch (e) {
    return DEFAULT_SETTINGS
  }
}

export function saveSettings(settings) {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings))
}
