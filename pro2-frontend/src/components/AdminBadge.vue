<template>
  <span class="admin-badge" title="社区管理员">
    <el-icon :size="12"><Aim /></el-icon> 管理员
  </span>
</template>

<script setup>
// 管理员盾牌徽章：渲染在 ADMIN 用户昵称旁(详情页/评论/导航等复用)
</script>

<style scoped>
.admin-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin-left: 5px;
  padding: 1px 7px;
  font-size: 12px;
  line-height: 16px;
  color: #fff;
  background: var(--brand-gradient);
  border-radius: 10px;
  vertical-align: 1px;
}
</style>
