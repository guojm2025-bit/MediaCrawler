package com.gjm.pk.exception;

/**
 * 非法游戏操作异常
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
public class IllegalGameActionException extends GameException {
    
    public IllegalGameActionException(String message) {
        super("ILLEGAL_ACTION", message);
    }
    
    public IllegalGameActionException(String message, Throwable cause) {
        super("ILLEGAL_ACTION", message, cause);
    }
}