<template>
  <!-- 首页左栏：首页/我的 + 分类 + 大家都在看（桌面左栏，移动端抽屉复用） -->
  <nav class="side">
    <div class="side-group">
      <button class="side-link" :class="{ active: isHome }" @click="goHome">
        <el-icon><HomeFilled /></el-icon>
        <span>首页</span>
      </button>
      <button class="side-link" @click="goMine">
        <el-icon><User /></el-icon>
        <span>我的</span>
      </button>
    </div>

    <div class="side-group">
      <p class="side-title">分类</p>
      <div class="side-cats">
        <button
          class="side-cat"
          :class="{ active: homeState.activeCategory == null }"
          @click="pickCategory(null)"
        >
          全部
        </button>
        <button
          v-for="c in homeState.categories"
          :key="c.id"
          class="side-cat"
          :class="{ active: homeState.activeCategory === c.id }"
          @click="pickCategory(c.id)"
        >
          {{ c.name }}
        </button>
      </div>
    </div>

    <div class="side-group">
      <p class="side-title">大家都在看</p>
      <ol v-if="homeState.hot.length" class="hot-list">
        <li v-for="(item, i) in homeState.hot" :key="item.postId" @click="openPost(item.postId)">
          <span class="hot-idx" :class="{ top: i < 3 }">{{ i + 1 }}</span>
          <span class="hot-title">{{ item.title }}</span>
        </li>
      </ol>
      <p v-else class="side-empty">暂无热度数据</p>
    </div>
  </nav>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, User } from '@element-plus/icons-vue'
import { homeState, setActiveCategory, ensureHomeData } from '../store/home'
import { uiState } from '../store/ui'
import { isLoggedIn, openLoginDialog } from '../store/user'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  // 桌面左栏与移动抽屉共用；在非首页打开抽屉时也能拿到分类/热度
  ensureHomeData()
})

const isHome = computed(() => route.name === 'home' && !route.query.q)

function goHome() {
  setActiveCategory(null)
  uiState.sidebarOpen = false
  if (route.name !== 'home' || route.query.q) {
    router.push('/')
  }
}

function goMine() {
  uiState.sidebarOpen = false
  if (isLoggedIn.value) {
    router.push('/profile')
  } else {
    openLoginDialog(() => router.push('/profile'))
  }
}

function pickCategory(id) {
  setActiveCategory(id)
  uiState.sidebarOpen = false
  if (route.name !== 'home') {
    router.push('/')
  }
}

function openPost(postId) {
  uiState.sidebarOpen = false
  if (postId) router.push(`/post/${postId}`)
}
</script>

<style scoped>
.side {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
  padding: var(--sp-2);
}

.side-group + .side-group {
  margin-top: var(--sp-2);
  padding-top: var(--sp-3);
  border-top: 1px solid var(--border-color);
}

.side-link {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  width: 100%;
  height: 40px;
  padding: 0 var(--sp-3);
  border: 0;
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--text-body);
  font-family: inherit;
  font-size: var(--fs-sm);
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease-out),
    background-color var(--dur-fast) var(--ease-out);
}

.side-link:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.side-link.active {
  background: var(--c-primary-weak);
  color: var(--c-primary);
  font-weight: var(--fw-bold);
}

.side-title {
  margin: 0 0 var(--sp-1);
  padding: 0 var(--sp-3);
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.side-cats {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.side-cat {
  height: 34px;
  padding: 0 var(--sp-3);
  border: 0;
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: var(--fs-sm);
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--dur-fast) var(--ease-out),
    background-color var(--dur-fast) var(--ease-out);
}

.side-cat:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.side-cat.active {
  background: var(--c-primary-weak);
  color: var(--c-primary);
  font-weight: var(--fw-medium);
}

.hot-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.hot-list li {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-2);
  padding: 6px var(--sp-3);
  border-radius: var(--r-sm);
  color: var(--text-body);
  font-size: var(--fs-sm);
  line-height: var(--lh-tight);
  cursor: pointer;
}

.hot-list li:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.hot-idx {
  flex: none;
  width: 16px;
  color: var(--text-tertiary);
  font-size: var(--fs-xs);
  font-variant-numeric: tabular-nums;
  text-align: center;
  line-height: 1.45;
}

.hot-idx.top {
  color: var(--c-accent);
  font-weight: var(--fw-bold);
}

.hot-title {
  min-width: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.side-empty {
  margin: 0;
  padding: var(--sp-2) var(--sp-3);
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}
</style>
