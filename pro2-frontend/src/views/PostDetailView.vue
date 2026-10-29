<template>
  <div class="detail-shell">
    <!-- 阅读进度 -->
    <div class="read-bar" :style="{ width: readProgress + '%' }" />

    <!-- 左栏：与首页同一套导航 -->
    <aside class="detail-left">
      <HomeSidebar :active-category-id="detail?.categoryId" />
    </aside>

    <!-- 中栏：正文 -->
    <main class="detail-main">
      <el-skeleton v-if="loading" class="detail-skeleton" :rows="10" animated />

      <template v-else-if="detail">
        <button class="back-link" @click="goBack">← 返回</button>

        <!-- 标题区 -->
        <article class="article-card">
          <div class="head-meta">
            <MiniTag :text="detail.categoryName || '综合'" tone="primary" />
            <span class="meta-text">发布于 {{ formatTime(detail.createTime) }}</span>
            <span v-if="edited" class="meta-text">编辑于 {{ formatTime(detail.updateTime) }}</span>
          </div>

          <h1 class="title">{{ detail.title }}</h1>

          <div class="author-line">
            <UserChip :user="detail.author" :size="32" />
            <MiniTag v-if="isOwner" text="作者本人" tone="warn" />
            <span class="head-ops">
              <template v-if="isOwner">
                <el-button size="small" :icon="EditPen" @click="goEdit">编辑</el-button>
                <el-button size="small" type="danger" plain :icon="Delete" @click="onDeletePost">
                  删除
                </el-button>
              </template>
              <el-button
                v-if="isAdmin && !isOwner"
                size="small"
                type="danger"
                plain
                :icon="Delete"
                @click="onAdminDeletePost"
              >
                管理员删除
              </el-button>
            </span>
          </div>
        </article>

        <!-- 配图 -->
        <div v-if="detail.imgUrls && detail.imgUrls.length" class="gallery">
          <el-image
            v-for="(u, i) in detail.imgUrls"
            :key="u"
            class="gallery-img"
            :src="u"
            fit="cover"
            :preview-src-list="detail.imgUrls"
            :initial-index="i"
            preview-teleported
          />
        </div>

        <!-- Markdown 正文 -->
        <div class="content-card">
          <MarkdownViewer :content="detail.content" />
        </div>

        <!-- 相关推荐：同分类优先，不足用最新补齐 -->
        <section v-if="related.length" class="related-card">
          <div class="related-head">
            <h2 class="related-title">相关推荐</h2>
          </div>
          <PostRow v-for="p in related" :key="p.id" :post="p" />
        </section>
      </template>

      <el-empty v-else-if="!loading" description="帖子不存在或已被删除" />
    </main>

    <!-- 右栏：互动 + 评论 -->
    <aside v-if="detail" id="comments" class="detail-side">
      <section class="side-card interact-card">
        <div class="interact-grid">
          <button class="interact-btn" :class="{ active: detail.liked }" @click="onLike">
            <el-icon :size="20"><CaretTop /></el-icon>
            <span class="interact-num">{{ detail.likeCount || 0 }}</span>
            <span class="interact-label">点赞</span>
          </button>
          <button class="interact-btn collect" :class="{ active: detail.collected }" @click="onCollect">
            <el-icon :size="20"><Star /></el-icon>
            <span class="interact-num">{{ detail.collectCount || 0 }}</span>
            <span class="interact-label">收藏</span>
          </button>
        </div>
        <div class="interact-meta">
          <span><el-icon><View /></el-icon>{{ detail.viewCount || 0 }} 次浏览</span>
          <span><el-icon><ChatDotRound /></el-icon>{{ commentTotal }} 条评论</span>
        </div>
      </section>

      <section class="side-card comment-card">
        <div class="comment-head-row">
          <h3 class="side-title">评论</h3>
          <span class="side-count">{{ commentTotal }}</span>
        </div>

        <el-alert
          v-if="isBanned"
          title="当前账号已被封禁，暂时无法发表评论"
          type="error"
          show-icon
          :closable="false"
          class="banned-tip"
        />

        <div class="comment-form" :class="{ 'need-login': !isLoggedIn }" @click="focusComment">
          <el-avatar v-if="isLoggedIn" :size="24" :src="authState.user?.avatarUrl">
            {{ (authState.user?.nickname || 'U').slice(0, 1) }}
          </el-avatar>
          <el-input
            ref="commentInputRef"
            v-model="commentText"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="2000"
            :disabled="isBanned"
            :placeholder="commentPlaceholder"
            @keydown.ctrl.enter="submitComment"
          />
        </div>
        <div class="comment-form-actions">
          <span class="comment-tip">{{ isLoggedIn ? 'Ctrl + Enter 发送' : '' }}</span>
          <el-button
            type="primary"
            size="small"
            :loading="commenting"
            :disabled="isBanned"
            @click="submitComment"
          >
            发表
          </el-button>
        </div>

        <div class="comment-list">
          <div v-if="!commentTotal && !commentLoading" class="comment-empty">
            还没有评论，来抢 1 楼
          </div>

          <div v-for="c in comments" :key="c.id" class="comment-item" :id="`comment-${c.id}`">
            <UserChip :user="c.author" :size="24" />
            <div class="comment-main">
              <div class="comment-meta">
                <MiniTag :text="`${c.floor}楼`" tone="default" />
                <span class="comment-time">{{ formatTime(c.createTime) }}</span>
                <span class="comment-ops">
                  <el-button
                    v-if="c.userId && String(c.userId) === String(authState.user?.id)"
                    link
                    type="danger"
                    size="small"
                    @click="onDeleteComment(c)"
                  >
                    删除
                  </el-button>
                  <el-button
                    v-if="isAdmin && String(c.userId) !== String(authState.user?.id)"
                    link
                    type="danger"
                    size="small"
                    @click="onAdminDeleteComment(c)"
                  >
                    管理员删除
                  </el-button>
                </span>
              </div>
              <p class="comment-content">{{ c.content }}</p>
            </div>
          </div>

          <el-pagination
            v-if="commentTotal > commentPageSize"
            class="comment-pager"
            small
            layout="prev, pager, next"
            :total="commentTotal"
            :page-size="commentPageSize"
            :current-page="commentPage"
            @current-change="loadComments"
          />
        </div>
      </section>
    </aside>

    <!-- 移动端吸底操作条（详情页不显示底部 Tab） -->
    <div v-if="detail" class="mobile-actions">
      <button :class="{ active: detail.liked }" @click="onLike">
        <el-icon><CaretTop /></el-icon>
        <span>{{ detail.likeCount || 0 }}</span>
      </button>
      <button :class="{ active: detail.collected }" @click="onCollect">
        <el-icon><Star /></el-icon>
        <span>{{ detail.collectCount || 0 }}</span>
      </button>
      <button @click="scrollToComments">
        <el-icon><ChatDotRound /></el-icon>
        <span>{{ commentTotal }}</span>
      </button>
      <button @click="goBack">
        <el-icon><Back /></el-icon>
        <span>返回</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete, EditPen, CaretTop, Star, View, ChatDotRound, Back
} from '@element-plus/icons-vue'
import MarkdownViewer from '../components/MarkdownViewer.vue'
import HomeSidebar from '../components/HomeSidebar.vue'
import PostRow from '../components/PostRow.vue'
import UserChip from '../components/UserChip.vue'
import MiniTag from '../components/MiniTag.vue'
import { apiPostDetail, apiPostList, apiLikePost, apiCollectPost, apiDeletePost, apiAdminDeletePost } from '../api/post'
import { apiCommentList, apiCreateComment, apiDeleteComment, apiAdminDeleteComment } from '../api/comment'
import { authState, isLoggedIn, openLoginDialog } from '../store/user'
import { formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const postId = computed(() => route.params.id)

const detail = ref(null)
const loading = ref(true)
const edited = computed(
  () => !!detail.value?.updateTime && detail.value.updateTime !== detail.value.createTime
)

// 阅读进度
const readProgress = ref(0)
function onScroll() {
  const total = document.documentElement.scrollHeight - window.innerHeight
  readProgress.value = total > 0 ? Math.min(100, Math.max(0, (window.scrollY / total) * 100)) : 0
}

// 评论
const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentPageSize = 10
const commentLoading = ref(false)
const commentText = ref('')
const commenting = ref(false)
const commentInputRef = ref()

// 相关推荐（正文下方）：同分类优先，不足用最新补齐
const related = ref([])
let relatedReq = 0

async function loadRelated() {
  const id = ++relatedReq
  const currentId = String(postId.value)
  try {
    let list = []
    if (detail.value?.categoryId) {
      const data = await apiPostList({ page: 1, size: 6, categoryId: detail.value.categoryId })
      list = (data.records || []).filter((p) => String(p.id) !== currentId)
    }
    if (list.length < 3) {
      const latest = await apiPostList({ page: 1, size: 6 })
      const seen = new Set([currentId, ...list.map((p) => String(p.id))])
      for (const p of latest.records || []) {
        if (seen.has(String(p.id))) continue
        list.push(p)
        seen.add(String(p.id))
        if (list.length >= 4) break
      }
    }
    if (id === relatedReq) related.value = list.slice(0, 4)
  } catch (e) {
    if (id === relatedReq) related.value = []
  }
}

const isAdmin = computed(() => authState.user?.role === 'ADMIN')
const isBanned = computed(() => !!authState.user?.banned)
const commentPlaceholder = computed(() => {
  if (isBanned.value) return '账号已被封禁，暂时无法评论'
  return isLoggedIn.value ? '友善评论，理性发言' : '登录后即可发表评论'
})
const isOwner = computed(
  () =>
    detail.value?.author?.userId &&
    String(detail.value.author.userId) === String(authState.user?.id)
)

async function loadDetail() {
  loading.value = true
  related.value = []
  try {
    detail.value = await apiPostDetail(postId.value)
    loadRelated()
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

/** 点赞/取消（游客先登录，登录后自动继续） */
async function onLike() {
  if (!isLoggedIn.value) {
    openLoginDialog(() => onLike())
    return
  }
  const res = await apiLikePost(postId.value)
  detail.value.liked = res.active
  detail.value.likeCount = res.count
}

/** 收藏/取消 */
async function onCollect() {
  if (!isLoggedIn.value) {
    openLoginDialog(() => onCollect())
    return
  }
  const res = await apiCollectPost(postId.value)
  detail.value.collected = res.active
  detail.value.collectCount = res.count
}

/** 游客点击评论框 → 登录，登录后聚焦输入框 */
function focusComment() {
  if (!isLoggedIn.value) {
    openLoginDialog(() => commentInputRef.value?.focus())
  }
}

/** 发表评论 */
async function submitComment() {
  if (!isLoggedIn.value) {
    openLoginDialog()
    return
  }
  if (isBanned.value) {
    ElMessage.warning('账号已被封禁，暂时无法评论')
    return
  }
  const content = commentText.value.trim()
  if (!content) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  commenting.value = true
  try {
    await apiCreateComment(postId.value, { content })
    ElMessage.success('已发表')
    commentText.value = ''
    commentPage.value = 1
    await loadComments(1)
  } finally {
    commenting.value = false
  }
}

/** 评论分页列表 */
async function loadComments(page = commentPage.value) {
  commentLoading.value = true
  try {
    const data = await apiCommentList(postId.value, { page, size: commentPageSize })
    comments.value = data.records
    commentTotal.value = Number(data.total) || 0
    commentPage.value = page
    // 从消息中心“评论了你”点击跳转：自动滚动到该楼层
    const jumpId = route.query.c
    if (jumpId) {
      setTimeout(() => {
        const el = document.getElementById(`comment-${jumpId}`)
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }, 350)
    }
  } finally {
    commentLoading.value = false
  }
}

function goEdit() {
  router.push(`/post/edit/${postId.value}`)
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

function scrollToComments() {
  document.getElementById('comments')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/** 作者删除自己的帖子 */
function onDeletePost() {
  ElMessageBox.confirm('删除后帖子将进入回收状态(软删除)，确定删除吗？', '删除帖子', { type: 'warning' })
    .then(async () => {
      await apiDeletePost(postId.value)
      ElMessage.success('已删除')
      router.push('/')
    })
    .catch(() => {})
}

/** 管理员删除任意帖子 */
function onAdminDeletePost() {
  ElMessageBox.confirm('作为管理员将软删除该帖子，确定吗？', '管理员操作', { type: 'warning' })
    .then(async () => {
      await apiAdminDeletePost(postId.value)
      ElMessage.success('已删除')
      router.push('/')
    })
    .catch(() => {})
}

/** 删除自己的评论 */
function onDeleteComment(c) {
  ElMessageBox.confirm('删除这条评论？', '提示', { type: 'warning' })
    .then(async () => {
      await apiDeleteComment(c.id)
      ElMessage.success('已删除')
      loadComments(commentPage.value)
    })
    .catch(() => {})
}

/** 管理员删除任意评论 */
function onAdminDeleteComment(c) {
  ElMessageBox.confirm('作为管理员将删除该评论，确定吗？', '管理员操作', { type: 'warning' })
    .then(async () => {
      await apiAdminDeleteComment(c.id)
      ElMessage.success('已删除')
      loadComments(commentPage.value)
    })
    .catch(() => {})
}

watch(postId, () => {
  loadDetail()
  loadComments(1)
})

onMounted(() => {
  loadDetail()
  loadComments(1)
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.read-bar {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 150;
  height: 3px;
  background: var(--c-primary);
  border-radius: 0 3px 3px 0;
  transition: width 0.12s linear;
}

.detail-shell {
  display: grid;
  grid-template-columns: var(--left-w) minmax(0, 1fr) 360px;
  gap: var(--gap-col);
  align-items: start;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: var(--sp-4) var(--sp-6) var(--sp-12);
}

.detail-left {
  position: sticky;
  top: calc(var(--header-h) + var(--sp-4));
  max-height: calc(100vh - var(--header-h) - var(--sp-8));
  overflow-y: auto;
}

.detail-side {
  display: flex;
  flex-direction: column;
  gap: var(--sp-3);
}

/* 宽屏：互动卡吸顶，评论自然滚动 */
@media (min-width: 1280px) {
  .interact-card {
    position: sticky;
    top: calc(var(--header-h) + var(--sp-4));
    z-index: 2;
  }
}

.detail-main {
  min-width: 0;
}

.detail-skeleton {
  padding: var(--sp-5);
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
}

.back-link {
  margin-bottom: var(--sp-3);
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: var(--fs-xs);
  cursor: pointer;
}

.back-link:hover {
  color: var(--c-primary);
}

/* ---------- 标题区 ---------- */
.article-card {
  padding: var(--sp-6);
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
}

.head-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.meta-text {
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.title {
  margin: var(--sp-3) 0 var(--sp-4);
  font-size: var(--fs-xl);
  line-height: 1.35;
  font-weight: var(--fw-bold);
  color: var(--text-primary);
  word-break: break-word;
}

.author-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--sp-2);
  padding-top: var(--sp-3);
  border-top: 1px solid var(--border-color);
}

.head-ops {
  display: inline-flex;
  gap: var(--sp-2);
  margin-left: auto;
}

/* ---------- 配图 ---------- */
.gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: var(--sp-2);
  margin-top: var(--sp-4);
}

.gallery-img {
  width: 100%;
  height: 150px;
  border-radius: var(--r-sm);
}

/* ---------- 正文 ---------- */
.content-card {
  margin-top: var(--sp-4);
  padding: var(--sp-6);
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
}

/* ---------- 右栏卡片 ---------- */
.side-card {
  padding: var(--sp-4);
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
}

.interact-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--sp-2);
}

.interact-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 64px;
  border: 1px solid var(--border-color);
  border-radius: var(--r-sm);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-family: inherit;
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease-out),
    background-color var(--dur-fast) var(--ease-out),
    border-color var(--dur-fast) var(--ease-out);
}

.interact-btn:hover {
  border-color: var(--c-primary-border);
  color: var(--c-primary);
}

.interact-num {
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.interact-label {
  font-size: var(--fs-xs);
}

.interact-btn.active {
  background: var(--c-like-weak);
  border-color: var(--c-like);
  color: var(--c-like);
}

.interact-btn.collect.active {
  background: var(--c-warning-weak);
  border-color: var(--c-accent);
  color: var(--c-warning);
}

.interact-meta {
  display: flex;
  align-items: center;
  gap: var(--sp-4);
  margin-top: var(--sp-3);
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.interact-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ---------- 评论 ---------- */
.comment-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.comment-head-row {
  display: flex;
  align-items: baseline;
  gap: var(--sp-2);
  margin-bottom: var(--sp-3);
}

.side-title {
  margin: 0;
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
  color: var(--text-primary);
}

.side-count {
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.banned-tip {
  margin-bottom: var(--sp-3);
}

.comment-form {
  display: flex;
  gap: var(--sp-2);
  align-items: flex-start;
}

.comment-form :deep(.el-textarea__inner) {
  font-family: inherit;
  font-size: var(--fs-sm);
  line-height: var(--lh-normal);
}

.comment-form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--sp-2);
  margin-bottom: var(--sp-3);
}

.comment-tip {
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.comment-list {
  border-top: 1px solid var(--border-color);
}

.comment-empty {
  padding: var(--sp-6) 0;
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.comment-item {
  display: flex;
  gap: var(--sp-2);
  padding: var(--sp-3) 0;
  border-bottom: 1px solid var(--border-color);
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.comment-time {
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.comment-ops {
  margin-left: auto;
}

.comment-content {
  margin: var(--sp-1) 0 0;
  font-size: var(--fs-sm);
  line-height: var(--lh-normal);
  color: var(--text-body);
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-pager {
  justify-content: center;
  margin-top: var(--sp-3);
}

/* ---------- 相关推荐 ---------- */
.related-card {
  margin-top: var(--sp-4);
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
  overflow: hidden;
}

.related-head {
  padding: var(--sp-3) var(--sp-4);
  border-bottom: 1px solid var(--border-color);
}

.related-title {
  margin: 0;
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
  color: var(--text-primary);
}

/* ---------- 移动端吸底操作条 ---------- */
.mobile-actions {
  display: none;
}

@media (max-width: 1279px) {
  .detail-shell {
    grid-template-columns: var(--left-w) minmax(0, 1fr);
  }

  .detail-side {
    grid-column: 2;
  }
}

@media (max-width: 959px) {
  .detail-shell {
    grid-template-columns: minmax(0, 1fr);
    padding: var(--sp-3) var(--sp-4) calc(72px + env(safe-area-inset-bottom));
  }

  .detail-left {
    display: none;
  }

  .detail-side {
    grid-column: auto;
  }

  .article-card,
  .content-card {
    padding: var(--sp-4);
  }

  .title {
    font-size: var(--fs-lg);
  }

  .gallery {
    grid-template-columns: repeat(2, 1fr);
  }

  .gallery-img {
    height: 110px;
  }

  .mobile-actions {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 60;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    height: calc(56px + env(safe-area-inset-bottom));
    padding-bottom: env(safe-area-inset-bottom);
    background: var(--bg-elevated);
    border-top: 1px solid var(--border-color);
  }

  .mobile-actions button {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    border: 0;
    background: transparent;
    color: var(--text-tertiary);
    font-family: inherit;
    font-size: var(--fs-xs);
    cursor: pointer;
  }

  .mobile-actions button .el-icon {
    font-size: 20px;
  }

  .mobile-actions button.active {
    color: var(--c-like);
  }

  .mobile-actions button:nth-child(2).active {
    color: var(--c-warning);
  }
}
</style>
