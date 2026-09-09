// Sa-Token 令牌与用户信息的本地存取
// token 存入 localStorage，请求时由 axios 拦截器自动放入请求头 satoken

const TOKEN_KEY = 'satoken'
const USER_KEY = 'community_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export function setStoredUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeStoredUser() {
  localStorage.removeItem(USER_KEY)
}

/** 清除全部本地登录态 */
export function clearAuth() {
  removeToken()
  removeStoredUser()
}

export function isLoggedIn() {
  return !!getToken()
}
