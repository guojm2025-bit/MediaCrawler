<template>
  <div class="game-controls">
    <!-- 加入游戏 -->
    <div class="player-join">
      <h3>加入游戏</h3>
      <input 
        v-model="playerName"
        type="text" 
        placeholder="玩家名称" 
        maxlength="20"
      >
      <input 
        v-model.number="playerChips"
        type="number" 
        placeholder="筹码数量" 
        min="100"
      >
      <button 
        :disabled="!isConnected || !playerName.trim()"
        class="control-btn"
        @click="handleJoinGame"
      >
        加入游戏
      </button>
    </div>
    
    <!-- 游戏控制 -->
    <div class="game-control">
      <button 
        :disabled="!isConnected"
        class="control-btn auto-btn"
        @click="$emit('create-auto')"
      >
        创建6人桌
      </button>
      
      <button 
        :disabled="!isConnected || autoGameRunning"
        class="control-btn auto-btn"
        @click="$emit('start-auto')"
      >
        开始自动游戏
      </button>
      
      <button 
        :disabled="!isConnected || !autoGameRunning"
        class="control-btn auto-btn"
        @click="$emit('stop-auto')"
      >
        停止自动游戏
      </button>
      
      <button 
        :disabled="!isConnected"
        class="control-btn"
        @click="$emit('start-game')"
      >
        开始游戏
      </button>
      
      <button 
        :disabled="!isConnected"
        class="control-btn"
        @click="$emit('reset-game')"
      >
        重置游戏
      </button>
    </div>
    
    <!-- 连接状态 -->
    <div class="connection-status">
      <span :class="isConnected ? 'connected' : 'disconnected'">
        {{ isConnected ? '已连接' : '未连接' }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  isConnected: {
    type: Boolean,
    default: false
  },
  autoGameRunning: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'join-game', 
  'start-game', 
  'reset-game', 
  'create-auto', 
  'start-auto', 
  'stop-auto'
])

const playerName = ref('')
const playerChips = ref(1000)

const handleJoinGame = () => {
  if (!playerName.value.trim()) {
    alert('请输入玩家名称')
    return
  }
  
  emit('join-game', {
    name: playerName.value.trim(),
    chips: playerChips.value || 1000
  })
}
</script>

<style scoped>
.game-controls {
  background: rgba(255, 255, 255, 0.1);
  padding: 20px;
  border-radius: 10px;
  backdrop-filter: blur(10px);
  height: fit-content;
  min-width: 280px;
}

.player-join, .game-control {
  margin-bottom: 20px;
}

.player-join h3, .game-control h3 {
  color: #fff;
  margin-bottom: 15px;
  font-size: 1.2em;
}

.game-controls input {
  width: 100%;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 1em;
  box-sizing: border-box;
}

.game-controls input:focus {
  outline: none;
  border-color: #2196F3;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.2);
}

.control-btn {
  width: 100%;
  padding: 12px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 5px;
  font-weight: bold;
  cursor: pointer;
  font-size: 1em;
  margin-bottom: 10px;
  transition: all 0.3s ease;
}

.control-btn:hover:not(:disabled) {
  background: #45a049;
  transform: translateY(-1px);
}

.control-btn:disabled {
  background: #666;
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
}

.auto-btn {
  background: #673AB7;
}

.auto-btn:hover:not(:disabled) {
  background: #5E35B1;
}

.connection-status {
  text-align: center;
  padding: 10px;
  border-radius: 5px;
  font-weight: bold;
}

.connected {
  background: rgba(76, 175, 80, 0.3);
  color: #4CAF50;
}

.disconnected {
  background: rgba(244, 67, 54, 0.3);
  color: #f44336;
}

@media (max-width: 1200px) {
  .game-controls {
    width: 100%;
    max-width: 400px;
    margin: 0 auto;
  }
}
</style>