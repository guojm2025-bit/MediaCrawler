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

      <PlayerHand :cards="gameStore.playerCards" />

      <ActionButtons
          :is-my-turn="gameStore.isMyTurn"
          :current-bet="gameStore.currentBet"
          @action="({ type, amount }) => gameStore.playerAction(type, amount)"
      />
    </div>

    <GameControls
        :is-connected="gameStore.isConnected"
        :auto-game-running="gameStore.autoGameRunning"
        @join-game="({ name, chips }) => gameStore.joinGame(name, chips)"
        @reset-game="gameStore.resetGame"
        @create-auto="gameStore.createAutoGame"
        @start-auto="gameStore.startAutoGame"
        @stop-auto="gameStore.stopAutoGame"
    />

    <MessageToast
        :message="uiStore.toastMessage"
        :type="uiStore.toastType"
        :show="uiStore.showToast"
    />

    <GameLog :logs="gameStore.gameLogs" />

    <div class="debug-panel" v-if="showDebug">
      <h4>实时游戏状态 (GameState)</h4>
      <pre>{{ gameStore.gameState }}</pre>
      <h4>我的玩家ID</h4>
      <pre>{{ gameStore.playerId }}</pre>
      <button @click="showDebug = false" class="close-debug">关闭</button>
    </div>

    <button @click="showDebug = !showDebug" class="debug-toggle">调试</button>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useGameStore } from './stores/game';
import { useUIStore } from './stores/ui';
import GameHeader from './components/GameHeader.vue';
import GameTable from './components/GameTable.vue';
import PlayerHand from './components/PlayerHand.vue';
import ActionButtons from './components/ActionButtons.vue';
import GameControls from './components/GameControls.vue';
import MessageToast from './components/MessageToast.vue';
import GameLog from './components/GameLog.vue';

const gameStore = useGameStore();
const uiStore = useUIStore();
const showDebug = ref(false);

onMounted(() => {
  gameStore.connectWebSocket();
});
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
  max-width: 500px;
  max-height: 80vh;
  overflow: auto;
  box-shadow: 0 0 20px rgba(0, 255, 0, 0.5);
}

.debug-panel h4 {
  margin-top: 0;
  color: #00ff00;
}

.debug-panel pre {
  font-family: monospace;
  font-size: 12px;
  color: #00ff00;
  white-space: pre-wrap;
  word-break: break-all;
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
</style>
