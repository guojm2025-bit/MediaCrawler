<template>
  <div class="game-table">
    <!-- 公共牌区域 -->
    <CommunityCards :cards="communityCards" />
    
    <!-- 玩家区域 -->
    <div class="players-area">
      <!-- 显示玩家 -->
      <PlayerCard
        v-for="(player, index) in players"
        :key="player.id || `player-${index}`"
        :player="player"
        :position="index"
        :is-current="player.id === playerId"
        :is-active="currentPlayer && currentPlayer.id === player.id"
      />
      
      <!-- 空位 -->
      <div
        v-for="i in Math.max(0, 6 - players.length)"
        :key="`empty-${i}`"
        :class="`player empty-seat pos-${players.length + i - 1} player-seat-${players.length + i}`"
      >
        <div class="empty-seat-label">空位</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import CommunityCards from './CommunityCards.vue'
import PlayerCard from './PlayerCard.vue'

defineProps({
  communityCards: {
    type: Array,
    default: () => []
  },
  players: {
    type: Array,
    default: () => []
  },
  currentPlayer: {
    type: Object,
    default: null
  },
  playerId: {
    type: String,
    default: null
  }
})
</script>

<style scoped>
.game-table {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-around;
  background: radial-gradient(ellipse at center, rgba(0, 120, 0, 0.4), rgba(0, 80, 0, 0.3));
  border-radius: 50%;
  border: 8px solid #8B4513;
  padding: 40px;
  position: relative;
  min-height: 600px;
  width: 800px;
  margin: 0 auto 30px auto;
}

.players-area {
  position: relative;
  width: 600px;
  height: 400px;
  margin: 20px auto;
}

.empty-seat {
  position: absolute;
  width: 120px;
  background: rgba(255, 255, 255, 0.1);
  border: 2px dashed rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  padding: 10px;
  text-align: center;
  backdrop-filter: blur(5px);
  opacity: 0.3;
}

.empty-seat-label {
  color: #999;
  font-style: italic;
  font-size: 0.8em;
}

/* 6人桌位置布局 */
.player-seat-1 { top: 0; left: 50%; transform: translateX(-50%); }
.player-seat-2 { top: 25%; right: 0; }
.player-seat-3 { bottom: 25%; right: 0; }
.player-seat-4 { bottom: 0; left: 50%; transform: translateX(-50%); }
.player-seat-5 { bottom: 25%; left: 0; }
.player-seat-6 { top: 25%; left: 0; }

@media (max-width: 1200px) {
  .game-table {
    width: 100%;
    max-width: 700px;
  }
  
  .players-area {
    width: 500px;
    height: 350px;
  }
}

@media (max-width: 768px) {
  .players-area {
    width: 400px;
    height: 280px;
  }
}
</style>