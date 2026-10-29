<template>
  <div class="page-container profile-user">
    <!-- 用户信息卡 -->
    <div class="u-card" v-loading="loading">
      <template v-if="profile">
        <el-avatar :size="84" :src="resolveAssetUrl(profile.avatarUrl)" class="big-avatar">
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
        <div v-if="!previewMode" class="u-actions">
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

            <el-button
              v-if="['FRIEND', 'NONE', 'PENDING_SENT'].includes(profile.relation)"
              plain
              round
              class="block-btn"
              @click="block"
            >
              拉黑
            </el-button>

            <!-- 举报入口 -->
            <el-dropdown
              v-if="profile.relation !== 'SELF' && !previewMode"
              trigger="click"
              class="more-menu"
              @command="onMenu"
            >
              <el-button plain round>···</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="report">举报主页</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </template>
    </div>

    <!-- 可配置主页 -->
    <div class="u-space" v-if="profile && layout" v-loading="spaceLoading">
      <SpaceRenderer
        :layout="layout"
        :user="profile"
        :stats="space?.stats || {}"
        :hidden-modules="space?.hiddenModules || []"
        :preview="previewMode"
        hide-identity
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminBadge from '../components/AdminBadge.vue'
import MiniTag from '../components/MiniTag.vue'
import SpaceRenderer from '../components/space/SpaceRenderer.vue'
import { resolveAssetUrl } from '../utils/asset'
import { apiUserProfile, apiFriendRequest, apiFriendAccept, apiFriendReject, apiFriendRemove, apiBlacklistAdd, apiBlacklistRemove } from '../api/relation'
import { apiSpace, apiReportSpace } from '../api/space'
import { isLoggedIn, openLoginDialog } from '../store/user'
import { formatTime } from '../utils/format'

const route = useRoute()
const targetId = () => route.params.id

const profile = ref(null)
const loading = ref(false)
const space = ref(null)
const spaceLoading = ref(false)
const previewLayout = ref(null)
const previewMode = computed(() => !!route.query.preview)
const layout = computed(() =>
  previewMode.value && previewLayout.value ? previewLayout.value : space.value?.layout
)

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await apiUserProfile(targetId())
    await loadSpace()
  } finally {
    loading.value = false
  }
}

async function loadSpace() {
  spaceLoading.value = true
  try {
    space.value = await apiSpace(targetId())
  } finally {
    spaceLoading.value = false
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

async function onMenu(command) {
  if (command !== 'report') return
  if (!isLoggedIn.value) {
    openLoginDialog()
    return
  }
  let reason
  try {
    const result = await ElMessageBox.prompt('请填写举报原因（200 字内）', '举报主页', {
      inputType: 'textarea',
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputValidator: (v) => (!!v && v.trim().length > 0 && v.trim().length <= 200) || '请填写 1-200 字'
    })
    reason = result.value.trim()
  } catch (e) {
    return
  }
  await apiReportSpace(profile.value.userId, 'profile', reason)
  ElMessage.success('举报已提交')
}

function onMessage(event) {
  if (event.origin !== window.location.origin) return
  if (event.data?.type === 'space-draft' && event.data.layout) {
    previewLayout.value = event.data.layout
  }
}

onMounted(() => {
  window.addEventListener('message', onMessage)
  loadProfile()
})

onBeforeUnmount(() => {
  window.removeEventListener('message', onMessage)
})
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
.u-space {
  margin-top: 18px;
}
.more-menu {
  margin-left: 0;
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
/* ---------- 移动端适配 ---------- */
@media (max-width: 640px) {
  .profile-user {
    max-width: 100%;
  }
  .u-card {
    padding: 20px 14px;
    border-radius: 14px;
  }
  .big-avatar {
    width: 68px;
    height: 68px;
  }
  .u-name {
    font-size: 19px;
  }
  .u-actions {
    gap: 8px;
  }
  .u-actions .el-button {
    margin-left: 0;
  }
  .post-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
</style>
