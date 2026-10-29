import { ElMessageBox } from 'element-plus'
import { Browser } from '@capacitor/browser'
import { apiAppVersion } from '../api/app'
import { isApp, APP_ORIGIN } from './asset'
import pkg from '../../package.json'

const CURRENT = pkg.version

/** 数字段比较；1.2.0-alpha.1 视为 1.2.0 的预发布版（低于 1.2.0） */
function compare(a, b) {
  const clean = (v) => String(v || '').replace(/^v/i, '')
  const [av, apre] = clean(a).split('-')
  const [bv, bpre] = clean(b).split('-')
  const an = av.split('.').map((n) => parseInt(n, 10) || 0)
  const bn = bv.split('.').map((n) => parseInt(n, 10) || 0)
  for (let i = 0; i < Math.max(an.length, bn.length); i++) {
    if ((an[i] || 0) !== (bn[i] || 0)) return (an[i] || 0) > (bn[i] || 0) ? 1 : -1
  }
  if (apre && !bpre) return -1
  if (!apre && bpre) return 1
  if (apre && bpre) return apre.localeCompare(bpre)
  return 0
}

function absolute(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return `${APP_ORIGIN}${url.startsWith('/') ? '' : '/'}${url}`
}

async function openDownload(url) {
  const target = absolute(url)
  try {
    await Browser.open({ url: target })
  } catch (e) {
    window.open(target, '_blank')
  }
}

/** App 启动时检查更新；Web 端直接跳过 */
export async function checkAppUpdate() {
  if (!isApp()) return
  let info
  try {
    info = await apiAppVersion()
  } catch (e) {
    return
  }
  if (!info || !info.latestVersion) return
  if (compare(info.latestVersion, CURRENT) <= 0) return

  const force = compare(info.minVersion, CURRENT) > 0
  const lines = [
    `当前版本：${CURRENT}`,
    `最新版本：${info.latestVersion}`,
    info.notes ? `更新内容：${info.notes}` : ''
  ].filter(Boolean)

  try {
    await ElMessageBox.confirm(lines.join('\n'), force ? '需要更新才能继续使用' : '发现新版本', {
      confirmButtonText: '去下载',
      cancelButtonText: force ? '退出' : '稍后',
      type: force ? 'warning' : 'info',
      showCancelButton: true
    })
    await openDownload(info.updateUrl)
  } catch (e) {
    if (force) {
      try {
        const { App } = await import('@capacitor/app')
        await App.exitApp()
      } catch (err) {
        /* ignore */
      }
    }
  }
}

export { compare as compareVersion, CURRENT as APP_VERSION }
