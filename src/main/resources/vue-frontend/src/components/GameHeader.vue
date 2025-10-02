<template>
  <header class="game-header">
    <h1>
      <i class="fas fa-spade"></i> 
      德州扑克游戏 (6人桌) 
      <i class="fas fa-heart"></i>
    </h1>
    <div class="game-info">
      <span class="pot-info">奖池: ${{ pot }}</span>
      <span class="phase-info">阶段: {{ translatePhase(phase) }}</span>
      <span class="players-info">玩家: {{ playerCount }}/6</span>
      <span class="auto-status-info">
        自动模式: {{ autoStatus ? '开启' : '关闭' }}
      </span>
    </div>
  </header>
</template>

<script setup>
defineProps({
  pot: {
    type: Number,
    default: 0
  },
  phase: {
    type: String,
    default: 'WAITING'
  },
  playerCount: {
    type: Number,
    default: 0
  },
  autoStatus: {
    type: Boolean,
    default: false
  }
})

const translatePhase = (phase) => {
  const phases = {
    'WAITING': '等待中',
    'PRE_FLOP': '翻牌前',
    'FLOP': '翻牌',
    'TURN': '转牌',
    'RIVER': '河牌',
    'SHOWDOWN': '摊牌',
    'FINISHED': '结束'
  }
  return phases[phase] || phase
}
</script>

<style scoped>
.game-header {
  text-align: center;
  background: rgba(255, 255, 255, 0.1);
  padding: 20px;
  border-radius: 10px;
  backdrop-filter: blur(10px);
  margin-bottom: 20px;
}

.game-header h1 {
  font-size: 2.5em;
  margin-bottom: 10px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
  color: #fff;
}

.game-info {
  display: flex;
  justify-content: center;
  gap: 30px;
  font-size: 1.2em;
  flex-wrap: wrap;
}

.pot-info, .phase-info, .players-info, .auto-status-info {
  background: rgba(255, 255, 255, 0.2);
  padding: 5px 15px;
  border-radius: 20px;
  margin: 2px;
  color: #fff;
}

@media (max-width: 768px) {
  .game-header h1 {
    font-size: 2em;
  }
  
  .game-info {
    gap: 15px;
    font-size: 1em;
  }
}
</style>