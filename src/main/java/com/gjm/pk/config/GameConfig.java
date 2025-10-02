package com.gjm.pk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 游戏配置类
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
@Data
@Component
@ConfigurationProperties(prefix = "game.poker")
public class GameConfig {
    
    /**
     * 小盲注金额
     */
    private int smallBlind = 10;
    
    /**
     * 大盲注金额
     */
    private int bigBlind = 20;
    
    /**
     * 最大玩家数
     */
    private int maxPlayers = 8;
    
    /**
     * 最小玩家数
     */
    private int minPlayers = 2;
    
    /**
     * 默认筹码数量
     */
    private int defaultChips = 1000;
    
    /**
     * 行动超时时间（秒）
     */
    private int actionTimeout = 30;
    
    /**
     * AI思考时间（秒）
     */
    private int aiThinkingTime = 3;
}