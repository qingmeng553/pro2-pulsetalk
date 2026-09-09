<template>
  <!-- Markdown 渲染组件：marked 解析 + DOMPurify 消毒后 v-html 输出 -->
  <div class="markdown-body" v-html="html"></div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  content: { type: String, default: '' }
})

// marked 基础配置(支持 emoji 原文展示)
marked.setOptions({
  breaks: true,        // 单换行渲染为 <br>，贴近贴吧书写习惯
  gfm: true
})

const html = computed(() => {
  if (!props.content) return ''
  const raw = marked.parse(props.content) // v12+ 默认返回 string
  // 消毒：移除脚本/事件属性，XSS 防护
  return DOMPurify.sanitize(raw)
})
</script>
