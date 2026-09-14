<template>
  <div class="page-container admin-page">
    <div class="section-title">
      用户管理
      <MiniTag icon="🛡️" text="仅管理员可见" tone="violet" class="ml" />
    </div>

    <!-- 搜索 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索账号 / 昵称"
        clearable
        class="search"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-button type="primary" :icon="Search" @click="load(1)">搜索</el-button>
    </div>

    <!-- 桌面端：表格；移动端自动横向滚动 -->
    <div class="table-wrap" v-loading="loading">
      <el-table :data="rows" stripe style="width: 100%" min-width="720">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="u-cell">
              <el-avatar :size="26" :src="row.avatarUrl">{{ (row.nickname || '?').slice(0, 1) }}</el-avatar>
              <span class="nick">{{ row.nickname }}</span>
              <AdminBadge v-if="row.role === 'ADMIN'" />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" min-width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <MiniTag
              :icon="row.banned ? '⛔' : '✅'"
              :text="row.banned ? '已封禁' : '正常'"
              :tone="row.banned ? 'danger' : 'success'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="160" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.banned"
              size="small"
              type="warning"
              plain
              :disabled="row.role === 'ADMIN'"
              @click="ban(row)"
            >
              封禁
            </el-button>
            <el-button
              v-else
              size="small"
              type="success"
              plain
              @click="unban(row)"
            >
              解封
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :disabled="row.role === 'ADMIN'"
              @click="removeUser(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination
      v-if="total > 10"
      class="pager"
      layout="prev, pager, next"
      :total="total"
      :page-size="10"
      :current-page="page"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import AdminBadge from '../components/AdminBadge.vue'
import MiniTag from '../components/MiniTag.vue'
import { apiAdminUserList, apiAdminUserBan, apiAdminUserUnban, apiAdminUserDelete } from '../api/admin'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const loading = ref(false)

async function load(p = page.value) {
  loading.value = true
  try {
    const data = await apiAdminUserList({ page: p, size: 10, keyword: keyword.value || undefined })
    rows.value = data.records
    total.value = Number(data.total) || 0
    page.value = p
  } finally {
    loading.value = false
  }
}

async function ban(row) {
  try {
    await ElMessageBox.confirm(
      `封禁「${row.nickname}」后，该用户将无法发帖与评论（仍可浏览），确定吗？`,
      '封禁用户',
      { type: 'warning', confirmButtonText: '封禁', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await apiAdminUserBan(row.id)
  ElMessage.success('已封禁该用户')
  load()
}

async function unban(row) {
  await apiAdminUserUnban(row.id)
  ElMessage.success('已解封该用户')
  load()
}

async function removeUser(row) {
  try {
    await ElMessageBox.confirm(
      `删除「${row.nickname}」后该账号将无法登录（逻辑删除，原账号名会被释放），确定吗？`,
      '删除用户',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await apiAdminUserDelete(row.id)
  ElMessage.success('已删除该用户')
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.admin-page {
  max-width: 1000px;
}
.ml {
  margin-left: 8px;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.search {
  max-width: 320px;
}
.table-wrap {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 8px;
  overflow-x: auto;
}
.u-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
.nick {
  font-weight: 600;
}
.pager {
  margin-top: 16px;
  justify-content: center;
}

/* ---------- 移动端适配 ---------- */
@media (max-width: 640px) {
  .admin-page {
    max-width: 100%;
  }
  .toolbar {
    flex-direction: column;
  }
  .search {
    max-width: 100%;
  }
  .table-wrap {
    border-radius: 12px;
    padding: 4px;
  }
}
</style>
