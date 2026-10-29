import { reactive } from 'vue'
import { apiCategoryList, apiHotRank } from '../api/post'

// 首页共享数据：分类、大家都在看、当前分类
// 侧栏（桌面左栏 / 移动抽屉）与信息流共用一份，避免重复请求
export const homeState = reactive({
  categories: [],
  hot: [],
  activeCategory: null,
  loaded: false
})

let inflight = null

/** 首次加载或强制刷新首页共享数据（并发调用会复用同一个 Promise） */
export function ensureHomeData(force = false) {
  if (homeState.loaded && !force) return Promise.resolve()
  if (inflight) return inflight
  inflight = Promise.all([
    apiCategoryList().catch(() => []),
    apiHotRank(5).catch(() => [])
  ])
    .then(([categories, hot]) => {
      homeState.categories = categories || []
      homeState.hot = hot || []
      homeState.loaded = true
    })
    .finally(() => {
      inflight = null
    })
  return inflight
}

export function setActiveCategory(id) {
  homeState.activeCategory = id == null ? null : id
}
