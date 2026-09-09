import request from './request'

/** 用户认证相关接口(返回体已被拦截器解包为 data) */

// POST /auth/register 用户注册
export const apiRegister = (data) => request.post('/auth/register', data)

// POST /auth/login 登录，返回 sa-token 与用户信息
export const apiLogin = (data) => request.post('/auth/login', data)

// POST /auth/logout 登出(需token)
export const apiLogout = () => request.post('/auth/logout')

// GET /auth/current-user 获取当前登录用户(返回 role)
export const apiCurrentUser = () => request.get('/auth/current-user')

// POST /auth/upload-avatar 上传头像(需登录，form-data: file)
export const apiUploadAvatar = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/auth/upload-avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
