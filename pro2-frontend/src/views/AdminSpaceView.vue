<template>
  <div class="page-container admin-page">
    <div class="section-title">
      主页举报
      <MiniTag icon="🛡️" text="仅管理员可见" tone="violet" class="ml" />
    </div>

    <div class="toolbar">
      <el-select v-model="status" class="status-select" @change="load(1)">
        <el-option label="待处理" :value="0" />
        <el-option label="已处理" :value="1" />
        <el-option label="全部" :value="''" />
      </el-select>
      <el-button :icon="Refresh" @click="load(page)">刷新</el-button>
    </div>

    <div class="table-wrap" v-loading="loading">
      <el-table :data="rows" stripe style="width: 100%">
        <el-table-column label="被举报主页" min-width="150">
          <template #default="{ row }">
            <router-link :to="`/user/${row.targetUserId}`" class="link">
              {{ row.targetNickname || `用户 ${row.targetUserId}` }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column label="举报人" min-width="120">
          <template #default="{ row }">{{ row.reporterNickname || `用户 ${row.reporterId}` }}</template>
        </el-table-column>
        <el-table-column label="对象" width="110">
          <template #default="{ row }">
            {{ row.moduleType === 'profile' ? '整页' : (MODULE_META[row.moduleType]?.name || row.moduleType) }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <MiniTag
              :icon="row.status === 1 ? '✅' : '⏳'"
              :text="row.status === 1 ? '已处理' : '待处理'"
              :tone="row.status === 1 ? 'success' : 'warn'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              size="small"
              type="primary"
              plain
              @click="handle(row)"
            >
              标记处理
            </el-button>
            <el-button size="small" type="warning" plain @click="openHide(row)">
              隐藏模块
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

    <el-dialog v-model="hideVisible" title="隐藏主页模块" width="420px" align-center>
      <p class="hide-tip">
        对「{{ hideTarget?.targetNickname || hideTarget?.targetUserId }}」隐藏模块，访客与自己都将看不到（bio 不可隐藏）。
      </p>
      <el-select v-model="hideModuleType" placeholder="选择模块" style="width: 100%">
        <el-option
          v-for="(meta, key) in hideableModules"
          :key="key"
          :label="meta.name"
          :value="key"
        />
      </el-select>
      <el-input
        v-model="hideReason"
        class="hide-reason"
        maxlength="200"
        show-word-limit
        placeholder="原因（可选）"
      />
      <template #footer>
        <el-button @click="hideVisible = false">取消</el-button>
        <el-button type="primary" :loading="hiding" @click="confirmHide">确定隐藏</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import MiniTag from '../components/MiniTag.vue'
import { MODULE_META } from '../utils/spaceThemes'
import { apiAdminReports, apiAdminHandleReport, apiAdminHideModule } from '../api/space'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const status = ref(0)
const loading = ref(false)

const hideVisible = ref(false)
const hideTarget = ref(null)
const hideModuleType = ref('')
const hideReason = ref('')
const hiding = ref(false)

const hideableModules = computed(() => {
  const map = {}
  Object.entries(MODULE_META).forEach(([key, meta]) => {
    if (key !== 'bio') map[key] = meta
  })
  return map
})

async function load(p = page.value) {
  loading.value = true
  try {
    const data = await apiAdminReports({
      page: p,
      size: 10,
      status: status.value === '' ? undefined : status.value
    })
    rows.value = data.records || []
    total.value = Number(data.total) || 0
    page.value = p
  } finally {
    loading.value = false
  }
}

async function handle(row) {
  await apiAdminHandleReport(row.id)
  ElMessage.success('已标记处理')
  load(page.value)
}

function openHide(row) {
  hideTarget.value = row
  hideModuleType.value = row.moduleType === 'profile' ? 'skills' : row.moduleType
  hideReason.value = ''
  hideVisible.value = true
}

async function confirmHide() {
  if (!hideModuleType.value) {
    ElMessage.warning('请选择要隐藏的模块')
    return
  }
  hiding.value = true
  try {
    await apiAdminHideModule(hideTarget.value.targetUserId, {
      moduleType: hideModuleType.value,
      hidden: true,
      reason: hideReason.value
    })
    ElMessage.success('已隐藏该模块')
    hideVisible.value = false
    load(page.value)
  } finally {
    hiding.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.admin-page {
  max-width: 1100px;
}
.ml {
  margin-left: 8px;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.status-select {
  width: 130px;
}
.link {
  color: var(--text-link);
}
.hide-tip {
  margin: 0 0 12px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-sub);
}
.hide-reason {
  margin-top: 10px;
}
</style>
