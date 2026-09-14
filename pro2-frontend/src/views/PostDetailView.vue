<template>
  <div class="page-container detail-page">
    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else-if="detail">
      <!-- 顶部信息 -->
      <div class="head">
        <div class="head-meta">
          <MiniTag icon="🗂️" :text="detail.categoryName || '综合'" tone="primary" />
          <span class="time">发布于 {{ formatTime(detail.createTime) }}</span>
          <span class="time" v-if="detail.updateTime && detail.updateTime !== detail.createTime">
            编辑于 {{ formatTime(detail.updateTime) }}
          </span>
        </div>
        <h1 class="title">{{ detail.title }}</h1>

        <div class="author-line">
          <UserChip :user="detail.author" :size="34" />
          <MiniTag v-if="isOwner" icon="✍️" text="作者本人" tone="warn" />

          <div class="head-ops">
            <!-- 作者：编辑 / 删除自己的帖子 -->
            <template v-if="isOwner">
              <el-button size="small" :icon="EditPen" @click="goEdit">编辑</el-button>
              <el-button size="small" type="danger" plain :icon="Delete" @click="onDeletePost">删除</el-button>
            </template>
            <!-- 管理员：软删除任意帖子 -->
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
          </div>
        </div>
      </div>

      <!-- 配图(多图) -->
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

      <!-- 互动操作栏 -->
      <div class="action-bar">
        <button
          class="action-btn"
          :class="{ active: detail.liked }"
          @click="onLike"
        >
          <el-icon :size="20"><CaretTop /></el-icon>
          <span>点赞 {{ detail.likeCount || 0 }}</span>
        </button>
        <button
          class="action-btn"
          :class="{ active: detail.collected }"
          @click="onCollect"
        >
          <el-icon :size="20"><Star /></el-icon>
          <span>收藏 {{ detail.collectCount || 0 }}</span>
        </button>
        <span class="action-static"><el-icon><View /></el-icon> {{ detail.viewCount || 0 }} 次浏览</span>
      </div>

      <!-- 评论 -->
      <div class="comment-section">
        <div class="section-title">全部评论（{{ commentTotal }}）</div>

        <!-- 评论输入 -->
        <el-alert
          v-if="isBanned"
          title="当前账号已被封禁，暂时无法发表评论"
          type="error"
          show-icon
          :closable="false"
          class="banned-tip"
        />
        <div class="comment-input" :class="{ 'need-login': !isLoggedIn }" @click="focusComment">
          <el-avatar :size="30" :src="authState.user?.avatarUrl" v-if="isLoggedIn">
            {{ (authState.user?.nickname || 'U').slice(0, 1) }}
          </el-avatar>
          <el-input
            ref="commentInputRef"
            v-model="commentText"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="2000"
            show-word-limit
            :disabled="isBanned"
            :placeholder="commentPlaceholder"
            @keydown.ctrl.enter="submitComment"
          />
          <el-button type="primary" :loading="commenting" :disabled="isBanned" @click="submitComment">
            发表评论
          </el-button>
        </div>

        <!-- 评论列表 -->
        <div class="comment-list">
          <div v-if="!commentTotal && !commentLoading" class="empty-tip">还没有评论，来抢 1 楼 👇</div>
          <div v-for="c in comments" :key="c.id" class="comment-item" :id="`comment-${c.id}`">
            <UserChip :user="c.author" :size="32" />
            <div class="comment-main">
              <div class="comment-head">
                <MiniTag icon="#" :text="`${c.floor}楼`" tone="default" />
                <span class="time">{{ formatTime(c.createTime) }}</span>
                <span class="ops">
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
              <div class="comment-content">{{ c.content }}</div>
            </div>
          </div>

          <!-- 分页 -->
          <el-pagination
            v-if="commentTotal > commentPageSize"
            class="pager"
            layout="prev, pager, next"
            :total="commentTotal"
            :page-size="commentPageSize"
            :current-page="commentPage"
            @current-change="loadComments"
          />
        </div>
      </div>
    </template>

    <div v-else-if="!loading" class="empty-tip">帖子不存在或已被删除 🫥</div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, EditPen } from '@element-plus/icons-vue'
import MarkdownViewer from '../components/MarkdownViewer.vue'
import AdminBadge from '../components/AdminBadge.vue'
import UserChip from '../components/UserChip.vue'
import MiniTag from '../components/MiniTag.vue'
import { apiPostDetail, apiLikePost, apiCollectPost, apiDeletePost, apiAdminDeletePost } from '../api/post'
import { apiCommentList, apiCreateComment, apiDeleteComment, apiAdminDeleteComment } from '../api/comment'
import { authState, isLoggedIn, openLoginDialog } from '../store/user'
import { formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const postId = computed(() => route.params.id)

const detail = ref(null)
const loading = ref(true)

// 评论
const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentPageSize = 10
const commentLoading = ref(false)
const commentText = ref('')
const commenting = ref(false)
const commentInputRef = ref()

const isAdmin = computed(() => authState.user?.role === 'ADMIN')
const isBanned = computed(() => !!authState.user?.banned)
const commentPlaceholder = computed(() => {
  if (isBanned.value) return '账号已被封禁，暂时无法评论'
  return isLoggedIn.value ? '友善评论，理性发言 ~' : '登录后即可发表评论'
})
const isOwner = computed(() => detail.value?.author?.userId && String(detail.value.author.userId) === String(authState.user?.id))

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await apiPostDetail(postId.value)
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

/** 点赞/取消(游客弹出登录确认) */
async function onLike() {
  if (!isLoggedIn.value) {
    openLoginDialog()
    return
  }
  const res = await apiLikePost(postId.value)
  detail.value.liked = res.active
  detail.value.likeCount = res.count
}

/** 收藏/取消 */
async function onCollect() {
  if (!isLoggedIn.value) {
    openLoginDialog()
    return
  }
  const res = await apiCollectPost(postId.value)
  detail.value.collected = res.active
  detail.value.collectCount = res.count
}

/** 游客点击评论框 → 登录 */
function focusComment() {
  if (!isLoggedIn.value) openLoginDialog()
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
    ElMessage.success('评论成功')
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
})
</script>

<style scoped>
.detail-page {
  max-width: 820px;
}
.head {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 22px 24px;
}
.head-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.category {
  background: #eef1ff;
  color: var(--brand);
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}
.time {
  font-size: 12px;
  color: var(--text-sub);
}
.title {
  margin: 12px 0 14px;
  font-size: 24px;
  line-height: 1.4;
}
.author-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nick {
  font-weight: 600;
}
.owner-tag {
  font-size: 11px;
  color: #fff;
  background: #ffb800;
  border-radius: 8px;
  padding: 1px 8px;
}
.head-ops {
  margin-left: auto;
}
.gallery {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
}
.gallery-img {
  width: 100%;
  height: 150px;
  border-radius: 12px;
}
.content-card {
  margin-top: 14px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px 26px;
}
.action-bar {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 14px 20px;
}
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: #f2f4fa;
  color: var(--text-sub);
  padding: 9px 20px;
  border-radius: 22px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}
.action-btn:hover {
  color: var(--brand);
  background: #e9edff;
}
.action-btn.active {
  background: var(--brand-gradient);
  color: #fff;
}
.action-static {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-sub);
  font-size: 13px;
}
.comment-section {
  margin-top: 20px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px 22px 26px;
}
.comment-input {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.comment-input :deep(.el-textarea__inner) {
  border-radius: 10px;
}
.comment-input .el-button {
  margin-top: 2px;
}
.comment-list {
  margin-top: 18px;
}
.comment-item {
  display: flex;
  gap: 10px;
  padding: 14px 2px;
  border-bottom: 1px dashed var(--border);
}
.comment-main {
  flex: 1;
  min-width: 0;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.floor {
  font-size: 12px;
  color: #fff;
  background: #d7ddf0;
  padding: 1px 7px;
  border-radius: 8px;
}
.nick {
  font-weight: 600;
  font-size: 14px;
}
.time {
  font-size: 12px;
  color: var(--text-sub);
}
.ops {
  margin-left: auto;
}
.comment-content {
  margin-top: 6px;
  font-size: 14.5px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}
.pager {
  margin-top: 18px;
  justify-content: center;
}
/* ---------- 移动端适配 ---------- */
@media (max-width: 640px) {
  .head {
    padding: 16px 14px;
    border-radius: 14px;
  }
  .title {
    font-size: 19px;
    margin: 10px 0 12px;
  }
  .author-line {
    flex-wrap: wrap;
    gap: 6px;
  }
  .head-ops {
    margin-left: 0;
    width: 100%;
    display: flex;
    gap: 8px;
  }
  .gallery {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }
  .gallery-img {
    height: 110px;
  }
  .content-card {
    padding: 16px 14px;
    border-radius: 14px;
  }
  .action-bar {
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px 14px;
  }
  .action-btn {
    padding: 8px 16px;
    font-size: 13.5px;
  }
  .action-static {
    margin-left: 0;
    width: 100%;
  }
  .comment-section {
    padding: 16px 14px 20px;
    border-radius: 14px;
  }
  .comment-input {
    flex-wrap: wrap;
  }
  .comment-input .el-button {
    width: 100%;
    margin-top: 8px;
  }
}
/* 封禁提示条 */
.banned-tip {
  margin-bottom: 12px;
  border-radius: 10px;
}
</style>
