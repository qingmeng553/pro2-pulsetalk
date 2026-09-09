<template>
  <div class="page-container profile-page">
    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <div class="avatar-wrap" title="点击更换头像" @click="pickAvatar">
        <el-avatar :size="92" :src="authState.user?.avatarUrl" class="avatar">
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
        <el-button type="danger" plain round @click="logout">
          <el-icon><SwitchButton /></el-icon>&nbsp;退出登录
        </el-button>
      </div>

      <p class="hint">头像将上传至 MinIO 对象存储并返回可访问 URL</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminBadge from '../components/AdminBadge.vue'
import { authState, resetAuth, refreshUser } from '../store/user'
import { apiUploadAvatar, apiLogout, apiCurrentUser } from '../api/auth'

const router = useRouter()
const fileInput = ref()
const uploading = ref(false)

function pickAvatar() {
  fileInput.value && fileInput.value.click()
}

/** 头像上传(游客不可达：路由已做 requiresAuth 守卫) */
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
    const url = await apiUploadAvatar(file)
    // 刷新本地与后端一致的用户信息
    const fresh = await apiCurrentUser()
    refreshUser(fresh)
    ElMessage.success('头像更新成功')
  } finally {
    uploading.value = false
  }
}

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
</script>

<style scoped>
.profile-page {
  max-width: 560px;
}
.profile-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 34px 30px;
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
  gap: 12px;
  flex-wrap: wrap;
}
.hint {
  margin-top: 20px;
  font-size: 12px;
  color: #b2b9cc;
}
</style>
