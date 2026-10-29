// 图片 / 静态资源地址解析
// 后端统一存相对路径 /files/...；历史数据里可能是 http(s) 绝对地址，原样返回。
// App(Capacitor) 壳里相对路径会指向本地，需要拼站点 base（构建时注入 VITE_APP_BASE）。
const APP_BASE = String(import.meta.env.VITE_APP_BASE || '').replace(/\/$/, '')

/** App 内使用的站点源（构建时注入；Web 为空） */
export const APP_ORIGIN = APP_BASE

export function isApp() {
  const cap = window.Capacitor
  return !!(cap && typeof cap.isNativePlatform === 'function' && cap.isNativePlatform())
}

/**
 * 把后端返回的资源地址转成当前端可直接用的地址
 * - http(s) / data / blob：原样返回（旧数据兼容）
 * - Web：相对路径原样返回（由 Nginx 或 Vite 的 /files 反代处理）
 * - App：拼上 VITE_APP_BASE
 */
export function resolveAssetUrl(url) {
  if (!url) return ''
  const value = String(url)
  if (/^(https?:|data:|blob:)/i.test(value)) return value
  if (isApp() && APP_BASE && value.startsWith('/')) return `${APP_BASE}${value}`
  return value
}

export function resolveAssetList(list) {
  return Array.isArray(list) ? list.map(resolveAssetUrl) : []
}
