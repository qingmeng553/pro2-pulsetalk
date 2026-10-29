<template>
  <!-- 首页右栏：登录/用户卡片 + App 下载占位 + 邮箱绑定占位 -->
  <aside class="rail">
    <section v-if="!isLoggedIn" class="rail-card">
      <h3 class="rail-title">登录 PulseTalk</h3>
      <p class="rail-text">登录后可以发帖、评论、点赞和收藏。</p>
      <el-button type="primary" class="rail-btn" @click="openLoginDialog()">登录 / 注册</el-button>
    </section>

    <section v-else class="rail-card">
      <div class="rail-user">
        <el-avatar :size="40" :src="authState.user?.avatarUrl">
          {{ (authState.user?.nickname || 'U').slice(0, 1) }}
        </el-avatar>
        <div class="rail-user-main">
          <div class="rail-name">
            <span>{{ authState.user?.nickname || authState.user?.username }}</span>
            <AdminBadge v-if="authState.user?.role === 'ADMIN'" />
          </div>
          <span class="rail-role">{{ authState.user?.role === 'ADMIN' ? '管理员' : '普通用户' }}</span>
        </div>
      </div>
      <router-link to="/profile" class="rail-link">个人中心</router-link>
    </section>

    <section class="rail-card">
      <h3 class="rail-title">PulseTalk App</h3>
      <p class="rail-text">1.2.0 内测中，Android 包完成后在这里放下载入口。</p>
      <el-button class="rail-btn" disabled>敬请期待</el-button>
    </section>

    <section class="rail-card">
      <h3 class="rail-title">绑定邮箱</h3>
      <p class="rail-text">绑定后可用邮箱验证码找回密码，和密保问题互为兜底。</p>
      <el-tag size="small" type="info" effect="plain">alpha2 开放</el-tag>
    </section>
  </aside>
</template>

<script setup>
import AdminBadge from './AdminBadge.vue'
import { authState, isLoggedIn, openLoginDialog } from '../store/user'
</script>

<style scoped>
.rail {
  display: flex;
  flex-direction: column;
  gap: var(--sp-3);
}

.rail-card {
  padding: var(--sp-4);
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
}

.rail-title {
  margin: 0 0 var(--sp-2);
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
  color: var(--text-primary);
}

.rail-text {
  margin: 0 0 var(--sp-3);
  font-size: var(--fs-xs);
  line-height: var(--lh-normal);
  color: var(--text-secondary);
}

.rail-btn {
  width: 100%;
}

.rail-user {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  margin-bottom: var(--sp-3);
}

.rail-user-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rail-name {
  display: flex;
  align-items: center;
  min-width: 0;
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
  color: var(--text-primary);
}

.rail-name > span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rail-role {
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
}

.rail-link {
  display: block;
  padding-top: var(--sp-2);
  border-top: 1px solid var(--border-color);
  font-size: var(--fs-xs);
  color: var(--text-link);
}
</style>
