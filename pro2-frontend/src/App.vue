<template>
  <div class="app-shell">
    <!-- 顶部导航 -->
    <NavBar />

    <!-- 页面主体 -->
    <router-view v-slot="{ Component }">
      <transition name="fade-slide" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>

    <!-- 移动端底部 Tab -->
    <MobileTabBar />

    <!-- 移动端左栏抽屉（内容与桌面左栏同一组件） -->
    <el-drawer
      v-model="uiState.sidebarOpen"
      title="导航"
      direction="ltr"
      size="288px"
      class="side-drawer"
    >
      <HomeSidebar />
    </el-drawer>

    <!-- 全局登录确认弹窗 -->
    <LoginDialog />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { Capacitor } from '@capacitor/core'
import { StatusBar, Style } from '@capacitor/status-bar'
import { SplashScreen } from '@capacitor/splash-screen'
import { App as CapApp } from '@capacitor/app'
import NavBar from './components/NavBar.vue'
import HomeSidebar from './components/HomeSidebar.vue'
import MobileTabBar from './components/MobileTabBar.vue'
import LoginDialog from './components/LoginDialog.vue'
import { uiState } from './store/ui'
import { checkAppUpdate } from './utils/appUpdate'

onMounted(async () => {
  if (!Capacitor.isNativePlatform()) return
  try {
    await StatusBar.setStyle({ style: Style.Light })
    await StatusBar.setBackgroundColor({ color: '#2563eb' })
  } catch (e) {
    /* 部分 Android 版本不支持动态改色 */
  }
  setTimeout(() => {
    SplashScreen.hide().catch(() => {})
  }, 400)

  CapApp.addListener('backButton', ({ canGoBack }) => {
    if (canGoBack) window.history.back()
    else CapApp.exitApp()
  })

  checkAppUpdate()
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--bg-page);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 959px) {
  .app-shell {
    padding-bottom: calc(56px + env(safe-area-inset-bottom));
  }
}
</style>

<style>
/* 抽屉是 teleport 到 body 的，只能用全局样式；前缀限定 .side-drawer */
.side-drawer .el-drawer__body {
  padding: var(--sp-3);
  background: var(--bg-subtle);
}
</style>
