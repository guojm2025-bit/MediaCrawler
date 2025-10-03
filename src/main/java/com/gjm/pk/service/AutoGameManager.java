package com.gjm.pk.service;

import com.gjm.pk.config.GameWebSocketHandler;
import com.gjm.pk.entity.Player;
import com.gjm.pk.service.impl.GameService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

/**
 * 自动游戏管理器 (已修复)
 * @author: guojianming
 * @date: 2025/09/23
 */
@Slf4j
@Service
public class AutoGameManager {

    private final GameService gameService;
    private GameWebSocketHandler webSocketHandler;

    private ScheduledExecutorService scheduler;
    @Getter private volatile boolean isRunning = false;
    private ScheduledFuture<?> humanPlayerTimeoutTask;

    private static final long AI_DECISION_DELAY = 2000;
    private static final long GAME_END_DELAY = 5000;
    private static final long HUMAN_PLAYER_TIMEOUT = 30000;

    @Autowired
    public AutoGameManager(GameService gameService) {
        this.gameService = gameService;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    @Autowired
    public void setWebSocketHandler(@Lazy GameWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 获取游戏状态
     */
    public boolean isAutoGameRunning() {
        return isRunning;
    }
    public void startAutoGame() {
        if (isRunning) {
            log.warn("自动游戏已在运行中");
            return;
        }
        if (!canStartGame()) {
            log.error("无法开始游戏：玩家数量不足或筹码不足");
            return;
        }

        isRunning = true;
        log.info("开始自动游戏循环");
        startNewHand();
    }

    public void stopAutoGame() {
        if (!isRunning) return;
        isRunning = false;

        if (humanPlayerTimeoutTask != null) {
            humanPlayerTimeoutTask.cancel(true);
        }

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = Executors.newScheduledThreadPool(2);
        }

        log.info("自动游戏循环已停止");
        webSocketHandler.broadcastGameState();
    }

    private void startNewHand() {
        if (!isRunning || gameService.isGameOver()) {
            log.info("游戏结束或自动模式已停止");
            stopAutoGame();
            return;
        }

        // GameService.startGame 内部会广播状态
        gameService.startGame(gameService.getPlayers());

        // startGame会设置好第一个行动的玩家，我们只需触发流程
        scheduleTask(this::processNextPlayerAction, AI_DECISION_DELAY);
    }

    private void processNextPlayerAction() {
        if (!isRunning) return;

        if (gameService.getCurrentPhase() == GameService.GamePhase.FINISHED) {
            log.info("本局结束，{}ms后开始新一局", GAME_END_DELAY);
            scheduleTask(this::startNewHand, GAME_END_DELAY);
            return;
        }

        Player currentPlayer = gameService.getCurrentPlayer();

        if (currentPlayer == null) {
            log.debug("当前无行动玩家，等待阶段转换...");
            return;
        }

        if (currentPlayer.isAi()) {
            handleAIPlayerAction(currentPlayer);
        } else {
            handleHumanPlayerAction(currentPlayer);
        }
    }

    private void handleAIPlayerAction(Player aiPlayer) {
        log.debug("处理AI玩家 {} 的行动", aiPlayer.getName());
        boolean success = gameService.executeAIDecision(aiPlayer.getId());
        if (success) {
            // GameService内部会广播并触发下一轮或阶段转换，这里只需安排下一次AI行动
            scheduleTask(this::processNextPlayerAction, AI_DECISION_DELAY);
        }
    }

    private void handleHumanPlayerAction(Player humanPlayer) {
        log.info("等待人类玩家 {} 的行动，阶段: {}", humanPlayer.getName(), gameService.getCurrentPhase());

        if (humanPlayerTimeoutTask != null) {
            humanPlayerTimeoutTask.cancel(true);
        }

        humanPlayerTimeoutTask = scheduleTask(() -> {
            if (isRunning && gameService.getCurrentPlayer() != null && gameService.getCurrentPlayer().getId().equals(humanPlayer.getId())) {
                log.warn("人类玩家 {} 行动超时，自动弃牌", humanPlayer.getName());
                gameService.playerAction(humanPlayer.getId(), "fold", 0);
                // 弃牌后，GameService会处理轮次，我们只需再次触发流程
                scheduleTask(this::processNextPlayerAction, AI_DECISION_DELAY);
            }
        }, HUMAN_PLAYER_TIMEOUT);
    }

    public void onHumanPlayerAction(String playerId) {
        if (!isRunning) return;

        if (humanPlayerTimeoutTask != null) {
            humanPlayerTimeoutTask.cancel(true);
        }

        // 玩家行动后，GameService已广播状态，我们调度下一个玩家的行动
        scheduleTask(this::processNextPlayerAction, 1000); // 延迟1秒让操作看起来更自然
    }

    public void createSixPlayerAutoGame() {
        stopAutoGame();
        gameService.createSixPlayerAutoGame();
        log.info("已创建6人桌自动游戏");
    }

    public boolean addRealPlayerToAutoGame(String playerId, String playerName, int chips) {
        return gameService.addRealPlayer(playerId, playerName, chips);
    }

    public boolean canStartGame() {
        return gameService.canStartGame();
    }

    private ScheduledFuture<?> scheduleTask(Runnable task, long delay) {
        if (scheduler == null || scheduler.isShutdown()) {
            return null;
        }
        return scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
    }
}
