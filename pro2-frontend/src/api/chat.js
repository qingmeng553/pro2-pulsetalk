import request from './request'

/** 私聊接口(全部需登录) */

// GET /chat/threads 会话列表
export const apiChatThreads = () => request.get('/chat/threads')

// GET /chat/messages/{userId} 消息(分页正序；afterId 增量轮询)
export const apiChatMessages = (userId, params) =>
  request.get(`/chat/messages/${userId}`, { params })

// POST /chat/send/{userId} 发送消息
export const apiChatSend = (userId, content) =>
  request.post(`/chat/send/${userId}`, { content })

// POST /chat/read/{userId} 会话标记已读
export const apiChatRead = (userId) => request.post(`/chat/read/${userId}`)

// GET /chat/unread-count 全部未读私信数
export const apiChatUnread = () => request.get('/chat/unread-count')
