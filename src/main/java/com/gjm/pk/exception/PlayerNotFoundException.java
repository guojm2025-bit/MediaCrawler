package com.gjm.pk.exception;

/**
 * 玩家未找到异常
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
public class PlayerNotFoundException extends GameException {
    
    public PlayerNotFoundException(String playerId) {
        super("PLAYER_NOT_FOUND", "玩家未找到: " + playerId);
    }
    
    public PlayerNotFoundException(String playerId, Throwable cause) {
        super("PLAYER_NOT_FOUND", "玩家未找到: " + playerId, cause);
    }
}