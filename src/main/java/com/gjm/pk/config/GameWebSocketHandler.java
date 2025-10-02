package com.gjm.pk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gjm.pk.entity.Card;
import com.gjm.pk.entity.Player;
import com.gjm.pk.service.impl.GameService;
import com.gjm.pk.service.AutoGameManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 游戏WebSocket处理器
 * @author: guojianming
 * @data 2025/09/17 17:49
 */
@Slf4j
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private final Map<String, String> playerIdToSessionId = new ConcurrentHashMap<>();
    
    @Autowired
    private GameService gameService;
    
    @Autowired
    private AutoGameManager autoGameManager;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("新的WebSocket连接建立: {}", session.getId());
        
        // 发送欢迎消息
        sendToSession(session, createMessage("connection", "连接成功", null));
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String playerId = sessionToPlayerId.remove(sessionId);
        
        if (playerId != null) {
            playerIdToSessionId.remove(playerId);
            log.info("玩家 {} 断开连接", playerId);
            
            // 通知其他玩家
            broadcastPlayerDisconnected(playerId);
        }
        
        sessions.remove(sessionId);
        log.info("WebSocket连接关闭: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> request = objectMapper.readValue(message.getPayload(), Map.class);
            String action = (String) request.get("action");
            
            log.info("收到消息: {}", message.getPayload());
            
            switch (action) {
                case "join":
                    handleJoinGame(session, request);
                    break;
                case "playerAction":
                    handlePlayerAction(session, request);
                    break;
                case "startGame":
                    handleStartGame(session, request);
                    break;
                case "createAutoGame":
                    handleCreateAutoGame(session, request);
                    break;
                case "startAutoGame":
                    handleStartAutoGame(session, request);
                    break;
                case "stopAutoGame":
                    handleStopAutoGame(session, request);
                    break;
                case "getGameState":
                    handleGetGameState(session, request);
                    break;
                default:
                    sendError(session, "未知的操作: " + action);
                    break;
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", e.getMessage(), e);
            sendError(session, "处理消息失败: " + e.getMessage());
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
    }
    
    /**
     * 处理加入游戏
     */
    private void handleJoinGame(WebSocketSession session, Map<String, Object> request) {
        try {
            String playerId = (String) request.get("playerId");
            String playerName = (String) request.get("playerName");
            Integer chips = (Integer) request.get("chips");
            Boolean isAi = (Boolean) request.get("isAi");
            
            if (playerId == null || playerName == null) {
                sendError(session, "缺少必要参数");
                return;
            }
            
            // 记录玩家与会话的对应关系
            sessionToPlayerId.put(session.getId(), playerId);
            playerIdToSessionId.put(playerId, session.getId());
            
            // 创建玩家对象
            Player player = new Player(playerId, playerName, 
                                     chips != null ? chips : 1000, 
                                     isAi != null ? isAi : false);
            
            // 将玩家添加到游戏中
            gameService.addRealPlayer(playerId, playerName, chips != null ? chips : 1000);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "加入游戏成功");
            response.put("player", convertPlayerToMap(player));
            
            sendToSession(session, createMessage("joinResult", "加入成功", response));
            
            // 广播玩家加入消息
            broadcastPlayerJoined(player);
            
            // 立即广播游戏状态，让新加入的玩家看到当前游戏状态
            broadcastGameState();
            
            log.info("玩家 {} ({}) 加入游戏", playerName, playerId);
            
        } catch (Exception e) {
            log.error("处理加入游戏失败: {}", e.getMessage(), e);
            sendError(session, "加入游戏失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理玩家行动
     */
    private void handlePlayerAction(WebSocketSession session, Map<String, Object> request) {
        try {
            String playerId = sessionToPlayerId.get(session.getId());
            if (playerId == null) {
                sendError(session, "请先加入游戏");
                return;
            }
            
            String actionType = (String) request.get("actionType");
            Integer amount = (Integer) request.get("amount");
            
            if (actionType == null) {
                sendError(session, "缺少行动类型");
                return;
            }
            
            log.info("玩家 {} 执行行动: {}, 金额: {}", playerId, actionType, amount);
            
            boolean success = gameService.playerAction(playerId, actionType, amount != null ? amount : 0);
            
            if (success) {
                // 通知自动游戏管理器人类玩家完成了行动
                if (autoGameManager.isAutoGameRunning()) {
                    autoGameManager.onHumanPlayerAction(playerId);
                }
                
                // 立即广播游戏状态更新
                broadcastGameState();
                log.info("玩家 {} 行动成功，已广播游戏状态", playerId);
            } else {
                sendError(session, "行动失败");
                log.warn("玩家 {} 行动失败: {}", playerId, actionType);
            }
            
        } catch (Exception e) {
            log.error("处理玩家行动失败: {}", e.getMessage(), e);
            sendError(session, "处理行动失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理开始游戏
     */
    private void handleStartGame(WebSocketSession session, Map<String, Object> request) {
        try {
            List<Player> players = gameService.getPlayers();
            
            if (players.size() < 2) {
                sendError(session, "至少需要两名玩家才能开始游戏");
                return;
            }
            
            gameService.startGame(players);
            
            // 广播游戏开始消息
            broadcastGameStarted();
            broadcastGameState();
            
            log.info("游戏开始，参与玩家数: {}", players.size());
            
        } catch (Exception e) {
            log.error("开始游戏失败: {}", e.getMessage(), e);
            sendError(session, "开始游戏失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理获取游戏状态
     */
    private void handleGetGameState(WebSocketSession session, Map<String, Object> request) {
        try {
            String playerId = sessionToPlayerId.get(session.getId());
            Map<String, Object> gameState = buildGameState(playerId);
            sendToSession(session, createMessage("gameState", "游戏状态", gameState));
        } catch (Exception e) {
            log.error("获取游戏状态失败: {}", e.getMessage(), e);
            sendError(session, "获取游戏状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理创建自动游戏
     */
    private void handleCreateAutoGame(WebSocketSession session, Map<String, Object> request) {
        try {
            autoGameManager.createSixPlayerAutoGame();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "6人桌自动游戏已创建");
            response.put("playersCount", gameService.getPlayers().size());
            
            sendToSession(session, createMessage("autoGameCreated", "自动游戏已创建", response));
            
            // 广播游戏状态更新
            broadcastGameState();
            
            log.info("已创建6人桌自动游戏");
            
        } catch (Exception e) {
            log.error("创建自动游戏失败: {}", e.getMessage(), e);
            sendError(session, "创建自动游戏失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理开始自动游戏
     */
    private void handleStartAutoGame(WebSocketSession session, Map<String, Object> request) {
        try {
            if (!autoGameManager.canStartGame()) {
                sendError(session, "无法开始游戏：玩家数量不足或玩家筹码不足");
                return;
            }
            
            autoGameManager.startAutoGame();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "自动游戏已开始");
            response.put("isRunning", autoGameManager.isAutoGameRunning());
            
            sendToSession(session, createMessage("autoGameStarted", "自动游戏已开始", response));
            
            // 广播游戏开始消息
            broadcast(createMessage("autoGameStarted", "自动游戏已开始", null));
            
            // 立即广播游戏状态，确保客户端收到最新状态
            broadcastGameState();
            
            log.info("自动游戏已开始");
            
        } catch (Exception e) {
            log.error("开始自动游戏失败: {}", e.getMessage(), e);
            sendError(session, "开始自动游戏失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理停止自动游戏
     */
    private void handleStopAutoGame(WebSocketSession session, Map<String, Object> request) {
        try {
            autoGameManager.stopAutoGame();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "自动游戏已停止");
            response.put("isRunning", autoGameManager.isAutoGameRunning());
            
            sendToSession(session, createMessage("autoGameStopped", "自动游戏已停止", response));
            
            // 广播游戏停止消息
            broadcast(createMessage("autoGameStopped", "自动游戏已停止", null));
            
            log.info("自动游戏已停止");
            
        } catch (Exception e) {
            log.error("停止自动游戏失败: {}", e.getMessage(), e);
            sendError(session, "停止自动游戏失败: " + e.getMessage());
        }
    }
    
    /**
     * 广播游戏状态更新
     */
    private void broadcastGameState() {
        try {
            for (Map.Entry<String, String> entry : sessionToPlayerId.entrySet()) {
                String sessionId = entry.getKey();
                String playerId = entry.getValue();
                WebSocketSession session = sessions.get(sessionId);
                
                if (session != null && session.isOpen()) {
                    Map<String, Object> gameState = buildGameState(playerId);
                    
                    // 添加更详细的调试日志
                    Player currentPlayer = gameService.getCurrentPlayer();
                    log.info("发送游戏状态更新给玩家 {} - 当前玩家: {} (ID: {}), 阶段: {}, 公共牌数量: {}, 玩家数量: {}", 
                            playerId,
                            currentPlayer != null ? currentPlayer.getName() : "null",
                            currentPlayer != null ? currentPlayer.getId() : "null",
                            gameService.getCurrentPhase(),
                            ((List<?>) gameState.get("communityCards")).size(),
                            ((List<?>) gameState.get("players")).size());
                    
                    sendToSession(session, createMessage("gameState", "游戏状态更新", gameState));
                }
            }
        } catch (Exception e) {
            log.error("广播游戏状态失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 广播玩家加入消息
     */
    private void broadcastPlayerJoined(Player player) {
        Map<String, Object> data = new HashMap<>();
        data.put("player", convertPlayerToMap(player));
        broadcast(createMessage("playerJoined", "玩家加入", data));
    }
    
    /**
     * 广播玩家断开消息
     */
    private void broadcastPlayerDisconnected(String playerId) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", playerId);
        broadcast(createMessage("playerDisconnected", "玩家断开", data));
    }
    
    /**
     * 广播游戏开始消息
     */
    private void broadcastGameStarted() {
        broadcast(createMessage("gameStarted", "游戏开始", null));
    }
    
    /**
     * 构建游戏状态数据
     */
    private Map<String, Object> buildGameState(String currentPlayerId) {
        Map<String, Object> gameState = new HashMap<>();
        
        // 基本游戏信息
        gameState.put("pot", gameService.getPot());
        gameState.put("currentBetAmount", gameService.getCurrentBetAmount());
        gameState.put("currentPhase", gameService.getCurrentPhase().toString());
        gameState.put("currentPlayerTurn", gameService.getCurrentPlayerTurn());
        
        // 公共牌
        List<Map<String, Object>> communityCards = gameService.getCommunityCards().stream()
                .map(this::convertCardToMap)
                .collect(Collectors.toList());
        gameState.put("communityCards", communityCards);
        
        // 玩家信息
        List<Map<String, Object>> players = gameService.getPlayers().stream()
                .map(player -> {
                    Map<String, Object> playerMap = convertPlayerToMap(player);
                    // 只对当前玩家显示手牌
                    if (player.getId().equals(currentPlayerId)) {
                        List<Map<String, Object>> holeCards = player.getHoleCards().stream()
                                .map(this::convertCardToMap)
                                .collect(Collectors.toList());
                        playerMap.put("holeCards", holeCards);
                    }
                    return playerMap;
                })
                .collect(Collectors.toList());
        gameState.put("players", players);
        
        // 当前玩家信息
        Player currentPlayer = gameService.getCurrentPlayer();
        if (currentPlayer != null) {
            gameState.put("currentPlayer", convertPlayerToMap(currentPlayer));
        }
        
        return gameState;
    }
    
    /**
     * 将玩家对象转换为Map
     */
    private Map<String, Object> convertPlayerToMap(Player player) {
        Map<String, Object> playerMap = new HashMap<>();
        playerMap.put("id", player.getId());
        playerMap.put("name", player.getName());
        playerMap.put("chips", player.getChips());
        playerMap.put("currentBet", player.getCurrentBet());
        playerMap.put("inGame", player.isInGame());
        playerMap.put("isAi", player.isAi());
        playerMap.put("hasFolded", player.isHasFolded());
        playerMap.put("isAllIn", player.isAllIn());
        playerMap.put("isDealer", player.isDealer());
        playerMap.put("isSmallBlind", player.isSmallBlind());
        playerMap.put("isBigBlind", player.isBigBlind());
        if (player.getLastAction() != null) {
            playerMap.put("lastAction", player.getLastAction().toString());
        }
        return playerMap;
    }
    
    /**
     * 将牌对象转换为Map
     */
    private Map<String, Object> convertCardToMap(Card card) {
        Map<String, Object> cardMap = new HashMap<>();
        cardMap.put("suit", card.getSuit());
        cardMap.put("rank", card.getRank());
        cardMap.put("suitName", card.getSuitName());
        cardMap.put("rankName", card.getRankName());
        cardMap.put("display", card.toString());
        cardMap.put("shortDisplay", card.toShortString());
        return cardMap;
    }
    
    /**
     * 创建消息
     */
    private Map<String, Object> createMessage(String type, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
    
    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = createMessage("error", errorMessage, null);
            sendToSession(session, error);
        } catch (Exception e) {
            log.error("发送错误消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送消息到指定会话
     */
    private void sendToSession(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("发送消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 广播消息给所有连接
     */
    private void broadcast(Object message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("序列化消息失败: {}", e.getMessage(), e);
            return;
        }
        
        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.error("广播消息失败: {}", e.getMessage());
            }
        }
    }
}