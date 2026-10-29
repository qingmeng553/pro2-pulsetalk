<template>
  <!-- 图形验证码：输入框 + 可点击刷新的图片；父组件用 v-model 绑 code、@update:id 拿 captchaId -->
  <div class="captcha-field">
    <el-input
      v-model="code"
      class="captcha-input"
      placeholder="图形验证码"
      maxlength="4"
      autocomplete="off"
    />
    <button type="button" class="captcha-img" title="点击刷新" @click="refresh">
      <img v-if="image" :src="image" alt="图形验证码" />
      <span v-else class="captcha-loading">加载中</span>
    </button>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { apiCaptcha } from '../api/auth'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'update:id'])

const code = ref(props.modelValue)
const image = ref('')
const captchaId = ref('')

watch(
  () => props.modelValue,
  (v) => {
    if (v !== code.value) code.value = v
  }
)
watch(code, (v) => emit('update:modelValue', v))

async function refresh() {
  try {
    const data = await apiCaptcha()
    captchaId.value = data.captchaId
    image.value = data.image
    emit('update:id', data.captchaId)
    code.value = ''
    emit('update:modelValue', '')
  } catch (e) {
    image.value = ''
  }
}

onMounted(refresh)
defineExpose({ refresh })
</script>

<style scoped>
.captcha-field {
  display: flex;
  gap: var(--sp-2);
  width: 100%;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-img {
  flex: none;
  width: 112px;
  height: 40px;
  padding: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--r-sm);
  background: var(--bg-subtle);
  cursor: pointer;
  overflow: hidden;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}
</style>
