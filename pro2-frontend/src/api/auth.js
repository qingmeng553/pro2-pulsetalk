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

// ==================== v3 账号自助 ====================

// PUT /auth/profile 修改昵称(改名)
export const apiUpdateProfile = (nickname) => request.put('/auth/profile', { nickname })

// PUT /auth/password 修改密码(校验原密码)
export const apiUpdatePassword = (oldPassword, newPassword) =>
  request.put('/auth/password', { oldPassword, newPassword })

// GET /auth/security-question 查询密保问题状态
export const apiSecurityQuestion = () => request.get('/auth/security-question')

// POST /auth/security-question 设置密保问题(问题/答案均为用户自定义，仅可设置一次)
export const apiSetSecurityQuestion = (question, answer) =>
  request.post('/auth/security-question', { question, answer })

// POST /auth/forgot/question 忘记密码-查询密保问题(公开)
export const apiForgotQuestion = (username) => request.post('/auth/forgot/question', { username })

// POST /auth/forgot/reset 忘记密码-校验答案并重置密码(公开)
export const apiForgotReset = (username, answer, newPassword) =>
  request.post('/auth/forgot/reset', { username, answer, newPassword })
