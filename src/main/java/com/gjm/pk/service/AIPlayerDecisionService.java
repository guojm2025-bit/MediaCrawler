package com.gjm.pk.service;

import com.gjm.pk.entity.Card;
import com.gjm.pk.entity.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * AI玩家决策服务
 * 提供不同难度级别的AI决策算法
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
@Slf4j
@Service
public class AIPlayerDecisionService {
    
    private final Random random = new Random();
    
    /**
     * AI难度级别
     */
    public enum AILevel {
        EASY("简单", 0.3),      // 30%的理性决策
        MEDIUM("中等", 0.6),    // 60%的理性决策
        HARD("困难", 0.85),     // 85%的理性决策
        EXPERT("专家", 0.95);   // 95%的理性决策
        
        private final String description;
        private final double rationality; // 理性程度（0-1）
        
        AILevel(String description, double rationality) {
            this.description = description;
            this.rationality = rationality;
        }
        
        public String getDescription() { return description; }
        public double getRationality() { return rationality; }
    }
    
    /**
     * AI决策结果
     */
    public static class AIDecision {
        private final Player.PlayerAction action;
        private final int amount;
        private final String reasoning;
        
        public AIDecision(Player.PlayerAction action, int amount, String reasoning) {
            this.action = action;
            this.amount = amount;
            this.reasoning = reasoning;
        }
        
        public Player.PlayerAction getAction() { return action; }
        public int getAmount() { return amount; }
        public String getReasoning() { return reasoning; }
    }
    
    /**
     * 生成AI决策
     * 
     * @param player 当前AI玩家
     * @param communityCards 公共牌
     * @param currentBetAmount 当前下注金额
     * @param pot 奖池金额
     * @param activePlayers 活跃玩家数量
     * @param gamePhase 游戏阶段
     * @param aiLevel AI难度级别
     * @return AI决策
     */
    public AIDecision makeDecision(Player player, List<Card> communityCards, 
                                 int currentBetAmount, int pot, int activePlayers,
                                 String gamePhase, AILevel aiLevel) {
        
        // 计算手牌强度
        double handStrength = calculateHandStrength(player.getHoleCards(), communityCards);
        
        // 计算底池赔率
        double potOdds = calculatePotOdds(currentBetAmount - player.getCurrentBet(), pot);
        
        // 计算位置优势
        double positionFactor = calculatePositionFactor(activePlayers);
        
        // 根据AI级别决定是否使用理性决策
        boolean useRationalDecision = random.nextDouble() < aiLevel.getRationality();
        
        AIDecision decision;
        
        if (useRationalDecision) {
            decision = makeRationalDecision(player, handStrength, potOdds, positionFactor, 
                                          currentBetAmount, pot, gamePhase);
        } else {
            decision = makeRandomDecision(player, currentBetAmount);
        }
        
        log.info("AI玩家 {} (等级:{}) 决策: {} - {}", 
                player.getName(), aiLevel.getDescription(), 
                decision.getAction(), decision.getReasoning());
        
        return decision;
    }
    
    /**
     * 理性决策算法
     */
    private AIDecision makeRationalDecision(Player player, double handStrength, 
                                          double potOdds, double positionFactor,
                                          int currentBetAmount, int pot, String gamePhase) {
        
        int callAmount = currentBetAmount - player.getCurrentBet();
        
        // 如果不需要额外下注，优先选择看牌
        if (callAmount <= 0) {
            return new AIDecision(Player.PlayerAction.CHECK, 0, "免费看牌");
        }
        
        // 如果筹码不足跟注，考虑全下或弃牌
        if (callAmount >= player.getChips()) {
            if (handStrength > 0.7) {
                return new AIDecision(Player.PlayerAction.ALL_IN, player.getChips(), 
                                    "强牌全下");
            } else {
                return new AIDecision(Player.PlayerAction.FOLD, 0, "筹码不足弃牌");
            }
        }
        
        // 综合评分计算
        double score = handStrength + potOdds * 0.3 + positionFactor * 0.2;
        
        // 根据游戏阶段调整策略
        score = adjustForGamePhase(score, gamePhase, handStrength);
        
        // 根据评分做决策
        if (score > 0.8) {
            // 非常强的情况，考虑加注
            int raiseAmount = calculateRaiseAmount(pot, player.getChips(), handStrength);
            return new AIDecision(Player.PlayerAction.RAISE, 
                                currentBetAmount + raiseAmount, 
                                String.format("强牌加注(评分:%.2f)", score));
        } else if (score > 0.6) {
            // 较强的情况，跟注
            return new AIDecision(Player.PlayerAction.CALL, callAmount, 
                                String.format("中等牌力跟注(评分:%.2f)", score));
        } else if (score > 0.4) {
            // 中等情况，根据底池赔率决定
            if (potOdds > 0.25) {
                return new AIDecision(Player.PlayerAction.CALL, callAmount, 
                                    "底池赔率合适跟注");
            } else {
                return new AIDecision(Player.PlayerAction.FOLD, 0, "底池赔率不佳弃牌");
            }
        } else {
            // 弱牌直接弃牌
            return new AIDecision(Player.PlayerAction.FOLD, 0, 
                                String.format("弱牌弃牌(评分:%.2f)", score));
        }
    }
    
    /**
     * 随机决策（低级AI或非理性时刻）
     */
    private AIDecision makeRandomDecision(Player player, int currentBetAmount) {
        int callAmount = currentBetAmount - player.getCurrentBet();
        
        if (callAmount <= 0) {
            return new AIDecision(Player.PlayerAction.CHECK, 0, "随机看牌");
        }
        
        // 简单的随机决策
        double randValue = random.nextDouble();
        
        if (randValue < 0.4) {
            return new AIDecision(Player.PlayerAction.FOLD, 0, "随机弃牌");
        } else if (randValue < 0.8) {
            if (callAmount <= player.getChips()) {
                return new AIDecision(Player.PlayerAction.CALL, callAmount, "随机跟注");
            } else {
                return new AIDecision(Player.PlayerAction.FOLD, 0, "筹码不足弃牌");
            }
        } else {
            // 随机加注
            if (player.getChips() > callAmount * 2) {
                int raiseAmount = random.nextInt(player.getChips() / 4) + callAmount;
                return new AIDecision(Player.PlayerAction.RAISE, 
                                    currentBetAmount + raiseAmount, "随机加注");
            } else {
                return new AIDecision(Player.PlayerAction.CALL, callAmount, "随机跟注");
            }
        }
    }
    
    /**
     * 计算手牌强度（简化版本）
     */
    private double calculateHandStrength(List<Card> holeCards, List<Card> communityCards) {
        if (holeCards.size() < 2) {
            return 0.0;
        }
        
        Card card1 = holeCards.get(0);
        Card card2 = holeCards.get(1);
        
        double strength = 0.0;
        
        // 基础牌力评估
        if (card1.getRank() == card2.getRank()) {
            // 对子
            strength = 0.5 + (card1.getRank() - 2) * 0.03; // 2-14 映射到 0.5-0.86
        } else if (card1.getSuit() == card2.getSuit()) {
            // 同花
            strength = 0.3 + Math.max(card1.getRank(), card2.getRank()) * 0.02;
        } else if (Math.abs(card1.getRank() - card2.getRank()) <= 4) {
            // 连牌可能
            strength = 0.2 + Math.max(card1.getRank(), card2.getRank()) * 0.015;
        } else {
            // 高牌
            strength = Math.max(card1.getRank(), card2.getRank()) * 0.01;
        }
        
        // 如果有公共牌，使用更精确的评估
        if (!communityCards.isEmpty()) {
            try {
                HandEvaluator.HandResult result = HandEvaluator.evaluateHand(holeCards, communityCards);
                strength = mapHandTypeToStrength(result.getHandType());
            } catch (Exception e) {
                log.debug("无法评估手牌强度，使用基础评估: {}", e.getMessage());
            }
        }
        
        return Math.min(1.0, strength);
    }
    
    /**
     * 将牌型映射到强度值
     */
    private double mapHandTypeToStrength(HandEvaluator.HandType handType) {
        switch (handType) {
            case HIGH_CARD: return 0.1;
            case ONE_PAIR: return 0.3;
            case TWO_PAIR: return 0.5;
            case THREE_OF_A_KIND: return 0.7;
            case STRAIGHT: return 0.8;
            case FLUSH: return 0.85;
            case FULL_HOUSE: return 0.9;
            case FOUR_OF_A_KIND: return 0.95;
            case STRAIGHT_FLUSH: return 0.98;
            case ROYAL_FLUSH: return 1.0;
            default: return 0.1;
        }
    }
    
    /**
     * 计算底池赔率
     */
    private double calculatePotOdds(int callAmount, int pot) {
        if (callAmount <= 0) return 1.0;
        return (double) pot / (pot + callAmount);
    }
    
    /**
     * 计算位置优势因子
     */
    private double calculatePositionFactor(int activePlayers) {
        // 简化的位置评估，后位优势
        return 0.1; // 这里可以根据具体位置信息优化
    }
    
    /**
     * 根据游戏阶段调整评分
     */
    private double adjustForGamePhase(double score, String gamePhase, double handStrength) {
        switch (gamePhase) {
            case "PRE_FLOP":
                // 翻牌前更保守
                return score * 0.9;
            case "FLOP":
                // 翻牌后正常
                return score;
            case "TURN":
                // 转牌更激进一些
                return score * 1.1;
            case "RIVER":
                // 河牌最激进
                return score * 1.2;
            default:
                return score;
        }
    }
    
    /**
     * 计算加注金额
     */
    private int calculateRaiseAmount(int pot, int chips, double handStrength) {
        // 根据牌力和奖池大小计算合理的加注
        double factor = 0.5 + handStrength * 0.5; // 0.5 到 1.0
        int baseRaise = (int) (pot * factor * 0.5);
        
        // 限制在筹码的一定比例内
        int maxRaise = chips / 3;
        
        return Math.max(Math.min(baseRaise, maxRaise), pot / 4);
    }
    
    /**
     * 生成随机AI名称
     */
    public String generateAIPlayerName(int index) {
        String[] names = {
            "AI_凯撒", "AI_拿破仑", "AI_亚历山大", "AI_汉尼拔", "AI_孙子",
            "AI_诸葛亮", "AI_韩信", "AI_岳飞", "AI_成吉思汗", "AI_萨拉丁"
        };
        return names[index % names.length];
    }
    
    /**
     * 生成随机AI难度
     */
    public AILevel generateRandomAILevel() {
        AILevel[] levels = AILevel.values();
        return levels[random.nextInt(levels.length)];
    }
}