import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { useUIStore } from './ui';

export const useGameStore = defineStore('game', () => {
  // --- 核心状态 ---
  const isConnected = ref(false);
  const ws = ref(null);
  const playerId = ref(null);
  const gameState = ref({}); // 单一数据源
  const gameLogs = ref([]);

  const uiStore = useUIStore();

  // --- 计算属性 (从gameState派生) ---
  const pot = computed(() => gameState.value.pot || 0);
  const currentPhase = computed(() => gameState.value.currentPhase || 'WAITING');
  const players = computed(() => gameState.value.players || []);
  const communityCards = computed(() => gameState.value.communityCards || []);
  const currentPlayer = computed(() => gameState.value.currentPlayer || null);
  const autoGameRunning = computed(() => gameState.value.isAutoGameRunning || false);
  const currentBet = computed(() => gameState.value.currentBetAmount || 0);

  const myPlayer = computed(() => players.value.find(p => p.id === playerId.value));
  const playerCards = computed(() => myPlayer.value?.holeCards || []);

  const isMyTurn = computed(() => {
    if (!currentPlayer.value || !playerId.value || !myPlayer.value) {
      return false;
    }
    const result = currentPlayer.value.id === playerId.value && !myPlayer.value.hasFolded;

    console.log('isMyTurn check:', {
      currentPlayer: currentPlayer.value?.name,
      myPlayer: myPlayer.value?.name,
      isFolded: myPlayer.value?.hasFolded,
      result
    });
    return result;
  });

  // --- WebSocket 方法 ---
  const connectWebSocket = () => {
    const wsUrl = `ws://${window.location.host}/ws/game`;
    if (ws.value && ws.value.readyState !== WebSocket.CLOSED) {
      return; // 防止重复连接
    }
    ws.value = new WebSocket(wsUrl);

    ws.value.onopen = () => {
      isConnected.value = true;
      addLog('成功连接到游戏服务器', 'success');
      uiStore.showToast('WebSocket连接成功', 'success');
    };

    ws.value.onmessage = (event) => {
      const message = JSON.parse(event.data);
      console.log('收到消息:', message);
      handleMessage(message);
    };

    ws.value.onclose = () => {
      isConnected.value = false;
      addLog('与服务器连接断开，3秒后尝试重连...', 'error');
      uiStore.showToast('WebSocket连接已断开', 'error');
      setTimeout(connectWebSocket, 3000);
    };

    ws.value.onerror = (error) => {
      console.error('WebSocket错误:', error);
      uiStore.showToast('连接发生错误', 'error');
      ws.value.close(); // 触发 onclose 中的重连逻辑
    };
  };

  const handleMessage = (message) => {
    switch (message.type) {
      case 'gameState':
        gameState.value = message.data;
        break;
      case 'joinResult':
        if (message.data?.success) {
          playerId.value = message.data.player.id;
          uiStore.showToast(`欢迎, ${message.data.player.name}!`, 'success');
          addLog(`您已成功加入游戏`, 'success');
        } else {
          uiStore.showToast(message.message || '加入游戏失败', 'error');
        }
        break;
      case 'error':
        uiStore.showToast(message.message, 'error');
        addLog(`错误: ${message.message}`, 'error');
        break;
      case 'playerDisconnected':
        addLog(`玩家 ${message.data.playerId} 已离开`, 'warning');
        break;
      default:
        if (message.message) {
          addLog(message.message, 'info');
        }
    }
  };

  // --- 游戏动作 ---
  const sendMessage = (message) => {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(message));
    } else {
      uiStore.showToast('未连接到服务器', 'error');
    }
  };

  const joinGame = (playerName, chips) => {
    sendMessage({ action: 'join', playerName, chips });
  };

  const playerAction = (actionType, amount = 0) => {
    if (!isMyTurn.value) {
      uiStore.showToast('还没轮到你的回合', 'warning');
      return;
    }
    addLog(`执行操作: ${actionType.toUpperCase()}`, 'info');
    sendMessage({ action: 'playerAction', actionType, amount });
  };

  const createAutoGame = () => sendMessage({ action: 'createAutoGame' });
  const startAutoGame = () => sendMessage({ action: 'startAutoGame' });
  const stopAutoGame = () => sendMessage({ action: 'stopAutoGame' });
  const resetGame = () => window.location.reload();

  // --- 工具方法 ---
  const addLog = (message, type = 'info') => {
    gameLogs.value.unshift({ message, type, timestamp: new Date().getTime() });
    if (gameLogs.value.length > 100) gameLogs.value.pop();
  };

  return {
    // 状态
    isConnected, playerId, gameLogs, gameState,
    // 计算属性
    pot, currentPhase, players, communityCards, currentPlayer,
    autoGameRunning, playerCards, isMyTurn, currentBet,
    // 方法
    connectWebSocket, joinGame, playerAction, createAutoGame,
    startAutoGame, stopAutoGame, resetGame
  };
});
