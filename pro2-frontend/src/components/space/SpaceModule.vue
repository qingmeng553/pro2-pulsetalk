<template>
  <div class="sp-module">
    <div v-if="type !== 'bio'" class="m-head">
      <span class="m-title">{{ meta.name }}</span>
    </div>

    <!-- 简介 -->
    <template v-if="type === 'bio'">
      <MarkdownViewer v-if="intro" :content="intro" class="bio-md" />
      <p v-else class="m-empty">这个人很懒，还没写简介。</p>
    </template>

    <!-- 置顶帖子 -->
    <template v-else-if="type === 'pinned'">
      <div v-if="pinned" class="pin-card" @click="openPost(pinned.id)">
        <span class="pin-tag">置顶</span>
        <span class="pin-title">{{ pinned.title }}</span>
        <span class="pin-meta">{{ pinned.categoryName || '综合' }} · {{ fromNow(pinned.createTime) }}</span>
      </div>
      <p v-else class="m-empty">还没有置顶帖子。</p>
    </template>

    <!-- 技能 -->
    <template v-else-if="type === 'skills'">
      <div v-if="skills.length" class="skill-list">
        <span v-for="s in skills" :key="s" class="skill">{{ s }}</span>
      </div>
      <p v-else class="m-empty">还没有技能标签。</p>
    </template>

    <!-- 作品 -->
    <template v-else-if="type === 'projects'">
      <div v-if="projects.length" class="project-grid">
        <component
          :is="p.url && !preview ? 'a' : 'div'"
          v-for="(p, i) in projects"
          :key="i"
          class="project-card"
          :href="p.url || undefined"
          target="_blank"
          rel="noopener noreferrer"
        >
          <div v-if="p.cover" class="project-cover">
            <img :src="resolveAssetUrl(p.cover)" alt="" loading="lazy" />
          </div>
          <div class="project-body">
            <h4>{{ p.title }}</h4>
            <p v-if="p.desc">{{ p.desc }}</p>
          </div>
        </component>
      </div>
      <p v-else class="m-empty">还没有作品。</p>
    </template>

    <!-- 图集 -->
    <template v-else-if="type === 'gallery'">
      <div v-if="gallery.length" class="gallery-grid">
        <figure v-for="(g, i) in gallery" :key="i" class="gallery-cell">
          <el-image
            :src="resolveAssetUrl(g.url)"
            :preview-src-list="previewList"
            :initial-index="i"
            fit="cover"
            preview-teleported
          />
          <figcaption v-if="g.caption">{{ g.caption }}</figcaption>
        </figure>
      </div>
      <p v-else class="m-empty">还没有图片。</p>
    </template>

    <!-- 外链 -->
    <template v-else-if="type === 'links'">
      <div v-if="links.length" class="link-list">
        <a
          v-for="(l, i) in links"
          :key="i"
          class="link-row"
          :href="preview ? undefined : l.url"
          target="_blank"
          rel="noopener noreferrer"
          @click="preview && $event.preventDefault()"
        >
          <LinkIcon :name="l.icon" />
          <span class="link-name">{{ l.name }}</span>
          <span class="link-host">{{ hostOf(l.url) }}</span>
        </a>
      </div>
      <p v-else class="m-empty">还没有外链。</p>
    </template>

    <!-- 我的帖子 -->
    <template v-else-if="type === 'posts'">
      <div v-if="posts.length" class="post-list">
        <PostRow v-for="p in posts" :key="p.id" :post="p" />
      </div>
      <p v-else class="m-empty">还没有发布过帖子。</p>
    </template>

    <!-- 数据 -->
    <template v-else-if="type === 'stats'">
      <div class="stats-grid">
        <div class="stat-cell"><strong>{{ stats.postCount || 0 }}</strong><span>发帖</span></div>
        <div class="stat-cell"><strong>{{ stats.likeCount || 0 }}</strong><span>获赞</span></div>
        <div class="stat-cell"><strong>{{ stats.friendCount || 0 }}</strong><span>好友</span></div>
        <div class="stat-cell"><strong>{{ stats.viewCount || 0 }}</strong><span>访问</span></div>
      </div>
    </template>

    <!-- 留言板 -->
    <template v-else-if="type === 'guestbook'">
      <div v-if="!preview" class="gb-form">
        <el-input
          v-model="gbText"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
          resize="none"
          :placeholder="isLoggedIn ? '友善留言，最多 200 字' : '登录后即可留言'"
          @click="ensureLogin"
        />
        <div class="gb-actions">
          <span class="gb-tip">1 分钟 1 条</span>
          <el-button type="primary" size="small" :loading="gbSending" @click="submitGuestbook">
            留言
          </el-button>
        </div>
      </div>

      <div v-if="guestbook.length" class="gb-list">
        <div v-for="g in guestbook" :key="g.id" class="gb-item">
          <UserChip v-if="g.fromUser" :user="g.fromUser" :size="22" />
          <div class="gb-main">
            <p class="gb-content">{{ g.content }}</p>
            <div class="gb-meta">
              <span>{{ formatTime(g.createTime) }}</span>
              <el-button
                v-if="canDelete(g)"
                link
                type="danger"
                size="small"
                @click="removeGuestbook(g)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <p v-else-if="!gbLoading" class="m-empty">还没有留言，来说两句吧。</p>

      <div v-if="guestbook.length < gbTotal" class="gb-more">
        <el-button text type="primary" :loading="gbLoading" @click="loadMoreGuestbook">
          加载更多
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownViewer from '../MarkdownViewer.vue'
import PostRow from '../PostRow.vue'
import UserChip from '../UserChip.vue'
import LinkIcon from './LinkIcon.vue'
import { resolveAssetUrl } from '../../utils/asset'
import { fromNow, formatTime } from '../../utils/format'
import { MODULE_META, isDarkTheme } from '../../utils/spaceThemes'
import { apiPostDetail, apiPostList } from '../../api/post'
import { apiGuestbook, apiAddGuestbook, apiDeleteGuestbook } from '../../api/space'
import { authState, isLoggedIn, openLoginDialog } from '../../store/user'

const props = defineProps({
  module: { type: Object, required: true },
  user: { type: Object, required: true },
  stats: { type: Object, default: () => ({}) },
  preview: { type: Boolean, default: false }
})

const router = useRouter()
const type = computed(() => props.module.type)
const meta = computed(() => MODULE_META[type.value] || { name: type.value })
const data = computed(() => props.module.data || {})

const intro = computed(() => data.value.intro || '')
const skills = computed(() => (Array.isArray(data.value) ? data.value : []))
const projects = computed(() => (Array.isArray(data.value) ? data.value : []))
const gallery = computed(() => (Array.isArray(data.value) ? data.value : []))
const links = computed(() => (Array.isArray(data.value) ? data.value : []))
const previewList = computed(() => gallery.value.map((g) => resolveAssetUrl(g.url)))

const pinned = ref(null)
const posts = ref([])
const guestbook = ref([])
const gbTotal = ref(0)
const gbPage = ref(1)
const gbLoading = ref(false)
const gbSending = ref(false)
const gbText = ref('')

function openPost(id) {
  if (!props.preview) router.push(`/post/${id}`)
}

function hostOf(url) {
  try {
    return new URL(url).host
  } catch (e) {
    return ''
  }
}

async function loadPinned() {
  const postId = data.value.postId
  if (!postId) return
  try {
    pinned.value = await apiPostDetail(postId)
  } catch (e) {
    pinned.value = null
  }
}

async function loadPosts() {
  try {
    const result = await apiPostList({ userId: props.user.userId, page: 1, size: 5 })
    posts.value = result.records || []
  } catch (e) {
    posts.value = []
  }
}

async function loadGuestbook(page = 1) {
  gbLoading.value = true
  try {
    const result = await apiGuestbook(props.user.userId, { page, size: 10 })
    gbTotal.value = Number(result.total) || 0
    gbPage.value = page
    guestbook.value = page === 1 ? result.records : [...guestbook.value, ...result.records]
  } finally {
    gbLoading.value = false
  }
}

function loadMoreGuestbook() {
  if (gbLoading.value || guestbook.value.length >= gbTotal.value) return
  loadGuestbook(gbPage.value + 1)
}

function ensureLogin() {
  if (!isLoggedIn.value) {
    openLoginDialog()
  }
}

async function submitGuestbook() {
  if (!isLoggedIn.value) {
    openLoginDialog()
    return
  }
  const content = gbText.value.trim()
  if (!content) {
    ElMessage.warning('留言内容不能为空')
    return
  }
  gbSending.value = true
  try {
    await apiAddGuestbook(props.user.userId, content)
    gbText.value = ''
    await loadGuestbook(1)
  } finally {
    gbSending.value = false
  }
}

function canDelete(g) {
  if (props.preview || !authState.user) return false
  return (
    String(authState.user.id) === String(g.fromUser?.userId) ||
    String(authState.user.id) === String(props.user.userId) ||
    authState.user.role === 'ADMIN'
  )
}

async function removeGuestbook(g) {
  try {
    await ElMessageBox.confirm('删除这条留言？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await apiDeleteGuestbook(g.id)
  ElMessage.success('已删除')
  await loadGuestbook(1)
}

function init() {
  if (type.value === 'pinned') loadPinned()
  else if (type.value === 'posts') loadPosts()
  else if (type.value === 'guestbook') loadGuestbook(1)
}

watch(() => props.module.type, init)
watch(() => data.value, init, { deep: true })
onMounted(init)

defineExpose({ reload: init })
</script>

<style scoped>
.sp-module {
  height: 100%;
}

.m-head {
  display: flex;
  align-items: center;
  margin-bottom: var(--sp-3);
}

.m-title {
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
  color: var(--sp-text);
}

.m-title::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 14px;
  margin-right: 8px;
  border-radius: 999px;
  background: var(--sp-accent);
  vertical-align: -2px;
}

.m-empty {
  margin: 0;
  padding: var(--sp-6) 0;
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--sp-muted);
}

.bio-md :deep(.markdown-body) {
  color: var(--sp-text);
}

/* 置顶 */
.pin-card {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-3) var(--sp-4);
  border: 1px solid var(--sp-border);
  border-radius: var(--r-sm);
  background: var(--sp-subtle);
  cursor: pointer;
}

.pin-tag {
  flex: none;
  padding: 1px 8px;
  border-radius: var(--r-pill);
  background: var(--sp-accent);
  color: #fff;
  font-size: var(--fs-xs);
}

.pin-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--fs-sm);
  color: var(--sp-text);
}

.pin-meta {
  flex: none;
  font-size: var(--fs-xs);
  color: var(--sp-muted);
}

/* 技能 */
.skill-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.skill {
  padding: 3px 10px;
  border: 1px solid var(--sp-border);
  border-radius: var(--r-pill);
  background: var(--sp-subtle);
  color: var(--sp-text);
  font-size: var(--fs-xs);
}

/* 作品 */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--sp-3);
}

.project-card {
  display: block;
  overflow: hidden;
  border: 1px solid var(--sp-border);
  border-radius: var(--r-md);
  background: var(--sp-card);
  color: inherit;
  transition: border-color var(--dur-fast) var(--ease-out);
}

.project-card:hover {
  border-color: var(--sp-accent);
}

.project-cover {
  height: 110px;
  overflow: hidden;
  background: var(--sp-subtle);
}

.project-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.project-body {
  padding: var(--sp-3);
}

.project-body h4 {
  margin: 0 0 4px;
  font-size: var(--fs-sm);
  color: var(--sp-text);
}

.project-body p {
  margin: 0;
  font-size: var(--fs-xs);
  line-height: var(--lh-normal);
  color: var(--sp-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 图集 */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--sp-2);
}

.gallery-cell {
  margin: 0;
}

.gallery-cell :deep(.el-image) {
  width: 100%;
  aspect-ratio: 1;
  border-radius: var(--r-sm);
  overflow: hidden;
  border: 1px solid var(--sp-border);
}

.gallery-cell figcaption {
  margin-top: 4px;
  font-size: var(--fs-xs);
  color: var(--sp-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 外链 */
.link-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.link-row {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: 8px;
  border-radius: var(--r-sm);
  color: var(--sp-text);
}

.link-row:hover {
  background: var(--sp-subtle);
}

.link-name {
  font-size: var(--fs-sm);
}

.link-host {
  margin-left: auto;
  font-size: var(--fs-xs);
  color: var(--sp-muted);
}

/* 帖子 */
.post-list {
  border: 1px solid var(--sp-border);
  border-radius: var(--r-sm);
  overflow: hidden;
}

/* 数据 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--sp-2);
}

.stat-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: var(--sp-3);
  border: 1px solid var(--sp-border);
  border-radius: var(--r-sm);
  background: var(--sp-subtle);
}

.stat-cell strong {
  font-size: var(--fs-lg);
  color: var(--sp-text);
}

.stat-cell span {
  font-size: var(--fs-xs);
  color: var(--sp-muted);
}

/* 留言 */
.gb-form {
  margin-bottom: var(--sp-3);
}

.gb-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--sp-2);
}

.gb-tip {
  font-size: var(--fs-xs);
  color: var(--sp-muted);
}

.gb-list {
  display: flex;
  flex-direction: column;
}

.gb-item {
  display: flex;
  gap: var(--sp-2);
  padding: var(--sp-3) 0;
  border-top: 1px solid var(--sp-border);
}

.gb-main {
  flex: 1;
  min-width: 0;
}

.gb-content {
  margin: 0;
  font-size: var(--fs-sm);
  line-height: var(--lh-normal);
  color: var(--sp-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.gb-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  font-size: var(--fs-xs);
  color: var(--sp-muted);
}

.gb-more {
  text-align: center;
  padding-top: var(--sp-2);
}

@media (max-width: 639px) {
  .gallery-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
