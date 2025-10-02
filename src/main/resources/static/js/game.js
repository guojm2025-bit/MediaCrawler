// 德州扑克游戏客户端 - 6人桌自动游戏版本
class PokerGame {
    constructor() {
        this.ws = null;
        this.playerId = null;
        this.isConnected = false;
        this.gameState = null;
        this.autoGameRunning = false;
        
        this.initializeUI();
        this.connectWebSocket();
        this.startStatusPolling();
    }
    
    initializeUI() {
        // 绑定按钮事件
        document.getElementById('join-btn').addEventListener('click', () => this.joinGame());
        document.getElementById('start-btn').addEventListener('click', () => this.startGame());
        document.getElementById('reset-btn').addEventListener('click', () => this.resetGame());
        
        // 绑定自动游戏按钮
        document.getElementById('create-auto-btn').addEventListener('click', () => this.createAutoGame());
        document.getElementById('start-auto-btn').addEventListener('click', () => this.startAutoGame());
        document.getElementById('stop-auto-btn').addEventListener('click', () => this.stopAutoGame());
        
        // 绑定游戏操作按钮
        document.getElementById('fold-btn').addEventListener('click', () => this.playerAction('fold'));
        document.getElementById('check-btn').addEventListener('click', () => this.playerAction('check'));
        document.getElementById('call-btn').addEventListener('click', () => this.playerAction('call'));
        document.getElementById('raise-btn').addEventListener('click', () => this.playerAction('raise'));
        document.getElementById('allin-btn').addEventListener('click', () => this.playerAction('allin'));
        
        // 初始化操作按钮状态
        this.updateActionButtons(false);
    }
    
    connectWebSocket() {
        const wsUrl = `ws://${window.location.host}/ws/game`;
        this.ws = new WebSocket(wsUrl);
        
        this.ws.onopen = () => {
            this.isConnected = true;
            this.updateConnectionStatus();
            this.showMessage('WebSocket连接成功', 'success');
            this.addLog('连接到游戏服务器');
        };
        
        this.ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                this.handleMessage(message);
            } catch (e) {
                console.error('解析消息失败:', e);
            }
        };
        
        this.ws.onclose = () => {
            this.isConnected = false;
            this.updateConnectionStatus();
            this.showMessage('WebSocket连接断开', 'error');
            this.addLog('与服务器连接断开');
            
            // 尝试重新连接
            setTimeout(() => this.connectWebSocket(), 3000);
        };
        
        this.ws.onerror = (error) => {
            console.error('WebSocket错误:', error);
            this.showMessage('连接错误', 'error');
        };
    }
    
    handleMessage(message) {
        console.log('收到消息:', message);
        
        switch (message.type) {
            case 'connection':
                this.addLog(message.message);
                break;
            case 'joinResult':
                this.handleJoinResult(message);
                break;
            case 'gameState':
                this.updateGameState(message.data);
                break;
            case 'gameStarted':
                this.addLog('游戏开始!');
                break;
            case 'playerJoined':
                this.addLog(`玩家 ${message.data.player.name} 加入游戏`);
                break;
            case 'playerDisconnected':
                this.addLog(`玩家 ${message.data.playerId} 离开游戏`);
                break;
            case 'autoGameCreated':
                this.handleAutoGameCreated(message);
                break;
            case 'autoGameStarted':
                this.handleAutoGameStarted(message);
                break;
            case 'autoGameStopped':
                this.handleAutoGameStopped(message);
                break;
            case 'error':
                this.showMessage(message.message, 'error');
                break;
            default:
                console.log('未知消息类型:', message.type);
        }
    }
    
    handleJoinResult(message) {
        if (message.data && message.data.success) {
            this.playerId = message.data.player.id;
            this.showMessage('成功加入游戏!', 'success');
            this.addLog(`以玩家 ${message.data.player.name} 身份加入游戏`);
            
            // 禁用加入按钮
            document.getElementById('join-btn').disabled = true;
        } else {
            this.showMessage('加入游戏失败', 'error');
        }
    }
    
    updateGameState(gameState) {
        this.gameState = gameState;
        
        // 更新基本信息
        document.getElementById('pot').textContent = gameState.pot || 0;
        document.getElementById('phase').textContent = this.translatePhase(gameState.currentPhase);
        document.getElementById('player-count').textContent = (gameState.players || []).length;
        
        // 更新公共牌
        this.updateCommunityCards(gameState.communityCards || []);
        
        // 更新玩家信息（支持6人桌）
        this.updatePlayers(gameState.players || []);
        
        // 更新玩家手牌
        this.updatePlayerCards(gameState);
        
        // 更新操作按钮
        this.updateActionButtons(this.isMyTurn(gameState));
    }
    
    updateCommunityCards(cards) {
        const container = document.getElementById('community-cards');
        container.innerHTML = '';
        
        for (let i = 0; i < 5; i++) {
            const cardDiv = document.createElement('div');
            cardDiv.className = 'card';
            
            if (i < cards.length) {
                const card = cards[i];
                cardDiv.textContent = card.display;
                cardDiv.className += card.suitName === '红桃' || card.suitName === '方块' ? ' red' : ' black';
            } else {
                cardDiv.className += ' placeholder';
                cardDiv.textContent = '?';
            }
            
            container.appendChild(cardDiv);
        }
    }
    
    updatePlayers(players) {
        const container = document.getElementById('players-area');
        container.innerHTML = '';
        
        // 支持6人桌布局
        players.forEach((player, index) => {
            const playerDiv = document.createElement('div');
            playerDiv.className = `player pos-${index} player-seat-${index + 1}`;
            
            if (player.id === this.playerId) {
                playerDiv.className += ' current-player';
            }
            
            if (this.gameState && this.gameState.currentPlayer && 
                this.gameState.currentPlayer.id === player.id) {
                playerDiv.className += ' active';
            }
            
            if (player.hasFolded) {
                playerDiv.className += ' folded';
            }
            
            if (player.isAi) {
                playerDiv.className += ' ai-player';
            }
            
            // 特殊标记
            let badges = '';
            if (player.isDealer) badges += '<span class="badge dealer">D</span>';
            if (player.isSmallBlind) badges += '<span class="badge sb">SB</span>';
            if (player.isBigBlind) badges += '<span class="badge bb">BB</span>';
            
            playerDiv.innerHTML = `
                <div class="player-name">${player.name} ${badges}</div>
                <div class="player-chips">筹码: $${player.chips}</div>
                ${player.currentBet > 0 ? `<div class="player-bet">下注: $${player.currentBet}</div>` : ''}
                <div class="player-status">${this.getPlayerStatus(player)}</div>
                ${player.isAi ? '<div class="ai-indicator">AI</div>' : ''}
            `;
            
            container.appendChild(playerDiv);
        });
        
        // 为空位置添加占位符
        for (let i = players.length; i < 6; i++) {
            const emptyDiv = document.createElement('div');
            emptyDiv.className = `player empty-seat pos-${i} player-seat-${i + 1}`;
            emptyDiv.innerHTML = `
                <div class="empty-seat-label">空位</div>
            `;
            container.appendChild(emptyDiv);
        }
    }
    
    updatePlayerCards(gameState) {
        const container = document.getElementById('player-cards');
        container.innerHTML = '';
        
        // 找到当前玩家的手牌
        const currentPlayer = gameState.players?.find(p => p.id === this.playerId);
        const holeCards = currentPlayer?.holeCards || [];
        
        for (let i = 0; i < 2; i++) {
            const cardDiv = document.createElement('div');
            cardDiv.className = 'card';
            
            if (i < holeCards.length) {
                const card = holeCards[i];
                cardDiv.textContent = card.display;
                cardDiv.className += card.suitName === '红桃' || card.suitName === '方块' ? ' red' : ' black';
            } else {
                cardDiv.className += ' placeholder';
                cardDiv.textContent = '?';
            }
            
            container.appendChild(cardDiv);
        }
    }
    
    updateActionButtons(isMyTurn) {
        const buttons = ['fold-btn', 'check-btn', 'call-btn', 'raise-btn', 'allin-btn'];
        buttons.forEach(btnId => {
            document.getElementById(btnId).disabled = !isMyTurn;
        });
        
        document.getElementById('bet-amount').disabled = !isMyTurn;
    }
    
    isMyTurn(gameState) {
        return gameState?.currentPlayer?.id === this.playerId;
    }
    
    getPlayerStatus(player) {
        if (player.hasFolded) return '已弃牌';
        if (player.isAllIn) return '全下';
        if (player.lastAction) {
            const actions = {
                'FOLD': '弃牌',
                'CHECK': '看牌', 
                'CALL': '跟注',
                'RAISE': '加注',
                'ALL_IN': '全下'
            };
            return actions[player.lastAction] || player.lastAction;
        }
        return '等待中';
    }
    
    translatePhase(phase) {
        const phases = {
            'WAITING': '等待中',
            'PRE_FLOP': '翻牌前',
            'FLOP': '翻牌',
            'TURN': '转牌',
            'RIVER': '河牌',
            'SHOWDOWN': '摊牌',
            'FINISHED': '结束'
        };
        return phases[phase] || phase;
    }
    
    joinGame() {
        const name = document.getElementById('player-name').value.trim();
        const chips = parseInt(document.getElementById('player-chips').value) || 1000;
        
        if (!name) {
            this.showMessage('请输入玩家名称', 'error');
            return;
        }
        
        if (!this.isConnected) {
            this.showMessage('未连接到服务器', 'error');
            return;
        }
        
        const message = {
            action: 'join',
            playerId: 'player_' + Date.now(),
            playerName: name,
            chips: chips,
            isAi: false
        };
        
        this.sendMessage(message);
    }
    
    startGame() {
        if (!this.isConnected) {
            this.showMessage('未连接到服务器', 'error');
            return;
        }
        
        const message = {
            action: 'startGame'
        };
        
        this.sendMessage(message);
    }
    
    resetGame() {
        if (!this.isConnected) {
            this.showMessage('未连接到服务器', 'error');
            return;
        }
        
        // 重新加载页面来重置游戏
        window.location.reload();
    }
    
    playerAction(actionType) {
        if (!this.isConnected || !this.playerId) {
            this.showMessage('请先加入游戏', 'error');
            return;
        }
        
        let amount = 0;
        if (actionType === 'raise') {
            amount = parseInt(document.getElementById('bet-amount').value) || 0;
            if (amount <= 0) {
                this.showMessage('请输入有效的加注金额', 'error');
                return;
            }
        }
        
        const message = {
            action: 'playerAction',
            actionType: actionType,
            amount: amount
        };
        
        this.sendMessage(message);
        this.addLog(`执行操作: ${this.translateAction(actionType)}`);
    }
    
    // ===== 自动游戏方法 =====
    
    createAutoGame() {
        if (!this.isConnected) {
            this.showMessage('未连接到服务器', 'error');
            return;
        }
        
        const message = {
            action: 'createAutoGame'
        };
        
        this.sendMessage(message);
        this.addLog('正在创建6人桌自动游戏...');
    }
    
    startAutoGame() {
        if (!this.isConnected) {
            this.showMessage('未连接到服务器', 'error');
            return;
        }
        
        const message = {
            action: 'startAutoGame'
        };
        
        this.sendMessage(message);
        this.addLog('正在开始自动游戏...');
    }
    
    stopAutoGame() {
        if (!this.isConnected) {
            this.showMessage('未连接到服务器', 'error');
            return;
        }
        
        const message = {
            action: 'stopAutoGame'
        };
        
        this.sendMessage(message);
        this.addLog('正在停止自动游戏...');
    }
    
    handleAutoGameCreated(message) {
        if (message.data && message.data.success) {
            this.showMessage('6人桌自动游戏已创建!', 'success');
            this.addLog(`已创建6人桌自动游戏，玩家数: ${message.data.playersCount}`);
            
            // 更新按钮状态
            document.getElementById('create-auto-btn').disabled = true;
            document.getElementById('start-auto-btn').disabled = false;
        } else {
            this.showMessage('创建自动游戏失败', 'error');
        }
    }
    
    handleAutoGameStarted(message) {
        this.autoGameRunning = true;
        this.showMessage('自动游戏已开始!', 'success');
        this.addLog('自动游戏已开始，AI玩家将自动进行游戏');
        
        // 更新按钮状态
        document.getElementById('start-auto-btn').disabled = true;
        document.getElementById('stop-auto-btn').disabled = false;
        document.getElementById('auto-status').textContent = '开启';
    }
    
    handleAutoGameStopped(message) {
        this.autoGameRunning = false;
        this.showMessage('自动游戏已停止', 'info');
        this.addLog('自动游戏已停止');
        
        // 更新按钮状态
        document.getElementById('start-auto-btn').disabled = false;
        document.getElementById('stop-auto-btn').disabled = true;
        document.getElementById('auto-status').textContent = '关闭';
    }
    
    // 定期检查游戏状态
    startStatusPolling() {
        setInterval(() => {
            if (this.isConnected) {
                this.fetchAutoGameStatus();
            }
        }, 5000); // 每5秒检查一次
    }
    
    fetchAutoGameStatus() {
        fetch('/api/game/auto/status')
            .then(response => response.json())
            .then(data => {
                if (data.isAutoGameRunning !== this.autoGameRunning) {
                    this.autoGameRunning = data.isAutoGameRunning;
                    document.getElementById('auto-status').textContent = 
                        this.autoGameRunning ? '开启' : '关闭';
                }
                
                // 更新按钮状态
                document.getElementById('start-auto-btn').disabled = this.autoGameRunning;
                document.getElementById('stop-auto-btn').disabled = !this.autoGameRunning;
                
                // 更新玩家数量
                if (data.gameStats && data.gameStats.totalPlayers) {
                    document.getElementById('player-count').textContent = data.gameStats.totalPlayers;
                }
                
                // 显示最终胜利者
                if (data.finalWinner) {
                    this.showMessage(`游戏结束! 胜利者: ${data.finalWinner}`, 'success');
                }
            })
            .catch(error => {
                console.error('获取自动游戏状态失败:', error);
            });
    }
    
    translateAction(action) {
        const actions = {
            'fold': '弃牌',
            'check': '看牌',
            'call': '跟注', 
            'raise': '加注',
            'allin': '全下'
        };
        return actions[action] || action;
    }
    
    sendMessage(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        } else {
            this.showMessage('连接已断开，无法发送消息', 'error');
        }
    }
    
    updateConnectionStatus() {
        const statusElement = document.getElementById('connection-status');
        if (this.isConnected) {
            statusElement.textContent = '已连接';
            statusElement.className = 'connected';
        } else {
            statusElement.textContent = '未连接';
            statusElement.className = 'disconnected';
        }
    }
    
    showMessage(message, type = 'info') {
        const toast = document.getElementById('message-toast');
        toast.textContent = message;
        toast.className = `message-toast ${type} show`;
        
        setTimeout(() => {
            toast.className = toast.className.replace('show', '');
        }, 3000);
    }
    
    addLog(message) {
        const logContainer = document.getElementById('game-log');
        const logEntry = document.createElement('div');
        logEntry.className = 'log-entry';
        logEntry.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
        
        logContainer.appendChild(logEntry);
        logContainer.scrollTop = logContainer.scrollHeight;
        
        // 限制日志条数
        while (logContainer.children.length > 50) {
            logContainer.removeChild(logContainer.firstChild);
        }
    }
}

// 初始化游戏
window.addEventListener('DOMContentLoaded', () => {
    new PokerGame();
});