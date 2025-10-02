package com.gjm.pk.service;

import com.gjm.pk.entity.Card;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 德州扑克手牌评估器
 * 负责判断牌型和比较牌力
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
@Slf4j
public class HandEvaluator {
    
    /**
     * 牌型枚举，按强度从低到高排序
     */
    public enum HandType {
        HIGH_CARD(1, "高牌"),
        ONE_PAIR(2, "一对"),
        TWO_PAIR(3, "两对"),
        THREE_OF_A_KIND(4, "三条"),
        STRAIGHT(5, "顺子"),
        FLUSH(6, "同花"),
        FULL_HOUSE(7, "葫芦"),
        FOUR_OF_A_KIND(8, "四条"),
        STRAIGHT_FLUSH(9, "同花顺"),
        ROYAL_FLUSH(10, "皇家同花顺");
        
        private final int strength;
        private final String description;
        
        HandType(int strength, String description) {
            this.strength = strength;
            this.description = description;
        }
        
        public int getStrength() { return strength; }
        public String getDescription() { return description; }
    }
    
    /**
     * 手牌结果类
     */
    public static class HandResult {
        private final HandType handType;
        private final List<Integer> ranks; // 用于比较的点数列表
        private final List<Card> bestHand; // 最佳5张牌
        
        public HandResult(HandType handType, List<Integer> ranks, List<Card> bestHand) {
            this.handType = handType;
            this.ranks = new ArrayList<>(ranks);
            this.bestHand = new ArrayList<>(bestHand);
        }
        
        public HandType getHandType() { return handType; }
        public List<Integer> getRanks() { return new ArrayList<>(ranks); }
        public List<Card> getBestHand() { return new ArrayList<>(bestHand); }
        
        @Override
        public String toString() {
            return String.format("%s: %s", 
                handType.getDescription(), 
                bestHand.stream().map(Card::toString).collect(Collectors.joining(", ")));
        }
    }
    
    /**
     * 评估最佳手牌
     * @param holeCards 手牌（2张）
     * @param communityCards 公共牌（最多5张）
     * @return 最佳手牌结果
     */
    public static HandResult evaluateHand(List<Card> holeCards, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(holeCards);
        allCards.addAll(communityCards);
        
        if (allCards.size() < 5) {
            throw new IllegalArgumentException("至少需要5张牌才能评估手牌");
        }
        
        // 生成所有可能的5张牌组合
        List<List<Card>> combinations = generateCombinations(allCards, 5);
        
        HandResult bestResult = null;
        
        for (List<Card> combination : combinations) {
            HandResult result = evaluateFiveCards(combination);
            if (bestResult == null || isHandBetter(result, bestResult)) {
                bestResult = result;
            }
        }
        
        return bestResult;
    }
    
    /**
     * 评估5张牌的牌型
     */
    private static HandResult evaluateFiveCards(List<Card> cards) {
        if (cards.size() != 5) {
            throw new IllegalArgumentException("必须是5张牌");
        }
        
        // 按点数排序
        List<Card> sortedCards = cards.stream()
                .sorted((a, b) -> Integer.compare(b.getRank(), a.getRank()))
                .collect(Collectors.toList());
        
        // 统计点数和花色
        Map<Integer, List<Card>> rankGroups = cards.stream()
                .collect(Collectors.groupingBy(Card::getRank));
        Map<Integer, List<Card>> suitGroups = cards.stream()
                .collect(Collectors.groupingBy(Card::getSuit));
        
        boolean isFlush = suitGroups.size() == 1;
        boolean isStraight = isStraight(cards);
        
        // 检查皇家同花顺
        if (isFlush && isStraight && sortedCards.get(0).getRank() == 14) {
            return new HandResult(HandType.ROYAL_FLUSH, Arrays.asList(14), sortedCards);
        }
        
        // 检查同花顺
        if (isFlush && isStraight) {
            return new HandResult(HandType.STRAIGHT_FLUSH, 
                    Arrays.asList(sortedCards.get(0).getRank()), sortedCards);
        }
        
        // 检查四条
        if (rankGroups.containsValue(rankGroups.values().stream()
                .filter(group -> group.size() == 4).findFirst().orElse(null))) {
            int fourKind = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 4)
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(0);
            int kicker = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 1)
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(0);
            return new HandResult(HandType.FOUR_OF_A_KIND, 
                    Arrays.asList(fourKind, kicker), sortedCards);
        }
        
        // 检查葫芦
        boolean hasThree = rankGroups.values().stream().anyMatch(group -> group.size() == 3);
        boolean hasPair = rankGroups.values().stream().anyMatch(group -> group.size() == 2);
        if (hasThree && hasPair) {
            int threeKind = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 3)
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(0);
            int pairKind = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 2)
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(0);
            return new HandResult(HandType.FULL_HOUSE, 
                    Arrays.asList(threeKind, pairKind), sortedCards);
        }
        
        // 检查同花
        if (isFlush) {
            List<Integer> ranks = sortedCards.stream()
                    .map(Card::getRank)
                    .collect(Collectors.toList());
            return new HandResult(HandType.FLUSH, ranks, sortedCards);
        }
        
        // 检查顺子
        if (isStraight) {
            return new HandResult(HandType.STRAIGHT, 
                    Arrays.asList(sortedCards.get(0).getRank()), sortedCards);
        }
        
        // 检查三条
        if (hasThree) {
            int threeKind = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 3)
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(0);
            List<Integer> kickers = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 1)
                    .map(Map.Entry::getKey)
                    .sorted(Collections.reverseOrder())
                    .collect(Collectors.toList());
            List<Integer> ranks = new ArrayList<>();
            ranks.add(threeKind);
            ranks.addAll(kickers);
            return new HandResult(HandType.THREE_OF_A_KIND, ranks, sortedCards);
        }
        
        // 检查对子
        List<Integer> pairs = rankGroups.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 2)
                .map(Map.Entry::getKey)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        
        if (pairs.size() == 2) {
            // 两对
            int kicker = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 1)
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(0);
            List<Integer> ranks = new ArrayList<>(pairs);
            ranks.add(kicker);
            return new HandResult(HandType.TWO_PAIR, ranks, sortedCards);
        } else if (pairs.size() == 1) {
            // 一对
            List<Integer> kickers = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 1)
                    .map(Map.Entry::getKey)
                    .sorted(Collections.reverseOrder())
                    .collect(Collectors.toList());
            List<Integer> ranks = new ArrayList<>();
            ranks.add(pairs.get(0));
            ranks.addAll(kickers);
            return new HandResult(HandType.ONE_PAIR, ranks, sortedCards);
        }
        
        // 高牌
        List<Integer> ranks = sortedCards.stream()
                .map(Card::getRank)
                .collect(Collectors.toList());
        return new HandResult(HandType.HIGH_CARD, ranks, sortedCards);
    }
    
    /**
     * 检查是否为顺子
     */
    private static boolean isStraight(List<Card> cards) {
        List<Integer> ranks = cards.stream()
                .map(Card::getRank)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        if (ranks.size() != 5) {
            return false;
        }
        
        // 检查普通顺子
        boolean normalStraight = true;
        for (int i = 1; i < ranks.size(); i++) {
            if (ranks.get(i) - ranks.get(i-1) != 1) {
                normalStraight = false;
                break;
            }
        }
        
        if (normalStraight) {
            return true;
        }
        
        // 检查A-2-3-4-5顺子
        return ranks.equals(Arrays.asList(2, 3, 4, 5, 14));
    }
    
    /**
     * 比较两个手牌结果
     * @param hand1 手牌1
     * @param hand2 手牌2
     * @return hand1是否比hand2更强
     */
    public static boolean isHandBetter(HandResult hand1, HandResult hand2) {
        if (hand1.handType.getStrength() != hand2.handType.getStrength()) {
            return hand1.handType.getStrength() > hand2.handType.getStrength();
        }
        
        // 相同牌型，比较点数
        List<Integer> ranks1 = hand1.getRanks();
        List<Integer> ranks2 = hand2.getRanks();
        
        for (int i = 0; i < Math.min(ranks1.size(), ranks2.size()); i++) {
            if (!ranks1.get(i).equals(ranks2.get(i))) {
                return ranks1.get(i) > ranks2.get(i);
            }
        }
        
        return false; // 完全相等
    }
    
    /**
     * 生成组合
     */
    private static List<List<Card>> generateCombinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        generateCombinations(cards, k, 0, new ArrayList<>(), result);
        return result;
    }
    
    private static void generateCombinations(List<Card> cards, int k, int start, 
                                           List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinations(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
    /**
     * 比较多个玩家的手牌，返回获胜者列表
     */
    public static List<Integer> compareHands(List<HandResult> hands) {
        if (hands.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Integer> winners = new ArrayList<>();
        HandResult bestHand = hands.get(0);
        winners.add(0);
        
        for (int i = 1; i < hands.size(); i++) {
            HandResult currentHand = hands.get(i);
            
            if (isHandBetter(currentHand, bestHand)) {
                // 发现更强的牌
                winners.clear();
                winners.add(i);
                bestHand = currentHand;
            } else if (!isHandBetter(bestHand, currentHand)) {
                // 平手
                winners.add(i);
            }
        }
        
        return winners;
    }
}