import request from './request'

/** 用户认证相关接口(返回体已被拦截器解包为 data) */

// POST /auth/register 用户注册（config 可传 { silent: true }，错误由弹窗内联展示）
export const apiRegister = (data, config) => request.post('/auth/register', data, config)

// POST /auth/login 登录，返回 sa-token 与用户信息
export const apiLogin = (data, config) => request.post('/auth/login', data, config)

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

// ==================== alpha2：邮箱绑定 / 找回 ====================

// GET /auth/captcha 图形验证码(返回 captchaId + dataURL)
export const apiCaptcha = () => request.get('/auth/captcha')

// POST /auth/email/send-code 发送邮箱验证码(scene=bind|rebind|reset)
export const apiSendEmailCode = (data) => request.post('/auth/email/send-code', data)

// POST /auth/email/bind 首次绑定邮箱
export const apiBindEmail = (data) => request.post('/auth/email/bind', data)

// POST /auth/email/rebind 换绑邮箱
export const apiRebindEmail = (data) => request.post('/auth/email/rebind', data)

// POST /auth/forgot/reset-by-code 邮箱验证码重置密码
export const apiResetByCode = (data) => request.post('/auth/forgot/reset-by-code', data)
