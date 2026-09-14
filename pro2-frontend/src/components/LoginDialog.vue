<template>
  <!-- 登录/注册/忘记密码 弹窗：游客触发写操作(点赞/收藏/发帖/评论/上传)时弹出 -->
  <el-dialog
    v-model="authState.dialogVisible"
    width="420px"
    align-center
    :show-close="true"
    @close="onClose"
  >
    <template #header>
      <div class="dlg-title">
        {{ mode === 'login' ? '登录 PulseTalk' : mode === 'register' ? '注册 PulseTalk 账号' : '找回密码' }}
      </div>
    </template>

    <!-- ============ 登录 / 注册 ============ -->
    <el-tabs v-if="mode !== 'forgot'" v-model="mode" stretch>
      <el-tab-pane label="登录" name="login">
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large" @keyup.enter="onLogin">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" class="submit-btn" size="large" :loading="loading" @click="onLogin">
            登 录
          </el-button>
          <div class="forgot-line">
            <el-link type="primary" :underline="false" @click="toForgot">忘记密码？</el-link>
          </div>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="注册" name="register">
        <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large" @keyup.enter="onRegister">
          <el-form-item prop="username">
            <el-input v-model="registerForm.username" placeholder="用户名(3-20位字母数字下划线)" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="nickname">
            <el-input v-model="registerForm.nickname" placeholder="昵称(选填)" :prefix-icon="Postcard" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="registerForm.password" type="password" placeholder="密码(6-32位)" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" class="submit-btn" size="large" :loading="loading" @click="onRegister">
            注 册
          </el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <!-- ============ 忘记密码：通过个人密保问题答案找回 ============ -->
    <div v-else class="forgot-pane">
      <!-- 第一步：用户名 → 取密保问题 -->
      <el-form v-if="!forgotQuestion" :model="forgotForm" size="large" @keyup.enter="loadForgotQuestion">
        <p class="forgot-tip">输入账号后，通过你设置的<b>个人密保问题</b>答案验证身份，即可重置密码。</p>
        <el-form-item>
          <el-input v-model="forgotForm.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-button type="primary" class="submit-btn" size="large" :loading="loading" @click="loadForgotQuestion">
          下一步
        </el-button>
        <div class="forgot-line">
          <el-link :underline="false" @click="mode = 'login'">返回登录</el-link>
        </div>
      </el-form>

      <!-- 第二步：回答问题 + 设置新密码 -->
      <el-form v-else :model="forgotForm" size="large" @keyup.enter="onForgotReset">
        <el-form-item label="密保问题">
          <div class="question-box">{{ forgotQuestion }}</div>
        </el-form-item>
        <el-form-item>
          <el-input v-model="forgotForm.answer" placeholder="请输入密保答案(不区分大小写)" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="forgotForm.newPassword" type="password" placeholder="设置新密码(6-32位)" show-password />
        </el-form-item>
        <el-form-item>
          <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="确认新密码" show-password />
        </el-form-item>
        <el-button type="primary" class="submit-btn" size="large" :loading="loading" @click="onForgotReset">
          重置密码
        </el-button>
        <div class="forgot-line">
          <el-link :underline="false" @click="forgotQuestion = ''">换个账号</el-link>
          <el-link :underline="false" class="ml" @click="mode = 'login'">返回登录</el-link>
        </div>
      </el-form>
    </div>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue'
import { authState, applyLogin, refreshUser } from '../store/user'
import { setBadges } from '../store/badge'
import {
  apiLogin, apiRegister, apiCurrentUser,
  apiForgotQuestion, apiForgotReset
} from '../api/auth'

const mode = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '' })

// 忘记密码
const forgotForm = reactive({ username: '', answer: '', newPassword: '', confirmPassword: '' })
const forgotQuestion = ref('')

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '3-20位字母/数字/下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度6-32位', trigger: 'blur' }
  ]
}

/** 关闭弹窗时复位 */
function onClose() {
  authState.dialogVisible = false
  authState.pendingAction = null
}

/** 切到忘记密码 */
function toForgot() {
  forgotQuestion.value = ''
  forgotForm.username = loginForm.username || ''
  forgotForm.answer = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
  mode.value = 'forgot'
}

/** 登录 */
async function onLogin() {
  try {
    await loginFormRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    // 1) 登录接口：拿到 token + 用户信息
    const data = await apiLogin(loginForm)
    setBadges(0, 0) // 切换账号后清空上一个账号的未读角标

    // 2) 立即写入全局响应式仓库(token + 用户/角色)，并取出“登录后待办动作”
    const pendingAction = applyLogin(data.user, data.tokenValue)

    // 3) 兜底：拉取当前登录用户权威信息刷新仓库(角色/封禁/密保状态以服务端为准)
    try {
      const fresh = await apiCurrentUser()
      refreshUser(fresh)
    } catch (err) {
      // getInfo 偶发失败不影响登录(已用登录返回值渲染)，静默即可
    }
    ElMessage.success(`欢迎回来，${data.user.nickname || data.user.username}`)

    // 4) 状态就绪后再执行路由跳转/待办动作(如登录后继续点赞)
    if (typeof pendingAction === 'function') {
      pendingAction()
    }
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

/** 注册 */
async function onRegister() {
  try {
    await registerFormRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await apiRegister(registerForm)
    ElMessage.success('注册成功，请登录')
    loginForm.username = registerForm.username
    mode.value = 'login'
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

/** 忘记密码第一步：查询密保问题 */
async function loadForgotQuestion() {
  const username = (forgotForm.username || '').trim()
  if (!username) {
    ElMessage.warning('请输入用户名')
    return
  }
  loading.value = true
  try {
    const data = await apiForgotQuestion(username)
    if (!data || !data.hasQuestion) {
      ElMessage.warning('该账号未设置密保问题，暂时无法自助找回，请联系管理员')
      return
    }
    forgotQuestion.value = data.question
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

/** 忘记密码第二步：校验答案并重置密码 */
async function onForgotReset() {
  const { username, answer, newPassword, confirmPassword } = forgotForm
  if (!answer || !answer.trim()) {
    ElMessage.warning('请输入密保答案')
    return
  }
  if (!newPassword || newPassword.length < 6 || newPassword.length > 32) {
    ElMessage.warning('新密码长度需在6-32位之间')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  loading.value = true
  try {
    await apiForgotReset(username.trim(), answer, newPassword)
    ElMessage.success('密码已重置，请用新密码登录')
    loginForm.username = username.trim()
    loginForm.password = ''
    forgotQuestion.value = ''
    mode.value = 'login'
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.dlg-title {
  font-weight: 800;
  font-size: 17px;
}
.submit-btn {
  width: 100%;
  margin-top: 4px;
}
.forgot-line {
  margin-top: 12px;
  text-align: center;
  font-size: 13px;
}
.forgot-pane {
  padding-top: 4px;
}
.forgot-tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.7;
}
.question-box {
  width: 100%;
  background: #f4f6fb;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 9px 12px;
  font-size: 14px;
  color: var(--text);
  line-height: 1.6;
}
.ml {
  margin-left: 12px;
}
</style>
