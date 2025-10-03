package com.gjm.pk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gjm.pk.entity.Card;
import com.gjm.pk.entity.Player;
import com.gjm.pk.service.impl.GameService;
import com.gjm.pk.service.AutoGameManager;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
 * 游戏WebSocket处理器 (已修复)
 * @author: guojianming
 * @data 2025/09/17 17:49
 */
@Slf4j
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();

    private final GameService gameService;
    private final AutoGameManager autoGameManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public GameWebSocketHandler(@Lazy GameService gameService, @Lazy AutoGameManager autoGameManager) {
        this.gameService = gameService;
        this.autoGameManager = autoGameManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("新的WebSocket连接建立: {}", session.getId());
        sendToSession(session, createMessage("connection", "连接成功", null));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String playerId = sessionToPlayerId.remove(sessionId);

        if (playerId != null) {
            log.info("玩家 {} 断开连接", playerId);
            gameService.removePlayer(playerId);
            broadcastPlayerDisconnected(playerId);
            broadcastGameState(); // 广播状态让其他客户端更新玩家列表
        }

        sessions.remove(sessionId);
        log.info("WebSocket连接关闭: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> request = objectMapper.readValue(message.getPayload(), Map.class);
            String action = (String) request.get("action");

            log.info("收到来自 {} 的消息: {}", session.getId(), message.getPayload());

            switch (action) {
                case "join":
                    handleJoinGame(session, request);
                    break;
                case "playerAction":
                    handlePlayerAction(session, request);
                    break;
                case "createAutoGame":
                    autoGameManager.createSixPlayerAutoGame();
                    break;
                case "startAutoGame":
                    autoGameManager.startAutoGame();
                    break;
                case "stopAutoGame":
                    autoGameManager.stopAutoGame();
                    break;
                case "getGameState":
                    sendGameState(session);
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

    private void handleJoinGame(WebSocketSession session, Map<String, Object> request) {
        String playerName = (String) request.get("playerName");
        Integer chips = (Integer) request.get("chips");
        String playerId = "player_" + System.nanoTime(); // 由后端生成可靠的ID

        sessionToPlayerId.put(session.getId(), playerId);

        boolean added = gameService.addRealPlayer(playerId, playerName, chips != null ? chips : 1000);

        if (added) {
            Player player = gameService.findPlayerById(playerId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("player", convertPlayerToMap(player));
            sendToSession(session, createMessage("joinResult", "加入成功", response));

            log.info("玩家 {} ({}) 加入游戏", playerName, playerId);
        } else {
            sendError(session, "加入游戏失败，可能游戏已满或已开始");
        }
    }

    private void handlePlayerAction(WebSocketSession session, Map<String, Object> request) {
        String playerId = sessionToPlayerId.get(session.getId());
        if (playerId == null) {
            sendError(session, "请先加入游戏");
            return;
        }

        if (!gameService.isPlayerTurn(playerId)) {
            sendError(session, "不是你的回合");
            return;
        }

        String actionType = (String) request.get("actionType");
        Integer amount = request.get("amount") instanceof Integer ? (Integer) request.get("amount") : 0;

        boolean success = gameService.playerAction(playerId, actionType, amount);

        if (success && autoGameManager.isAutoGameRunning()) {
            autoGameManager.onHumanPlayerAction(playerId);
        }
    }

    public void sendGameState(WebSocketSession session) {
        String playerId = sessionToPlayerId.get(session.getId());
        Map<String, Object> gameState = buildGameState(playerId);
        sendToSession(session, createMessage("gameState", "游戏状态更新", gameState));
    }

    public void broadcastGameState() {
        log.debug("准备广播游戏状态...");
        sessions.values().forEach(this::sendGameState);
        Player currentPlayer = gameService.getCurrentPlayer();
        log.info("游戏状态已广播给所有 {} 个连接。当前轮到: {} (ID: {})", sessions.size(),
                currentPlayer != null ? currentPlayer.getName() : "无",
                currentPlayer != null ? currentPlayer.getId() : "无");
    }

    private void broadcastPlayerDisconnected(String playerId) {
        broadcast(createMessage("playerDisconnected", "玩家断开", Collections.singletonMap("playerId", playerId)));
    }

    private Map<String, Object> buildGameState(String recipientPlayerId) {
        Map<String, Object> gameState = new HashMap<>();

        gameState.put("pot", gameService.getPot());
        gameState.put("currentBetAmount", gameService.getCurrentBetAmount());
        gameState.put("currentPhase", gameService.getCurrentPhase().toString());

        gameState.put("communityCards", gameService.getCommunityCards().stream()
                .map(this::convertCardToMap)
                .collect(Collectors.toList()));

        gameState.put("players", gameService.getPlayers().stream()
                .map(player -> {
                    Map<String, Object> playerMap = convertPlayerToMap(player);
                    // 只对当前玩家或者在摊牌阶段显示手牌
                    if (player.getId().equals(recipientPlayerId) || gameService.getCurrentPhase() == GameService.GamePhase.SHOWDOWN) {
                        playerMap.put("holeCards", player.getHoleCards().stream()
                                .map(this::convertCardToMap)
                                .collect(Collectors.toList()));
                    }
                    return playerMap;
                })
                .collect(Collectors.toList()));

        Player currentPlayer = gameService.getCurrentPlayer();
        if (currentPlayer != null) {
            gameState.put("currentPlayer", convertPlayerToMap(currentPlayer));
        }

        gameState.put("isAutoGameRunning", autoGameManager.isAutoGameRunning());

        return gameState;
    }

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

    private void broadcast(Object message) {
        sessions.values().forEach(session -> sendToSession(session, message));
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        sendToSession(session, createMessage("error", errorMessage, null));
    }

    private void sendToSession(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("发送消息到 session {} 失败: {}", session.getId(), e.getMessage());
        }
    }

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
}
