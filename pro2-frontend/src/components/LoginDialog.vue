<template>
  <!-- 登录 / 注册弹窗：游客触发写操作时弹出；桌面居中弹窗，移动端底部抽屉 -->
  <el-dialog
    v-model="authState.dialogVisible"
    class="pt-auth"
    width="420"
    align-center
    :show-close="true"
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    @close="onClose"
    @opened="onOpened"
  >
    <template #header>
      <h2 class="pt-auth__title">{{ isRegister ? '注册 PulseTalk' : '登录 PulseTalk' }}</h2>
    </template>

    <!-- 分段切换：登录 / 注册 -->
    <div class="pt-auth__seg" role="group" aria-label="登录或注册">
      <button
        type="button"
        class="pt-auth__seg-btn"
        :class="{ active: !isRegister }"
        :aria-pressed="!isRegister"
        @click="switchMode('login')"
      >
        登录
      </button>
      <button
        type="button"
        class="pt-auth__seg-btn"
        :class="{ active: isRegister }"
        :aria-pressed="isRegister"
        @click="switchMode('register')"
      >
        注册
      </button>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :show-message="false"
      size="large"
      @submit.prevent
      @keyup.enter="onSubmit"
    >
      <el-form-item prop="username">
        <el-input
          ref="usernameRef"
          v-model.trim="form.username"
          placeholder="用户名"
          :prefix-icon="User"
          autocomplete="username"
          maxlength="20"
          clearable
          @input="clearError"
          @blur="checkField('username')"
        />
      </el-form-item>

      <el-form-item v-if="isRegister" prop="nickname">
        <el-input
          v-model.trim="form.nickname"
          placeholder="昵称（选填）"
          :prefix-icon="Postcard"
          maxlength="20"
          clearable
          @input="clearError"
        />
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="密码"
          :prefix-icon="Lock"
          :autocomplete="isRegister ? 'new-password' : 'current-password'"
          maxlength="32"
          @input="clearError"
          @blur="checkField('password')"
        />
      </el-form-item>

      <!-- 错误提示：固定预留一行高度，出现/消失都不撑动按钮 -->
      <p class="pt-auth__error" role="alert" aria-live="polite">{{ errorText }}</p>

      <el-button
        type="primary"
        class="pt-auth__submit"
        size="large"
        :loading="loading"
        @click="onSubmit"
      >
        {{ isRegister ? '注册' : '登录' }}
      </el-button>
    </el-form>

    <p v-if="isRegister" class="pt-auth__hint">注册成功后自动登录</p>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue'
import { authState, applyLogin, refreshUser } from '../store/user'
import { setBadges } from '../store/badge'
import { apiLogin, apiRegister, apiCurrentUser } from '../api/auth'

const isRegister = ref(false)
const loading = ref(false)
const errorText = ref('')
const formRef = ref()
const usernameRef = ref()

const form = reactive({ username: '', nickname: '', password: '' })

// 规则与后端 RegisterDTO 保持一致：3-20 位字母/数字/下划线、密码 6-32 位
const rules = computed(() => {
  const username = [{ required: true, message: '请输入用户名', trigger: 'blur' }]
  const password = [{ required: true, message: '请输入密码', trigger: 'blur' }]
  if (isRegister.value) {
    username.push({
      pattern: /^[a-zA-Z0-9_]{3,20}$/,
      message: '用户名需为3-20位字母/数字/下划线',
      trigger: 'blur'
    })
    password.push({ min: 6, max: 32, message: '密码长度需在6-32位之间', trigger: 'blur' })
  }
  return { username, password }
})

/** 取第一条校验错误文案（Element 的 validate 会抛字段对象） */
function firstError(e) {
  if (!e) return '请检查输入内容'
  if (typeof e === 'string') return e
  if (Array.isArray(e)) return firstError(e[0])
  if (e.message) return e.message
  const first = Object.values(e)[0]
  return firstError(first) || '请检查输入内容'
}

/** 请求错误 -> 人话；后端 message 已经是中文就直接用 */
function messageOf(e, fallback) {
  const raw = e && typeof e === 'object' && typeof e.message === 'string' ? e.message : ''
  if (!raw) return fallback
  // axios / 代理层的技术文案不直接给用户看
  if (/^request failed with status code|network\s*error|timeout|failed to fetch/i.test(raw)) {
    return fallback
  }
  return raw
}

function clearError() {
  if (errorText.value) errorText.value = ''
}

/** 失焦即时校验：结果落在固定错误行 */
function checkField(prop) {
  const f = formRef.value
  if (!f) return
  f.validateField(prop).catch((e) => {
    errorText.value = firstError(e)
  })
}

function switchMode(mode) {
  const next = mode === 'register'
  if (next === isRegister.value) return
  isRegister.value = next
  errorText.value = ''
  nextTick(() => {
    formRef.value?.clearValidate()
    usernameRef.value?.focus()
  })
}

/** 打开动画结束后：桌面自动聚焦用户名（移动端不主动弹键盘） */
function onOpened() {
  if (window.matchMedia('(min-width: 640px)').matches) {
    usernameRef.value?.focus()
  }
}

/** 关闭时复位敏感字段与状态 */
function onClose() {
  authState.dialogVisible = false
  // 未登录时用户主动关闭：清掉待办，避免残留旧闭包
  // 已登录时（applyLogin 触发的关闭）保留，防止把守卫记住的目标页面清掉
  if (!authState.user) {
    authState.pendingAction = null
  }
  loading.value = false
  errorText.value = ''
  form.password = ''
  isRegister.value = false
}

async function onSubmit() {
  if (loading.value) return
  loading.value = true
  errorText.value = ''

  try {
    await formRef.value.validate()
  } catch (e) {
    errorText.value = firstError(e)
    loading.value = false
    return
  }

  try {
    const data = isRegister.value ? await registerThenLogin() : await login()
    await afterAuth(data)
    ElMessage.success(
      isRegister.value
        ? '注册成功，已自动登录'
        : `欢迎回来，${data.user.nickname || data.user.username}`
    )
  } catch (e) {
    errorText.value = messageOf(e, isRegister.value ? '注册失败，请稍后再试' : '登录失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

// 登录 / 注册都静默请求：错误由弹窗内固定错误行展示，不再叠全屏 toast
function login() {
  return apiLogin({ username: form.username, password: form.password }, { silent: true })
}

async function registerThenLogin() {
  await apiRegister(
    { username: form.username, nickname: form.nickname, password: form.password },
    { silent: true }
  )
  return login()
}

/** 登录成功统一处理：写状态 → 校准用户 → 执行待办 */
async function afterAuth(data) {
  setBadges(0, 0)
  const pendingAction = applyLogin(data.user, data.tokenValue)
  try {
    const fresh = await apiCurrentUser()
    refreshUser(fresh)
  } catch (err) {
    // 校准失败时保留登录返回值，避免界面退回未登录
    applyLogin(data.user, data.tokenValue)
  }
  if (typeof pendingAction === 'function') {
    pendingAction()
  }
}
</script>

<style scoped>
.pt-auth__title {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  line-height: var(--lh-tight);
  color: var(--text-primary);
}

.pt-auth__seg {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--sp-1);
  padding: var(--sp-1);
  margin-bottom: var(--sp-5);
  background: var(--c-100);
  border-radius: var(--r-sm);
}

.pt-auth__seg-btn {
  height: 32px;
  padding: 0;
  border: 0;
  background: transparent;
  border-radius: var(--r-sm);
  color: var(--text-secondary);
  font-family: inherit;
  font-size: var(--fs-sm);
  font-weight: var(--fw-medium);
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease-out),
    background-color var(--dur-fast) var(--ease-out);
}

.pt-auth__seg-btn:hover {
  color: var(--text-primary);
}

.pt-auth__seg-btn.active {
  background: var(--bg-elevated);
  color: var(--c-primary);
  font-weight: var(--fw-bold);
}

.pt-auth__error {
  min-height: 20px;
  margin: 0 0 var(--sp-2);
  font-size: var(--fs-xs);
  line-height: 20px;
  color: var(--c-danger);
}

.pt-auth__submit {
  width: 100%;
  margin-top: var(--sp-1);
}

.pt-auth__hint {
  margin: var(--sp-3) 0 0;
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}
</style>

<style>
/* 弹窗外壳：Element Plus 会把 dialog teleport 到 body，只能用全局样式，前缀限定 .pt-auth */
.pt-auth.el-dialog {
  padding: 0;
}

.pt-auth .el-dialog__header {
  padding: var(--sp-5) var(--sp-6) 0;
  margin: 0;
}

.pt-auth .el-dialog__body {
  padding: var(--sp-4) var(--sp-6) var(--sp-6);
}

.pt-auth .el-dialog__headerbtn {
  top: var(--sp-4);
  right: var(--sp-4);
  width: 32px;
  height: 32px;
}

.pt-auth .el-dialog__headerbtn .el-dialog__close {
  color: var(--text-secondary);
}

.pt-auth .el-form-item {
  margin-bottom: var(--sp-3);
}

/* 移动端（< 640）：底部抽屉形态 + iOS 安全区 */
@media (max-width: 639px) {
  .pt-auth.el-dialog {
    position: fixed;
    top: auto;
    right: 0;
    bottom: 0;
    left: 0;
    width: 100% !important;
    max-width: 100%;
    max-height: 90vh;
    margin: 0 !important;
    overflow-y: auto;
    border-radius: var(--r-md) var(--r-md) 0 0;
  }

  .pt-auth .el-dialog__header {
    padding: var(--sp-5) var(--sp-5) 0;
  }

  .pt-auth .el-dialog__body {
    padding: var(--sp-4) var(--sp-5) calc(var(--sp-6) + env(safe-area-inset-bottom));
  }
}
</style>
