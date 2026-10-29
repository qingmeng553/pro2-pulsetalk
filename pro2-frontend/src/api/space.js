import request from './request'

/** 可配置主页接口 */

export const apiSpace = (id) => request.get(`/user/${id}/space`)

export const apiSaveSpace = (layout, version) =>
  request.put('/user/profile/space', { layout, version })

export const apiUploadSpaceImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/user/profile/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const apiGuestbook = (id, params) =>
  request.get(`/user/${id}/guestbook`, { params })

export const apiAddGuestbook = (id, content) =>
  request.post(`/user/${id}/guestbook`, { content })

export const apiDeleteGuestbook = (id) => request.delete(`/user/guestbook/${id}`)

export const apiReportSpace = (id, moduleType, reason) =>
  request.post(`/user/${id}/report`, { moduleType, reason })

export const apiAdminReports = (params) => request.get('/admin/space/reports', { params })

export const apiAdminHandleReport = (id) => request.post(`/admin/space/reports/${id}/handle`)

export const apiAdminHideModule = (id, data) =>
  request.post(`/admin/user/${id}/space/hide`, data)
