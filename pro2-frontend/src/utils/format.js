// 时间格式化工具
export function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 相对时间(用于首页卡片“x分钟前”) */
export function fromNow(value) {
  if (!value) return ''
  const diff = Date.now() - new Date(value).getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 3600 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400 * 1000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 7 * 86400 * 1000) return `${Math.floor(diff / 86400000)} 天前`
  return formatTime(value)
}

/** 数字友好展示(如 1.2w) */
export function fmtCount(n) {
  const num = Number(n) || 0
  if (num >= 10000) return (num / 10000).toFixed(1).replace(/\.0$/, '') + 'w'
  return String(num)
}
