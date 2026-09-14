<template>
  <div class="page-container msg-page">
    <div class="section-title">
      消息中心
      <el-button size="small" text :icon="Check" v-if="tab === 'notify' && notifyUnread > 0" @click="readAllNotify">
        全部已读
      </el-button>
      <!-- 管理员：向全体用户发送官方消息 -->
      <el-button
        v-if="isAdmin"
        size="small"
        type="primary"
        plain
        :icon="Promotion"
        class="ml"
        @click="broadcastVisible = true"
      >
        发送官方消息
      </el-button>
      <el-button size="small" text :icon="Refresh" class="ml" @click="reloadAll">刷新</el-button>
    </div>

    <el-tabs v-model="tab">
      <!-- ============ 通知：谁赞了我/评论了我/收藏了我/加我好友 ============ -->
      <el-tab-pane label="通知" name="notify">
        <div v-if="notifyList.length" class="n-list">
          <div
            v-for="n in notifyList"
            :key="n.id"
            class="n-item"
            :class="{ unread: !n.read }"
            @click="openNotify(n)"
          >
            <div class="n-icon" :class="typeClass(n.type)">{{ typeIcon(n.type) }}</div>
            <UserChip :user="n.actor" :size="34" />
            <div class="n-body">
              <div class="n-text">
                <template v-if="n.type === 'LIKE'">
                  <b>{{ n.actor?.nickname }}</b> 赞了你的帖子
                  <span class="link">{{ n.postTitle }}</span>
                </template>
                <template v-else-if="n.type === 'COLLECT'">
                  <b>{{ n.actor?.nickname }}</b> 收藏了你的帖子
                  <span class="link">{{ n.postTitle }}</span>
                </template>
                <template v-else-if="n.type === 'COMMENT'">
                  <b>{{ n.actor?.nickname }}</b> 评论了你的帖子
                  <span class="link">{{ n.postTitle }}</span>
                  <div class="n-preview">“{{ n.content }}”</div>
                </template>
                <template v-else-if="n.type === 'FRIEND'">
                  <b>{{ n.actor?.nickname }}</b> {{ n.content }}
                  <span v-if="!pendingMap[n.actor?.userId]" class="done-tag">已处理</span>
                  <span v-else class="ops" @click.stop>
                    <el-button size="small" type="primary" @click="acceptReq(n.actor?.userId)">通过</el-button>
                    <el-button size="small" @click="rejectReq(n.actor?.userId)">拒绝</el-button>
                  </span>
                </template>
                <template v-else-if="n.type === 'SYSTEM'">
                  <span class="official">📢 官方消息</span>
                  <div class="n-preview">{{ n.content }}</div>
                </template>
              </div>
              <div class="n-time">{{ fromNow(n.createTime) }}</div>
            </div>
            <span v-if="!n.read" class="n-dot" title="未读" />
          </div>
          <el-pagination
            v-if="notifyTotal > 20"
            class="pager"
            layout="prev, pager, next"
            :total="notifyTotal"
            :page-size="20"
            :current-page="notifyPage"
            @current-change="loadNotify"
          />
        </div>
        <div v-else class="empty-tip">还没有新消息通知，去社区互动一下吧 ✨</div>
      </el-tab-pane>

      <!-- ============ 私信：会话列表 ============ -->
      <el-tab-pane label="私信" name="chat">
        <div class="chat-toolbar">
          <span class="tip">私信会话（非好友双方仅可各发 1 条打招呼）</span>
          <div class="toolbar-right">
            <el-button v-if="anyChatUnread" size="small" :icon="Check" @click="readAllChats">全部已读</el-button>
            <el-button size="small" round @click="$router.push('/friends')">好友管理</el-button>
          </div>
        </div>
        <div v-if="threads.length" class="t-list">
          <div
            v-for="t in threads"
            :key="t.user?.userId"
            class="t-item"
            @click="$router.push(`/chat/${t.user.userId}`)"
          >
            <UserChip :user="t.user" :size="40" />
            <div class="t-main">
              <div class="t-top">
                <span class="t-name">{{ t.user?.nickname }}</span>
                <span class="t-time">{{ fromNow(t.lastTime) }}</span>
              </div>
              <div class="t-last">{{ t.lastMessage }}</div>
            </div>
            <span v-if="(t.unread || 0) > 0" class="t-dot" title="未读" />
          </div>
        </div>
        <div v-else class="empty-tip">
          暂无会话<br />
          <el-button type="primary" round class="mt8" @click="$router.push('/friends')">去找好友聊聊</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 管理员：发送官方消息弹窗 -->
    <el-dialog v-model="broadcastVisible" title="发送官方消息" width="480px" align-center>
      <p class="bc-tip">官方消息将以 SYSTEM 通知发送给<b>全体用户</b>的消息中心（不影响自身）。</p>
      <el-input
        v-model="broadcastText"
        type="textarea"
        :rows="5"
        maxlength="500"
        show-word-limit
        resize="none"
        placeholder="输入要通知全体用户的官方消息…"
      />
      <template #footer>
        <el-button @click="broadcastVisible = false">取消</el-button>
        <el-button type="primary" :loading="broadcasting" @click="sendBroadcast">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Promotion, Refresh } from '@element-plus/icons-vue'
import UserChip from '../components/UserChip.vue'
import { apiNotifyList, apiNotifyUnread, apiNotifyRead, apiNotifyBroadcast } from '../api/notify'
import { apiChatThreads, apiChatUnread, apiChatRead } from '../api/chat'
import { apiFriendPendingList, apiFriendAccept, apiFriendReject } from '../api/relation'
import { fromNow } from '../utils/format'
import { badgeState, setBadges } from '../store/badge'
import { authState } from '../store/user'

const router = useRouter()
const tab = ref('notify')
const isAdmin = computed(() => authState.user?.role === 'ADMIN')

// ---- 通知 ----
const notifyList = ref([])
const notifyTotal = ref(0)
const notifyPage = ref(1)
const notifyUnread = ref(0)
const pendingMap = ref({}) // actorId -> 存在待处理申请

// ---- 私信 ----
const threads = ref([])

// ---- 管理员官方消息 ----
const broadcastVisible = ref(false)
const broadcastText = ref('')
const broadcasting = ref(false)

async function sendBroadcast() {
  const text = broadcastText.value.trim()
  if (!text) {
    ElMessage.warning('请输入官方消息内容')
    return
  }
  broadcasting.value = true
  try {
    await apiNotifyBroadcast(text)
    ElMessage.success('官方消息已发送给全体用户')
    broadcastVisible.value = false
    broadcastText.value = ''
    // 自己也能确认消息(官方消息不通知自身，仅刷新列表)
    loadNotify(notifyPage.value)
  } finally {
    broadcasting.value = false
  }
}

async function loadNotify(page = notifyPage.value) {
  const data = await apiNotifyList({ page, size: 20 })
  notifyList.value = data.records
  notifyTotal.value = Number(data.total) || 0
  notifyPage.value = page
  refreshNotifyUnread()
}

async function refreshNotifyUnread() {
  const n = await apiNotifyUnread()
  notifyUnread.value = n?.count || 0
  setBadges(n?.count || 0, badgeState.chat || 0)
}

async function loadPendingMap() {
  try {
    const list = await apiFriendPendingList()
    const map = {}
    list.forEach((u) => { map[u.userId] = true })
    pendingMap.value = map
  } catch (e) {
    pendingMap.value = {}
  }
}

async function loadThreads() {
  threads.value = await apiChatThreads()
  const c = await apiChatUnread()
  setBadges(badgeState.notify || 0, c?.count || 0)
}

/** 是否有任意私信会话未读 */
const anyChatUnread = computed(() => (threads.value || []).some((t) => (t.unread || 0) > 0))

/** 一键清空全部私信未读(逐会话标记已读后刷新) */
const chatReading = ref(false)
async function readAllChats() {
  if (chatReading.value) return
  chatReading.value = true
  try {
    for (const t of threads.value) {
      if ((t.unread || 0) > 0) {
        await apiChatRead(t.user?.userId)
      }
    }
    ElMessage.success('私信已全部读啦')
    await loadThreads()
  } finally {
    chatReading.value = false
  }
}

function reloadAll() {
  loadNotify(1)
  loadPendingMap()
  loadThreads()
}

function typeClass(t) {
  return t === 'LIKE' ? 'like' : t === 'COMMENT' ? 'comment' : t === 'COLLECT' ? 'collect' : t === 'SYSTEM' ? 'system' : 'friend'
}
function typeIcon(t) {
  return t === 'LIKE' ? '👍' : t === 'COLLECT' ? '⭐' : t === 'COMMENT' ? '💬' : t === 'SYSTEM' ? '📢' : '👥'
}

/** 点击通知：已读 + 跳转(评论跳帖子定位楼层/官方消息仅已读) */
async function openNotify(n) {
  if (!n.read) {
    await apiNotifyRead([n.id])
    n.read = true
    notifyUnread.value = Math.max(0, notifyUnread.value - 1)
    setBadges(notifyUnread.value, badgeState.chat || 0)
  }
  if (n.type === 'SYSTEM') return
  if (n.type === 'COMMENT') {
    router.push(`/post/${n.postId}?c=${n.commentId}`)
  } else if (n.type === 'LIKE' || n.type === 'COLLECT') {
    router.push(`/post/${n.postId}`)
  }
}

async function readAllNotify() {
  await apiNotifyRead([])
  loadNotify(notifyPage.value)
}

async function acceptReq(userId) {
  try {
    await apiFriendAccept(userId)
  } catch (e) {
    return
  }
  ElMessage.success('已通过，你们现在是好友啦')
  loadNotify(notifyPage.value)
  loadPendingMap()
  loadThreads()
}

async function rejectReq(userId) {
  try {
    await ElMessageBox.confirm('拒绝该好友申请？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await apiFriendReject(userId)
  ElMessage.success('已拒绝')
  loadNotify(notifyPage.value)
  loadPendingMap()
}

watch(tab, (v) => {
  if (v === 'chat') loadThreads()
  if (v === 'notify') loadNotify(notifyPage.value)
})

onMounted(() => {
  loadNotify(1)
  loadPendingMap()
  loadThreads()
})
</script>

<style scoped>
.msg-page {
  max-width: 780px;
}
.ml {
  margin-left: 4px;
}
.n-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 13px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.n-item:hover {
  background: #f6f8ff;
}
.n-item.unread {
  background: #f2f5ff;
}
.n-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.n-icon.like { background: #ffe9e9; }
.n-icon.comment { background: #e8f1ff; }
.n-icon.collect { background: #fff4df; }
.n-icon.friend { background: #e9f7ef; }
.n-icon.system { background: #f0ecff; }
.n-body {
  flex: 1;
  min-width: 0;
}
/* 通知未读小红点(不显示数字) */
.n-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex: none;
  align-self: center;
  margin-left: 6px;
}
.n-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text);
}
.n-text .link {
  color: var(--brand);
}
.n-preview {
  margin-top: 4px;
  padding: 6px 10px;
  background: #f3f5fb;
  border-radius: 8px;
  color: var(--text-sub);
  font-size: 13px;
}
.done-tag {
  margin-left: 6px;
  font-size: 12px;
  color: var(--text-sub);
  background: #eef1f8;
  padding: 1px 8px;
  border-radius: 8px;
}
.ops {
  margin-left: 8px;
  display: inline-flex;
  gap: 6px;
  vertical-align: middle;
}
.n-time {
  margin-top: 3px;
  font-size: 12px;
  color: #a4abc0;
}
.pager {
  margin-top: 14px;
  justify-content: center;
}
.chat-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tip {
  font-size: 12px;
  color: var(--text-sub);
}
.t-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 12px;
  border-radius: 12px;
  cursor: pointer;
  border-bottom: 1px dashed var(--border);
  transition: background 0.15s;
}
.t-item:hover {
  background: #f6f8ff;
}
/* 未读红点(不显示数字) */
.t-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex: none;
}
.t-main {
  flex: 1;
  min-width: 0;
}
.t-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.t-name {
  font-weight: 700;
  font-size: 14.5px;
}
.t-time {
  font-size: 12px;
  color: #a4abc0;
}
.t-last {
  margin-top: 3px;
  font-size: 13px;
  color: var(--text-sub);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mt8 {
  margin-top: 8px;
}
.official {
  color: #8b5cf6;
  font-weight: 700;
}
.bc-tip {
  font-size: 13px;
  color: var(--text-sub);
  margin: 0 0 10px;
}
/* ---------- 移动端适配 ---------- */
@media (max-width: 640px) {
  .msg-page {
    max-width: 100%;
  }
  .n-item {
    padding: 11px 8px;
    gap: 8px;
  }
  .n-preview {
    font-size: 12.5px;
  }
  .ops {
    display: flex;
    gap: 6px;
    margin-top: 6px;
    margin-left: 0;
  }
  .t-item {
    padding: 11px 8px;
    gap: 8px;
  }
  .t-last {
    max-width: 62vw;
  }
  .chat-toolbar {
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>
