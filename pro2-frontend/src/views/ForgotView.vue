<template>
  <div class="page-container forgot-page">
    <div class="forgot-card">
      <h1 class="forgot-title">找回密码</h1>
      <p class="forgot-desc">
        通过绑定邮箱接收验证码后重置密码。未绑定邮箱的账号无法自助找回，请联系管理员。
      </p>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="邮箱">
          <el-input
            v-model.trim="form.email"
            placeholder="name@example.com"
            maxlength="128"
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
            <el-input
              v-model.trim="form.code"
              placeholder="6 位数字"
              maxlength="6"
              autocomplete="one-time-code"
            />
            <el-button :disabled="!canSend" :loading="sending" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="新密码">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            maxlength="32"
            placeholder="6-32 位"
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item label="确认新密码">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            maxlength="32"
            placeholder="再次输入新密码"
            autocomplete="new-password"
          />
        </el-form-item>

        <el-button type="primary" class="submit" :loading="submitting" @click="submit">
          重置密码
        </el-button>

        <div class="forgot-foot">
          <router-link to="/">返回首页</router-link>
          <span class="dot">·</span>
          <button type="button" class="link-btn" @click="backLogin">返回登录</button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CaptchaField from '../components/CaptchaField.vue'
import { apiSendEmailCode, apiResetByCode } from '../api/auth'
import { resetAuth, openLoginDialog } from '../store/user'

const router = useRouter()
const captchaRef = ref()
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let timer = null

const form = reactive({
  email: '',
  captchaId: '',
  captchaCode: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
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
      scene: 'reset',
      captchaId: form.captchaId,
      captchaCode: form.captchaCode
    })
    ElMessage.success('如果该邮箱已绑定，验证码已发送')
    startCountdown()
    form.code = ''
    await captchaRef.value?.refresh()
  } catch (e) {
    // 错误由请求拦截器提示；图形码已被消费，刷新一张
    await captchaRef.value?.refresh()
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!emailValid.value) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  if (!/^\d{6}$/.test(form.code)) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }
  if (!form.newPassword || form.newPassword.length < 6 || form.newPassword.length > 32) {
    ElMessage.warning('新密码长度需在6-32位之间')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  submitting.value = true
  try {
    await apiResetByCode({
      email: form.email,
      code: form.code,
      newPassword: form.newPassword
    })
    resetAuth()
    ElMessage.success('密码已重置，请用新密码登录')
    openLoginDialog()
    router.push('/')
  } catch (e) {
    // 错误由拦截器提示
  } finally {
    submitting.value = false
  }
}

function backLogin() {
  openLoginDialog()
  router.push('/')
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.forgot-page {
  max-width: 520px;
  padding-top: var(--sp-10);
}

.forgot-card {
  padding: var(--sp-6);
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
}

.forgot-title {
  margin: 0 0 var(--sp-2);
  font-size: var(--fs-xl);
}

.forgot-desc {
  margin: 0 0 var(--sp-5);
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

.submit {
  width: 100%;
  margin-top: var(--sp-2);
}

.forgot-foot {
  margin-top: var(--sp-4);
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.forgot-foot .dot {
  margin: 0 var(--sp-1);
}

.link-btn {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-link);
  font-family: inherit;
  font-size: inherit;
  cursor: pointer;
}

@media (max-width: 639px) {
  .forgot-page {
    padding-top: var(--sp-4);
    padding-left: var(--sp-3);
    padding-right: var(--sp-3);
  }

  .forgot-card {
    padding: var(--sp-4);
  }

  .forgot-title {
    font-size: var(--fs-lg);
  }
}
</style>
