<template>
  <!-- 移动端底部 Tab（<960 显示）：首页 / 消息 / 发帖 / 我的；详情页让位给吸底操作条 -->
  <nav v-if="!hidden" class="tabbar" aria-label="主导航">
    <router-link to="/" class="tab" :class="{ active: route.name === 'home' }">
      <el-icon><HomeFilled /></el-icon>
      <span>首页</span>
    </router-link>
    <router-link to="/msg" class="tab" :class="{ active: route.name === 'msg' }">
      <el-icon><ChatDotRound /></el-icon>
      <span>消息</span>
      <i v-if="totalBadge() > 0" class="tab-dot" />
    </router-link>
    <router-link to="/post/create" class="tab" :class="{ active: route.name === 'post-create' }">
      <el-icon><EditPen /></el-icon>
      <span>发帖</span>
    </router-link>
    <router-link to="/profile" class="tab" :class="{ active: route.name === 'profile' }">
      <el-icon><User /></el-icon>
      <span>我的</span>
    </router-link>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, ChatDotRound, EditPen, User } from '@element-plus/icons-vue'
import { totalBadge } from '../store/badge'

const route = useRoute()
const hidden = computed(() => route.name === 'post-detail')
</script>

<style scoped>
.tabbar {
  display: none;
}

@media (max-width: 959px) {
  .tabbar {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 60;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    height: calc(56px + env(safe-area-inset-bottom));
    padding-bottom: env(safe-area-inset-bottom);
    background: var(--bg-elevated);
    border-top: 1px solid var(--border-color);
  }

  .tab {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    color: var(--text-tertiary);
    font-size: var(--fs-xs);
    line-height: 1;
  }

  .tab .el-icon {
    font-size: 20px;
  }

  .tab.active {
    color: var(--c-primary);
    font-weight: var(--fw-medium);
  }

  .tab-dot {
    position: absolute;
    top: 6px;
    right: calc(50% - 16px);
    width: 7px;
    height: 7px;
    border-radius: var(--r-circle);
    background: var(--c-danger);
    box-shadow: 0 0 0 2px var(--bg-elevated);
  }
}
</style>
