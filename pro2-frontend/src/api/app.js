import request from './request'

/** App 版本 / 下载信息（公开） */
export const apiAppVersion = () => request.get('/app/version')
