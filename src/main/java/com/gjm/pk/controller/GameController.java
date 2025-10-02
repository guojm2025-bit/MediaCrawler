package com.gjm.pk.controller;

import com.gjm.pk.entity.Player;
import com.gjm.pk.service.impl.GameService;
import com.gjm.pk.service.AutoGameManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏控制器
 * 提供REST API接口
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
@Slf4j
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {
    
    @Autowired
    private GameService gameService;
    
    @Autowired
    private AutoGameManager autoGameManager;
    
    /**
     * 获取游戏状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getGameStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("phase", gameService.getCurrentPhase());
            status.put("pot", gameService.getPot());
            status.put("currentBetAmount", gameService.getCurrentBetAmount());
            status.put("currentPlayerTurn", gameService.getCurrentPlayerTurn());
            status.put("playersCount", gameService.getPlayers().size());
            status.put("communityCardsCount", gameService.getCommunityCards().size());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取游戏状态失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取所有玩家信息
     */
    @GetMapping("/players")
    public ResponseEntity<List<Player>> getPlayers() {
        try {
            List<Player> players = gameService.getPlayers();
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            log.error("获取玩家信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取公共牌
     */
    @GetMapping("/community-cards")
    public ResponseEntity<Map<String, Object>> getCommunityCards() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("cards", gameService.getCommunityCards());
            result.put("count", gameService.getCommunityCards().size());
            result.put("phase", gameService.getCurrentPhase());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取公共牌失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建新游戏
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createGame(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "游戏房间创建成功");
            response.put("gameId", "game_" + System.currentTimeMillis());
            
            log.info("创建新游戏房间");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建游戏失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 重置游戏
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetGame() {
        try {
            // 这里可以添加重置游戏的逻辑
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "游戏已重置");
            
            log.info("游戏已重置");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("重置游戏失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取游戏统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getGameStats() {
        try {
            Map<String, Object> stats = gameService.getGameStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取游戏统计失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建6人桌自动游戏
     */
    @PostMapping("/auto/create")
    public ResponseEntity<Map<String, Object>> createAutoGame() {
        try {
            autoGameManager.createSixPlayerAutoGame();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "6人桌自动游戏已创建");
            response.put("playersCount", gameService.getPlayers().size());
            response.put("gameStats", gameService.getGameStats());
            
            log.info("已创建6人桌自动游戏");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建自动游戏失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 开始自动游戏
     */
    @PostMapping("/auto/start")
    public ResponseEntity<Map<String, Object>> startAutoGame() {
        try {
            if (!autoGameManager.canStartGame()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无法开始游戏：玩家数量不足或玩家筹码不足");
                return ResponseEntity.badRequest().body(response);
            }
            
            autoGameManager.startAutoGame();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "自动游戏已开始");
            response.put("isRunning", autoGameManager.isAutoGameRunning());
            response.put("gameStats", gameService.getGameStats());
            
            log.info("自动游戏已开始");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("开始自动游戏失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 停止自动游戏
     */
    @PostMapping("/auto/stop")
    public ResponseEntity<Map<String, Object>> stopAutoGame() {
        try {
            autoGameManager.stopAutoGame();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "自动游戏已停止");
            response.put("isRunning", autoGameManager.isAutoGameRunning());
            
            log.info("自动游戏已停止");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("停止自动游戏失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取自动游戏状态
     */
    @GetMapping("/auto/status")
    public ResponseEntity<Map<String, Object>> getAutoGameStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("isAutoGameRunning", autoGameManager.isAutoGameRunning());
            status.put("canStartGame", autoGameManager.canStartGame());
            status.put("gameStats", gameService.getGameStats());
            status.put("currentPhase", gameService.getCurrentPhase());
            status.put("isGameOver", gameService.isGameOver());
            
            Player finalWinner = gameService.getFinalWinner();
            if (finalWinner != null) {
                status.put("finalWinner", finalWinner.getName());
            }
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取自动游戏状态失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 添加真实玩家到自动游戏
     */
    @PostMapping("/auto/add-player")
    public ResponseEntity<Map<String, Object>> addPlayerToAutoGame(@RequestBody Map<String, Object> request) {
        try {
            String playerId = (String) request.get("playerId");
            String playerName = (String) request.get("playerName");
            Integer chips = (Integer) request.get("chips");
            
            if (playerId == null || playerName == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "缺少必要参数");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean added = autoGameManager.addRealPlayerToAutoGame(
                playerId, playerName, chips != null ? chips : GameService.DEFAULT_CHIPS);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", added);
            response.put("message", added ? "玩家添加成功" : "玩家添加失败");
            response.put("playersCount", gameService.getPlayers().size());
            
            log.info("添加真实玩家 {} 到自动游戏: {}", playerName, added ? "成功" : "失败");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("添加玩家到自动游戏失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "Texas Hold'em Poker Game");
        return ResponseEntity.ok(health);
    }
}