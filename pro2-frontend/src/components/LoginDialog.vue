<template>
  <!-- 登录确认弹窗：游客触发写操作(点赞/收藏/发帖/评论/上传)时弹出，提供【去登录 / 取消】 -->
  <el-dialog
    v-model="authState.dialogVisible"
    width="420px"
    align-center
    :show-close="true"
    @close="closeLoginDialog"
  >
    <template #header>
      <div class="dlg-title">{{ mode === 'login' ? '登录 PulseTalk' : '注册 PulseTalk 账号' }}</div>
    </template>

    <el-tabs v-model="mode" stretch>
      <!-- 登录 -->
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
          <div class="tip-line">
            测试账号：admin / 123456（管理员）　test / 123456（普通用户）
          </div>
        </el-form>
      </el-tab-pane>

      <!-- 注册 -->
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
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue'
import { authState, closeLoginDialog, applyLogin } from '../store/user'
import { setBadges } from '../store/badge'
import { apiLogin, apiRegister } from '../api/auth'

const mode = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '' })

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

/** 登录 */
async function onLogin() {
  try {
    await loginFormRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    const data = await apiLogin(loginForm)
    setBadges(0, 0) // 切换账号后清空上一个账号的未读角标
    const action = applyLogin(data.user, data.tokenValue)
    ElMessage.success(`欢迎回来，${data.user.nickname || data.user.username}`)
    // 登录成功后继续用户原本想做的动作(如点赞)
    if (typeof action === 'function') {
      action()
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
.tip-line {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.8;
}
</style>
