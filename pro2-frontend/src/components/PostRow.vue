<template>
  <!-- 首页信息流行（紧凑，约 76px）：标题 + 分类/作者/时间/计数 + 可选缩略图 -->
  <article
    class="post-row"
    role="link"
    tabindex="0"
    @click="goDetail"
    @keydown.enter.prevent="goDetail"
    @keydown.space.prevent="goDetail"
  >
    <div class="row-body">
      <h3 class="row-title">{{ post.title }}</h3>
      <div class="row-meta">
        <MiniTag v-if="post.categoryName" :text="post.categoryName" tone="primary" />
        <UserChip v-if="post.author" :user="post.author" :size="20" />
        <span class="row-time">{{ fromNow(post.createTime) }}</span>
        <span class="row-stats">
          <span class="stat"><el-icon><View /></el-icon>{{ fmtCount(post.viewCount) }}</span>
          <span class="stat"><HeartIcon class="stat-icon" />{{ fmtCount(post.likeCount) }}</span>
          <span class="stat"><el-icon><Star /></el-icon>{{ fmtCount(post.collectCount) }}</span>
        </span>
        <el-button
          v-if="adminMode"
          type="danger"
          link
          size="small"
          :icon="Delete"
          class="row-del"
          @click.stop="askAdminDelete"
        >
          删除
        </el-button>
      </div>
    </div>

    <div v-if="post.cover" class="row-cover">
      <el-image :src="resolveAssetUrl(post.cover)" fit="cover" lazy />
    </div>
  </article>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Star, Delete } from '@element-plus/icons-vue'
import UserChip from './UserChip.vue'
import MiniTag from './MiniTag.vue'
import HeartIcon from './HeartIcon.vue'
import { resolveAssetUrl } from '../utils/asset'
import { fromNow, fmtCount } from '../utils/format'
import { apiAdminDeletePost } from '../api/post'

const props = defineProps({
  post: { type: Object, required: true },
  adminMode: { type: Boolean, default: false }
})
const emit = defineEmits(['deleted'])
const router = useRouter()

function goDetail() {
  router.push(`/post/${props.post.id}`)
}

/** 管理员软删除任意帖子（仅管理员登录时渲染按钮） */
async function askAdminDelete() {
  try {
    await ElMessageBox.confirm(
      `管理员将软删除帖子「${props.post.title}」，确定吗？`,
      '管理员操作',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await apiAdminDeletePost(props.post.id)
  ElMessage.success('已删除该帖')
  emit('deleted', props.post.id)
}
</script>

<style scoped>
.post-row {
  display: flex;
  align-items: center;
  gap: var(--sp-4);
  padding: 10px var(--sp-4);
  min-height: 76px;
  background: var(--bg-elevated);
  cursor: pointer;
  transition: background-color var(--dur-fast) var(--ease-out);
}

.post-row + .post-row {
  border-top: 1px solid var(--border-color);
}

.post-row:hover {
  background: var(--bg-subtle);
}

.post-row:hover .row-title {
  color: var(--c-primary);
}

.row-body {
  flex: 1;
  min-width: 0;
}

.row-title {
  margin: 0 0 6px;
  font-size: var(--fs-md);
  font-weight: var(--fw-medium);
  line-height: var(--lh-tight);
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--dur-fast) var(--ease-out);
}

.row-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--sp-2);
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.row-time {
  white-space: nowrap;
}

.row-stats {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-3);
  margin-left: var(--sp-2);
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-variant-numeric: tabular-nums;
}

.stat-icon {
  width: 12px;
  height: 12px;
  color: var(--c-like);
}

.row-del {
  margin-left: auto;
}

.row-cover {
  flex: none;
  width: 88px;
  height: 56px;
  border-radius: var(--r-sm);
  overflow: hidden;
  background: var(--bg-subtle);
}

.row-cover :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 窄屏：压缩成两行，避免元信息逐项换行把行高撑到 300px+ */
@media (max-width: 639px) {
  .post-row {
    gap: var(--sp-3);
    padding: 10px var(--sp-3);
  }

  .post-row + .post-row {
    border-top: 1px solid var(--border-color);
  }

  .row-title {
    margin-bottom: 4px;
    font-size: var(--fs-sm);
  }

  .row-meta {
    gap: 6px;
  }

  .row-meta :deep(.tag-frame),
  .row-meta :deep(.admin-badge),
  .row-del {
    display: none;
  }

  .row-meta :deep(.user-chip) {
    max-width: 55%;
  }

  .row-stats {
    margin-left: 0;
  }

  .row-stats .stat:nth-child(n + 2) {
    display: none;
  }

  .row-cover {
    width: 64px;
    height: 64px;
  }
}
</style>
