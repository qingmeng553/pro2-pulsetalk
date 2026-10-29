<template>
  <div
    class="space"
    :class="[
      { 'is-dark': dark, 'is-app': mode === 'app', 'is-preview': preview },
      `card-${theme.cardStyle || 'border'}`
    ]"
    :style="styleVars"
  >
    <!-- 顶部：头图 + 身份 -->
    <div
      v-if="!hideIdentity || header"
      class="space-hero"
      :class="{ 'has-cover': header, 'identity-hidden': hideIdentity }"
    >
      <div v-if="header" class="hero-cover">
        <img :src="resolveAssetUrl(header)" alt="" loading="lazy" />
      </div>
      <div v-if="!hideIdentity" class="hero-main">
        <el-avatar :size="76" :src="resolveAssetUrl(user.avatarUrl)" class="hero-avatar">
          {{ (user.nickname || '?').slice(0, 1) }}
        </el-avatar>
        <div class="hero-text">
          <h1 class="hero-name">
            {{ user.nickname }}
            <AdminBadge v-if="user.role === 'ADMIN'" />
          </h1>
          <p class="hero-meta">
            加入于 {{ joinDate }}
            <template v-if="stats.viewCount !== undefined">
              <span class="dot">·</span>访问 {{ stats.viewCount || 0 }}
            </template>
          </p>
        </div>
      </div>
      <div v-if="!hideIdentity && theme.widget && theme.widget !== 'none'" class="hero-widget" :class="`widget-${theme.widget}`"></div>
    </div>

    <!-- 模块栅格 -->
    <div v-if="modules.length" class="space-grid">
      <div
        v-for="m in modules"
        :key="m.type"
        class="space-cell"
        :class="`w-${mode === 'app' ? 'full' : m.width}`"
      >
        <SpaceModule :module="m" :user="user" :stats="stats" :preview="preview" />
      </div>
    </div>
    <div v-else class="space-empty">主页还没有可展示的内容。</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AdminBadge from '../AdminBadge.vue'
import SpaceModule from './SpaceModule.vue'
import { resolveAssetUrl } from '../../utils/asset'
import { isDarkTheme, themeStyle } from '../../utils/spaceThemes'

const props = defineProps({
  layout: { type: Object, required: true },
  user: { type: Object, required: true },
  stats: { type: Object, default: () => ({}) },
  hiddenModules: { type: Array, default: () => [] },
  preview: { type: Boolean, default: false },
  mode: { type: String, default: 'web' },
  hideIdentity: { type: Boolean, default: false }
})

const theme = computed(() => props.layout?.theme || {})
const dark = computed(() => isDarkTheme(theme.value.pack))
const header = computed(() => theme.value.header || '')
const styleVars = computed(() => themeStyle(theme.value))
const joinDate = computed(() => (props.user.createTime || '').slice(0, 10))

const modules = computed(() => {
  const hidden = new Set(props.hiddenModules || [])
  return (props.layout?.modules || [])
    .filter((m) => m.visible !== false && !hidden.has(m.type) && hasContent(m))
    .slice()
    .sort((a, b) => (a.order || 0) - (b.order || 0))
})

/** 空模块不渲染，避免默认主页出现一排“还没有…” */
function hasContent(m) {
  if (m.type === 'bio') return !!((m.data && m.data.intro) || '').trim()
  if (m.type === 'pinned') return !!(m.data && m.data.postId)
  if (['skills', 'projects', 'gallery', 'links'].includes(m.type)) {
    return Array.isArray(m.data) && m.data.length > 0
  }
  return true
}
</script>

<style scoped>
.space {
  --sp-radius: 16px;
  padding: var(--sp-4);
  border-radius: var(--sp-radius);
  background: var(--sp-bg);
  color: var(--sp-text);
  transition: background var(--dur-base), color var(--dur-base);
}

/* 顶部 */
.space-hero {
  position: relative;
  overflow: hidden;
  border-radius: var(--r-md);
  background: var(--sp-subtle);
}

.hero-cover {
  height: 160px;
  overflow: hidden;
}

.hero-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-main {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: var(--sp-4);
}

.has-cover .hero-main {
  margin-top: -36px;
  padding-top: 0;
}

.identity-hidden .hero-cover {
  height: 190px;
}

.hero-avatar {
  flex: none;
  border: 3px solid var(--sp-bg);
  background: var(--sp-card);
}

.hero-text {
  min-width: 0;
}

.hero-name {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: var(--fs-xl);
  color: var(--sp-text);
}

.hero-meta {
  margin: 4px 0 0;
  font-size: var(--fs-xs);
  color: var(--sp-muted);
}

.hero-meta .dot {
  margin: 0 4px;
}

/* 挂件：纯 CSS 纹理，不引第三方图 */
.hero-widget {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.5;
}

.widget-grid {
  background-image: radial-gradient(var(--sp-accent) 1px, transparent 1px);
  background-size: 14px 14px;
  mask-image: linear-gradient(to bottom left, #000, transparent 70%);
}

.widget-spark {
  background:
    radial-gradient(circle at 82% 22%, var(--sp-accent) 0 1.5px, transparent 2px),
    radial-gradient(circle at 90% 38%, var(--sp-accent) 0 1px, transparent 1.5px),
    radial-gradient(circle at 72% 42%, var(--sp-accent) 0 1px, transparent 1.5px),
    radial-gradient(circle at 86% 60%, var(--sp-accent) 0 1.5px, transparent 2px);
}

.widget-wave {
  background: repeating-linear-gradient(
    -45deg,
    transparent 0 10px,
    var(--sp-border) 10px 11px
  );
  mask-image: linear-gradient(to bottom left, #000, transparent 65%);
}

.widget-leaf {
  background:
    radial-gradient(ellipse at 88% 20%, var(--sp-accent) 0 4%, transparent 5%),
    radial-gradient(ellipse at 94% 34%, var(--sp-accent) 0 3%, transparent 4%);
}

.widget-star {
  background:
    conic-gradient(from 0deg at 88% 22%, var(--sp-accent) 0 10%, transparent 10% 100%);
  background-size: 48px 48px;
  background-repeat: no-repeat;
  background-position: right 20px top 16px;
}

/* 栅格 */
.space-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: var(--sp-3);
  margin-top: var(--sp-3);
}

.space-cell {
  min-width: 0;
  padding: var(--sp-4);
  border: 1px solid var(--sp-border);
  border-radius: var(--r-md);
  background: var(--sp-card);
}

.card-flat .space-cell {
  border-color: transparent;
  background: var(--sp-subtle);
}

.w-full {
  grid-column: span 12;
}

.w-half {
  grid-column: span 6;
}

.space-empty {
  padding: var(--sp-10) 0;
  text-align: center;
  font-size: var(--fs-sm);
  color: var(--sp-muted);
}

.is-app {
  padding: var(--sp-3);
}

.is-app .space-grid {
  grid-template-columns: 1fr;
  gap: var(--sp-2);
}

.is-app .space-cell {
  grid-column: span 1;
  padding: var(--sp-3);
}

.is-preview .space-cell {
  pointer-events: none;
}

@media (max-width: 639px) {
  .space {
    padding: var(--sp-2);
    border-radius: var(--r-md);
  }

  .space-grid {
    grid-template-columns: 1fr;
    gap: var(--sp-2);
  }

  .space-cell {
    grid-column: span 1;
    padding: var(--sp-3);
  }

  .hero-cover {
    height: 120px;
  }

  .hero-name {
    font-size: var(--fs-lg);
  }
}
</style>
