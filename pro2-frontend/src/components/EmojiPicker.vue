<template>
  <!-- emoji 选择器：点击笑脸按钮展开面板，点选后向父组件发送 pick -->
  <div class="emoji-picker">
    <button type="button" class="emoji-trigger" title="插入表情" @click="visible = !visible">😊</button>
    <transition name="emoji-pop">
      <div v-show="visible" class="emoji-panel" @mouseleave="visible = false">
        <button
          v-for="(e, i) in EMOJIS"
          :key="i"
          type="button"
          class="emoji-cell"
          @click="pick(e)"
        >
          {{ e }}
        </button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// 常用 emoji 集合(可自行增删)
const EMOJIS = [
  '😀', '😄', '😁', '😆', '😅', '😂', '🤣', '😊',
  '😍', '🥰', '😘', '😎', '🤩', '😋', '🤔', '😏',
  '😭', '😢', '😡', '😱', '🤯', '😴', '🤗', '🙃',
  '👍', '👎', '👏', '🙏', '🤝', '💪', '✌️', '🤙',
  '❤️', '🧡', '💛', '💚', '💙', '💜', '💖', '💯',
  '🔥', '✨', '⭐', '🌟', '💡', '📌', '🎉', '🎊',
  '🚀', '🌈', '🍉', '🍔', '☕', '🐱', '🐶', '🐼',
  '🌸', '🍀', '⚡', '❄️', '✅', '❌', '❓', '❗'
]

const visible = ref(false)
const emit = defineEmits(['pick'])

function pick(e) {
  emit('pick', e)
}
</script>

<style scoped>
.emoji-picker {
  position: relative;
  display: inline-flex;
}
.emoji-trigger {
  width: 28px;
  height: 28px;
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 50%;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s;
}
.emoji-trigger:hover {
  transform: scale(1.15);
  border-color: var(--brand);
}
.emoji-panel {
  position: absolute;
  bottom: calc(100% + 6px);
  left: 0;
  width: 288px;
  max-height: 220px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.12);
  padding: 10px;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
  z-index: 30;
}
.emoji-cell {
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
  border-radius: 8px;
  padding: 3px 0;
  transition: background 0.15s, transform 0.15s;
}
.emoji-cell:hover {
  background: #f0f3ff;
  transform: scale(1.18);
}
.emoji-pop-enter-active,
.emoji-pop-leave-active {
  transition: opacity 0.18s, transform 0.18s;
}
.emoji-pop-enter-from,
.emoji-pop-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
