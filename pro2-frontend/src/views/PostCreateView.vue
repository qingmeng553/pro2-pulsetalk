<template>
  <div class="page-container create-page">
    <div class="card">
      <div class="section-title">{{ isEdit ? '编辑帖子' : '发布新帖' }}</div>

      <!-- v3：封禁用户不可发帖 -->
      <el-alert
        v-if="authState.user?.banned"
        title="当前账号已被封禁，无法发布或编辑帖子（可正常浏览与互动）"
        type="error"
        show-icon
        :closable="false"
        class="banned-tip"
      />

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="一句话讲清主题，支持 emoji ✨" />
        </el-form-item>

        <el-form-item label="板块分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 240px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="正文(Markdown)">
          <div class="editor-head">
            <el-radio-group v-model="mode" size="small">
              <el-radio-button value="write">编辑</el-radio-button>
              <el-radio-button value="preview">预览</el-radio-button>
            </el-radio-group>
            <EmojiPicker @pick="insertEmoji" />
            <span class="markdown-tip">支持 Markdown / 表情 / 拖拽图片外链，Ctrl+Enter 快速发布</span>
          </div>
          <div class="editor-area">
            <el-input
              v-if="mode === 'write'"
              ref="editorRef"
              v-model="form.content"
              type="textarea"
              :rows="14"
              maxlength="20000"
              show-word-limit
              resize="none"
              placeholder="分享你的想法…（支持 **加粗**、`代码`、emoji 😎）"
              @keydown.ctrl.enter="submit"
            />
            <div v-else class="preview-box">
              <MarkdownViewer :content="form.content || '*（正文为空）*'" />
            </div>
          </div>
        </el-form-item>

        <el-form-item label="配图">
          <ImageUploader v-model="form.imgUrls" :max="9" />
        </el-form-item>

        <div class="actions">
          <el-button @click="$router.back()">取消</el-button>
          <el-button type="primary" :loading="submitting" :disabled="authState.user?.banned" @click="submit">
            {{ isEdit ? '保存修改' : '发布帖子' }}
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownViewer from '../components/MarkdownViewer.vue'
import EmojiPicker from '../components/EmojiPicker.vue'
import ImageUploader from '../components/ImageUploader.vue'
import { authState } from '../store/user'
import { apiCategoryList, apiCreatePost, apiUpdatePost, apiPostDetail } from '../api/post'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const formRef = ref()
const editorRef = ref()
const categories = ref([])
const mode = ref('write')
const submitting = ref(false)

const form = reactive({
  title: '',
  categoryId: null,
  content: '',
  imgUrls: []
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

async function loadCategories() {
  categories.value = await apiCategoryList()
}

/** 编辑模式：回填原帖 */
async function loadForEdit() {
  if (!isEdit.value) return
  const d = await apiPostDetail(route.params.id)
  form.title = d.title
  form.categoryId = d.categoryId
  form.content = d.content
  form.imgUrls = d.imgUrls || []
  // 校验作者权限：详情页作者非本人时直接拦截
  const me = JSON.parse(localStorage.getItem('community_user') || 'null')
  if (!me || String(d.author?.userId) !== String(me.id)) {
    ElMessage.error('只能编辑自己发布的帖子')
    router.replace('/')
  }
}

/** emoji 插入到光标处 */
function insertEmoji(emoji) {
  const ta = editorRef.value && editorRef.value.textarea
  if (ta && document.activeElement === ta) {
    const start = ta.selectionStart
    const end = ta.selectionEnd
    form.content = form.content.slice(0, start) + emoji + form.content.slice(end)
    // 光标后移
    requestAnimationFrame(() => {
      ta.focus()
      const pos = start + emoji.length
      ta.setSelectionRange(pos, pos)
    })
  } else {
    form.content += emoji
  }
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  if (!form.content || !form.content.trim()) {
    ElMessage.warning('正文不能为空')
    return
  }
  submitting.value = true
  try {
    const payload = {
      title: form.title.trim(),
      categoryId: form.categoryId,
      content: form.content,
      imgUrls: form.imgUrls
    }
    if (isEdit.value) {
      await apiUpdatePost(route.params.id, payload)
      ElMessage.success('修改成功')
      router.push(`/post/${route.params.id}`)
    } else {
      const created = await apiCreatePost(payload)
      ElMessage.success('发布成功 🎉')
      router.push(`/post/${created.id}`)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadForEdit()
})
</script>

<style scoped>
.create-page {
  max-width: 860px;
}
.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px 26px;
}
.editor-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.markdown-tip {
  font-size: 12px;
  color: var(--text-sub);
}
.editor-area :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.7;
  border-radius: 12px;
}
.preview-box {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 18px;
  min-height: 220px;
  max-height: 460px;
  overflow: auto;
  background: #fff;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}
/* ---------- 移动端适配 ---------- */
@media (max-width: 640px) {
  .card {
    padding: 16px 14px;
    border-radius: 14px;
  }
  .editor-head {
    flex-wrap: wrap;
    gap: 8px;
  }
  .markdown-tip {
    display: none;
  }
  .actions {
    gap: 8px;
  }
  .actions .el-button {
    flex: 1;
  }
  .preview-box {
    padding: 12px 12px;
  }
}
/* 封禁提示条 */
.banned-tip {
  margin-bottom: 14px;
  border-radius: 10px;
}
</style>
