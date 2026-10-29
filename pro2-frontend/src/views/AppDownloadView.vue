<template>
  <div class="page-container app-page">
    <section class="app-hero">
      <div class="hero-copy">
        <span class="app-tag">Android · APK 侧载</span>
        <h1>把 PulseTalk 装进口袋</h1>
        <p class="hero-sub">
          和网页同一套账号与内容：发帖、评论、私聊、好友、通知、可配置个人主页都在。
        </p>

        <div class="hero-actions">
          <a class="download-btn" :href="apkUrl">
            <el-icon><Download /></el-icon>
            下载 Android 版
          </a>
          <el-button round @click="copyLink">复制下载链接</el-button>
        </div>

        <p class="hero-meta">
          版本 {{ version.latestVersion || '1.2.0-alpha.1' }}
          <span class="dot">·</span> Android 8.0+
          <span class="dot">·</span> 约 3.8MB
          <span class="dot">·</span> 无需应用商店
        </p>
        <p v-if="isApp()" class="in-app-tip">你正在 App 内浏览，可把链接发给朋友。</p>
      </div>

      <div class="phone" aria-hidden="true">
        <div class="phone-notch"></div>
        <div class="phone-screen">
          <div class="phone-logo">P</div>
          <div class="phone-title">PulseTalk</div>
          <div class="phone-line"></div>
          <div class="phone-line short"></div>
          <div class="phone-card"></div>
          <div class="phone-card"></div>
        </div>
      </div>
    </section>

    <section class="feature-grid">
      <div class="feature">
        <div class="feature-icon">📱</div>
        <h3>全功能对齐</h3>
        <p>不是阉割版：网页能做的，App 里同样能做。</p>
      </div>
      <div class="feature">
        <div class="feature-icon">📷</div>
        <h3>相机 / 相册</h3>
        <p>发帖配图、头像、主页头图直接调用系统相机与相册。</p>
      </div>
      <div class="feature">
        <div class="feature-icon">↗️</div>
        <h3>系统分享</h3>
        <p>帖子一键分享到微信 / QQ / 系统分享面板。</p>
      </div>
      <div class="feature">
        <div class="feature-icon">🔄</div>
        <h3>内置更新检查</h3>
        <p>启动时检查新版本，下载页始终指向最新 APK。</p>
      </div>
    </section>

    <section class="install-card">
      <h2>安装说明</h2>
      <ol>
        <li>点击「下载 Android 版」，浏览器会下载 <code>pulsetalk-{{ version.latestVersion || '1.2.0-alpha.1' }}.apk</code>。</li>
        <li>首次安装时，系统可能提示“禁止安装未知来源应用”，在设置里允许当前浏览器安装即可。</li>
        <li>安装完成用现有账号登录；如果提示版本过低，回到本页下载最新包覆盖安装。</li>
      </ol>
      <p class="install-note">当前仅提供 Android APK；iOS 需要开发者账号与 TestFlight，本期不做。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { apiAppVersion } from '../api/app'
import { APP_ORIGIN, isApp } from '../utils/asset'

const version = reactive({
  latestVersion: '',
  minVersion: '',
  updateUrl: '',
  notes: ''
})

const apkUrl = computed(() => {
  const url = version.updateUrl || '/download/pulsetalk-1.2.0-alpha.1.apk'
  if (/^https?:\/\//i.test(url)) return url
  return `${APP_ORIGIN}${url.startsWith('/') ? '' : '/'}${url}`
})

async function load() {
  try {
    Object.assign(version, await apiAppVersion())
  } catch (e) {
    /* 下载页不阻塞 */
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(apkUrl.value)
    ElMessage.success('下载链接已复制')
  } catch (e) {
    ElMessage.info(apkUrl.value)
  }
}

onMounted(load)
</script>

<style scoped>
.app-page {
  max-width: 980px;
  padding-top: var(--sp-6);
}

.app-hero {
  display: flex;
  align-items: center;
  gap: var(--sp-8);
  padding: var(--sp-8);
  border-radius: var(--r-lg);
  background:
    radial-gradient(900px 380px at 88% -10%, rgba(37, 99, 235, 0.18), transparent 60%),
    linear-gradient(135deg, #0f172a, #1e3a8a);
  color: #fff;
  overflow: hidden;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.app-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.14);
  font-size: var(--fs-xs);
}

.app-hero h1 {
  margin: var(--sp-3) 0 var(--sp-2);
  font-size: clamp(26px, 4vw, 38px);
  line-height: 1.2;
  color: #fff;
  overflow-wrap: anywhere;
}

.hero-sub {
  margin: 0;
  max-width: 520px;
  font-size: var(--fs-sm);
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.78);
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  margin-top: var(--sp-5);
  flex-wrap: wrap;
}

.download-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 22px;
  border-radius: var(--r-pill);
  background: #fff;
  color: #1d4ed8;
  font-weight: 600;
  font-size: var(--fs-sm);
}

.download-btn:hover {
  background: #eff6ff;
}

.hero-meta {
  margin: var(--sp-3) 0 0;
  font-size: var(--fs-xs);
  color: rgba(255, 255, 255, 0.6);
}

.hero-meta .dot {
  margin: 0 4px;
}

.in-app-tip {
  margin: var(--sp-2) 0 0;
  font-size: var(--fs-xs);
  color: #93c5fd;
}

/* 手机模型 */
.phone {
  flex: none;
  width: 190px;
  height: 350px;
  padding: 10px;
  border-radius: 30px;
  background: #0b1220;
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.35);
  transform: rotate(4deg);
}

.phone-notch {
  width: 70px;
  height: 14px;
  margin: 0 auto 8px;
  border-radius: 0 0 10px 10px;
  background: #0b1220;
}

.phone-screen {
  height: calc(100% - 22px);
  padding: 20px 14px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff, #eef4ff);
  color: #0f172a;
}

.phone-logo {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #2563eb;
  color: #fff;
  font-weight: 800;
  font-size: 22px;
}

.phone-title {
  margin-top: 10px;
  font-weight: 700;
}

.phone-line {
  height: 7px;
  margin-top: 10px;
  border-radius: 4px;
  background: #dbeafe;
}

.phone-line.short {
  width: 60%;
}

.phone-card {
  height: 54px;
  margin-top: 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

/* 功能 */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--sp-3);
  margin-top: var(--sp-4);
}

.feature {
  padding: var(--sp-4);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
  background: var(--bg-elevated);
}

.feature-icon {
  font-size: 22px;
}

.feature h3 {
  margin: var(--sp-2) 0 4px;
  font-size: var(--fs-sm);
}

.feature p {
  margin: 0;
  font-size: var(--fs-xs);
  line-height: 1.7;
  color: var(--text-secondary);
}

.install-card {
  margin-top: var(--sp-4);
  padding: var(--sp-5);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
  background: var(--bg-elevated);
}

.install-card h2 {
  margin: 0 0 var(--sp-3);
  font-size: var(--fs-lg);
}

.install-card ol {
  margin: 0;
  padding-left: 20px;
  font-size: var(--fs-sm);
  line-height: 2;
  color: var(--text-secondary);
}

.install-card code {
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--bg-subtle);
  font-size: 12px;
}

.install-note {
  margin: var(--sp-3) 0 0;
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

@media (max-width: 959px) {
  .phone {
    display: none;
  }
}

@media (max-width: 639px) {
  .app-page {
    padding-top: var(--sp-3);
  }

  .app-hero {
    padding: var(--sp-4);
  }

  .app-hero h1 {
    font-size: 24px;
  }

  .hero-actions {
    align-items: stretch;
  }

  .download-btn {
    justify-content: center;
  }

  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .install-card {
    padding: var(--sp-4);
  }
}
</style>
