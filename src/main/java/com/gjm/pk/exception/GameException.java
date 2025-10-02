package com.gjm.pk.exception;

/**
 * 游戏异常基类
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
public class GameException extends RuntimeException {
    
    private final String errorCode;
    
    public GameException(String message) {
        super(message);
        this.errorCode = "GAME_ERROR";
    }
    
    public GameException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public GameException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GAME_ERROR";
    }
    
    public GameException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}