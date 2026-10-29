<template>
  <header class="navbar" :class="{ scrolled }">
    <div class="navbar-inner">
      <!-- 移动端左栏抽屉入口 -->
      <button class="menu-btn" aria-label="打开菜单" @click="uiState.sidebarOpen = true">
        <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
          <path
            d="M2.5 4.5h13M2.5 9h13M2.5 13.5h13"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
          />
        </svg>
      </button>

      <!-- 品牌 -->
      <router-link to="/" class="brand">
        <span class="brand-logo">P</span>
        <span class="brand-text">PulseTalk</span>
      </router-link>

      <!-- 搜索：回车后回到首页并按关键词筛选 -->
      <div class="search">
        <el-input
          v-model="keyword"
          placeholder="搜索帖子"
          :prefix-icon="Search"
          clearable
          @keyup.enter="submitSearch"
          @clear="onClear"
        />
      </div>

      <!-- 右侧操作 -->
      <div class="actions">
        <router-link v-if="isLoggedIn" to="/post/create" class="act">
          <el-icon><EditPen /></el-icon>
          <span>发帖</span>
        </router-link>
        <router-link v-if="isLoggedIn" to="/msg" class="act">
          <el-icon><ChatDotRound /></el-icon>
          <span>消息</span>
          <i v-if="totalBadge() > 0" class="dot" />
        </router-link>
        <button class="act act-app" @click="downloadApp">
          <el-icon><Iphone /></el-icon>
          <span>App 下载</span>
        </button>

        <template v-if="isLoggedIn">
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="28" :src="authState.user?.avatarUrl" class="avatar">
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
          <el-button type="primary" round size="small" @click="openLoginDialog()">登录 / 注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, EditPen, ChatDotRound, Iphone,
  ArrowDown, User, Bell, Setting, SwitchButton
} from '@element-plus/icons-vue'
import AdminBadge from './AdminBadge.vue'
import {
  authState, isLoggedIn, openLoginDialog, resetAuth, refreshUser, needsHydrate
} from '../store/user'
import { totalBadge, setBadges } from '../store/badge'
import { uiState } from '../store/ui'
import { apiNotifyUnread } from '../api/notify'
import { apiChatUnread } from '../api/chat'
import { apiLogout, apiCurrentUser } from '../api/auth'

const router = useRouter()
const route = useRoute()

// 搜索框与地址栏 ?q= 同步
const keyword = ref(String(route.query.q || ''))
watch(
  () => route.query.q,
  (q) => {
    keyword.value = String(q || '')
  }
)

function submitSearch() {
  const q = keyword.value.trim()
  if (!q) {
    if (route.query.q) router.push('/')
    return
  }
  if (route.name === 'home' && String(route.query.q || '') === q) return
  router.push({ path: '/', query: { q } })
}

function onClear() {
  if (route.query.q) router.push('/')
}

function downloadApp() {
  ElMessage.info('App 1.2.0 内测中，下载入口敬请期待')
}

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

// 滚动时给导航加一层极浅投影
const scrolled = ref(false)
function onScroll() {
  scrolled.value = window.scrollY > 8
}

watch(
  () => authState.version,
  () => {
    hydrateUser()
    refreshBadges()
  }
)

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
  z-index: var(--z-header);
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-color);
  transition: box-shadow var(--dur-base) var(--ease-out);
}

.navbar.scrolled {
  box-shadow: var(--shadow-1);
}

.navbar-inner {
  display: flex;
  align-items: center;
  gap: var(--sp-4);
  max-width: var(--content-max);
  height: var(--header-h);
  margin: 0 auto;
  padding: 0 var(--sp-6);
}

.menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 18px;
  cursor: pointer;
}

.menu-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  flex: none;
  color: var(--text-primary);
  font-weight: var(--fw-bold);
  font-size: var(--fs-md);
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--r-sm);
  background: var(--c-primary);
  color: var(--text-inverse);
  font-size: var(--fs-sm);
}

.search {
  flex: 1;
  max-width: 480px;
}

.actions {
  display: flex;
  align-items: center;
  gap: var(--sp-1);
  margin-left: auto;
}

.act {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 34px;
  padding: 0 var(--sp-3);
  border: 0;
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: var(--fs-sm);
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease-out),
    background-color var(--dur-fast) var(--ease-out);
}

.act:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.dot {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 7px;
  height: 7px;
  border-radius: var(--r-circle);
  background: var(--c-danger);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-left: var(--sp-2);
  color: var(--text-primary);
  cursor: pointer;
  outline: none;
}

.user-chip .nick {
  font-size: var(--fs-sm);
  font-weight: var(--fw-medium);
}

@media (max-width: 959px) {
  .navbar-inner {
    gap: var(--sp-2);
    padding: 0 var(--sp-4);
  }

  .menu-btn {
    display: inline-flex;
  }

  .act {
    display: none;
  }

  .user-chip .nick,
  .user-chip :deep(.admin-badge) {
    display: none;
  }
}

@media (max-width: 639px) {
  .navbar-inner {
    padding: 0 var(--sp-3);
  }

  .brand-text {
    display: none;
  }

  .search {
    max-width: none;
  }
}
</style>
