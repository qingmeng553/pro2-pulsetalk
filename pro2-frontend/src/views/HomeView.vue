<template>
  <div class="home-shell">
    <!-- 左栏：首页/我的 + 分类 + 大家都在看（移动端收进抽屉） -->
    <aside class="home-left">
      <HomeSidebar />
    </aside>

    <!-- 中栏：信息流 -->
    <main class="home-main">
      <!-- 置顶规则帖 -->
      <div v-if="pinnedPost && !keyword && !homeState.activeCategory" class="pin-banner" @click="goPinned">
        <span class="pin-flag">置顶</span>
        <div class="pin-body">
          <span class="pin-title">{{ pinnedPost.title }}</span>
          <span class="pin-desc">社区规则 · 点击查看</span>
        </div>
      </div>

      <!-- 信息流标题 -->
      <div class="feed-head">
        <h1 class="feed-title">{{ feedTitle }}</h1>
        <span v-if="total > 0" class="feed-count">{{ total }} 条</span>
        <button v-if="keyword" class="feed-clear" @click="clearSearch">清除搜索</button>
      </div>

      <!-- 列表 -->
      <section class="feed-card">
        <el-skeleton v-if="loading && !posts.length" class="feed-skeleton" :rows="6" animated />

        <template v-else-if="posts.length">
          <PostRow
            v-for="p in posts"
            :key="p.id"
            :post="p"
            :admin-mode="isAdmin"
            @deleted="onAdminDeleted"
          />
        </template>

        <el-empty
          v-else-if="!loading && !error"
          :description="emptyText"
          :image-size="72"
        />

        <div v-else-if="error" class="feed-error">
          <span>加载失败，请稍后再试</span>
          <el-button link type="primary" @click="fetchPosts(true)">重试</el-button>
        </div>
      </section>

      <!-- 加载更多（滚动到底自动触发，按钮兜底） -->
      <div v-if="posts.length && hasMore" class="feed-more">
        <el-button text type="primary" :loading="loading" @click="loadMore">
          {{ loading ? '加载中' : '加载更多' }}
        </el-button>
      </div>
      <p v-else-if="posts.length" class="feed-end">— 到底了 —</p>
    </main>

    <!-- 右栏：登录/用户卡片 + 活动位 -->
    <aside class="home-rail">
      <HomeRail />
    </aside>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PostRow from '../components/PostRow.vue'
import HomeSidebar from '../components/HomeSidebar.vue'
import HomeRail from '../components/HomeRail.vue'
import { apiPostList, apiPinnedPost } from '../api/post'
import { authState } from '../store/user'
import { homeState, ensureHomeData } from '../store/home'

const route = useRoute()
const router = useRouter()

const posts = ref([])
const page = ref(1)
const pageSize = 15
const total = ref(0)
const loading = ref(false)
const error = ref(false)
const pinnedPost = ref(null)

// 关键词来自地址栏 ?q=（顶栏搜索框只负责跳转）
const keyword = computed(() => String(route.query.q || '').trim())

const isAdmin = computed(() => authState.user?.role === 'ADMIN')
const hasMore = computed(() => posts.value.length < total.value)

const feedTitle = computed(() => {
  if (keyword.value) return `搜索「${keyword.value}」`
  if (homeState.activeCategory) {
    const cat = homeState.categories.find((c) => String(c.id) === String(homeState.activeCategory))
    return cat ? cat.name : '分类帖子'
  }
  return '最新帖子'
})

const emptyText = computed(() => {
  if (keyword.value) return '没有找到相关帖子，换个词试试'
  if (homeState.activeCategory) return '这个分类还没有帖子'
  return '还没有帖子，发一条试试'
})

// 请求序号：切换分类/搜索时丢弃过期响应
let reqId = 0

async function fetchPosts(reset = false) {
  const id = ++reqId
  loading.value = true
  error.value = false
  try {
    const data = await apiPostList({
      page: reset ? 1 : page.value,
      size: pageSize,
      categoryId: homeState.activeCategory || undefined,
      keyword: keyword.value || undefined
    })
    if (id !== reqId) return
    total.value = Number(data.total) || 0
    if (reset) {
      posts.value = data.records || []
      page.value = 1
    } else {
      posts.value = [...posts.value, ...(data.records || [])]
    }
  } catch (e) {
    if (id === reqId) error.value = true
  } finally {
    if (id === reqId) loading.value = false
  }
}

async function loadMore() {
  if (loading.value || !hasMore.value) return
  page.value += 1
  await fetchPosts(false)
}

function clearSearch() {
  router.push('/')
}

/** 管理员删除后从当前列表移除，并刷新左栏热度 */
function onAdminDeleted(postId) {
  posts.value = posts.value.filter((p) => String(p.id) !== String(postId))
  total.value = Math.max(0, total.value - 1)
  ensureHomeData(true)
}

async function loadPinned() {
  try {
    const data = await apiPinnedPost()
    if (data) pinnedPost.value = data
  } catch (e) {
    /* 无置顶帖属正常 */
  }
}

function goPinned() {
  if (pinnedPost.value) router.push(`/post/${pinnedPost.value.id}`)
}

// 滚动到底自动加载
function onScroll() {
  if (!hasMore.value || loading.value) return
  const el = document.documentElement
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 320) loadMore()
}

watch([keyword, () => homeState.activeCategory], () => fetchPosts(true))

onMounted(() => {
  ensureHomeData()
  loadPinned()
  fetchPosts(true)
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.home-shell {
  display: grid;
  grid-template-columns: var(--left-w) minmax(0, 1fr) var(--right-w);
  gap: var(--gap-col);
  align-items: start;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: var(--sp-4) var(--sp-6) var(--sp-12);
}

.home-left,
.home-rail {
  position: sticky;
  top: calc(var(--header-h) + var(--sp-4));
  max-height: calc(100vh - var(--header-h) - var(--sp-8));
  overflow-y: auto;
}

.home-main {
  min-width: 0;
}

/* ---------- 置顶规则帖 ---------- */
.pin-banner {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  margin-bottom: var(--sp-3);
  padding: var(--sp-2) var(--sp-4);
  background: var(--c-warning-weak);
  border: 1px solid var(--c-warning);
  border-radius: var(--r-md);
  cursor: pointer;
}

.pin-banner:hover {
  background: var(--c-accent-weak);
}

.pin-flag {
  flex: none;
  padding: 1px 8px;
  border-radius: var(--r-pill);
  background: var(--c-warning);
  color: var(--text-inverse);
  font-size: var(--fs-xs);
  line-height: 18px;
}

.pin-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.pin-title {
  overflow: hidden;
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
  color: var(--text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pin-desc {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

/* ---------- 信息流标题 ---------- */
.feed-head {
  display: flex;
  align-items: baseline;
  gap: var(--sp-2);
  margin-bottom: var(--sp-2);
  padding: 0 var(--sp-1);
}

.feed-title {
  margin: 0;
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  color: var(--text-primary);
}

.feed-count {
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.feed-clear {
  margin-left: auto;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-link);
  font-family: inherit;
  font-size: var(--fs-xs);
  cursor: pointer;
}

/* ---------- 列表容器 ---------- */
.feed-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
  overflow: hidden;
}

.feed-skeleton {
  padding: var(--sp-4);
}

.feed-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--sp-2);
  padding: var(--sp-10) 0;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}

.feed-more {
  display: flex;
  justify-content: center;
  padding: var(--sp-4) 0 var(--sp-2);
}

.feed-end {
  margin: var(--sp-4) 0 0;
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

@media (max-width: 1279px) {
  .home-shell {
    grid-template-columns: var(--left-w) minmax(0, 1fr);
  }

  .home-rail {
    display: none;
  }
}

@media (max-width: 959px) {
  .home-shell {
    grid-template-columns: minmax(0, 1fr);
    padding: var(--sp-3) var(--sp-4) var(--sp-8);
  }

  .home-left {
    display: none;
  }
}

@media (max-width: 639px) {
  .home-shell {
    padding: var(--sp-2) var(--sp-3) var(--sp-6);
  }

  .feed-head {
    padding: 0 var(--sp-1);
  }
}
</style>
