<template>
  <!-- 相关推荐：正文下方（宽屏）或评论区之后（窄屏），数据由详情页传入 -->
  <section v-if="posts.length" class="related-card">
    <div class="related-head">
      <h2 class="related-title">相关推荐</h2>
    </div>
    <PostRow v-for="p in posts" :key="p.id" :post="p" />
  </section>
</template>

<script setup>
import PostRow from './PostRow.vue'

defineProps({
  posts: { type: Array, default: () => [] }
})
</script>

<style scoped>
.related-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--r-md);
  overflow: hidden;
}

.related-head {
  padding: var(--sp-3) var(--sp-4);
  border-bottom: 1px solid var(--border-color);
}

.related-title {
  margin: 0;
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
  color: var(--text-primary);
}
</style>
