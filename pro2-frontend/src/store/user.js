import { reactive, computed } from 'vue'
import { getToken, setToken, getStoredUser, setStoredUser, clearAuth } from '../utils/auth'

// 全局登录态：轻量响应式单例
export const authState = reactive({
  user: getStoredUser(),
  dialogVisible: false,
  pendingAction: null,
  version: 0
})

// 是否已登录：以用户信息为准（token 由请求拦截器读取）
export const isLoggedIn = computed(() => !!authState.user)

// 打开登录弹窗（可带登录后要执行的动作）
export function openLoginDialog(action = null) {
  authState.pendingAction = action
  authState.dialogVisible = true
}

export function closeLoginDialog() {
  authState.dialogVisible = false
  authState.pendingAction = null
}

// 写入登录态：token + 用户 + 关闭弹窗，返回待执行动作
export function applyLogin(user, token) {
  setToken(token)
  setStoredUser(user)
  authState.user = user ? { ...user } : null
  authState.dialogVisible = false
  authState.version++
  const action = authState.pendingAction
  authState.pendingAction = null
  return action
}

export function resetAuth() {
  clearAuth()
  authState.user = null
  authState.pendingAction = null
  authState.version++
}

// 刷新用户信息（头像、昵称、角色、密保/封禁状态）
export function refreshUser(user) {
  authState.user = user ? { ...user } : null
  if (user) setStoredUser(user)
  authState.version++
}

// 有 token 但没有用户信息时（如仅本地存了 token），标记待水合
export function needsHydrate() {
  return !!getToken() && !authState.user
}
