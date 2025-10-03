package com.gjm.pk.service.impl;

import com.gjm.pk.config.GameWebSocketHandler;
import com.gjm.pk.entity.Card;
import com.gjm.pk.entity.Player;
import com.gjm.pk.service.HandEvaluator;
import com.gjm.pk.service.AIPlayerDecisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private GameWebSocketHandler webSocketHandler;
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
    private int currentDealer = -1;
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

    @Autowired
    public GameService(AIPlayerDecisionService aiPlayerDecisionService) {
        this.aiPlayerDecisionService = aiPlayerDecisionService;
        this.gameId = System.currentTimeMillis();
    }

    @Autowired
    public void setWebSocketHandler(@Lazy GameWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
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
    public void startGame(List<Player> incomingPlayers) {
        // 过滤掉没有筹码的玩家
        this.players = new ArrayList<>(incomingPlayers.stream()
                .filter(p -> p.getChips() > 0)
                .collect(Collectors.toList()));

        if (this.players.size() < MIN_PLAYERS) {
            log.warn("有筹码的玩家不足 {} 人，无法开始游戏", MIN_PLAYERS);
            return;
        }

        this.players.forEach(Player::resetForNewHand);

        initializeDeck();
        communityCards.clear();
        pot = 0;

        // 安全地确定庄家位置
        if (currentDealer < 0 || currentDealer >= this.players.size()) {
            currentDealer = 0;
        } else {
            currentDealer = (currentDealer + 1) % this.players.size();
        }

        setupBlinds();
        dealHoleCards();

        currentPlayerTurn = getNextActivePlayer(getBigBlindPosition(),false);
        currentPhase = GamePhase.PRE_FLOP;

        log.info("游戏开始，庄家: {}, 小盲注: {}, 大盲注: {}",
                this.players.get(currentDealer).getName(),
                this.players.get(getSmallBlindPosition()).getName(),
                this.players.get(getBigBlindPosition()).getName());

        webSocketHandler.broadcastGameState();
    }
    private void setupBlinds() {
        players.forEach(p -> {
            p.setDealer(false);
            p.setSmallBlind(false);
            p.setBigBlind(false);
        });

        int smallBlindPos = getSmallBlindPosition();
        int bigBlindPos = getBigBlindPosition();

        Player dealerPlayer = players.get(currentDealer);
        Player smallBlindPlayer = players.get(smallBlindPos);
        Player bigBlindPlayer = players.get(bigBlindPos);

        dealerPlayer.setDealer(true);
        smallBlindPlayer.setSmallBlind(true);
        bigBlindPlayer.setBigBlind(true);

        int smallBlindBet = smallBlindPlayer.bet(DEFAULT_SMALL_BLIND);
        int bigBlindBet = bigBlindPlayer.bet(DEFAULT_BIG_BLIND);

        pot += smallBlindBet + bigBlindBet;
        currentBetAmount = DEFAULT_BIG_BLIND;

        log.info("小盲注: {} 下注 {}, 大盲注: {} 下注 {}",
                smallBlindPlayer.getName(), smallBlindBet,
                bigBlindPlayer.getName(), bigBlindBet);
    }

    private int getSmallBlindPosition() {
        if (players.size() == 2) return currentDealer;
        return getNextActivePlayer(currentDealer, false);
    }

    private int getBigBlindPosition() {
        return getNextActivePlayer(getSmallBlindPosition(), false);
    }

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

    public void flop() {
        if (currentPhase != GamePhase.PRE_FLOP) return;

        burnCard();
        for (int i = 0; i < 3; i++) communityCards.add(deck.remove(0));
        currentPhase = GamePhase.FLOP;
        resetBettingRound();

        log.info("翻牌阶段，公共牌: {}", getCommunityCardsString());
        webSocketHandler.broadcastGameState();
    }

    public void turn() {
        if (currentPhase != GamePhase.FLOP) return;

        burnCard();
        communityCards.add(deck.remove(0));
        currentPhase = GamePhase.TURN;
        resetBettingRound();

        log.info("转牌阶段，公共牌: {}", getCommunityCardsString());
        webSocketHandler.broadcastGameState();
    }

    public void river() {
        if (currentPhase != GamePhase.TURN) return;

        burnCard();
        communityCards.add(deck.remove(0));
        currentPhase = GamePhase.RIVER;
        resetBettingRound();

        log.info("河牌阶段，公共牌: {}", getCommunityCardsString());
        webSocketHandler.broadcastGameState();
    }

    private void burnCard() {
        if (!deck.isEmpty()) deck.remove(0);
    }
    /**
     * 重置下注回合
     */
    private void resetBettingRound() {
        currentBetAmount = 0;
        players.forEach(Player::resetCurrentBet);
        currentPlayerTurn = getNextActivePlayer(currentDealer, false);
    }

    public boolean playerAction(String playerId, String action, int amount) {
        Player player = findPlayerById(playerId);
        if (player == null || !isPlayerTurn(playerId)) {
            log.warn("非法操作: 玩家 {} 不存在或未轮到其行动", playerId);
            return false;
        }

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
                    pot += player.call(callAmount);
                    log.info("玩家 {} 跟注 {}", player.getName(), callAmount);
                } else {
                    player.check();
                    log.info("玩家 {} 看牌 (无需跟注)", player.getName());
                }
                break;
            case "raise":
                if (amount <= currentBetAmount) {
                    log.warn("加注金额 {} 必须大于当前下注金额 {}", amount, currentBetAmount);
                    return false;
                }
                int totalRaise = amount - player.getCurrentBet();
                pot += player.raise(totalRaise);
                currentBetAmount = player.getCurrentBet();
                log.info("玩家 {} 加注到 {}", player.getName(), currentBetAmount);
                break;
            case "allin":
                int allInAmount = player.allIn();
                pot += allInAmount;
                if (player.getCurrentBet() > currentBetAmount) {
                    currentBetAmount = player.getCurrentBet();
                }
                log.info("玩家 {} 全下 {}", player.getName(), allInAmount);
                break;
            default:
                log.warn("未知的行动: {}", action);
                return false;
        }

        nextPlayerTurn();

        boolean roundComplete = isBettingRoundComplete();

        webSocketHandler.broadcastGameState();

        if (roundComplete) {
            new Timer().schedule(new TimerTask() {
                @Override public void run() { proceedToNextPhase(); }
            }, 1500);
        }

        return true;
    }

    private void proceedToNextPhase() {
        List<Player> activePlayers = getActivePlayersInHand();

        if (activePlayers.size() <= 1) {
            determineWinner();
            currentPhase = GamePhase.FINISHED;
            webSocketHandler.broadcastGameState();
            return;
        }

        switch (currentPhase) {
            case PRE_FLOP: flop(); break;
            case FLOP: turn(); break;
            case TURN: river(); break;
            case RIVER: showdown(); break;
        }
    }
    /**
     * 【已完善】执行AI决策
     * @param playerId AI玩家的ID
     * @return 操作是否成功
     */
    public boolean executeAIDecision(String playerId) {
        Player player = findPlayerById(playerId);
        if (player == null || !player.isAi()) {
            log.warn("执行AI决策失败：玩家 {} 不是AI或不存在", playerId);
            return false;
        }

        // 调用AI决策服务获取决策
        AIPlayerDecisionService.AIDecision decision = aiPlayerDecisionService.makeDecision(
                player,
                communityCards,
                currentBetAmount,
                pot,
                getActivePlayersInHand().size(),
                currentPhase.toString(),
                player.getAiLevel() != null ? player.getAiLevel() : AIPlayerDecisionService.AILevel.MEDIUM
        );

        // 根据决策执行相应的玩家动作
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
                log.warn("AI玩家 {} 未知决策: {}，默认弃牌", player.getName(), decision.getAction());
                return playerAction(playerId, "fold", 0); // 安全默认操作
        }
    }

    /**
     * 【已完善】获取仍在当前牌局中的活跃玩家（未弃牌）
     * @return 活跃玩家列表
     */
    public List<Player> getActivePlayersInHand() {
        return players.stream()
                .filter(p -> p.isInGame() && !p.isHasFolded())
                .collect(Collectors.toList());
    }
    private void showdown() {
        log.info("开始摊牌");
        currentPhase = GamePhase.SHOWDOWN;
        webSocketHandler.broadcastGameState();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                determineWinner();
                currentPhase = GamePhase.FINISHED;
                webSocketHandler.broadcastGameState();
            }
        }, 3000);
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

        // 确保在最后广播状态
        webSocketHandler.broadcastGameState();
    }

    // 辅助方法

    public Player findPlayerById(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public boolean isPlayerTurn(String playerId) {
        return currentPlayerTurn >= 0 &&
               currentPlayerTurn < players.size() &&
               players.get(currentPlayerTurn).getId().equals(playerId);
    }

    private void nextPlayerTurn() {
        int oldTurn = currentPlayerTurn;
        currentPlayerTurn = getNextActivePlayer(currentPlayerTurn, true);
        log.info("玩家轮次变更: {} -> {}", oldTurn, currentPlayerTurn);
    }

    private int getNextActivePlayer(int startIndex, boolean checkTurn) {
        if (players.isEmpty()) return -1;
        int nextIndex = (startIndex + 1) % players.size();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(nextIndex);
            if (player.isInGame() && !player.isAllIn() && (!checkTurn || !player.isHasFolded())) {
                return nextIndex;
            }
            nextIndex = (nextIndex + 1) % players.size();
        }
        return -1; // 没有可行动的玩家
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
