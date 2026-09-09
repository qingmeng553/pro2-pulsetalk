<template>
  <header class="navbar">
    <div class="navbar-inner">
      <!-- 品牌 -->
      <router-link to="/" class="brand">
        <span class="brand-logo">P</span>
        <span class="brand-text">PulseTalk</span>
      </router-link>

      <!-- 主导航 -->
      <nav class="nav">
        <router-link to="/" class="nav-item">首页</router-link>
        <router-link to="/rank" class="nav-item">热度榜</router-link>
        <!-- v2：消息中心(通知+私信)，未读数>0 才显示红标 -->
        <router-link to="/msg" class="nav-item" v-if="isLoggedIn">
          <span v-if="totalBadge() > 0">
            <el-badge :value="totalBadge()" :max="99" class="msg-badge">消息</el-badge>
          </span>
          <span v-else>消息</span>
        </router-link>
        <router-link to="/post/create" class="nav-item nav-post" v-if="isLoggedIn">发帖</router-link>
      </nav>

      <!-- 右侧用户区 -->
      <div class="user-area">
        <template v-if="isLoggedIn">
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="30" :src="authState.user?.avatarUrl" class="avatar">
                {{ (authState.user?.nickname || 'U').slice(0, 1) }}
              </el-avatar>
              <span class="nick">{{ authState.user?.nickname }}</span>
              <AdminBadge v-if="authState.user?.role === 'ADMIN'" />
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="msg">
                  <el-icon><Bell /></el-icon> 消息中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" round @click="openLoginDialog()">登录 / 注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminBadge from './AdminBadge.vue'
import { authState, isLoggedIn, openLoginDialog, resetAuth } from '../store/user'
import { badgeState, totalBadge, setBadges } from '../store/badge'
import { apiNotifyUnread } from '../api/notify'
import { apiChatUnread } from '../api/chat'
import { apiLogout } from '../api/auth'

const router = useRouter()

/** 登录后轮询刷新未读角标(15s 一次) */
let timer = null
async function refreshBadges() {
  if (!isLoggedIn.value) return
  try {
    const [n, c] = await Promise.all([apiNotifyUnread(), apiChatUnread()])
    setBadges(n?.count || 0, c?.count || 0)
  } catch (e) {
    /* 静默 */
  }
}

function onCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'msg') {
    router.push('/msg')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        try {
          await apiLogout()
        } catch (e) {
          // 即使登出接口失败也清理本地态
        }
        resetAuth()
        setBadges(0, 0)
        ElMessage.success('已退出登录')
        if (router.currentRoute.value.meta.requiresAuth) router.push('/')
      })
      .catch(() => {})
  }
}

onMounted(() => {
  refreshBadges()
  timer = setInterval(refreshBadges, 15000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}
.navbar-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 16px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 28px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text);
  font-weight: 800;
  font-size: 18px;
}
.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--brand-gradient);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
}
.nav {
  flex: 1;
  display: flex;
  gap: 6px;
  align-items: center;
}
.nav-item {
  padding: 6px 14px;
  border-radius: 18px;
  color: var(--text-sub);
  font-size: 15px;
  transition: all 0.2s;
}
.nav-item:hover {
  color: var(--brand);
  background: #eef1ff;
}
.nav-item.router-link-exact-active {
  color: var(--brand);
  background: #eef1ff;
  font-weight: 600;
}
.nav-post {
  color: var(--brand);
  border: 1px solid var(--brand);
}
.msg-badge {
  line-height: 1;
}
.user-area {
  display: flex;
  align-items: center;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--text);
  outline: none;
}
.user-chip .nick {
  font-weight: 600;
  font-size: 14px;
}
</style>
