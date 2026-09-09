<template>
  <!-- 帖子多图上传预览组件：多选 → 逐张上传到 /post/image → 得到 URL 数组(v-model) -->
  <div class="img-uploader">
    <div class="img-grid">
      <div v-for="(url, i) in modelValue" :key="url" class="img-item">
        <el-image :src="url" fit="cover" :preview-src-list="modelValue" :initial-index="i" preview-teleported />
        <span class="img-del" title="移除" @click.stop="removeAt(i)">
          <el-icon><Close /></el-icon>
        </span>
      </div>

      <div v-if="modelValue.length < max" class="img-add" :class="{ uploading }" @click="pickFile">
        <template v-if="uploading">
          <el-icon class="is-loading"><Loading /></el-icon>
        </template>
        <template v-else>
          <el-icon :size="22"><Plus /></el-icon>
          <span>上传图片</span>
        </template>
      </div>
    </div>
    <div class="img-hint">支持 jpg / png / gif / webp，单张 ≤ 5MB，最多 {{ max }} 张</div>
    <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple hidden @change="onFiles" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiUploadPostImage } from '../api/post'
import { isLoggedIn, openLoginDialog } from '../store/user'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 9 }
})
const emit = defineEmits(['update:modelValue'])

const fileInput = ref()
const uploading = ref(false)

function pickFile() {
  // 游客直接触发登录弹窗
  if (!isLoggedIn.value) {
    openLoginDialog()
    return
  }
  fileInput.value && fileInput.value.click()
}

async function onFiles(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (!files.length) return
  if (props.modelValue.length + files.length > props.max) {
    ElMessage.warning(`最多只能上传 ${props.max} 张图片`)
    return
  }
  uploading.value = true
  const urls = [...props.modelValue]
  for (const file of files) {
    try {
      const url = await apiUploadPostImage(file)
      urls.push(url)
    } catch (err) {
      // 单张失败不中断(错误已由拦截器提示)
    }
  }
  emit('update:modelValue', urls)
  uploading.value = false
}

function removeAt(i) {
  const next = [...props.modelValue]
  next.splice(i, 1)
  emit('update:modelValue', next)
}
</script>

<style scoped>
.img-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.img-item {
  position: relative;
  width: 108px;
  height: 108px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--border);
}
.img-item :deep(.el-image) {
  width: 100%;
  height: 100%;
}
.img-del {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}
.img-item:hover .img-del {
  opacity: 1;
}
.img-add {
  width: 108px;
  height: 108px;
  border: 1.5px dashed #c6cde0;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--text-sub);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.img-add:hover {
  border-color: var(--brand);
  color: var(--brand);
  background: #f6f8ff;
}
.img-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-sub);
}
</style>
