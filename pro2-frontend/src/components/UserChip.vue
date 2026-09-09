<template>
  <!-- 用户小卡片：点头像/昵称即可进入对方主页(了解彼此 + 私聊/加好友/拉黑入口) -->
  <span class="user-chip" :class="{ pointer: !!user?.userId }" @click.stop="go">
    <el-avatar :size="size" :src="user?.avatarUrl" class="u-avatar">
      {{ (user?.nickname || '?').slice(0, 1) }}
    </el-avatar>
    <span v-if="showNick" class="u-nick">{{ user?.nickname }}</span>
    <AdminBadge v-if="showNick && user?.role === 'ADMIN'" />
  </span>
</template>

<script setup>
import { useRouter } from 'vue-router'
import AdminBadge from './AdminBadge.vue'

const props = defineProps({
  // 用户摘要 {userId, nickname, avatarUrl, role}
  user: { type: Object, default: null },
  size: { type: Number, default: 28 },
  showNick: { type: Boolean, default: true }
})
const router = useRouter()

function go() {
  if (!props.user || !props.user.userId) return
  router.push(`/user/${props.user.userId}`)
}
</script>

<style scoped>
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.user-chip.pointer {
  cursor: pointer;
}
.user-chip:hover .u-nick {
  color: var(--brand);
}
.u-nick {
  font-size: 13px;
  color: var(--text-sub);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.15s;
}
</style>
