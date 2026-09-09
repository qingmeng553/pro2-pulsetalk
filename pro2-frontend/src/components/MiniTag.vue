<template>
  <!-- 全局统一小标签：图标 + 文字的圆角框(分类/楼层/置顶/关系/公告等复用) -->
  <span class="tag-frame" :class="`tone-${tone}`">
    <span v-if="icon" class="tag-ico">{{ icon }}</span>
    <span class="tag-txt">{{ text }}</span>
  </span>
</template>

<script setup>
defineProps({
  // 前缀图标(emoji/字符)
  icon: { type: String, default: '' },
  // 标签文字
  text: { type: [String, Number], default: '' },
  // 配色: default/primary/success/warn/danger/violet
  tone: { type: String, default: 'default' }
})
</script>

<style scoped>
.tag-frame {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--line, #dfe4f0);
  background: #f7f9ff;
  color: #6b7490;
  border-radius: 999px;
  padding: 1px 10px;
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
  vertical-align: 1px;
}
.tag-ico {
  font-size: 13px;
  line-height: 1;
}
.tone-primary {
  border-color: #cdd9ff;
  background: #eef1ff;
  color: var(--brand);
}
.tone-success {
  border-color: #bde5cc;
  background: #e9f7ef;
  color: #2f9e63;
}
.tone-warn {
  border-color: #ffe1a8;
  background: #fff6e6;
  color: #c07d10;
}
.tone-danger {
  border-color: #f5c2c2;
  background: #fdeeee;
  color: #d64545;
}
.tone-violet {
  border-color: #d8cdf7;
  background: #f0ecff;
  color: #7c5ce0;
}
</style>
