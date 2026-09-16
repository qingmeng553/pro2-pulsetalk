<template>
  <header class="navbar" :class="{ scrolled }">
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
        <!-- 消息中心(通知+私信)：有未读只显示红点，不显示数字 -->
        <router-link
          to="/msg"
          class="nav-item msg-item"
          v-if="isLoggedIn"
          :title="`通知 ${badgeState.notify || 0} 条 · 私信 ${badgeState.chat || 0} 条（悬停查看明细）`"
        >
          消息
          <span v-if="totalBadge() > 0" class="nav-dot" />
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
                <!-- v3：仅管理员可见的用户管理入口 -->
                <el-dropdown-item v-if="authState.user?.role === 'ADMIN'" command="adminUsers">
                  <el-icon><Setting /></el-icon> 用户管理
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
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminBadge from './AdminBadge.vue'
import {
  authState, isLoggedIn, openLoginDialog, resetAuth, refreshUser, needsHydrate
} from '../store/user'
import { badgeState, totalBadge, setBadges } from '../store/badge'
import { apiNotifyUnread } from '../api/notify'
import { apiChatUnread } from '../api/chat'
import { apiLogout, apiCurrentUser } from '../api/auth'

const router = useRouter()

// 有 token 但缺用户信息时补拉一次，避免"已登录却显示未登录"
async function hydrateUser() {
  if (!needsHydrate()) return
  try {
    refreshUser(await apiCurrentUser())
  } catch (e) {
    /* 令牌失效交给拦截器处理 */
  }
}

// 未读角标轮询（15s）
let timer = null
async function refreshBadges() {
  if (!isLoggedIn.value) {
    setBadges(0, 0)
    return
  }
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
  } else if (cmd === 'adminUsers') {
    router.push('/admin/users')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        try {
          await apiLogout()
        } catch (e) {
          /* 忽略 */
        }
        resetAuth()
        setBadges(0, 0)
        ElMessage.success('已退出登录')
        if (router.currentRoute.value.meta.requiresAuth) router.push('/')
      })
      .catch(() => {})
  }
}

// 滚动时给导航加投影
const scrolled = ref(false)
function onScroll() {
  scrolled.value = window.scrollY > 8
}

// 登录状态变化后立即刷新角标
watch(() => authState.version, () => {
  hydrateUser()
  refreshBadges()
})

onMounted(() => {
  hydrateUser()
  refreshBadges()
  timer = setInterval(refreshBadges, 15000)
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('scroll', onScroll)
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
  transition: box-shadow 0.25s ease;
}
.navbar.scrolled {
  box-shadow: 0 6px 20px rgba(40, 60, 120, 0.08);
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
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.brand:hover .brand-logo {
  transform: rotate(-8deg) scale(1.08);
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
/* 未读小红点(有未读才显示) */
.msg-item {
  position: relative;
}
.nav-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f56c6c;
  margin-left: 5px;
  vertical-align: 2px;
  box-shadow: 0 0 0 2px #fff;
  animation: dotPulse 1.8s ease-in-out infinite;
}
@keyframes dotPulse {
  0%, 100% { box-shadow: 0 0 0 2px #fff; }
  50% { box-shadow: 0 0 0 4px rgba(245, 108, 108, 0.28); }
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
/* ---------- 移动端适配 ---------- */
@media (max-width: 640px) {
  .navbar-inner {
    gap: 10px;
    padding: 0 10px;
    height: 54px;
  }
  .brand {
    font-size: 16px;
  }
  .brand-logo {
    width: 28px;
    height: 28px;
    font-size: 15px;
  }
  .nav {
    overflow-x: auto;
    flex-wrap: nowrap;
    -webkit-overflow-scrolling: touch;
  }
  .nav::-webkit-scrollbar {
    display: none;
  }
  .nav-item {
    padding: 6px 10px;
    font-size: 14px;
    white-space: nowrap;
  }
  .user-chip .nick {
    display: none;
  }
}
@media (max-width: 400px) {
  .brand-text {
    display: none;
  }
}
</style>
