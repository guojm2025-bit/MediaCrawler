package com.gjm.pk.exception;

/**
 * 游戏状态异常
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
public class InvalidGameStateException extends GameException {
    
    public InvalidGameStateException(String message) {
        super("INVALID_GAME_STATE", message);
    }
    
    public InvalidGameStateException(String message, Throwable cause) {
        super("INVALID_GAME_STATE", message, cause);
    }
}