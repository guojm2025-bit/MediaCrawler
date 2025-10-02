package com.gjm.pk.service;

import com.gjm.pk.entity.Player;
import com.gjm.pk.service.impl.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 自动游戏管理器
 * 处理自动发牌、AI决策和游戏流程
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
@Slf4j
@Service
public class AutoGameManager {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private GameService gameService;
    
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    
    // 添加广播游戏状态方法
    private void broadcastGameState() {
        // 注意：这是一个简化的实现，实际应该注入WebSocket处理程序
        log.debug("AI玩家行动后广播游戏状态");
    }
    
    public AutoGameManager() {
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    // 时间常量（毫秒）
    private static final long AI_DECISION_DELAY = 2000;  // AI思考时间
    private static final long PHASE_TRANSITION_DELAY = 3000;  // 阶段转换延迟
    private static final long GAME_END_DELAY = 5000;  // 游戏结束后延迟
    private static final long HUMAN_PLAYER_TIMEOUT = 30000;  // 人类玩家超时
    
    /**
     * 开始自动游戏循环
     */
    public void startAutoGame() {
        if (isRunning) {
            log.warn("自动游戏已在运行中");
            return;
        }
        
        if (!gameService.canStartGame()) {
            log.error("无法开始游戏：玩家数量不足或玩家筹码不足");
            return;
        }
        
        isRunning = true;
        gameService.enableAutoGame();
        
        log.info("开始自动游戏循环");
        
        // 开始新一局
        startNewHand();
    }
    
    /**
     * 停止自动游戏循环
     */
    public void stopAutoGame() {
        isRunning = false;
        gameService.disableAutoGame();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = Executors.newScheduledThreadPool(2);
        }
        
        log.info("自动游戏循环已停止");
    }
    
    /**
     * 开始新一局
     */
    private void startNewHand() {
        if (!isRunning) return;
        
        try {
            // 检查游戏是否结束
            if (gameService.isGameOver()) {
                Player winner = gameService.getFinalWinner();
                log.info("游戏结束！最终胜利者: {}", winner != null ? winner.getName() : "无");
                stopAutoGame();
                return;
            }
            
            // 开始新局游戏
            gameService.startGame(gameService.getPlayers());
            
            // 等待片刻后开始处理玩家行动
            scheduleTask(() -> processNextPlayerAction(), AI_DECISION_DELAY);
            
        } catch (Exception e) {
            log.error("开始新局游戏时发生错误: {}", e.getMessage(), e);
            stopAutoGame();
        }
    }
    
    /**
     * 处理下一个玩家的行动
     */
    private void processNextPlayerAction() {
        if (!isRunning) return;
        
        try {
            Player currentPlayer = gameService.getCurrentPlayer();
            
            if (currentPlayer == null) {
                log.debug("没有当前玩家，可能需要进入下一阶段");
                handlePhaseTransition();
                return;
            }
            
            if (currentPlayer.isAi()) {
                // AI玩家自动决策
                handleAIPlayerAction(currentPlayer);
            } else {
                // 人类玩家，设置超时
                handleHumanPlayerAction(currentPlayer);
            }
            
        } catch (Exception e) {
            log.error("处理玩家行动时发生错误: {}", e.getMessage(), e);
            // 继续游戏，不停止整个循环
            scheduleTask(() -> processNextPlayerAction(), AI_DECISION_DELAY);
        }
    }
    
    /**
     * 处理AI玩家行动
     */
    private void handleAIPlayerAction(Player aiPlayer) {
        log.debug("处理AI玩家 {} 的行动", aiPlayer.getName());
        
        // AI玩家执行决策
        boolean actionSuccess = gameService.executeAIDecision(aiPlayer.getId());
        
        if (!actionSuccess) {
            log.warn("AI玩家 {} 行动失败", aiPlayer.getName());
        }
        
        // 广播游戏状态更新
        broadcastGameState();
        
        // 延迟后处理下一个玩家或阶段转换
        scheduleTask(() -> {
            if (gameService.getCurrentPhase() == GameService.GamePhase.FINISHED) {
                handleGameEnd();
            } else {
                processNextPlayerAction();
            }
        }, AI_DECISION_DELAY);
    }
    
    /**
     * 处理人类玩家行动（设置超时）
     */
    private void handleHumanPlayerAction(Player humanPlayer) {
        log.info("等待人类玩家 {} 的行动，游戏阶段: {}", humanPlayer.getName(), gameService.getCurrentPhase());
        
        // 设置超时，如果人类玩家在规定时间内没有行动，自动弃牌
        scheduleTask(() -> {
            Player currentPlayer = gameService.getCurrentPlayer();
            if (currentPlayer != null && currentPlayer.getId().equals(humanPlayer.getId())) {
                log.warn("人类玩家 {} 行动超时，自动弃牌", humanPlayer.getName());
                gameService.playerAction(humanPlayer.getId(), "fold", 0);
                
                // 继续处理下一个玩家
                scheduleTask(() -> processNextPlayerAction(), AI_DECISION_DELAY);
            }
        }, HUMAN_PLAYER_TIMEOUT);
        
        // 不要立即继续处理下一个玩家，等待人类玩家的行动
        log.debug("人类玩家 {} 的回合开始，等待玩家操作...", humanPlayer.getName());
    }
    
    /**
     * 处理阶段转换
     */
    private void handlePhaseTransition() {
        log.debug("处理阶段转换，当前阶段: {}", gameService.getCurrentPhase());
        
        GameService.GamePhase currentPhase = gameService.getCurrentPhase();
        
        switch (currentPhase) {
            case PRE_FLOP:
                // 自动进入翻牌阶段
                gameService.flop();
                break;
            case FLOP:
                // 自动进入转牌阶段
                gameService.turn();
                break;
            case TURN:
                // 自动进入河牌阶段
                gameService.river();
                break;
            case RIVER:
                // 河牌阶段结束，等待摊牌
                break;
            case SHOWDOWN:
            case FINISHED:
                handleGameEnd();
                return;
            default:
                break;
        }
        
        // 添加日志以跟踪公共牌变化
        log.info("阶段转换完成，当前阶段: {}，公共牌数量: {}", 
                gameService.getCurrentPhase(), 
                gameService.getCommunityCards().size());
        
        // 阶段转换后继续处理玩家行动
        scheduleTask(() -> processNextPlayerAction(), PHASE_TRANSITION_DELAY);
    }
    
    /**
     * 处理游戏结束
     */
    private void handleGameEnd() {
        log.info("当前局游戏结束，阶段: {}", gameService.getCurrentPhase());
        
        // 等待一段时间后开始下一局
        scheduleTask(() -> startNewHand(), GAME_END_DELAY);
    }
    
    /**
     * 人类玩家手动行动后的回调
     */
    public void onHumanPlayerAction(String playerId) {
        if (!isRunning) return;
        
        Player player = gameService.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
        
        if (player != null && !player.isAi()) {
            log.debug("人类玩家 {} 完成行动", player.getName());
            
            // 延迟后继续处理下一个玩家
            scheduleTask(() -> {
                if (gameService.getCurrentPhase() == GameService.GamePhase.FINISHED) {
                    handleGameEnd();
                } else {
                    processNextPlayerAction();
                }
            }, 1000); // 短暂延迟
        }
    }
    
    /**
     * 创建6人桌自动游戏
     */
    public void createSixPlayerAutoGame() {
        stopAutoGame(); // 停止当前游戏
        
        gameService.createSixPlayerAutoGame();
        
        log.info("已创建6人桌自动游戏");
    }
    
    /**
     * 添加真实玩家到自动游戏
     */
    public boolean addRealPlayerToAutoGame(String playerId, String playerName, int chips) {
        boolean added = gameService.addRealPlayer(playerId, playerName, chips);
        
        if (added) {
            log.info("真实玩家 {} 已加入自动游戏", playerName);
        }
        
        return added;
    }
    
    /**
     * 检查是否可以开始游戏
     */
    public boolean canStartGame() {
        return gameService.canStartGame();
    }
    
    /**
     * 获取游戏状态
     */
    public boolean isAutoGameRunning() {
        return isRunning;
    }
    
    /**
     * 辅助方法：调度任务
     */
    private void scheduleTask(Runnable task, long delay) {
        if (!isRunning || scheduler == null || scheduler.isShutdown()) {
            return;
        }
        
        scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
    }
}