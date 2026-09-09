import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '../utils/auth'
import { resetAuth, openLoginDialog } from '../store/user'

/**
 * axios 实例
 *
 * 统一约定(与后端 R<T> 对应)：
 *  - 后端 HTTP 状态恒为 200，业务结果由响应体 code 表达
 *  - code === 200  -> 直接返回 data
 *  - code === 401  -> 未登录/登录失效：清空本地态并弹出【去登录/取消】确认弹窗(不出现403空白页)
 *  - code === 403  -> 无权限(如普通用户点管理员按钮)
 *  - code === 429  -> 接口限流
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 请求拦截：自动携带 sa-token
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

// 响应拦截：解包 R<T>
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      // 登录态失效：弹出登录确认弹窗
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
