/** 个人主页：主题包 / 色板 / 挂件 / 外链图标（与后端白名单保持一致） */

export const THEMES = {
  'minimal-blue': {
    name: '极简蓝', dark: false, accent: '#2563eb',
    vars: { bg: '#ffffff', card: '#ffffff', text: '#111827', muted: '#6b7280', border: '#e5e7eb', subtle: '#f9fafb' }
  },
  'warm-paper': {
    name: '暖纸', dark: false, accent: '#b45309',
    vars: { bg: '#faf7f2', card: '#fffdf9', text: '#292524', muted: '#78716c', border: '#e7e0d5', subtle: '#f5f0e8' }
  },
  forest: {
    name: '森绿', dark: false, accent: '#15803d',
    vars: { bg: '#f7faf8', card: '#ffffff', text: '#1f2937', muted: '#6b7280', border: '#dbe7df', subtle: '#eef6f0' }
  },
  slate: {
    name: '石板', dark: false, accent: '#475569',
    vars: { bg: '#f8fafc', card: '#ffffff', text: '#0f172a', muted: '#64748b', border: '#e2e8f0', subtle: '#f1f5f9' }
  },
  rose: {
    name: '玫瑰', dark: false, accent: '#be123c',
    vars: { bg: '#fff7f8', card: '#ffffff', text: '#1f2937', muted: '#6b7280', border: '#f3d9df', subtle: '#fdf0f3' }
  },
  ocean: {
    name: '海蓝', dark: false, accent: '#0e7490',
    vars: { bg: '#f5fbfc', card: '#ffffff', text: '#102a33', muted: '#64748b', border: '#d7e9ee', subtle: '#eaf6f8' }
  },
  lavender: {
    name: '雾紫', dark: false, accent: '#6d28d9',
    vars: { bg: '#faf8ff', card: '#ffffff', text: '#1f1b2e', muted: '#6b7280', border: '#e6e0f5', subtle: '#f3efff' }
  },
  sand: {
    name: '沙丘', dark: false, accent: '#a16207',
    vars: { bg: '#fbf8f3', card: '#ffffff', text: '#2b2620', muted: '#78716c', border: '#ece4d7', subtle: '#f7f1e7' }
  },
  'dark-ink': {
    name: '墨夜', dark: true, accent: '#60a5fa',
    vars: { bg: '#0f172a', card: '#1e293b', text: '#e2e8f0', muted: '#94a3b8', border: '#334155', subtle: '#17233a' }
  },
  'dark-graphite': {
    name: '石墨', dark: true, accent: '#f59e0b',
    vars: { bg: '#18181b', card: '#27272a', text: '#e4e4e7', muted: '#a1a1aa', border: '#3f3f46', subtle: '#202023' }
  }
}

export const THEME_ORDER = [
  'minimal-blue', 'warm-paper', 'forest', 'slate', 'rose',
  'ocean', 'lavender', 'sand', 'dark-ink', 'dark-graphite'
]

export const ACCENTS = [
  { value: '#2563eb', name: '蓝' },
  { value: '#7c3aed', name: '紫' },
  { value: '#e11d48', name: '玫红' },
  { value: '#d97706', name: '琥珀' },
  { value: '#16a34a', name: '绿' },
  { value: '#0d9488', name: '青' },
  { value: '#475569', name: '灰蓝' },
  { value: '#dc2626', name: '红' }
]

export const CARD_STYLES = [
  { value: 'border', name: '边框' },
  { value: 'flat', name: '浅底' }
]

export const WIDGETS = [
  { value: 'none', name: '无' },
  { value: 'spark', name: '星光' },
  { value: 'leaf', name: '叶片' },
  { value: 'grid', name: '点阵' },
  { value: 'star', name: '星标' },
  { value: 'wave', name: '波浪' }
]

export const LINK_ICONS = [
  { value: 'github', name: 'GitHub' },
  { value: 'bilibili', name: 'B站' },
  { value: 'juejin', name: '掘金' },
  { value: 'zhihu', name: '知乎' },
  { value: 'weibo', name: '微博' },
  { value: 'email', name: '邮箱' },
  { value: 'website', name: '个人站' },
  { value: 'other', name: '其他' }
]

export const MODULE_META = {
  bio: { name: '个人简介', icon: '👤' },
  pinned: { name: '置顶帖子', icon: '📌' },
  skills: { name: '技能标签', icon: '🏷️' },
  projects: { name: '作品', icon: '🧩' },
  gallery: { name: '图集', icon: '🖼️' },
  links: { name: '外链', icon: '🔗' },
  posts: { name: '我的帖子', icon: '📝' },
  stats: { name: '数据', icon: '📊' },
  guestbook: { name: '留言板', icon: '💬' }
}

export function themeStyle(theme) {
  const pack = THEMES[theme?.pack] || THEMES['minimal-blue']
  const v = pack.vars
  return {
    '--sp-bg': v.bg,
    '--sp-card': v.card,
    '--sp-text': v.text,
    '--sp-muted': v.muted,
    '--sp-border': v.border,
    '--sp-subtle': v.subtle,
    '--sp-accent': theme?.accent || pack.accent
  }
}

export function isDarkTheme(packId) {
  return !!(THEMES[packId] && THEMES[packId].dark)
}
