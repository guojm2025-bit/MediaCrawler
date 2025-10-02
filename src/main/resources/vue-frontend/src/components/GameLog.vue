<template>
  <div class="game-log">
    <h4>游戏日志</h4>
    <div class="log-content" ref="logContainer">
      <div 
        v-for="(log, index) in logs" 
        :key="index"
        class="log-entry"
        :class="log.type"
      >
        <span class="log-time">[{{ formatTime(log.timestamp) }}]</span>
        <span class="log-message">{{ log.message }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  logs: {
    type: Array,
    default: () => []
  }
})

const logContainer = ref(null)

// 当日志更新时自动滚动到底部
watch(() => props.logs.length, async () => {
  await nextTick()
  if (logContainer.value) {
    logContainer.value.scrollTop = logContainer.value.scrollHeight
  }
})

const formatTime = (timestamp) => {
  if (!timestamp) return new Date().toLocaleTimeString()
  return new Date(timestamp).toLocaleTimeString()
}
</script>

<style scoped>
.game-log {
  position: fixed;
  bottom: 20px;
  left: 20px;
  width: 400px;
  max-height: 200px;
  background: rgba(0, 0, 0, 0.8);
  border-radius: 5px;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.game-log h4 {
  color: #fff;
  padding: 10px 15px;
  margin: 0;
  background: rgba(255, 255, 255, 0.1);
  font-size: 1em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.log-content {
  max-height: 150px;
  overflow-y: auto;
  padding: 10px;
  font-size: 0.9em;
  line-height: 1.4;
}

.log-entry {
  color: #fff;
  margin-bottom: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.05);
  transition: background 0.2s ease;
}

.log-entry:hover {
  background: rgba(255, 255, 255, 0.1);
}

.log-entry.info {
  border-left: 3px solid #2196F3;
}

.log-entry.success {
  border-left: 3px solid #4CAF50;
}

.log-entry.warning {
  border-left: 3px solid #FF9800;
}

.log-entry.error {
  border-left: 3px solid #f44336;
}

.log-time {
  color: #999;
  font-size: 0.8em;
  margin-right: 8px;
}

.log-message {
  color: #fff;
}

/* 滚动条样式 */
.log-content::-webkit-scrollbar {
  width: 6px;
}

.log-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.log-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

.log-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

@media (max-width: 768px) {
  .game-log {
    width: calc(100% - 40px);
    max-width: 350px;
    bottom: 10px;
    left: 20px;
    right: 20px;
  }
}
</style>