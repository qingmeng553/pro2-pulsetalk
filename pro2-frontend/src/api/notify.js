import request from './request'

/** 站内消息(通知)接口 */

// GET /notify/list 通知分页(需登录)
export const apiNotifyList = (params) => request.get('/notify/list', { params })

// GET /notify/unread-count 未读通知数
export const apiNotifyUnread = () => request.get('/notify/unread-count')

// POST /notify/read 标记已读(ids 为空 = 全部)
export const apiNotifyRead = (ids = []) => request.post('/notify/read', { ids })

// POST /notify/broadcast 管理员向全体用户发送官方消息(需 admin)
export const apiNotifyBroadcast = (content) => request.post('/notify/broadcast', { content })
