import { reactive } from 'vue'

/**
 * 顶栏未读角标 store(通知 + 私信)
 * 由 NavBar 定时轮询刷新；各页面操作后也可调用更新
 */
export const badgeState = reactive({
  notify: 0,
  chat: 0
})

export function setBadges(notify = 0, chat = 0) {
  badgeState.notify = notify
  badgeState.chat = chat
}

export function totalBadge() {
  return (badgeState.notify || 0) + (badgeState.chat || 0)
}
