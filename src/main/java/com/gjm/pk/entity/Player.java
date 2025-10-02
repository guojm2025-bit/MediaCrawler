package com.gjm.pk.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gjm.pk.service.AIPlayerDecisionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 玩家实体类
 * @author: guojianming
 * @data 2025/09/17 17:46
 */
@Data
@NoArgsConstructor
public class Player {
    private String id;
    private String name;
    private int chips;
    private List<Card> holeCards = new ArrayList<>();
    private boolean isAi;
    private boolean inGame;
    private int currentBet;
    private PlayerAction lastAction;
    private boolean isDealer;
    private boolean isSmallBlind;
    private boolean isBigBlind;
    private boolean hasFolded;
    private AIPlayerDecisionService.AILevel aiLevel;
    
    // 玩家行动枚举
    public enum PlayerAction {
        FOLD, CHECK, CALL, RAISE, ALL_IN
    }

    public Player(String id, String name, int chips, boolean isAi) {
        this.id = id;
        this.name = name;
        this.chips = chips;
        this.isAi = isAi;
        this.inGame = true;
        this.currentBet = 0;
        this.hasFolded = false;
        this.aiLevel = isAi ? AIPlayerDecisionService.AILevel.MEDIUM : null;
    }
    
    public Player(String id, String name, int chips, boolean isAi, AIPlayerDecisionService.AILevel aiLevel) {
        this(id, name, chips, isAi);
        this.aiLevel = aiLevel;
    }

    /**
     * 下注操作
     * @param amount 下注金额
     * @return 实际下注金额
     */
    public int bet(int amount) {
        if (amount <= 0) {
            return 0;
        }
        
        int actualBet = Math.min(amount, chips);
        chips -= actualBet;
        currentBet += actualBet;
        return actualBet;
    }

    /**
     * 全下
     * @return 全下金额
     */
    public int allIn() {
        int allInAmount = chips;
        chips = 0;
        currentBet += allInAmount;
        lastAction = PlayerAction.ALL_IN;
        return allInAmount;
    }

    /**
     * 弃牌
     */
    public void fold() {
        hasFolded = true;
        inGame = false;
        lastAction = PlayerAction.FOLD;
    }

    /**
     * 跟注
     * @param callAmount 需要跟注的金额
     * @return 实际跟注金额
     */
    public int call(int callAmount) {
        int actualCall = bet(callAmount);
        lastAction = PlayerAction.CALL;
        return actualCall;
    }

    /**
     * 加注
     * @param raiseAmount 加注金额
     * @return 实际加注金额
     */
    public int raise(int raiseAmount) {
        int actualRaise = bet(raiseAmount);
        lastAction = PlayerAction.RAISE;
        return actualRaise;
    }

    /**
     * 看牌（不下注）
     */
    public void check() {
        lastAction = PlayerAction.CHECK;
    }

    /**
     * 获得奖金
     * @param amount 奖金金额
     */
    public void addWinnings(int amount) {
        chips += amount;
    }

    /**
     * 重置当前回合下注
     */
    public void resetCurrentBet() {
        currentBet = 0;
    }

    /**
     * 重置状态为新一局
     */
    public void resetForNewHand() {
        holeCards.clear();
        currentBet = 0;
        lastAction = null;
        isDealer = false;
        isSmallBlind = false;
        isBigBlind = false;
        hasFolded = false;
        inGame = chips > 0; // 只有有筹码的玩家才能参与下一局
    }

    /**
     * 判断是否全下
     */
    public boolean isAllIn() {
        return chips == 0 && currentBet > 0;
    }

    /**
     * 判断是否可以继续游戏
     */
    public boolean canContinue() {
        return chips > 0 && !hasFolded;
    }

    /**
     * 获取手牌的字符串表示
     */
    @JsonIgnore
    public String getHoleCardsString() {
        if (holeCards.size() >= 2) {
            return holeCards.get(0).toString() + ", " + holeCards.get(1).toString();
        }
        return "无手牌";
    }

    /**
     * 获取玩家状态信息
     */
    public String getStatusString() {
        if (hasFolded) return "弃牌";
        if (isAllIn()) return "全下";
        if (inGame) return "游戏中";
        return "离开";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Player player = (Player) obj;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Player{id='%s', name='%s', chips=%d, status='%s'}", 
                           id, name, chips, getStatusString());
    }
}
