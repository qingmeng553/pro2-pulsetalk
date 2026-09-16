import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '../utils/auth'
import { resetAuth, openLoginDialog } from '../store/user'

// axios 实例：后端统一 HTTP 200，业务结果看响应体 code

const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 自动携带 sa-token
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers['satoken'] = token
  }
  return config
})

/** 依据业务 code 分类提示 */
function notifyByCode(code, msg) {
  if (code === 401) return // 未登录统一走登录弹窗，不额外弹错误
  if (code === 403) ElMessage.warning(msg || '没有权限执行该操作')
  else if (code === 429) ElMessage.warning(msg || '操作太频繁啦，请稍后再试')
  else ElMessage.error(msg || '请求失败，请稍后再试')
}

// 解包响应体
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      // 登录态失效弹登录框
      if (body.code === 401) {
        resetAuth()
        openLoginDialog()
      }
      notifyByCode(body.code, body.message)
      return Promise.reject({ code: body.code, message: body.message })
    }
    return body
  },
  (error) => {
    const status = error.response && error.response.status
    const body = error.response && error.response.data
    if (status === 401 || (body && body.code === 401)) {
      resetAuth()
      openLoginDialog()
    }
    notifyByCode(status || (body && body.code), (body && body.message) || error.message)
    return Promise.reject(error)
  }
)

export default request
