import { reactive, computed } from 'vue'
import {
  getToken, setToken, getStoredUser, setStoredUser, clearAuth
} from '../utils/auth'

/**
 * 全局登录态 store(轻量响应式单例，无需 Pinia)
 *
 * 职责：
 *  - 维护当前登录用户(含 role)
 *  - 控制“登录确认弹窗”(游客点击需登录操作时打开)
 *  - 登录成功后的待办回调(如登录后继续上一次点赞操作)
 */
export const authState = reactive({
  user: getStoredUser(),      // 当前登录用户(含 role)，刷新页面从 localStorage 恢复
  dialogVisible: false,       // 登录确认弹窗是否可见
  pendingAction: null         // 登录成功后的待执行回调
})

export const isLoggedIn = computed(() => !!getToken() && !!authState.user)

/** 打开登录确认弹窗(可传入登录成功后要执行的动作) */
export function openLoginDialog(action = null) {
  authState.pendingAction = action
  authState.dialogVisible = true
}

/** 关闭登录弹窗 */
export function closeLoginDialog() {
  authState.dialogVisible = false
  authState.pendingAction = null
}

/** 登录/注册成功后写入登录态 */
export function applyLogin(user, token) {
  setToken(token)
  setStoredUser(user)
  authState.user = user
  authState.dialogVisible = false
  const action = authState.pendingAction
  authState.pendingAction = null
  return action
}

/** 退出登录 / 登录态失效 */
export function resetAuth() {
  clearAuth()
  authState.user = null
  authState.pendingAction = null
}

/** 刷新本地用户信息(头像上传等场景) */
export function refreshUser(user) {
  authState.user = user
  setStoredUser(user)
}
