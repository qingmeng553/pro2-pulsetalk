import request from './request'

/** 评论相关接口 */

// GET /post/{postId}/comment/list 评论分页列表(游客可访问)
export const apiCommentList = (postId, params) =>
  request.get(`/post/${postId}/comment/list`, { params })

// POST /post/{postId}/comment 发表评论(需登录)
export const apiCreateComment = (postId, data) =>
  request.post(`/post/${postId}/comment`, data)

// DELETE /comment/{id} 删除自己的评论(需登录)
export const apiDeleteComment = (id) => request.delete(`/comment/${id}`)

// DELETE /comment/admin/{id} 管理员软删除任意评论
export const apiAdminDeleteComment = (id) => request.delete(`/comment/admin/${id}`)
