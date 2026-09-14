<template>
  <div class="page-container friends-page">
    <div class="section-title">好友管理</div>

    <el-tabs v-model="tab">
      <!-- 好友 -->
      <el-tab-pane label="我的好友" name="friends">
        <div v-if="friends.length" class="row-list">
          <div v-for="f in friends" :key="f.userId" class="row-item">
            <UserChip :user="f" :size="38" />
            <div class="row-ops">
              <el-button size="small" type="primary" round @click="$router.push(`/chat/${f.userId}`)">
                <el-icon><ChatDotRound /></el-icon>&nbsp;发私信
              </el-button>
              <el-button size="small" plain round @click="$router.push(`/user/${f.userId}`)">主页</el-button>
              <el-button size="small" text type="danger" @click="removeFriend(f)">删除好友</el-button>
            </div>
          </div>
        </div>
        <div v-else class="empty-tip">还没有好友，去大家的主页加好友吧 🧑‍🤝‍🧑</div>
      </el-tab-pane>

      <!-- 好友申请 -->
      <el-tab-pane :label="`好友申请${pending.length ? `(${pending.length})` : ''}`" name="pending">
        <div v-if="pending.length" class="row-list">
          <div v-for="f in pending" :key="f.userId" class="row-item">
            <UserChip :user="f" :size="38" />
            <div class="row-ops">
              <el-button size="small" type="primary" round @click="accept(f)">通过</el-button>
              <el-button size="small" plain round @click="reject(f)">拒绝</el-button>
              <el-button size="small" text @click="$router.push(`/user/${f.userId}`)">看主页</el-button>
            </div>
          </div>
        </div>
        <div v-else class="empty-tip">暂无好友申请</div>
      </el-tab-pane>

      <!-- 黑名单 -->
      <el-tab-pane label="黑名单" name="black">
        <div v-if="blackList.length" class="row-list">
          <div v-for="b in blackList" :key="b.userId" class="row-item">
            <UserChip :user="b" :size="38" />
            <div class="row-ops">
              <el-button size="small" round @click="unblock(b)">解除拉黑</el-button>
              <el-button size="small" text @click="$router.push(`/user/${b.userId}`)">看主页</el-button>
            </div>
          </div>
        </div>
        <div v-else class="empty-tip">黑名单是空的，说明你很友善 😊</div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserChip from '../components/UserChip.vue'
import { apiFriendList, apiFriendPendingList, apiFriendAccept, apiFriendReject, apiFriendRemove, apiBlacklistList, apiBlacklistRemove } from '../api/relation'

const tab = ref('friends')
const friends = ref([])
const pending = ref([])
const blackList = ref([])

async function loadFriends() {
  friends.value = await apiFriendList()
}
async function loadPending() {
  pending.value = await apiFriendPendingList()
}
async function loadBlack() {
  blackList.value = await apiBlacklistList()
}

function loadAll() {
  loadFriends()
  loadPending()
  loadBlack()
}

async function accept(u) {
  await apiFriendAccept(u.userId)
  ElMessage.success(`已通过 ${u.nickname} 的好友申请`)
  loadAll()
}
async function reject(u) {
  try {
    await ElMessageBox.confirm(`拒绝 ${u.nickname} 的好友申请？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await apiFriendReject(u.userId)
  ElMessage.success('已拒绝')
  loadAll()
}
async function removeFriend(u) {
  try {
    await ElMessageBox.confirm(`删除好友 ${u.nickname}？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await apiFriendRemove(u.userId)
  ElMessage.success('已删除好友')
  loadAll()
}
async function unblock(b) {
  await apiBlacklistRemove(b.userId)
  ElMessage.success('已解除拉黑')
  loadAll()
}

watch(tab, (v) => {
  if (v === 'friends') loadFriends()
  if (v === 'pending') loadPending()
  if (v === 'black') loadBlack()
})

onMounted(loadAll)
</script>

<style scoped>
.friends-page {
  max-width: 720px;
}
.row-list {
  display: flex;
  flex-direction: column;
}
.row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 11px 8px;
  border-bottom: 1px dashed var(--border);
}
.row-ops {
  display: flex;
  align-items: center;
  gap: 6px;
}
/* ---------- 移动端适配 ---------- */
@media (max-width: 640px) {
  .friends-page {
    max-width: 100%;
  }
  .row-item {
    flex-wrap: wrap;
    gap: 8px;
    padding: 10px 4px;
  }
  .row-ops {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}
</style>
