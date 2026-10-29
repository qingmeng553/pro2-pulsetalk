import request from './request'

/** 管理端接口(仅 ADMIN，后端 @SaCheckRole("admin") 兜底鉴权) */

// GET /admin/user/list 用户表分页(支持 keyword 账号/昵称搜索)
export const apiAdminUserList = (params) => request.get('/admin/user/list', { params })

// POST /admin/user/{id}/ban 封禁用户
export const apiAdminUserBan = (id) => request.post(`/admin/user/${id}/ban`)

// POST /admin/user/{id}/unban 解封用户
export const apiAdminUserUnban = (id) => request.post(`/admin/user/${id}/unban`)

// DELETE /admin/user/{id} 删除用户(逻辑删除)
export const apiAdminUserDelete = (id) => request.delete(`/admin/user/${id}`)

// POST /admin/user/{id}/reset-password 重置用户密码（会踢出该用户所有会话）
export const apiAdminResetPassword = (id, newPassword) =>
  request.post(`/admin/user/${id}/reset-password`, { newPassword })
