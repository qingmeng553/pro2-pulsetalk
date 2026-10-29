<template>
  <div class="page-container profile-page">
    <!-- 封禁提示 -->
    <el-alert
      v-if="authState.user?.banned"
      title="当前账号已被封禁：你可以正常浏览，但暂时无法发帖与评论"
      type="error"
      show-icon
      :closable="false"
      class="banned-tip"
    />

    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <div class="avatar-wrap" title="点击更换头像" @click="pickAvatar">
        <el-avatar :size="92" :src="resolveAssetUrl(authState.user?.avatarUrl)" class="avatar">
          {{ (authState.user?.nickname || 'U').slice(0, 1) }}
        </el-avatar>
        <div class="avatar-mask">
          <el-icon :size="22"><Camera /></el-icon>
          <span>更换头像</span>
        </div>
      </div>
      <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" hidden @change="uploadAvatar" />

      <h2 class="name">
        {{ authState.user?.nickname }}
        <AdminBadge v-if="authState.user?.role === 'ADMIN'" />
      </h2>
      <div class="account">@{{ authState.user?.username }}</div>

      <div class="role-tag" :class="authState.user?.role === 'ADMIN' ? 'admin' : 'user'">
        {{ authState.user?.role === 'ADMIN' ? '社区管理员' : '普通用户' }}
      </div>

      <el-divider />

      <div class="actions">
        <el-button type="primary" round @click="$router.push('/post/create')">
          <el-icon><EditPen /></el-icon>&nbsp;去发帖
        </el-button>
        <el-button round @click="$router.push('/friends')">
          <el-icon><User /></el-icon>&nbsp;好友管理
        </el-button>
        <el-button round @click="$router.push('/msg')">
          <el-icon><Bell /></el-icon>&nbsp;消息中心
        </el-button>
        <el-button v-if="isAdmin" round @click="$router.push('/admin/users')">
          <el-icon><Setting /></el-icon>&nbsp;用户管理
        </el-button>
        <el-button type="danger" plain round @click="logout">
          <el-icon><SwitchButton /></el-icon>&nbsp;退出登录
        </el-button>
      </div>
    </div>

    <!-- 账号安全 -->
    <div class="section-title sec">账号安全</div>

    <!-- 1) 改名 -->
    <div class="card">
      <div class="card-title">✏️ 修改昵称</div>
      <div class="row">
        <el-input v-model="nickname" maxlength="32" show-word-limit placeholder="输入新的昵称" />
        <el-button type="primary" :loading="savingNick" @click="saveNickname">保存</el-button>
      </div>
    </div>

    <!-- 2) 修改密码 -->
    <div class="card">
      <div class="card-title">🔒 修改密码</div>
      <el-form :model="pwdForm" label-width="88px" class="card-form">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 3) 邮箱：alpha2 后唯一自助找回通道（密保入口下线） -->
    <div class="card">
      <div class="card-title">📮 邮箱</div>

      <div v-if="emailBound" class="email-row">
        <div class="email-main">
          <div class="email-line">
            <span class="email-val">{{ maskEmail(authState.user?.email) }}</span>
            <MiniTag text="已验证" tone="success" />
          </div>
          <p class="hint">可用于找回密码；换绑需要当前密码 + 新邮箱验证码，旧邮箱会收到通知。</p>
        </div>
        <el-button @click="openEmailDialog('rebind')">更换邮箱</el-button>
      </div>

      <div v-else class="email-main">
        <p class="hint">未绑定邮箱。忘记密码将无法自助找回，只能联系管理员。</p>
        <el-button type="primary" @click="openEmailDialog('bind')">绑定邮箱</el-button>
      </div>
    </div>

    <EmailBindDialog v-model="emailDialogVisible" :mode="emailMode" @success="onEmailBound" />

    <p class="page-hint">头像将上传至 MinIO 对象存储并返回可访问 URL</p>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminBadge from '../components/AdminBadge.vue'
import MiniTag from '../components/MiniTag.vue'
import EmailBindDialog from '../components/EmailBindDialog.vue'
import { resolveAssetUrl } from '../utils/asset'
import { authState, resetAuth, refreshUser, openLoginDialog } from '../store/user'
import {
  apiUploadAvatar, apiLogout, apiCurrentUser,
  apiUpdateProfile, apiUpdatePassword
} from '../api/auth'

const router = useRouter()
const fileInput = ref()
const uploading = ref(false)

const isAdmin = computed(() => authState.user?.role === 'ADMIN')
const emailBound = computed(() => !!authState.user?.email)

// 改名
const nickname = ref(authState.user?.nickname || '')
const savingNick = ref(false)

// 改密
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const savingPwd = ref(false)

// 邮箱绑定 / 换绑
const emailDialogVisible = ref(false)
const emailMode = ref('bind')

function maskEmail(email) {
  if (!email) return ''
  const at = email.indexOf('@')
  if (at <= 0) return '***'
  return `${email.charAt(0)}***${email.substring(at)}`
}

function openEmailDialog(mode) {
  emailMode.value = mode
  emailDialogVisible.value = true
}

/** 绑定/换绑成功后刷新权威用户信息（email/emailVerified） */
async function onEmailBound() {
  try {
    const fresh = await apiCurrentUser()
    refreshUser(fresh)
    nickname.value = fresh.nickname || ''
  } catch (e) {
    /* 静默 */
  }
}

function pickAvatar() {
  fileInput.value && fileInput.value.click()
}

/** 头像上传 */
async function uploadAvatar(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    await apiUploadAvatar(file)
    const fresh = await apiCurrentUser()
    refreshUser(fresh)
    ElMessage.success('头像更新成功')
  } finally {
    uploading.value = false
  }
}

/** 改名 */
async function saveNickname() {
  const name = (nickname.value || '').trim()
  if (!name) {
    ElMessage.warning('昵称不能为空')
    return
  }
  if (name === authState.user?.nickname) {
    ElMessage.info('昵称没有变化')
    return
  }
  savingNick.value = true
  try {
    const fresh = await apiUpdateProfile(name)
    refreshUser(fresh)
    ElMessage.success('昵称已更新')
  } finally {
    savingNick.value = false
  }
}

/** 修改密码 */
async function savePassword() {
  const { oldPassword, newPassword, confirmPassword } = pwdForm
  if (!oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!newPassword || newPassword.length < 6 || newPassword.length > 32) {
    ElMessage.warning('新密码长度需在6-32位之间')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPwd.value = true
  try {
    await apiUpdatePassword(oldPassword, newPassword)
    // 后端改密成功后会踢掉全部会话，前端同步退出并弹登录
    resetAuth()
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    ElMessage.success('密码已修改，请用新密码重新登录')
    openLoginDialog()
    router.push('/')
  } finally {
    savingPwd.value = false
  }
}

/** 设置密保问题已下线（alpha2 起找回只走邮箱，后端接口保留） */

function logout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await apiLogout()
      } catch (e) {
        /* 忽略 */
      }
      resetAuth()
      ElMessage.success('已退出登录')
      router.push('/')
    })
    .catch(() => {})
}

onMounted(async () => {
  // 刷新一次权威用户信息(邮箱/封禁状态实时)
  try {
    const fresh = await apiCurrentUser()
    refreshUser(fresh)
    nickname.value = fresh.nickname || ''
  } catch (e) {
    /* 静默 */
  }
})
</script>

<style scoped>
.profile-page {
  max-width: 620px;
}
.banned-tip {
  margin-bottom: 14px;
  border-radius: 12px;
}
.profile-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 30px;
  text-align: center;
}
.avatar-wrap {
  position: relative;
  width: 92px;
  height: 92px;
  margin: 0 auto;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
}
.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-wrap:hover .avatar-mask {
  opacity: 1;
}
.name {
  margin: 14px 0 2px;
  font-size: 22px;
}
.account {
  color: var(--text-sub);
  font-size: 13px;
}
.role-tag {
  display: inline-block;
  margin-top: 12px;
  padding: 3px 14px;
  border-radius: 14px;
  font-size: 13px;
}
.role-tag.admin {
  background: var(--brand-gradient);
  color: #fff;
}
.role-tag.user {
  background: #eef1f8;
  color: var(--text-sub);
}
.actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}
.sec {
  margin-top: 22px;
}
.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 18px 20px;
  margin-bottom: 14px;
}
.card-title {
  font-weight: 700;
  margin-bottom: 12px;
}
.row {
  display: flex;
  gap: 10px;
  align-items: center;
}
.email-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.email-main {
  min-width: 0;
}
.email-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.email-val {
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--text);
}
.card-form {
  max-width: 460px;
}
.done-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.done-tip {
  font-size: 12.5px;
  color: var(--text-sub);
}
.hint {
  margin: 0 0 12px;
  font-size: 12.5px;
  color: var(--text-sub);
  line-height: 1.7;
}
.page-hint {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #b2b9cc;
}

/* ---------- 移动端适配 ---------- */
@media (max-width: 640px) {
  .profile-page {
    max-width: 100%;
  }
  .profile-card {
    padding: 22px 16px;
  }
  .name {
    font-size: 19px;
  }
  .actions {
    gap: 8px;
  }
  .row {
    flex-direction: column;
    align-items: stretch;
  }
  .email-row {
    flex-direction: column;
    align-items: stretch;
  }
  .card-form {
    max-width: 100%;
  }
  .card-form :deep(.el-form-item__label) {
    width: 76px !important;
  }
}
</style>
