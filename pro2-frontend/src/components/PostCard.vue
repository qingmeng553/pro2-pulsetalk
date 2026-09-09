<template>
  <!-- 帖子卡片(首页帖子流) -->
  <article class="post-card anim-item" @click="goDetail">
    <!-- 封面(可选) -->
    <div v-if="post.cover" class="cover">
      <el-image :src="post.cover" fit="cover" lazy />
    </div>

    <div class="body">
      <div class="meta-top">
        <MiniTag :icon="post.categoryName ? '🗂️' : ''" :text="post.categoryName || '综合'" tone="primary" />
        <span class="top-right">
          <el-button
            v-if="adminMode"
            type="danger"
            link
            size="small"
            :icon="Delete"
            class="admin-del"
            @click.stop="askAdminDelete"
          >
            删除
          </el-button>
          <span class="date">{{ fromNow(post.createTime) }}</span>
        </span>
      </div>

      <h3 class="title">{{ post.title }}</h3>

      <div class="footer">
        <UserChip :user="post.author" :size="24" />
        <div class="stats">
          <span class="stat"><el-icon><View /></el-icon> {{ fmtCount(post.viewCount) }}</span>
          <span class="stat"><el-icon><CaretTop /></el-icon> {{ fmtCount(post.likeCount) }}</span>
          <span class="stat"><el-icon><Star /></el-icon> {{ fmtCount(post.collectCount) }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import UserChip from './UserChip.vue'
import MiniTag from './MiniTag.vue'
import { fromNow, fmtCount } from '../utils/format'
import { apiAdminDeletePost } from '../api/post'

const props = defineProps({
  post: { type: Object, required: true },
  // 管理员模式：帖子流中显示管理员删除按钮
  adminMode: { type: Boolean, default: false }
})
const emit = defineEmits(['deleted'])
const router = useRouter()

function goDetail() {
  router.push(`/post/${props.post.id}`)
}

/** 管理员软删除任意帖子(需已登录 ADMIN，按钮仅在管理员登录时渲染) */
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
.post-card {
  background: var(--card);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.18s, transform 0.18s;
  display: flex;
  flex-direction: column;
}
.cover {
  height: 150px;
  overflow: hidden;
}
.cover :deep(.el-image) {
  width: 100%;
  height: 100%;
  transition: transform 0.4s;
}
.post-card:hover .cover :deep(.el-image) {
  transform: scale(1.05);
}
.body {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 10px;
}
.meta-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.category {
  background: #eef1ff;
  color: var(--brand);
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}
.date {
  font-size: 12px;
  color: var(--text-sub);
}
.top-right {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.admin-del {
  font-size: 12px;
}
.title {
  margin: 0;
  font-size: 16px;
  line-height: 1.5;
  font-weight: 700;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 48px;
}
.footer {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.author {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.author .nick {
  font-size: 13px;
  color: var(--text-sub);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stats {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-sub);
  font-size: 12.5px;
  white-space: nowrap;
}
.stat {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
</style>
