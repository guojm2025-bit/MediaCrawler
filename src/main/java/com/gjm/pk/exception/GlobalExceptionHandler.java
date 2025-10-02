package com.gjm.pk.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * @author: guojianming
 * @date: 2025/09/23
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理游戏异常
     */
    @ExceptionHandler(GameException.class)
    public ResponseEntity<Map<String, Object>> handleGameException(GameException e, WebRequest request) {
        log.error("游戏异常: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = createErrorResponse(
            e.getErrorCode(),
            e.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false)
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理非法游戏操作异常
     */
    @ExceptionHandler(IllegalGameActionException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalGameActionException(
            IllegalGameActionException e, WebRequest request) {
        log.warn("非法游戏操作: {}", e.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            e.getErrorCode(),
            e.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false)
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理玩家未找到异常
     */
    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePlayerNotFoundException(
            PlayerNotFoundException e, WebRequest request) {
        log.warn("玩家未找到: {}", e.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            e.getErrorCode(),
            e.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * 处理游戏状态异常
     */
    @ExceptionHandler(InvalidGameStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidGameStateException(
            InvalidGameStateException e, WebRequest request) {
        log.warn("无效游戏状态: {}", e.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            e.getErrorCode(),
            e.getMessage(),
            HttpStatus.CONFLICT.value(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException e, WebRequest request) {
        log.warn("参数错误: {}", e.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "INVALID_PARAMETER",
            "参数错误: " + e.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false)
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理一般运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException e, WebRequest request) {
        log.error("运行时异常: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "RUNTIME_ERROR",
            "系统运行异常，请稍后重试",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理一般异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception e, WebRequest request) {
        log.error("系统异常: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "SYSTEM_ERROR",
            "系统发生异常，请联系管理员",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, 
                                                   int status, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status);
        errorResponse.put("error", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        errorResponse.put("success", false);
        
        return errorResponse;
    }
}