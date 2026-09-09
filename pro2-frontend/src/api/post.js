import request from './request'

/** 帖子相关接口 */

// GET /post/category/list 获取全部帖子分类(游客可访问)
export const apiCategoryList = () => request.get('/post/category/list')

// GET /post/list 帖子分页列表(游客可访问)
export const apiPostList = (params) => request.get('/post/list', { params })

// GET /post/{id} 帖子详情(游客可访问；浏览量 Redis 自增)
export const apiPostDetail = (id) => request.get(`/post/${id}`)

// POST /post 新建帖子(需登录)
export const apiCreatePost = (data) => request.post('/post', data)

// PUT /post/{id} 修改自己的帖子(需登录)
export const apiUpdatePost = (id, data) => request.put(`/post/${id}`, data)

// DELETE /post/{id} 删除自己的帖子(需登录)
export const apiDeletePost = (id) => request.delete(`/post/${id}`)

// DELETE /post/admin/{id} 管理员软删除任意帖子
export const apiAdminDeletePost = (id) => request.delete(`/post/admin/${id}`)

// POST /post/{id}/like 点赞/取消点赞(需登录)
export const apiLikePost = (id) => request.post(`/post/${id}/like`)

// POST /post/{id}/collect 收藏/取消收藏(需登录)
export const apiCollectPost = (id) => request.post(`/post/${id}/collect`)

// GET /post/hot/rank 热度榜单(游客可访问)
export const apiHotRank = (topN = 10) => request.get('/post/hot/rank', { params: { topN } })

// GET /post/pinned 当前置顶规则帖(游客可访问)
export const apiPinnedPost = () => request.get('/post/pinned')

// PUT /post/admin/pin/{id} 管理员设为置顶规则帖(需 admin)
export const apiPinPost = (id) => request.put(`/post/admin/pin/${id}`)

// DELETE /post/admin/pin 管理员取消置顶(需 admin)
export const apiUnpinPost = () => request.delete('/post/admin/pin')

// POST /post/image 上传帖子配图(需登录，form-data: file)
export const apiUploadPostImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/post/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
