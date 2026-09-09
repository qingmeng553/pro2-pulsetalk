import request from './request'

/** 用户主页 + 好友 + 黑名单 接口 */

// GET /user/{id} 用户公开主页(游客可访问)
export const apiUserProfile = (id) => request.get(`/user/${id}`)

// GET /post/list?userId= 他人帖子(个人主页“TA的帖子”)
export const apiUserPosts = (userId, params) =>
  request.get('/post/list', { params: { ...params, userId } })

// ---- 好友 ----
export const apiFriendRequest = (userId) => request.post(`/friend/request/${userId}`)
export const apiFriendAccept = (userId) => request.post(`/friend/accept/${userId}`)
export const apiFriendReject = (userId) => request.post(`/friend/reject/${userId}`)
export const apiFriendRemove = (userId) => request.post(`/friend/remove/${userId}`)
export const apiFriendList = () => request.get('/friend/list')
export const apiFriendPendingList = () => request.get('/friend/pending/list')

// ---- 黑名单 ----
export const apiBlacklistAdd = (userId) => request.post(`/blacklist/add/${userId}`)
export const apiBlacklistRemove = (userId) => request.post(`/blacklist/remove/${userId}`)
export const apiBlacklistList = () => request.get('/blacklist/list')
