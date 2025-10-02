package com.gjm.pk.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Objects;

/**
 * 扑克牌实体类
 * @author: guojianming
 * @data 2025/09/17 17:45
 */
@Data
@NoArgsConstructor
public class Card {
    // 花色：0-黑桃，1-红桃，2-方块，3-梅花
    private int suit;
    // 点数：2-14（A=14，K=13，Q=12，J=11，T=10）
    private int rank;

    // 花色常量
    public static final int SPADES = 0;    // 黑桃
    public static final int HEARTS = 1;    // 红桃
    public static final int DIAMONDS = 2;  // 方块
    public static final int CLUBS = 3;     // 梅花
    
    // 点数常量
    public static final int ACE_HIGH = 14;
    public static final int KING = 13;
    public static final int QUEEN = 12;
    public static final int JACK = 11;
    public static final int TEN = 10;
    
    // 花色字符串数组
    private static final String[] SUIT_NAMES = {"黑桃", "红桃", "方块", "梅花"};
    // 点数字符串数组
    private static final String[] RANK_NAMES = {"", "", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

    public Card(int suit, int rank) {
        if (suit < 0 || suit > 3) {
            throw new IllegalArgumentException("非法的花色值: " + suit);
        }
        if (rank < 2 || rank > 14) {
            throw new IllegalArgumentException("非法的点数值: " + rank);
        }
        this.suit = suit;
        this.rank = rank;
    }

    /**
     * 获取花色名称
     */
    public String getSuitName() {
        return SUIT_NAMES[suit];
    }
    
    /**
     * 获取点数名称
     */
    public String getRankName() {
        return RANK_NAMES[rank];
    }
    
    /**
     * 判断是否为A
     */
    public boolean isAce() {
        return rank == ACE_HIGH;
    }
    
    /**
     * 判断是否为红色牌（红桃或方块）
     */
    public boolean isRed() {
        return suit == HEARTS || suit == DIAMONDS;
    }
    
    /**
     * 判断是否为黑色牌（黑桃或梅花）
     */
    public boolean isBlack() {
        return suit == SPADES || suit == CLUBS;
    }

    /**
     * 牌的字符串表示（如"黑桃A"）
     */
    @Override
    public String toString() {
        return SUIT_NAMES[suit] + RANK_NAMES[rank];
    }
    
    /**
     * 简短表示（如"SA"）
     */
    public String toShortString() {
        String[] shortSuits = {"S", "H", "D", "C"};
        String[] shortRanks = {"", "", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
        return shortSuits[suit] + shortRanks[rank];
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Card card = (Card) obj;
        return suit == card.suit && rank == card.rank;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(suit, rank);
    }
}