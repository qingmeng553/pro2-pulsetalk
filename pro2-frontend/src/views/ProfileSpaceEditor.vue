<template>
  <div class="space-editor" @input="syncPreview" @change="syncPreview">
    <!-- 顶栏 -->
    <div class="ed-top">
      <div>
        <h1 class="ed-title">编辑主页</h1>
        <p class="ed-sub">保存后立即生效 · 未保存的修改只出现在右侧预览</p>
      </div>
      <div class="ed-actions">
        <el-button @click="openMySpace">查看主页</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </div>

    <div v-if="loading" class="ed-loading" v-loading="true"></div>

    <div v-else-if="!draft" class="ed-error">
      <h3>主页编辑器加载失败</h3>
      <p>{{ loadError || '请检查登录状态后重试' }}</p>
      <el-button type="primary" @click="load">重新加载</el-button>
    </div>

    <div v-else class="ed-body">
      <!-- 左侧配置 -->
      <aside class="ed-panel">
        <div class="panel-tabs">
          <button type="button" class="panel-tab" :class="{ active: activeTab === 'modules' }" @click="activeTab = 'modules'">模块</button>
          <button type="button" class="panel-tab" :class="{ active: activeTab === 'content' }" @click="activeTab = 'content'">内容</button>
          <button type="button" class="panel-tab" :class="{ active: activeTab === 'theme' }" @click="activeTab = 'theme'">主题</button>
        </div>

        <!-- 模块开关/排序 -->
        <section v-show="activeTab === 'modules'" class="panel-sec">
          <h3 class="panel-title">模块</h3>
          <p class="panel-tip">拖拽或上下箭头排序；关闭的模块访客看不到</p>
          <div
            v-for="(m, i) in draft.modules"
            :key="m.type"
            class="module-row"
            draggable="true"
            @dragstart="dragIndex = i"
            @dragover.prevent
            @drop="dropModule(i)"
          >
            <span class="drag-handle">⋮⋮</span>
            <span class="module-name">{{ MODULE_META[m.type]?.name || m.type }}</span>
            <el-switch v-model="m.visible" size="small" :disabled="m.type === 'bio'" />
            <el-select
              v-model="m.width"
              size="small"
              class="width-select"
              :disabled="m.type === 'bio'"
            >
              <el-option label="整行" value="full" />
              <el-option label="半行" value="half" />
            </el-select>
            <el-button-group class="move-group">
              <el-button size="small" :disabled="i === 0" @click="moveModule(i, -1)">↑</el-button>
              <el-button size="small" :disabled="i === draft.modules.length - 1" @click="moveModule(i, 1)">↓</el-button>
            </el-button-group>
          </div>
        </section>

        <!-- 内容 -->
        <section v-show="activeTab === 'content'" class="panel-sec">
          <h3 class="panel-title">内容</h3>

          <div v-for="m in contentModules" :key="m.type" class="edit-block">
            <div class="edit-title">{{ MODULE_META[m.type]?.name }}</div>

            <!-- 简介 -->
            <template v-if="m.type === 'bio'">
              <el-input
                v-model="m.data.intro"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="支持 Markdown（不支持 HTML）"
              />
            </template>

            <!-- 置顶 -->
            <template v-else-if="m.type === 'pinned'">
              <el-input v-model="m.data.postId" placeholder="填写你自己帖子的 ID" clearable />
              <p class="field-tip">保存时会校验帖子存在且属于你。</p>
            </template>

            <!-- 技能 -->
            <template v-else-if="m.type === 'skills'">
              <el-input
                v-model="skillsText"
                maxlength="200"
                placeholder="逗号分隔，如：Java, Redis, Vue"
                @input="onSkillsInput"
                @blur="formatSkills"
              />
              <p class="field-tip">最多 20 个，每个 ≤12 字。</p>
            </template>

            <!-- 作品 -->
            <template v-else-if="m.type === 'projects'">
              <div v-for="(p, i) in m.data" :key="i" class="item-card">
                <div class="item-head">
                  <span>作品 {{ i + 1 }}</span>
                  <el-button link type="danger" size="small" @click="m.data.splice(i, 1)">删除</el-button>
                </div>
                <el-input v-model="p.title" maxlength="60" placeholder="标题" />
                <el-input v-model="p.desc" type="textarea" :rows="2" maxlength="200" placeholder="一句话描述" />
                <el-input v-model="p.url" maxlength="300" placeholder="链接（https://...，可空）" />
                <div class="cover-row">
                  <el-button size="small" @click="pickImage((url) => (p.cover = url))">上传封面</el-button>
                  <img v-if="p.cover" :src="resolveAssetUrl(p.cover)" class="cover-thumb" alt="" />
                  <el-button v-if="p.cover" link size="small" @click="p.cover = ''">移除</el-button>
                </div>
              </div>
              <el-button v-if="m.data.length < 6" size="small" @click="m.data.push({ title: '', desc: '', url: '', cover: '' })">
                + 添加作品
              </el-button>
            </template>

            <!-- 图集 -->
            <template v-else-if="m.type === 'gallery'">
              <div class="gallery-edit">
                <div v-for="(g, i) in m.data" :key="i" class="gallery-item">
                  <img :src="resolveAssetUrl(g.url)" alt="" />
                  <el-input v-model="g.caption" maxlength="60" size="small" placeholder="图注" />
                  <el-button link type="danger" size="small" @click="m.data.splice(i, 1)">删除</el-button>
                </div>
              </div>
              <el-button
                v-if="m.data.length < 9"
                size="small"
                @click="pickImage((url) => m.data.push({ url, caption: '' }))"
              >
                + 上传图片
              </el-button>
            </template>

            <!-- 外链 -->
            <template v-else-if="m.type === 'links'">
              <div v-for="(l, i) in m.data" :key="i" class="item-card">
                <div class="item-head">
                  <span>外链 {{ i + 1 }}</span>
                  <el-button link type="danger" size="small" @click="m.data.splice(i, 1)">删除</el-button>
                </div>
                <div class="link-edit-row">
                  <el-select v-model="l.icon" size="small" class="icon-select">
                    <el-option v-for="ic in LINK_ICONS" :key="ic.value" :label="ic.name" :value="ic.value" />
                  </el-select>
                  <el-input v-model="l.name" maxlength="20" placeholder="名称" />
                </div>
                <el-input v-model="l.url" maxlength="300" placeholder="https://..." />
              </div>
              <el-button v-if="m.data.length < 10" size="small" @click="m.data.push({ name: '', url: '', icon: 'website' })">
                + 添加外链
              </el-button>
            </template>
          </div>
        </section>

        <!-- 主题 -->
        <section v-show="activeTab === 'theme'" class="panel-sec">
          <h3 class="panel-title">主题</h3>
          <div class="theme-grid">
            <button
              v-for="id in THEME_ORDER"
              :key="id"
              type="button"
              class="theme-card"
              :class="{ active: draft.theme.pack === id }"
              @click="selectPack(id)"
            >
              <span class="theme-dot" :style="{ background: THEMES[id].accent }"></span>
              <span class="theme-name">{{ THEMES[id].name }}</span>
            </button>
          </div>

          <div class="field-label">强调色</div>
          <div class="accent-row">
            <button
              v-for="a in ACCENTS"
              :key="a.value"
              type="button"
              class="accent-dot"
              :class="{ active: draft.theme.accent === a.value }"
              :style="{ background: a.value }"
              :title="a.name"
              @click="draft.theme.accent = a.value"
            ></button>
          </div>

          <div class="field-row">
            <div>
              <div class="field-label">卡片</div>
              <el-radio-group v-model="draft.theme.cardStyle" size="small">
                <el-radio-button v-for="s in CARD_STYLES" :key="s.value" :value="s.value">{{ s.name }}</el-radio-button>
              </el-radio-group>
            </div>
            <div>
              <div class="field-label">挂件</div>
              <el-select v-model="draft.theme.widget" size="small">
                <el-option v-for="w in WIDGETS" :key="w.value" :label="w.name" :value="w.value" />
              </el-select>
            </div>
          </div>

          <div class="field-label">头图（1500×500 最佳）</div>
          <div class="header-row">
            <img v-if="draft.theme.header" :src="resolveAssetUrl(draft.theme.header)" class="header-thumb" alt="" />
            <div class="header-btns">
              <el-button size="small" @click="pickImage((url) => (draft.theme.header = url))">上传头图</el-button>
              <el-button v-if="draft.theme.header" link size="small" @click="draft.theme.header = null">移除</el-button>
            </div>
          </div>
        </section>
      </aside>

      <!-- 右侧双端预览 -->
      <main class="ed-preview">
        <div class="preview-col web">
          <div class="pv-label">Web · 1440</div>
          <div class="pv-box web">
            <iframe ref="webFrame" :src="previewUrl" class="pv-frame web" @load="syncPreview"></iframe>
          </div>
        </div>
        <div class="preview-col app">
          <div class="pv-label">App · 390</div>
          <div class="pv-box app">
            <iframe ref="appFrame" :src="previewUrl" class="pv-frame app" @load="syncPreview"></iframe>
          </div>
        </div>
      </main>
    </div>

    <input ref="imgInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" hidden @change="onFile" />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resolveAssetUrl } from '../utils/asset'
import {
  THEMES, THEME_ORDER, ACCENTS, CARD_STYLES, WIDGETS, LINK_ICONS, MODULE_META
} from '../utils/spaceThemes'
import { apiSpace, apiSaveSpace, apiUploadSpaceImage } from '../api/space'
import { authState } from '../store/user'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const loadError = ref('')
const activeTab = ref('modules')
const draft = ref(null)
const version = ref(0)
const imgInput = ref()
const pendingCb = ref(null)
const webFrame = ref()
const appFrame = ref()
let dragIndex = null

const me = computed(() => authState.user)
const previewUrl = computed(() => `/user/${me.value?.id}?preview=web`)
const contentModules = computed(() =>
  (draft.value?.modules || []).filter((m) => ['bio', 'pinned', 'skills', 'projects', 'gallery', 'links'].includes(m.type))
)

const skillsText = ref('')

function getModule(type) {
  return draft.value?.modules.find((m) => m.type === type)
}

/** 输入过程中只同步到 data，不回写输入框，否则逗号会被 split/join 吃掉 */
function onSkillsInput(value) {
  const m = getModule('skills')
  if (!m) return
  m.data = value
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean)
    .slice(0, 20)
}

function formatSkills() {
  const m = getModule('skills')
  if (!m) return
  skillsText.value = (m.data || []).join(', ')
}

function syncSkillsText() {
  const m = getModule('skills')
  skillsText.value = (m?.data || []).join(', ')
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const space = await apiSpace(me.value.id)
    draft.value = normalizeDraft(space.layout)
    version.value = space.version
    syncSkillsText()
    await nextTick()
    syncPreview()
  } catch (e) {
    draft.value = null
    loadError.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 老布局可能省略空模块的 data（尤其 pinned），编辑器必须保证每种模块都有可编辑结构 */
function normalizeDraft(layout) {
  const data = JSON.parse(JSON.stringify(layout || {}))
  data.modules = (data.modules || []).map((m) => {
    if (m.type === 'bio') {
      m.data = { intro: '', ...(m.data || {}) }
    } else if (m.type === 'pinned') {
      m.data = { postId: m.data?.postId ?? null }
    } else if (['skills', 'projects', 'gallery', 'links'].includes(m.type)) {
      m.data = Array.isArray(m.data) ? m.data : []
    }
    return m
  })
  return data
}

function moveModule(index, delta) {
  const arr = draft.value.modules
  const j = index + delta
  if (j < 0 || j >= arr.length) return
  const [m] = arr.splice(index, 1)
  arr.splice(j, 0, m)
  reindex()
}

function dropModule(index) {
  if (dragIndex === null || dragIndex === index) return
  const arr = draft.value.modules
  const [m] = arr.splice(dragIndex, 1)
  arr.splice(index, 0, m)
  dragIndex = null
  reindex()
}

function reindex() {
  draft.value.modules.forEach((m, i) => (m.order = i + 1))
}

function selectPack(id) {
  draft.value.theme.pack = id
  draft.value.theme.accent = THEMES[id].accent
}

function pickImage(cb) {
  pendingCb.value = cb
  imgInput.value?.click()
}

async function onFile(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const raw = await apiUploadSpaceImage(file)
    // 上传接口可能返回绝对地址；布局 schema 统一存 /files/...
    const idx = raw.indexOf('/files/')
    const url = idx > 0 ? raw.slice(idx) : raw
    if (pendingCb.value) pendingCb.value(url)
  } finally {
    uploading.value = false
    pendingCb.value = null
  }
}

function syncPreview() {
  if (!draft.value) return
  let layout
  try {
    // 不能直接 postMessage(reactive proxy)：结构化克隆会得到空对象，预览永远不更新
    layout = JSON.parse(JSON.stringify(draft.value))
  } catch (e) {
    return
  }
  const payload = { type: 'space-draft', layout }
  webFrame.value?.contentWindow?.postMessage(payload, window.location.origin)
  appFrame.value?.contentWindow?.postMessage(payload, window.location.origin)
}

function openMySpace() {
  router.push(`/user/${me.value.id}`)
}

async function save() {
  if (saving.value || !draft.value) return
  saving.value = true
  try {
    const result = await apiSaveSpace(draft.value, version.value)
    version.value = result.version
    ElMessage.success('主页已保存')
  } catch (e) {
    if (e?.code === 409) {
      try {
        await ElMessageBox.confirm('主页已在别处更新，是否加载最新版本？', '保存冲突', {
          confirmButtonText: '加载最新',
          cancelButtonText: '取消'
        })
        await load()
      } catch (err) {
        /* 留在当前页 */
      }
    }
  } finally {
    saving.value = false
  }
}

watch(
  draft,
  () => {
    nextTick(syncPreview)
  },
  { deep: true }
)

onMounted(load)
onBeforeUnmount(() => {
  /* 预览 iframe 随组件卸载 */
})
</script>

<style scoped>
.space-editor {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--sp-4) var(--sp-4) var(--sp-10);
}

.ed-top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--sp-4);
  margin-bottom: var(--sp-4);
}

.ed-title {
  margin: 0;
  font-size: var(--fs-xl);
}

.ed-sub {
  margin: 4px 0 0;
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.ed-actions {
  display: flex;
  gap: var(--sp-2);
}

.ed-loading {
  height: 400px;
}

.ed-body {
  display: grid;
  grid-template-columns: 430px minmax(0, 1fr);
  gap: var(--sp-4);
  align-items: start;
}

.ed-panel {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
  max-height: calc(100vh - 140px);
  overflow-y: auto;
}

.panel-tabs {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  padding: 6px;
  gap: 4px;
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-color);
}

.panel-tab {
  flex: 1;
  padding: 7px 0;
  border: 0;
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: var(--fs-xs);
  cursor: pointer;
}

.panel-tab.active {
  background: var(--bg-subtle);
  color: var(--c-primary);
  font-weight: var(--fw-medium);
}

.ed-error {
  max-width: 460px;
  margin: 40px auto;
  padding: var(--sp-6);
  text-align: center;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
}

.ed-error h3 {
  margin: 0 0 var(--sp-2);
}

.ed-error p {
  margin: 0 0 var(--sp-4);
  color: var(--text-secondary);
  font-size: var(--fs-sm);
}

.panel-sec {
  padding: var(--sp-4);
  border-bottom: 1px solid var(--border-color);
}

.panel-sec:last-child {
  border-bottom: 0;
}

.panel-title {
  margin: 0 0 var(--sp-2);
  font-size: var(--fs-sm);
}

.panel-tip,
.field-tip {
  margin: 0 0 var(--sp-2);
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.field-tip {
  margin-top: 4px;
}

.module-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 8px;
  margin-bottom: 4px;
  border: 1px solid var(--border-color);
  border-radius: var(--r-sm);
  background: var(--bg-subtle);
}

.drag-handle {
  color: var(--text-tertiary);
  cursor: grab;
  font-size: 12px;
}

.module-name {
  flex: 1;
  min-width: 60px;
  font-size: var(--fs-xs);
}

.width-select {
  width: 76px;
}

.move-group :deep(.el-button) {
  padding: 4px 6px;
}

.edit-block {
  padding: var(--sp-2) 0;
  border-top: 1px dashed var(--border-color);
}

.edit-title {
  font-size: var(--fs-xs);
  font-weight: var(--fw-medium);
  margin-bottom: var(--sp-2);
}

.item-card {
  padding: var(--sp-2);
  margin-bottom: var(--sp-2);
  border: 1px solid var(--border-color);
  border-radius: var(--r-sm);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

.cover-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cover-thumb {
  width: 54px;
  height: 36px;
  object-fit: cover;
  border-radius: 4px;
}

.link-edit-row {
  display: flex;
  gap: 6px;
}

.icon-select {
  width: 92px;
  flex: none;
}

.gallery-edit {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.gallery-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.gallery-item img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--r-sm);
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
  margin-bottom: var(--sp-3);
}

.theme-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--r-sm);
  background: var(--bg-elevated);
  cursor: pointer;
  font-size: var(--fs-xs);
  color: var(--text);
}

.theme-card.active {
  border-color: var(--c-primary);
  box-shadow: 0 0 0 1px var(--c-primary) inset;
}

.theme-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  flex: none;
}

.field-label {
  margin: var(--sp-3) 0 var(--sp-2);
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

.accent-row {
  display: flex;
  gap: 8px;
}

.accent-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
}

.accent-dot.active {
  border-color: var(--text);
  box-shadow: 0 0 0 2px var(--bg-elevated) inset;
}

.field-row {
  display: flex;
  gap: var(--sp-4);
}

.header-row {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.header-thumb {
  width: 120px;
  height: 48px;
  object-fit: cover;
  border-radius: var(--r-sm);
}

.header-btns {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

/* 预览 */
.ed-preview {
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
  min-width: 0;
}

.pv-label {
  margin-bottom: 6px;
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.pv-box {
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
  background: #eef1f8;
}

.pv-box.web {
  width: 100%;
  height: 560px;
}

.pv-box.app {
  width: 330px;
  height: 560px;
}

.pv-frame {
  border: 0;
  transform-origin: top left;
}

.pv-frame.web {
  width: 1440px;
  height: 900px;
  transform: scale(0.62);
}

.pv-frame.app {
  width: 390px;
  height: 844px;
  transform: scale(0.78);
}

@media (max-width: 1100px) {
  .ed-body {
    grid-template-columns: 1fr;
  }

  .ed-panel {
    max-height: none;
  }

  .ed-preview {
    display: none;
  }
}

@media (max-width: 639px) {
  .space-editor {
    padding: var(--sp-3);
  }

  .ed-top {
    flex-direction: column;
    align-items: stretch;
  }

  .ed-actions {
    justify-content: flex-end;
  }
}
</style>
