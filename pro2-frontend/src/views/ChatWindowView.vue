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
const otherUserId = computed(() => route.params.userId)
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
let maxId = 0
let timer = null

function scrollBottom() {
  nextTick(() => {
    const el = scrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function refreshBadge() {
  try {
    const c = await apiChatUnread()
    setBadges(badgeState.notify || 0, c?.count || 0)
  } catch (e) {
    /* 静默 */
  }
}

async function markRead() {
  if (blocked.value || !myId.value) return
  try {
    await apiChatRead(otherUserId.value)
    await refreshBadge()
  } catch (e) {
    /* 静默 */
  }
}

/** 加载会话：取最后一页(正序) */
async function loadHistory() {
  loadingHistory.value = true
  blocked.value = false
  friendHint.value = false
  try {
    const p = await apiUserProfile(otherUserId.value)
    other.value = { userId: p.userId, nickname: p.nickname, avatarUrl: p.avatarUrl, role: p.role }
    relation.value = p.relation
    if (p.relation === 'BLACKED') {
      blocked.value = true
      loadingHistory.value = false
      return
    }
    const first = await apiChatMessages(otherUserId.value, { page: 1, size: 1 })
    const total = Number(first.total) || 0
    const lastPage = Math.max(1, Math.ceil(total / 50))
    const data = total === 0 ? { records: [] } : await apiChatMessages(otherUserId.value, { page: lastPage, size: 50 })
    messages.value = data.records
    maxId = messages.value.length ? Number(messages.value[messages.value.length - 1].id) : 0
    scrollBottom()
    markRead()
  } catch (e) {
    blocked.value = true
  } finally {
    loadingHistory.value = false
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content || blocked.value) return
  sending.value = true
  try {
    const msg = await apiChatSend(otherUserId.value, content)
    messages.value.push(msg)
    maxId = Math.max(maxId, Number(msg.id))
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

/** 轮询增量拉取(3 秒)：非好友/好友聊天均适用 */
function startPolling() {
  if (timer) clearInterval(timer)
  timer = setInterval(async () => {
    if (blocked.value || !myId.value) return
    try {
      const data = await apiChatMessages(otherUserId.value, { afterId: maxId || undefined })
      if (data.records && data.records.length) {
        messages.value.push(...data.records)
        maxId = Math.max(maxId, Number(data.records[data.records.length - 1].id))
        scrollBottom()
        markRead()
      }
    } catch (e) {
      /* 轮询失败静默，等待下轮 */
    }
  }, 3000)
}

watch(otherUserId, () => {
  messages.value = []
  maxId = 0
  loadHistory()
})

onMounted(() => {
  loadHistory()
  startPolling()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
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
</style>
