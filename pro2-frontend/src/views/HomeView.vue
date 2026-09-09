<template>
  <div class="page-container home">
    <!-- v2：置顶规则帖(管理员共同维护的社区规则，仅一条) -->
    <div v-if="pinnedPost" class="pin-banner" @click="goPinned">
      <div class="pin-flag">📌 置顶公告</div>
      <div class="pin-body">
        <span class="pin-title">{{ pinnedPost.title }}</span>
        <span class="pin-desc">社区规则 · 点击查看详情</span>
      </div>
    </div>

    <!-- 分类筛选 -->
    <div class="cat-bar">
      <span
        v-for="c in categories"
        :key="c.id"
        class="cat-chip"
        :class="{ active: activeCategory === c.id }"
        @click="switchCategory(c.id)"
      >
        {{ c.name }}
      </span>
    </div>

    <!-- 帖子流 -->
    <div v-loading="loading && !posts.length" class="post-grid-wrap">
      <div ref="gridRef" class="post-grid">
        <PostCard
          v-for="p in posts"
          :key="p.id"
          :post="p"
          :admin-mode="isAdmin"
          class="post-card"
          @deleted="onAdminDeleted"
        />
      </div>
      <div v-if="!loading && !posts.length" class="empty-tip">这个分类下还没有帖子，快来抢沙发吧 🛋️</div>
    </div>

    <!-- 加载更多 -->
    <div v-if="posts.length && hasMore" class="load-more">
      <el-button :loading="loading" round @click="loadMore">加载更多</el-button>
    </div>
    <div v-else-if="posts.length" class="load-more done">—— 到底啦 ——</div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import PostCard from '../components/PostCard.vue'
import { apiCategoryList, apiPostList, apiPinnedPost } from '../api/post'
import { runCardEntrance, bindHoverLift } from '../utils/d3fx'
import { authState } from '../store/user'

const router = useRouter()
const categories = ref([])
const activeCategory = ref(null) // null=全部
const posts = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const gridRef = ref()
const pinnedPost = ref(null)

// 管理员登录后帖子卡片显示管理员删除按钮
const isAdmin = computed(() => authState.user?.role === 'ADMIN')

const hasMore = () => posts.value.length < total.value

/** 管理员删除后从当前列表移除该帖 */
function onAdminDeleted(postId) {
  posts.value = posts.value.filter((p) => String(p.id) !== String(postId))
}

/** 置顶规则帖详情 */
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

async function loadCategories() {
  categories.value = await apiCategoryList()
}

async function fetchPosts(reset = false) {
  loading.value = true
  try {
    const data = await apiPostList({
      page: reset ? 1 : page.value,
      size: pageSize,
      categoryId: activeCategory.value || undefined
    })
    total.value = Number(data.total) || 0
    if (reset) {
      posts.value = data.records
      page.value = 1
    } else {
      posts.value = [...posts.value, ...data.records]
    }
    // D3 入场动画(新卡片错落淡入)
    await nextTick()
    runCardEntrance('.home .post-grid')
    bindHoverLift('.home .post-card')
  } finally {
    loading.value = false
  }
}

function switchCategory(id) {
  activeCategory.value = id === activeCategory.value ? null : id
  fetchPosts(true)
}

async function loadMore() {
  if (loading.value) return
  page.value += 1
  await fetchPosts(false)
}

onMounted(() => {
  loadCategories()
  loadPinned()
  fetchPosts(true)
})
</script>

<style scoped>
.cat-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}
.pin-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(90deg, #fff7e8, #fff);
  border: 1px solid #ffe1a8;
  border-radius: 14px;
  padding: 12px 18px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.pin-banner:hover {
  border-color: #f5b94c;
  box-shadow: 0 6px 18px rgba(245, 185, 76, 0.18);
  transform: translateY(-1px);
}
.pin-flag {
  background: #f5a623;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 10px;
  white-space: nowrap;
}
.pin-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.pin-title {
  font-weight: 700;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pin-desc {
  font-size: 12px;
  color: var(--text-sub);
}
.cat-chip {
  padding: 6px 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--border);
  color: var(--text-sub);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.cat-chip:hover {
  color: var(--brand);
  border-color: var(--brand);
}
.cat-chip.active {
  background: var(--brand-gradient);
  border-color: transparent;
  color: #fff;
  font-weight: 600;
}
.post-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.load-more {
  text-align: center;
  margin-top: 26px;
  color: var(--text-sub);
  font-size: 13px;
}
</style>
