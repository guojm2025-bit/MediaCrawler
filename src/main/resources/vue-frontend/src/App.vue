<template>
  <div class="poker-app">
    <GameHeader 
      :pot="gameStore.pot"
      :phase="gameStore.currentPhase"
      :player-count="gameStore.players.length"
      :auto-status="gameStore.autoGameRunning"
    />
    
    <div class="game-container">
      <GameTable 
        :community-cards="gameStore.communityCards"
        :players="gameStore.players"
        :current-player="gameStore.currentPlayer"
        :player-id="gameStore.playerId"
      />
      
      <PlayerHand 
        :cards="gameStore.playerCards"
      />
      
      <ActionButtons 
        :is-my-turn="gameStore.isMyTurn"
        :current-bet="gameStore.currentBet"
        @action="handlePlayerAction"
      />
    </div>
    
    <GameControls 
      :is-connected="gameStore.isConnected"
      :auto-game-running="gameStore.autoGameRunning"
      @join-game="handleJoinGame"
      @start-game="handleStartGame"
      @reset-game="handleResetGame"
      @create-auto="handleCreateAuto"
      @start-auto="handleStartAuto"
      @stop-auto="handleStopAuto"
    />
    
    <MessageToast 
      :message="uiStore.toastMessage"
      :type="uiStore.toastType"
      :show="uiStore.showToast"
    />
    
    <GameLog 
      :logs="gameStore.gameLogs"
    />
    
    <!-- 调试面板 -->
    <div class="debug-panel" v-if="showDebug">
      <h4>调试信息</h4>
      <div class="debug-info">
        <p><strong>我的ID:</strong> {{ gameStore.playerId || '未设置' }}</p>
        <p><strong>当前玩家ID:</strong> {{ gameStore.currentPlayer?.id || '无' }}</p>
        <p><strong>当前玩家名称:</strong> {{ gameStore.currentPlayer?.name || '无' }}</p>
        <p><strong>游戏阶段:</strong> {{ gameStore.currentPhase }}</p>
        <p><strong>是否轮到我:</strong> {{ gameStore.isMyTurn }}</p>
        <p><strong>连接状态:</strong> {{ gameStore.isConnected ? '已连接' : '未连接' }}</p>
        <p><strong>玩家数量:</strong> {{ gameStore.players?.length || 0 }}</p>
        <p><strong>奖池:</strong> ${{ gameStore.pot || 0 }}</p>
        <p><strong>自动游戏:</strong> {{ gameStore.autoGameRunning ? '开启' : '关闭' }}</p>
        <div class="players-debug">
          <strong>玩家列表:</strong>
          <div v-for="(player, index) in gameStore.players" :key="player.id || index" class="player-debug">
            {{ index + 1 }}. {{ player.name }} (ID: {{ player.id }}) - 筹码: ${{ player.chips }}
          </div>
        </div>
      </div>
      <button @click="showDebug = false" class="close-debug">关闭</button>
    </div>
    
    <!-- 调试按钮 -->
    <button @click="showDebug = !showDebug" class="debug-toggle">调试</button>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useGameStore } from './stores/game'
import { useUIStore } from './stores/ui'
import GameHeader from './components/GameHeader.vue'
import GameTable from './components/GameTable.vue'
import PlayerHand from './components/PlayerHand.vue'
import ActionButtons from './components/ActionButtons.vue'
import GameControls from './components/GameControls.vue'
import MessageToast from './components/MessageToast.vue'
import GameLog from './components/GameLog.vue'

const gameStore = useGameStore()
const uiStore = useUIStore()
const showDebug = ref(false)

onMounted(() => {
  gameStore.connectWebSocket()
  gameStore.startStatusPolling()
})

const handlePlayerAction = (action) => {
  gameStore.playerAction(action.type, action.amount)
}

const handleJoinGame = (playerData) => {
  gameStore.joinGame(playerData.name, playerData.chips)
}

const handleStartGame = () => {
  gameStore.startGame()
}

const handleResetGame = () => {
  gameStore.resetGame()
}

const handleCreateAuto = () => {
  gameStore.createAutoGame()
}

const handleStartAuto = () => {
  gameStore.startAutoGame()
}

const handleStopAuto = () => {
  gameStore.stopAutoGame()
}
</script>

<style>
.poker-app {
  font-family: 'Arial', sans-serif;
  background: linear-gradient(135deg, #0f4c3a, #1a5f4a);
  min-height: 100vh;
  padding: 20px;
  color: white;
}

.game-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 调试面板样式 */
.debug-panel {
  position: fixed;
  top: 20px;
  right: 20px;
  background: rgba(0, 0, 0, 0.9);
  padding: 20px;
  border-radius: 10px;
  border: 2px solid #00ff00;
  z-index: 1000;
  min-width: 300px;
}

.debug-info p {
  margin: 5px 0;
  font-family: monospace;
  font-size: 12px;
}

.players-debug {
  max-height: 120px;
  overflow-y: auto;
  margin-top: 10px;
  padding: 5px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.player-debug {
  font-size: 10px;
  margin: 2px 0;
  color: #ccc;
}

.debug-toggle {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: #ff6b6b;
  color: white;
  border: none;
  padding: 10px 15px;
  border-radius: 5px;
  cursor: pointer;
  z-index: 999;
}

.close-debug {
  background: #666;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 3px;
  cursor: pointer;
  margin-top: 10px;
}

@media (max-width: 768px) {
  .poker-app {
    padding: 10px;
  }
  
  .debug-panel {
    top: 10px;
    right: 10px;
    left: 10px;
    min-width: auto;
  }
}
</style>