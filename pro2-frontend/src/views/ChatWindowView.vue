<template>
  <div class="page-container chat-page">
    <!-- 会话头部 -->
    <div class="chat-head" v-if="other">
      <UserChip :user="other" :size="38" />
      <div class="head-right">
        <MiniTag v-if="relation === 'FRIEND'" icon="🤝" text="好友" tone="success" />
        <MiniTag v-else-if="relation && relation !== 'BLACKED' && relation !== 'SELF'" icon="📩" text="非好友·各限1条" tone="warn" />
        <el-button size="small" text @click="$router.push(`/user/${other.userId}`)">个人主页</el-button>
      </div>
    </div>

    <!-- 状态横幅 -->
    <el-alert
      v-if="blocked"
      title="你们处于拉黑状态，无法查看或发送消息（双向阻断）"
      type="error"
      :closable="false"
      show-icon
      class="banner"
    />
    <el-alert
      v-else-if="friendHint"
      title="非好友双方各限一条打招呼消息，已成为好友后才能继续畅聊"
      type="warning"
      :closable="false"
      show-icon
      class="banner"
    />

    <!-- 消息区 -->
    <div ref="scrollRef" class="chat-body">
      <div v-if="loadingHistory" v-loading="true" class="body-loading" />
      <template v-else>
        <div v-for="m in messages" :key="m.id" class="msg-row" :class="String(m.fromUserId) === String(myId) ? 'mine' : 'other'">
          <div class="bubble">{{ m.content }}</div>
          <div class="msg-time">{{ formatTime(m.createTime) }}</div>
        </div>
        <div v-if="!messages.length && !blocked && !loadingHistory" class="empty-tip">
          打个招呼开始聊天吧 👋
        </div>
      </template>
    </div>

    <!-- 输入区 -->
    <div class="chat-input-bar" v-if="!blocked">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="2"
        resize="none"
        maxlength="2000"
        :disabled="friendHint"
        placeholder="友善交流…(Ctrl+Enter 发送)"
        @keydown.ctrl.enter="send"
      />
      <div class="send-row">
        <el-button v-if="friendHint" round type="primary" @click="$router.push(`/user/${other.userId}`)">
          去加好友
        </el-button>
        <el-button v-else type="primary" :loading="sending" round @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserChip from '../components/UserChip.vue'
import MiniTag from '../components/MiniTag.vue'
import { apiChatMessages, apiChatSend, apiChatRead, apiChatUnread } from '../api/chat'
import { apiUserProfile } from '../api/relation'
import { formatTime } from '../utils/format'
import { authState } from '../store/user'
import { badgeState, setBadges } from '../store/badge'

const route = useRoute()
const otherUserId = computed(() => String(route.params.userId || ''))
const myId = computed(() => String(authState.user?.id || ''))

const other = ref(null)
const relation = ref('NONE')
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const blocked = ref(false)
const friendHint = ref(false)
const loadingHistory = ref(true)
const scrollRef = ref()

// 消息去重与合并
const seenIds = new Set()   // 已渲染消息 id
let maxId = 0               // 轮询游标
let disposed = false        // 离开或切换会话后作废在途请求

// 按 id 去重后追加，返回新增条数
function mergeIncoming(list) {
  if (!Array.isArray(list) || !list.length) return 0
  let added = 0
  for (const m of list) {
    if (!m || m.id == null) continue
    const key = String(m.id)
    if (seenIds.has(key)) continue   // 重复消息：跳过，不追加渲染
    seenIds.add(key)
    messages.value.push(m)
    if (Number(m.id) > maxId) maxId = Number(m.id)
    added++
  }
  return added
}

function resetConversation() {
  messages.value = []
  seenIds.clear()
  maxId = 0
  blocked.value = false
  friendHint.value = false
}

function scrollBottom() {
  nextTick(() => {
    const el = scrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

// 已读与角标

async function refreshBadge() {
  try {
    const c = await apiChatUnread()
    setBadges(badgeState.notify || 0, c?.count || 0)
  } catch (e) {
    /* 静默 */
  }
}

// 标记已读
async function markRead() {
  if (blocked.value || !myId.value || disposed) return
  try {
    await apiChatRead(otherUserId.value)
    await refreshBadge()
  } catch (e) {
    /* 静默 */
  }
}

// 历史消息

async function loadHistory() {
  const target = otherUserId.value
  loadingHistory.value = true
  resetConversation()
  try {
    const p = await apiUserProfile(target)
    if (disposed || otherUserId.value !== target) return // 期间已切换会话
    other.value = { userId: p.userId, nickname: p.nickname, avatarUrl: p.avatarUrl, role: p.role }
    relation.value = p.relation
    if (p.relation === 'BLACKED') {
      blocked.value = true
      return
    }
    const first = await apiChatMessages(target, { page: 1, size: 1 })
    if (disposed || otherUserId.value !== target) return
    const total = Number(first.total) || 0
    const lastPage = Math.max(1, Math.ceil(total / 50))
    const data = total === 0 ? { records: [] } : await apiChatMessages(target, { page: lastPage, size: 50 })
    if (disposed || otherUserId.value !== target) return
    // 历史同样去重
    mergeIncoming(data.records || [])
    scrollBottom()
    markRead()
  } catch (e) {
    if (!disposed && otherUserId.value === target) {
      blocked.value = true
    }
  } finally {
    if (!disposed && otherUserId.value === target) {
      loadingHistory.value = false
    }
  }
}

// 发送

async function send() {
  const content = draft.value.trim()
  if (!content || blocked.value) return
  sending.value = true
  try {
    const msg = await apiChatSend(otherUserId.value, content)
    // 发送成功后去重追加
    mergeIncoming([msg])
    draft.value = ''
    scrollBottom()
  } catch (e) {
    const text = (e && e.message) || ''
    if (text.includes('拉黑')) {
      blocked.value = true
    } else if (text.includes('还不是好友') || text.includes('一条')) {
      friendHint.value = true
    }
    ElMessage.warning(text || '发送失败，请稍后再试')
  } finally {
    sending.value = false
  }
}

// 轮询增量（防重入 + 会话校验）

let timer = null
let pollingInFlight = false // 防止上一次请求未返回时下一轮并发，避免同批消息被拉两次

async function pollNew() {
  if (pollingInFlight || blocked.value || !myId.value || disposed) return
  const target = otherUserId.value
  pollingInFlight = true
  try {
    const data = await apiChatMessages(target, { afterId: maxId || undefined })
    // 已切换会话则丢弃响应
    if (disposed || otherUserId.value !== target) return
    const added = mergeIncoming(data.records || [])
    if (added > 0) {
      scrollBottom()
      markRead()
    }
  } catch (e) {
    /* 静默 */
  } finally {
    pollingInFlight = false
  }
}

function startPolling() {
  if (timer) clearInterval(timer)
  timer = setInterval(pollNew, 3000)
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

// 生命周期

// 切换会话：重置状态并重新加载，旧请求靠 target 比对丢弃

watch(otherUserId, () => {
  resetConversation()
  loadHistory()
})

onMounted(() => {
  loadHistory()
  startPolling()
})

onBeforeUnmount(() => {
  disposed = true          // 作废在途轮询/历史/已读请求
  stopPolling()            // 销毁定时器，杜绝离开页面后仍在拉取
})
</script>

<style scoped>
.chat-page {
  max-width: 760px;
}
.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 14px 14px 0 0;
  padding: 12px 18px;
}
.head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.banner {
  border-radius: 0;
}
.chat-body {
  background: #fbfcff;
  border-left: 1px solid var(--border);
  border-right: 1px solid var(--border);
  height: 52vh;
  overflow-y: auto;
  padding: 16px 18px;
}
.body-loading {
  height: 120px;
}
.msg-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 14px;
  max-width: 70%;
}
.msg-row.mine {
  align-items: flex-end;
  margin-left: auto;
}
.msg-row.other {
  align-items: flex-start;
  margin-right: auto;
}
.bubble {
  padding: 9px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.mine .bubble {
  background: var(--brand-gradient);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.other .bubble {
  background: #fff;
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
}
.msg-time {
  margin-top: 4px;
  font-size: 11px;
  color: #b0b7ca;
}
.chat-input-bar {
  background: var(--card);
  border: 1px solid var(--border);
  border-top: none;
  border-radius: 0 0 14px 14px;
  padding: 12px 14px;
}
.send-row {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
/* 移动端 */
@media (max-width: 640px) {
  .chat-head {
    padding: 10px 12px;
    flex-wrap: wrap;
    gap: 8px;
    border-radius: 12px 12px 0 0;
  }
  .head-right {
    margin-left: auto;
  }
  .chat-body {
    height: 58vh;
    padding: 12px 12px;
  }
  .msg-row {
    max-width: 84%;
  }
  .bubble {
    font-size: 14px;
    padding: 8px 12px;
  }
  .chat-input-bar {
    padding: 10px 12px;
    border-radius: 0 0 12px 12px;
  }
}
</style>
