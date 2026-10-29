<template>
  <el-dialog
    v-model="visible"
    :title="isBind ? '绑定邮箱' : '更换邮箱'"
    width="440"
    align-center
    class="email-dialog"
  >
    <p class="email-tip">
      {{ isBind
        ? '绑定后可用邮箱验证码找回密码；未绑定邮箱的账号无法自助找回。'
        : '换绑需要当前密码 + 新邮箱验证码，旧邮箱会收到安全通知。' }}
    </p>

    <el-form label-position="top" @submit.prevent>
      <el-form-item label="当前密码">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          maxlength="32"
          placeholder="请输入当前密码"
          autocomplete="current-password"
        />
      </el-form-item>

      <el-form-item :label="isBind ? '邮箱' : '新邮箱'">
        <el-input
          v-model.trim="form.email"
          maxlength="128"
          placeholder="name@example.com"
          autocomplete="email"
        />
      </el-form-item>

      <el-form-item label="图形验证码">
        <CaptchaField
          ref="captchaRef"
          v-model="form.captchaCode"
          @update:id="form.captchaId = $event"
        />
      </el-form-item>

      <el-form-item label="邮箱验证码">
        <div class="code-row">
          <el-input v-model.trim="form.code" maxlength="6" placeholder="6 位数字" />
          <el-button :disabled="!canSend" :loading="sending" @click="sendCode">
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ isBind ? '绑定' : '确认更换' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import CaptchaField from './CaptchaField.vue'
import { apiSendEmailCode, apiBindEmail, apiRebindEmail } from '../api/auth'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'bind' }
})
const emit = defineEmits(['update:modelValue', 'success'])

const captchaRef = ref()
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let timer = null

const form = reactive({
  password: '',
  email: '',
  captchaId: '',
  captchaCode: '',
  code: ''
})

const isBind = computed(() => props.mode === 'bind')
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
const canSend = computed(
  () =>
    emailValid.value &&
    form.captchaCode.length === 4 &&
    !!form.captchaId &&
    countdown.value === 0 &&
    !sending.value
)

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    form.password = ''
    form.email = ''
    form.code = ''
    form.captchaCode = ''
    form.captchaId = ''
    countdown.value = 0
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    await nextTick()
    captchaRef.value?.refresh()
  }
)

function startCountdown() {
  countdown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
      countdown.value = 0
    }
  }, 1000)
}

async function sendCode() {
  if (!canSend.value) return
  sending.value = true
  try {
    await apiSendEmailCode({
      email: form.email,
      scene: isBind.value ? 'bind' : 'rebind',
      captchaId: form.captchaId,
      captchaCode: form.captchaCode
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown()
    form.code = ''
    await captchaRef.value?.refresh()
  } catch (e) {
    await captchaRef.value?.refresh()
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!form.password) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!emailValid.value) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  if (!/^\d{6}$/.test(form.code)) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }

  submitting.value = true
  try {
    if (isBind.value) {
      await apiBindEmail({ email: form.email, code: form.code, password: form.password })
      ElMessage.success('邮箱绑定成功')
    } else {
      await apiRebindEmail({
        newEmail: form.email,
        code: form.code,
        password: form.password
      })
      ElMessage.success('邮箱已更换')
    }
    emit('success')
    visible.value = false
  } catch (e) {
    // 错误由拦截器提示
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.email-tip {
  margin: 0 0 var(--sp-3);
  font-size: var(--fs-xs);
  line-height: var(--lh-normal);
  color: var(--text-secondary);
}

.code-row {
  display: flex;
  gap: var(--sp-2);
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.code-row .el-button {
  flex: none;
  min-width: 108px;
}
</style>
