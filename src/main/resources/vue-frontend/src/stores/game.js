import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUIStore } from './ui'

export const useGameStore = defineStore('game', () => {
  // 基础状态
  const isConnected = ref(false)
  const ws = ref(null)
  const playerId = ref(null)
  const gameState = ref(null)
  const autoGameRunning = ref(false)
  const gameLogs = ref([])
  
  // 游戏数据
  const pot = ref(0)
  const currentPhase = ref('WAITING')
  const players = ref([])
  const communityCards = ref([])
  const currentPlayer = ref(null)
  const currentBet = ref(0)
  
  // 计算属性
  const playerCards = computed(() => {
    const player = players.value.find(p => p.id === playerId.value)
    return player?.holeCards || []
  })
  
  const isMyTurn = computed(() => {
    // 添加调试日志
    const result = currentPlayer.value?.id === playerId.value
    console.log('isMyTurn计算:', {
      currentPlayerId: currentPlayer.value?.id,
      myPlayerId: playerId.value,
      result: result,
      currentPlayerName: currentPlayer.value?.name,
      gamePhase: currentPhase.value
    })
    return result
  })
  
  // WebSocket连接
  const connectWebSocket = () => {
    // 在开发环境下使用代理，生产环境下使用当前域名
    const isDevelopment = import.meta.env.DEV
    const wsUrl = isDevelopment 
      ? `ws://localhost:8080/ws/game`
      : `ws://${window.location.host}/ws/game`
      
    console.log('连接WebSocket:', wsUrl)
    ws.value = new WebSocket(wsUrl)
    
    ws.value.onopen = () => {
      isConnected.value = true
      addLog('连接到游戏服务器', 'success')
      useUIStore().showToast('WebSocket连接成功', 'success')
    }
    
    ws.value.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        handleMessage(message)
      } catch (e) {
        console.error('解析消息失败:', e)
      }
    }
    
    ws.value.onclose = () => {
      isConnected.value = false
      addLog('与服务器连接断开', 'error')
      useUIStore().showToast('WebSocket连接断开', 'error')
      
      // 尝试重新连接
      setTimeout(() => connectWebSocket(), 3000)
    }
    
    ws.value.onerror = (error) => {
      console.error('WebSocket错误:', error)
      useUIStore().showToast('连接错误', 'error')
    }
  }
  
  // 处理WebSocket消息
  const handleMessage = (message) => {
    console.log('收到消息:', message)
    
    switch (message.type) {
      case 'connection':
        addLog(message.message, 'info')
        break
      case 'joinResult':
        handleJoinResult(message)
        break
      case 'gameState':
        updateGameState(message.data)
        break
      case 'gameStarted':
        addLog('游戏开始!', 'success')
        break
      case 'playerJoined':
        addLog(`玩家 ${message.data.player.name} 加入游戏`, 'info')
        break
      case 'playerDisconnected':
        addLog(`玩家 ${message.data.playerId} 离开游戏`, 'warning')
        break
      case 'autoGameCreated':
        handleAutoGameCreated(message)
        break
      case 'autoGameStarted':
        handleAutoGameStarted(message)
        break
      case 'autoGameStopped':
        handleAutoGameStopped(message)
        break
      case 'error':
        useUIStore().showToast(message.message, 'error')
        addLog(`错误: ${message.message}`, 'error')
        break
      default:
        console.log('未知消息类型:', message.type)
    }
  }
  
  // 处理加入游戏结果
  const handleJoinResult = (message) => {
    if (message.data && message.data.success) {
      playerId.value = message.data.player.id
      useUIStore().showToast('成功加入游戏!', 'success')
      addLog(`以玩家 ${message.data.player.name} 身份加入游戏`, 'success')
    } else {
      useUIStore().showToast('加入游戏失败', 'error')
      addLog('加入游戏失败', 'error')
    }
  }
  
  // 更新游戏状态
  const updateGameState = (newGameState) => {
    console.log('更新游戏状态:', newGameState)
    gameState.value = newGameState
    
    // 更新基本信息
    pot.value = newGameState.pot || 0
    currentPhase.value = newGameState.currentPhase || 'WAITING'
    players.value = newGameState.players || []
    communityCards.value = newGameState.communityCards || []
    currentPlayer.value = newGameState.currentPlayer || null
    currentBet.value = newGameState.currentBet || 0
    
    // 添加详细日志
    console.log('游戏状态更新完成:', {
      玩家数量: players.value.length,
      当前玩家: currentPlayer.value?.name,
      当前阶段: currentPhase.value,
      奖池: pot.value
    })
  }
  
  // 自动游戏处理方法
  const handleAutoGameCreated = (message) => {
    if (message.data && message.data.success) {
      useUIStore().showToast('6人桌自动游戏已创建!', 'success')
      addLog(`已创建6人桌自动游戏，玩家数: ${message.data.playersCount}`, 'success')
    } else {
      useUIStore().showToast('创建自动游戏失败', 'error')
      addLog('创建自动游戏失败', 'error')
    }
  }
  
  const handleAutoGameStarted = (message) => {
    autoGameRunning.value = true
    useUIStore().showToast('自动游戏已开始!', 'success')
    addLog('自动游戏已开始，AI玩家将自动进行游戏', 'success')
  }
  
  const handleAutoGameStopped = (message) => {
    autoGameRunning.value = false
    useUIStore().showToast('自动游戏已停止', 'info')
    addLog('自动游戏已停止', 'info')
  }
  
  // 发送消息
  const sendMessage = (message) => {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(message))
    } else {
      useUIStore().showToast('连接已断开，无法发送消息', 'error')
    }
  }
  
  // 游戏操作方法
  const joinGame = (playerName, chips) => {
    if (!playerName) {
      useUIStore().showToast('请输入玩家名称', 'error')
      return
    }
    
    if (!isConnected.value) {
      useUIStore().showToast('未连接到服务器', 'error')
      return
    }
    
    const message = {
      action: 'join',
      playerId: 'player_' + Date.now(),
      playerName: playerName,
      chips: chips,
      isAi: false
    }
    
    sendMessage(message)
  }
  
  const startGame = () => {
    if (!isConnected.value) {
      useUIStore().showToast('未连接到服务器', 'error')
      return
    }
    
    sendMessage({ action: 'startGame' })
  }
  
  const resetGame = () => {
    if (!isConnected.value) {
      useUIStore().showToast('未连接到服务器', 'error')
      return
    }
    
    // 重新加载页面来重置游戏
    window.location.reload()
  }
  
  const playerAction = (actionType, amount = 0) => {
    if (!isConnected.value || !playerId.value) {
      useUIStore().showToast('请先加入游戏', 'error')
      return
    }
    
    const message = {
      action: 'playerAction',
      actionType: actionType,
      amount: amount
    }
    
    sendMessage(message)
    addLog(`执行操作: ${translateAction(actionType)}`, 'info')
  }
  
  const createAutoGame = () => {
    if (!isConnected.value) {
      useUIStore().showToast('未连接到服务器', 'error')
      return
    }
    
    sendMessage({ action: 'createAutoGame' })
    addLog('正在创建6人桌自动游戏...', 'info')
  }
  
  const startAutoGame = () => {
    if (!isConnected.value) {
      useUIStore().showToast('未连接到服务器', 'error')
      return
    }
    
    sendMessage({ action: 'startAutoGame' })
    addLog('正在开始自动游戏...', 'info')
  }
  
  const stopAutoGame = () => {
    if (!isConnected.value) {
      useUIStore().showToast('未连接到服务器', 'error')
      return
    }
    
    sendMessage({ action: 'stopAutoGame' })
    addLog('正在停止自动游戏...', 'info')
  }
  
  // 状态轮询
  const startStatusPolling = () => {
    setInterval(() => {
      if (isConnected.value) {
        fetchAutoGameStatus()
      }
    }, 5000) // 每5秒检查一次
  }
  
  const fetchAutoGameStatus = async () => {
    try {
      // 在开发环境下使用代理，生产环境下使用相对路径
      const isDevelopment = import.meta.env.DEV
      const apiUrl = isDevelopment 
        ? 'http://localhost:8080/api/game/auto/status'
        : '/api/game/auto/status'
        
      const response = await fetch(apiUrl)
      const data = await response.json()
      
      if (data.isAutoGameRunning !== autoGameRunning.value) {
        autoGameRunning.value = data.isAutoGameRunning
      }
      
      // 显示最终胜利者
      if (data.finalWinner) {
        useUIStore().showToast(`游戏结束! 胜利者: ${data.finalWinner}`, 'success')
        addLog(`游戏结束! 胜利者: ${data.finalWinner}`, 'success')
      }
    } catch (error) {
      console.error('获取自动游戏状态失败:', error)
    }
  }
  
  // 工具方法
  const translateAction = (action) => {
    const actions = {
      'fold': '弃牌',
      'check': '看牌',
      'call': '跟注', 
      'raise': '加注',
      'allin': '全下'
    }
    return actions[action] || action
  }
  
  const addLog = (message, type = 'info') => {
    const logEntry = {
      message,
      type,
      timestamp: new Date().getTime()
    }
    
    gameLogs.value.push(logEntry)
    
    // 限制日志条数
    if (gameLogs.value.length > 50) {
      gameLogs.value.shift()
    }
  }
  
  return {
    // 状态
    isConnected,
    playerId,
    gameState,
    autoGameRunning,
    gameLogs,
    pot,
    currentPhase,
    players,
    communityCards,
    currentPlayer,
    currentBet,
    
    // 计算属性
    playerCards,
    isMyTurn,
    
    // 方法
    connectWebSocket,
    joinGame,
    startGame,
    resetGame,
    playerAction,
    createAutoGame,
    startAutoGame,
    stopAutoGame,
    startStatusPolling,
    addLog
  }
})