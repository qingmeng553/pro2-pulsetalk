<template>
  <div class="page-container profile-user">
    <!-- 用户信息卡 -->
    <div class="u-card" v-loading="loading">
      <template v-if="profile">
        <el-avatar :size="84" :src="profile.avatarUrl" class="big-avatar">
          {{ (profile.nickname || '?').slice(0, 1) }}
        </el-avatar>
        <h2 class="u-name">
          {{ profile.nickname }}
          <AdminBadge v-if="profile.role === 'ADMIN'" />
        </h2>
        <div class="u-meta">
          <span>加入于 {{ formatTime(profile.createTime) }}</span>
          <span>·</span>
          <span>帖子 {{ profile.postCount }}</span>
        </div>

        <!-- 关系操作区 -->
        <div class="u-actions">
          <!-- 游客：仅登录引导 -->
          <template v-if="!isLoggedIn">
            <el-button type="primary" round @click="openLoginDialog()">登录后可互动</el-button>
          </template>

          <template v-else>
            <!-- 自己 -->
            <el-button v-if="profile.relation === 'SELF'" round @click="$router.push('/profile')">
              <el-icon><Setting /></el-icon>&nbsp;去我的个人中心
            </el-button>

            <!-- 好友 -->
            <el-button
              v-if="profile.relation === 'FRIEND'"
              type="primary"
              round
              @click="$router.push(`/chat/${profile.userId}`)"
            >
              <el-icon><ChatDotRound /></el-icon>&nbsp;发私信
            </el-button>
            <el-button v-if="profile.relation === 'FRIEND'" plain round @click="removeFriend">
              删除好友
            </el-button>

            <!-- 我发起了申请 -->
            <MiniTag v-if="profile.relation === 'PENDING_SENT'" icon="⏳" text="已发送好友申请，等待对方通过" tone="warn" />
            <el-button
              v-if="profile.relation === 'PENDING_SENT'"
              plain
              round
              @click="$router.push(`/chat/${profile.userId}`)"
            >
              私信TA
            </el-button>

            <!-- 对方申请了我 -->
            <template v-if="profile.relation === 'PENDING_RECEIVED'">
              <el-button type="primary" round @click="acceptFriend">通过好友申请</el-button>
              <el-button plain round @click="rejectFriend">拒绝</el-button>
              <el-button plain round @click="$router.push(`/chat/${profile.userId}`)">私信TA</el-button>
            </template>

            <!-- 无关系 -->
            <template v-if="profile.relation === 'NONE'">
              <el-button type="primary" round @click="requestFriend">＋ 加好友</el-button>
              <el-button plain round @click="$router.push(`/chat/${profile.userId}`)">发私信</el-button>
            </template>

            <!-- 拉黑(双向阻断) -->
            <template v-if="profile.relation === 'BLACKED'">
              <MiniTag icon="🚫" text="你们处于拉黑状态（双向阻断互动）" tone="danger" />
              <el-button plain round @click="unblock">解除拉黑</el-button>
            </template>

            <!-- 拉黑入口(好友/无关系/申请中时均可) -->
            <el-button
              v-if="['FRIEND', 'NONE', 'PENDING_SENT'].includes(profile.relation)"
              plain
              round
              class="block-btn"
              @click="block"
            >
              拉黑
            </el-button>
          </template>
        </div>
      </template>
    </div>

    <!-- TA 的帖子 -->
    <div class="u-posts" v-if="profile">
      <div class="section-title">{{ profile.nickname }} 的帖子</div>
      <div v-if="userPosts.length" class="post-grid">
        <PostCard v-for="p in userPosts" :key="p.id" :post="p" />
      </div>
      <div v-else class="empty-tip">TA 还没有发布过帖子</div>
      <div v-if="userPosts.length && userPosts.length < userTotal" class="load-more">
        <el-button :loading="postsLoading" round @click="loadPosts">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminBadge from '../components/AdminBadge.vue'
import PostCard from '../components/PostCard.vue'
import MiniTag from '../components/MiniTag.vue'
import { apiUserProfile, apiUserPosts, apiFriendRequest, apiFriendAccept, apiFriendReject, apiFriendRemove, apiBlacklistAdd, apiBlacklistRemove } from '../api/relation'
import { isLoggedIn, openLoginDialog } from '../store/user'
import { formatTime } from '../utils/format'

const route = useRoute()
const targetId = () => route.params.id

const profile = ref(null)
const loading = ref(false)
const userPosts = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const postsLoading = ref(false)

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await apiUserProfile(targetId())
    userPage.value = 1
    userPosts.value = []
    await loadPosts(true)
  } finally {
    loading.value = false
  }
}

async function loadPosts(reset = false) {
  postsLoading.value = true
  try {
    const data = await apiUserPosts(targetId(), {
      page: reset ? 1 : userPage.value,
      size: 8
    })
    userTotal.value = Number(data.total) || 0
    userPosts.value = reset ? data.records : [...userPosts.value, ...data.records]
    if (!reset) userPage.value += 1
  } finally {
    postsLoading.value = false
  }
}

async function act(fn, okMsg, confirmMsg) {
  if (!isLoggedIn.value) {
    openLoginDialog()
    return
  }
  if (confirmMsg) {
    try {
      await ElMessageBox.confirm(confirmMsg, '提示', { type: 'warning' })
    } catch (e) {
      return
    }
  }
  await fn()
  ElMessage.success(okMsg)
  loadProfile()
}

const requestFriend = () => act(() => apiFriendRequest(targetId()), '好友申请已发送，等待对方通过')
const acceptFriend = () => act(() => apiFriendAccept(targetId()), '已通过申请，你们现在是好友啦')
const rejectFriend = () => act(() => apiFriendReject(targetId()), '已拒绝该申请')
const removeFriend = () => act(() => apiFriendRemove(targetId()), '已删除好友', '确定删除这位好友吗？')
const block = () => act(() => apiBlacklistAdd(targetId()), '已拉黑（双向阻断互动，好友关系已解除）', '拉黑后将无法互发私信且自动解除好友，确定吗？')
const unblock = () => act(() => apiBlacklistRemove(targetId()), '已解除拉黑')

onMounted(loadProfile)
</script>

<style scoped>
.profile-user {
  max-width: 900px;
}
.u-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 30px;
  text-align: center;
}
.big-avatar {
  border: 3px solid #fff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
}
.u-name {
  margin: 12px 0 4px;
  font-size: 22px;
}
.u-meta {
  color: var(--text-sub);
  font-size: 13px;
  margin-bottom: 18px;
}
.u-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.block-btn {
  color: var(--danger);
  border-color: #f3baba;
}
.u-posts {
  margin-top: 22px;
}
.post-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.load-more {
  text-align: center;
  margin-top: 20px;
}
</style>
