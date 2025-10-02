<template>
  <div 
    :class="[
      'player',
      `pos-${position}`,
      `player-seat-${position + 1}`,
      {
        'current-player': isCurrent,
        'active': isActive,
        'folded': player.hasFolded,
        'ai-player': player.isAi
      }
    ]"
  >
    <div class="player-name">
      {{ player.name || '未知玩家' }}
      <span v-if="player.isDealer" class="badge dealer">D</span>
      <span v-if="player.isSmallBlind" class="badge sb">SB</span>
      <span v-if="player.isBigBlind" class="badge bb">BB</span>
    </div>
    
    <div class="player-chips">筹码: ${{ player.chips || 0 }}</div>
    
    <div v-if="(player.currentBet || 0) > 0" class="player-bet">
      下注: ${{ player.currentBet }}
    </div>
    
    <div class="player-status">{{ getPlayerStatus(player) }}</div>
    
    <div v-if="player.isAi" class="ai-indicator">AI</div>
  </div>
</template>

<script setup>
defineProps({
  player: {
    type: Object,
    required: true
  },
  position: {
    type: Number,
    required: true
  },
  isCurrent: {
    type: Boolean,
    default: false
  },
  isActive: {
    type: Boolean,
    default: false
  }
})

const getPlayerStatus = (player) => {
  if (player.hasFolded) return '已弃牌'
  if (player.isAllIn) return '全下'
  if (player.lastAction) {
    const actions = {
      'FOLD': '弃牌',
      'CHECK': '看牌', 
      'CALL': '跟注',
      'RAISE': '加注',
      'ALL_IN': '全下'
    }
    return actions[player.lastAction] || player.lastAction
  }
  return '等待中'
}
</script>

<style scoped>
.player {
  position: absolute;
  width: 120px;
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  padding: 10px;
  text-align: center;
  backdrop-filter: blur(5px);
  transition: all 0.3s ease;
  color: #fff;
}

.player.current-player {
  border-color: #FFD700;
  box-shadow: 0 0 15px rgba(255, 215, 0, 0.5);
}

.player.active {
  border-color: #4CAF50;
  box-shadow: 0 0 15px rgba(76, 175, 80, 0.5);
  transform: scale(1.05);
}

.player.folded {
  opacity: 0.5;
  filter: grayscale(100%);
}

.player.ai-player {
  border-left: 4px solid #2196F3;
}

.player-name {
  font-weight: bold;
  margin-bottom: 5px;
  font-size: 0.9em;
}

.player-chips {
  font-size: 0.8em;
  color: #FFD700;
  margin-bottom: 3px;
}

.player-bet {
  font-size: 0.8em;
  color: #FF6B6B;
  font-weight: bold;
  margin-bottom: 3px;
}

.player-status {
  font-size: 0.7em;
  color: #FFF;
  opacity: 0.8;
}

.ai-indicator {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #2196F3;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  font-size: 0.6em;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.badge {
  display: inline-block;
  background: #FF5722;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  font-size: 0.6em;
  line-height: 20px;
  margin-left: 2px;
  font-weight: bold;
}

.badge.dealer { background: #9C27B0; }
.badge.sb { background: #FF9800; }
.badge.bb { background: #F44336; }

/* 6人桌位置布局 */
.player-seat-1 { top: 0; left: 50%; transform: translateX(-50%); }
.player-seat-2 { top: 25%; right: 0; }
.player-seat-3 { bottom: 25%; right: 0; }
.player-seat-4 { bottom: 0; left: 50%; transform: translateX(-50%); }
.player-seat-5 { bottom: 25%; left: 0; }
.player-seat-6 { top: 25%; left: 0; }

@media (max-width: 1200px) {
  .player {
    width: 100px;
    padding: 8px;
  }
}

@media (max-width: 768px) {
  .player {
    width: 80px;
    padding: 6px;
    font-size: 0.8em;
  }
}
</style>