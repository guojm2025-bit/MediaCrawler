package com.gjm.pk.service.impl;

import com.gjm.pk.entity.Card;
import com.gjm.pk.entity.Player;
import com.gjm.pk.service.HandEvaluator;
import com.gjm.pk.service.AIPlayerDecisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏服务类
 * @author: guojianming
 * @data 2025/09/17 17:47
 */
@Slf4j
@Service
public class GameService {
    
    @Autowired
    private AIPlayerDecisionService aiPlayerDecisionService;
    
    // 游戏常量
    public static final int MAX_PLAYERS = 6;
    public static final int MIN_PLAYERS = 2;
    public static final int DEFAULT_CHIPS = 1000;
    public static final int DEFAULT_SMALL_BLIND = 10;
    public static final int DEFAULT_BIG_BLIND = 20;
    
    // 游戏状态枚举
    public enum GamePhase {
        WAITING,      // 等待玩家
        PRE_FLOP,     // 翻牌前
        FLOP,         // 翻牌
        TURN,         // 转牌
        RIVER,        // 河牌
        SHOWDOWN,     // 摄牌
        FINISHED      // 结束
    }
    
    private List<Card> deck = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private List<Card> communityCards = new ArrayList<>();
    private int currentDealer;
    private int currentPlayerTurn;
    private int pot = 0;
    private int currentBetAmount = 0;
    private GamePhase currentPhase = GamePhase.WAITING;
    private int smallBlindAmount = DEFAULT_SMALL_BLIND;
    private int bigBlindAmount = DEFAULT_BIG_BLIND;
    private Map<String, Integer> sidePots = new HashMap<>();
    private boolean autoGameEnabled = false;
    private Timer autoGameTimer;
    private long gameId;
    
    public GameService() {
        this.gameId = System.currentTimeMillis();
    }
    
    /**
     * 初始化牌组
     */
    private void initializeDeck() {
        deck.clear();
        for (int suit = 0; suit < 4; suit++) {
            for (int rank = 2; rank <= 14; rank++) {
                deck.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(deck);
        log.info("牌组已初始化并洗牌");
    }

    /**
     * 开始新游戏
     */
    public void startGame(List<Player> players) {
        if (players.size() < MIN_PLAYERS) {
            throw new IllegalArgumentException("至少需要" + MIN_PLAYERS + "名玩家");
        }
        
        if (players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("最多支持" + MAX_PLAYERS + "名玩家");
        }
        
        this.players = new ArrayList<>(players);
        
        // 重置所有玩家状态
        players.forEach(Player::resetForNewHand);
        
        initializeDeck();
        communityCards.clear();
        pot = 0;
        currentBetAmount = 0;
        sidePots.clear();
        
        // 移动庄家位置
        currentDealer = (currentDealer + 1) % players.size();
        
        // 设置庄家、小盲注、大盲注
        setupBlinds();
        
        // 发手牌
        dealHoleCards();
        
        // 设置第一个行动玩家（大盲注后的下一个玩家）
        currentPlayerTurn = getNextActivePlayer(getBigBlindPosition());
        currentPhase = GamePhase.PRE_FLOP;
        
        log.info("游戏开始，庄家: {}, 小盲注: {}, 大盲注: {}", 
                players.get(currentDealer).getName(),
                players.get(getSmallBlindPosition()).getName(),
                players.get(getBigBlindPosition()).getName());
    }
    
    /**
     * 创建6人桌自动游戏
     */
    public void createSixPlayerAutoGame() {
        List<Player> autoPlayers = new ArrayList<>();
        
        // 生成5个AI玩家
        for (int i = 0; i < 5; i++) {
            String aiId = "AI_" + gameId + "_" + i;
            String aiName = aiPlayerDecisionService.generateAIPlayerName(i);
            AIPlayerDecisionService.AILevel aiLevel = aiPlayerDecisionService.generateRandomAILevel();
            
            Player aiPlayer = new Player(aiId, aiName, DEFAULT_CHIPS, true, aiLevel);
            autoPlayers.add(aiPlayer);
        }
        
        this.players = autoPlayers;
        log.info("创建6人桌自动游戏，已生成{}个AI玩家", autoPlayers.size());
    }
    
    /**
     * 添加真实玩家到游戏中
     */
    public boolean addRealPlayer(String playerId, String playerName, int chips) {
        if (players.size() >= MAX_PLAYERS) {
            log.warn("游戏已满员，无法添加玩家: {}", playerName);
            return false;
        }
        
        // 检查玩家是否已存在
        boolean playerExists = players.stream()
                .anyMatch(p -> p.getId().equals(playerId));
        
        if (playerExists) {
            log.warn("玩家已存在: {}", playerId);
            return false;
        }
        
        Player realPlayer = new Player(playerId, playerName, chips, false);
        players.add(realPlayer);
        
        log.info("真实玩家 {} 加入游戏，当前玩家数: {}", playerName, players.size());
        return true;
    }
    
    /**
     * 移除玩家
     */
    public boolean removePlayer(String playerId) {
        boolean removed = players.removeIf(p -> p.getId().equals(playerId));
        if (removed) {
            log.info("玩家 {} 离开游戏，当前玩家数: {}", playerId, players.size());
        }
        return removed;
    }
    
    /**
     * 自动补充AI玩家到6人
     */
    public void fillWithAIPlayers() {
        while (players.size() < MAX_PLAYERS) {
            String aiId = "AI_" + gameId + "_" + System.currentTimeMillis();
            String aiName = aiPlayerDecisionService.generateAIPlayerName(players.size());
            AIPlayerDecisionService.AILevel aiLevel = aiPlayerDecisionService.generateRandomAILevel();
            
            Player aiPlayer = new Player(aiId, aiName, DEFAULT_CHIPS, true, aiLevel);
            players.add(aiPlayer);
        }
        
        log.info("已补充AI玩家，当前玩家数: {}", players.size());
    }
    
    /**
     * AI玩家自动决策
     */
    public AIPlayerDecisionService.AIDecision makeAIDecision(Player aiPlayer) {
        if (!aiPlayer.isAi()) {
            throw new IllegalArgumentException("只有AI玩家才能使用自动决策");
        }
        
        return aiPlayerDecisionService.makeDecision(
            aiPlayer,
            communityCards,
            currentBetAmount,
            pot,
            getActivePlayers().size(),
            currentPhase.toString(),
            aiPlayer.getAiLevel() != null ? aiPlayer.getAiLevel() : AIPlayerDecisionService.AILevel.MEDIUM
        );
    }
    
    /**
     * 执行AI决策
     */
    public boolean executeAIDecision(String playerId) {
        Player player = findPlayerById(playerId);
        if (player == null || !player.isAi()) {
            return false;
        }
        
        AIPlayerDecisionService.AIDecision decision = makeAIDecision(player);
        
        switch (decision.getAction()) {
            case FOLD:
                return playerAction(playerId, "fold", 0);
            case CHECK:
                return playerAction(playerId, "check", 0);
            case CALL:
                return playerAction(playerId, "call", 0);
            case RAISE:
                return playerAction(playerId, "raise", decision.getAmount());
            case ALL_IN:
                return playerAction(playerId, "allin", 0);
            default:
                log.warn("AI玩家 {} 未知决策: {}", player.getName(), decision.getAction());
                return playerAction(playerId, "fold", 0);
        }
    }
    
    /**
     * 是否可以开始游戏
     */
    public boolean canStartGame() {
        return players.size() >= MIN_PLAYERS && 
               players.stream().allMatch(p -> p.getChips() > 0);
    }
    
    /**
     * 获取游戏统计信息
     */
    public Map<String, Object> getGameStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("gameId", gameId);
        stats.put("totalPlayers", players.size());
        stats.put("activePlayers", getActivePlayers().size());
        stats.put("aiPlayers", players.stream().mapToLong(p -> p.isAi() ? 1 : 0).sum());
        stats.put("humanPlayers", players.stream().mapToLong(p -> !p.isAi() ? 1 : 0).sum());
        stats.put("totalChips", players.stream().mapToInt(Player::getChips).sum());
        stats.put("currentPot", pot);
        stats.put("gamePhase", currentPhase);
        stats.put("currentBetAmount", currentBetAmount);
        stats.put("dealerPosition", currentDealer);
        stats.put("currentPlayerTurn", currentPlayerTurn);
        
        return stats;
    }
    
    /**
     * 开启自动游戏模式
     */
    public void enableAutoGame() {
        autoGameEnabled = true;
        log.info("自动游戏模式已开启");
    }
    
    /**
     * 关闭自动游戏模式
     */
    public void disableAutoGame() {
        autoGameEnabled = false;
        if (autoGameTimer != null) {
            autoGameTimer.cancel();
            autoGameTimer = null;
        }
        log.info("自动游戏模式已关闭");
    }
    
    /**
     * 检查是否在自动游戏模式
     */
    public boolean isAutoGameEnabled() {
        return autoGameEnabled;
    }
    
    /**
     * 获取当前玩家是否为AI
     */
    public boolean isCurrentPlayerAI() {
        Player currentPlayer = getCurrentPlayer();
        return currentPlayer != null && currentPlayer.isAi();
    }
    
    /**
     * 检查游戏是否结束（只剩一个玩家有筹码）
     */
    public boolean isGameOver() {
        long playersWithChips = players.stream()
                .filter(p -> p.getChips() > 0)
                .count();
        return playersWithChips <= 1;
    }
    
    /**
     * 获取最终胜利者
     * 只有当游戏真正结束时（只剩一个玩家有筹码）才返回该玩家
     */
    public Player getFinalWinner() {
        List<Player> playersWithChips = players.stream()
                .filter(p -> p.getChips() > 0)
                .collect(Collectors.toList());
        
        // 只有当恰好有一个玩家有筹码时，该玩家才是最终胜利者
        if (playersWithChips.size() == 1) {
            return playersWithChips.get(0);
        }
        
        // 如果没有玩家有筹码或者有多个玩家有筹码，则没有最终胜利者
        return null;
    }
    
    /**
     * 设置盲注
     */
    private void setupBlinds() {
        int smallBlindPos = getSmallBlindPosition();
        int bigBlindPos = getBigBlindPosition();
        
        Player smallBlindPlayer = players.get(smallBlindPos);
        Player bigBlindPlayer = players.get(bigBlindPos);
        
        // 设置标记
        smallBlindPlayer.setSmallBlind(true);
        bigBlindPlayer.setBigBlind(true);
        players.get(currentDealer).setDealer(true);
        
        // 下盲注
        int smallBlindBet = smallBlindPlayer.bet(smallBlindAmount);
        int bigBlindBet = bigBlindPlayer.bet(bigBlindAmount);
        
        pot += smallBlindBet + bigBlindBet;
        currentBetAmount = bigBlindAmount;
        
        log.info("小盲注: {} 下注 {}, 大盲注: {} 下注 {}", 
                smallBlindPlayer.getName(), smallBlindBet,
                bigBlindPlayer.getName(), bigBlindBet);
    }
    
    /**
     * 获取小盲注位置
     */
    private int getSmallBlindPosition() {
        if (players.size() == 2) {
            return currentDealer; // 两人游戏时，庄家是小盲注
        }
        return (currentDealer + 1) % players.size();
    }
    
    /**
     * 获取大盲注位置
     */
    private int getBigBlindPosition() {
        if (players.size() == 2) {
            return (currentDealer + 1) % players.size();
        }
        return (currentDealer + 2) % players.size();
    }

    /**
     * 发手牌
     */
    private void dealHoleCards() {
        for (int i = 0; i < 2; i++) {
            for (Player player : players) {
                if (player.isInGame()) {
                    player.getHoleCards().add(deck.remove(0));
                }
            }
        }
        log.info("已发手牌，每名玩家获得2张牌");
    }

    /**
     * 翻牌（发3张公共牌）
     */
    public void flop() {
        if (currentPhase != GamePhase.PRE_FLOP) {
            throw new IllegalStateException("只能在翻牌前阶段进行翻牌");
        }
        
        burnCard(); // 弃一张牌
        for (int i = 0; i < 3; i++) {
            communityCards.add(deck.remove(0));
        }
        currentPhase = GamePhase.FLOP;
        resetBettingRound();
        
        log.info("翻牌阶段，公共牌: {}", getCommunityCardsString());
        log.info("当前公共牌数量: {}", communityCards.size());
    }

    /**
     * 转牌（发第4张公共牌）
     */
    public void turn() {
        if (currentPhase != GamePhase.FLOP) {
            throw new IllegalStateException("只能在翻牌阶段进行转牌");
        }
        
        burnCard();
        communityCards.add(deck.remove(0));
        currentPhase = GamePhase.TURN;
        resetBettingRound();
        
        log.info("转牌阶段，公共牌: {}", getCommunityCardsString());
        log.info("当前公共牌数量: {}", communityCards.size());
    }

    /**
     * 河牌（发第5张公共牌）
     */
    public void river() {
        if (currentPhase != GamePhase.TURN) {
            throw new IllegalStateException("只能在转牌阶段进行河牌");
        }
        
        burnCard();
        communityCards.add(deck.remove(0));
        currentPhase = GamePhase.RIVER;
        resetBettingRound();
        
        log.info("河牌阶段，公共牌: {}", getCommunityCardsString());
        log.info("当前公共牌数量: {}", communityCards.size());
    }

    /**
     * 弃牌
     */
    private void burnCard() {
        if (!deck.isEmpty()) {
            deck.remove(0);
        }
    }
    
    /**
     * 重置下注回合
     */
    private void resetBettingRound() {
        currentBetAmount = 0;
        players.forEach(Player::resetCurrentBet);
        currentPlayerTurn = getNextActivePlayer(currentDealer);
    }

    /**
     * 处理玩家下注
     */
    public boolean playerAction(String playerId, String action, int amount) {
        Player player = findPlayerById(playerId);
        if (player == null) {
            log.warn("未找到玩家: {}", playerId);
            return false;
        }
        
        if (!isPlayerTurn(playerId)) {
            log.warn("不是玩家 {} 的回合", playerId);
            return false;
        }
        
        int actionAmount = 0;
        
        switch (action.toLowerCase()) {
            case "fold":
                player.fold();
                log.info("玩家 {} 弃牌", player.getName());
                break;
                
            case "check":
                if (currentBetAmount > player.getCurrentBet()) {
                    log.warn("玩家 {} 不能看牌，需要跟注", player.getName());
                    return false;
                }
                player.check();
                log.info("玩家 {} 看牌", player.getName());
                break;
                
            case "call":
                int callAmount = currentBetAmount - player.getCurrentBet();
                if (callAmount > 0) {
                    actionAmount = player.call(callAmount);
                    pot += actionAmount;
                    log.info("玩家 {} 跟注 {}", player.getName(), actionAmount);
                } else {
                    player.check();
                    log.info("玩家 {} 看牌", player.getName());
                }
                break;
                
            case "raise":
                if (amount <= currentBetAmount) {
                    log.warn("加注金额不能小于或等于当前下注金额");
                    return false;
                }
                int totalRaise = amount - player.getCurrentBet();
                actionAmount = player.raise(totalRaise);
                pot += actionAmount;
                currentBetAmount = player.getCurrentBet();
                log.info("玩家 {} 加注到 {}", player.getName(), currentBetAmount);
                break;
                
            case "allin":
                actionAmount = player.allIn();
                pot += actionAmount;
                if (player.getCurrentBet() > currentBetAmount) {
                    currentBetAmount = player.getCurrentBet();
                }
                log.info("玩家 {} 全下 {}", player.getName(), actionAmount);
                break;
                
            default:
                log.warn("未知的行动: {}", action);
                return false;
        }
        
        // 移动到下一个玩家
        nextPlayerTurn();
        
        // 检查是否结束下注回合
        if (isBettingRoundComplete()) {
            proceedToNextPhase();
        }
        
        return true;
    }
    
    /**
     * 检查下注回合是否结束
     */
    private boolean isBettingRoundComplete() {
        List<Player> activePlayers = getActivePlayers();
        if (activePlayers.size() <= 1) {
            return true;
        }
        
        // 检查所有活跃玩家是否都已行动且下注金额相同
        int targetBet = activePlayers.stream()
                .filter(p -> !p.isAllIn())
                .mapToInt(Player::getCurrentBet)
                .max().orElse(0);
                
        return activePlayers.stream()
                .allMatch(p -> p.isAllIn() || p.getCurrentBet() == targetBet);
    }
    
    /**
     * 进入下一阶段
     */
    private void proceedToNextPhase() {
        List<Player> activePlayers = getActivePlayers();
        
        if (activePlayers.size() <= 1) {
            // 只有一个玩家或无玩家，直接结束
            currentPhase = GamePhase.FINISHED;
            determineWinner();
            return;
        }
        
        switch (currentPhase) {
            case PRE_FLOP:
                flop();
                break;
            case FLOP:
                turn();
                break;
            case TURN:
                river();
                break;
            case RIVER:
                currentPhase = GamePhase.SHOWDOWN;
                showdown();
                break;
            default:
                break;
        }
    }
    
    /**
     * 摘牌
     */
    private void showdown() {
        log.info("开始摘牌");
        determineWinner();
        currentPhase = GamePhase.FINISHED;
    }
    
    /**
     * 决定胜者
     */
    private void determineWinner() {
        List<Player> activePlayers = getActivePlayers();
        
        // 如果只剩一个活跃玩家，该玩家获胜
        if (activePlayers.size() == 1) {
            Player winner = activePlayers.get(0);
            winner.addWinnings(pot);
            log.info("玩家 {} 获胜（其他玩家已弃牌），奖金: {}", winner.getName(), pot);
        } 
        // 如果有多个活跃玩家，进行牌型比较
        else if (activePlayers.size() > 1) {
            // 评估所有玩家的手牌
            List<HandEvaluator.HandResult> handResults = new ArrayList<>();
            Map<Integer, Player> playerMap = new HashMap<>();
            
            for (int i = 0; i < activePlayers.size(); i++) {
                Player player = activePlayers.get(i);
                HandEvaluator.HandResult result = HandEvaluator.evaluateHand(
                    player.getHoleCards(), communityCards);
                handResults.add(result);
                playerMap.put(i, player);
                
                log.info("玩家 {} 的牌型: {}", player.getName(), result.toString());
            }
            
            // 比较手牌，找出获胜者
            List<Integer> winnerIndices = HandEvaluator.compareHands(handResults);
            int winningsPerPlayer = pot / winnerIndices.size();
            
            for (int winnerIndex : winnerIndices) {
                Player winner = playerMap.get(winnerIndex);
                winner.addWinnings(winningsPerPlayer);
                log.info("玩家 {} 获胜，奖金: {}", winner.getName(), winningsPerPlayer);
            }
            
            // 处理余数
            int remainder = pot % winnerIndices.size();
            if (remainder > 0 && !winnerIndices.isEmpty()) {
                Player firstWinner = playerMap.get(winnerIndices.get(0));
                firstWinner.addWinnings(remainder);
                log.info("余数 {} 分配给玩家 {}", remainder, firstWinner.getName());
            }
        } 
        // 如果没有活跃玩家（理论上不应该发生）
        else {
            log.warn("没有活跃玩家，奖池将被重置");
        }
        
        pot = 0;
    }
    
    // 辅助方法
    
    private Player findPlayerById(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
    
    private boolean isPlayerTurn(String playerId) {
        return currentPlayerTurn >= 0 && 
               currentPlayerTurn < players.size() && 
               players.get(currentPlayerTurn).getId().equals(playerId);
    }
    
    private void nextPlayerTurn() {
        int oldPlayerTurn = currentPlayerTurn;
        Player oldPlayer = getCurrentPlayer();
        currentPlayerTurn = getNextActivePlayer(currentPlayerTurn);
        Player newPlayer = getCurrentPlayer();
        
        log.info("玩家轮次变更: {} ({}) -> {} ({})", 
                oldPlayer != null ? oldPlayer.getName() : "null",
                oldPlayerTurn,
                newPlayer != null ? newPlayer.getName() : "null", 
                currentPlayerTurn);
    }
    
    private int getNextActivePlayer(int startIndex) {
        int index = (startIndex + 1) % players.size();
        while (index != startIndex) {
            Player player = players.get(index);
            if (player.isInGame() && !player.isHasFolded() && !player.isAllIn()) {
                return index;
            }
            index = (index + 1) % players.size();
        }
        return -1; // 没有活跃玩家
    }
    
    private List<Player> getActivePlayers() {
        return players.stream()
                .filter(p -> p.isInGame() && !p.isHasFolded())
                .collect(Collectors.toList());
    }
    
    private String getCommunityCardsString() {
        return communityCards.stream()
                .map(Card::toString)
                .collect(Collectors.joining(", "));
    }
    
    // Getters
    public List<Player> getPlayers() { return new ArrayList<>(players); }
    public List<Card> getCommunityCards() { return new ArrayList<>(communityCards); }
    public int getPot() { return pot; }
    public int getCurrentBetAmount() { return currentBetAmount; }
    public GamePhase getCurrentPhase() { return currentPhase; }
    public int getCurrentPlayerTurn() { return currentPlayerTurn; }
    public Player getCurrentPlayer() {
        if (currentPlayerTurn >= 0 && currentPlayerTurn < players.size()) {
            return players.get(currentPlayerTurn);
        }
        return null;
    }
}
