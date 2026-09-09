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

    <!-- 全局登录确认弹窗 -->
    <LoginDialog />
  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
import LoginDialog from './components/LoginDialog.vue'
</script>

<style scoped>
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
</style>
