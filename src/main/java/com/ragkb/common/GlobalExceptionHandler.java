package com.ragkb.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 拦截所有Controller层抛出的异常，统一转换为Result格式返回给前端
 * 这样Controller里就不需要每个方法都写try-catch了
 *
 * 异常捕获优先级：精确异常 > 通用异常
 * 先匹配具体的异常类型，最后用Exception兜底
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 — 返回400
     * 场景：用户名已存在、数据不存在、操作失败等
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常(@Valid) — 返回400
     * 场景：@NotBlank、@Size等校验不通过
     * 把所有字段的校验错误信息拼接返回
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.VALIDATION_ERROR, msg);
    }

    /**
     * 用户名或密码错误 — 返回401
     * 场景：登录时密码不对
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentials(BadCredentialsException e) {
        return Result.fail(ResultCode.AUTH_FAILED, "用户名或密码错误");
    }

    /**
     * 认证失败 — 返回401
     * 场景：未登录、Token过期、Token无效等
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthentication(AuthenticationException e) {
        return Result.fail(ResultCode.UNAUTHORIZED, e.getMessage());
    }

    /**
     * 权限不足 — 返回403
     * 场景：普通用户访问管理员接口
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccess(AccessDeniedException e) {
        return Result.fail(ResultCode.PERMISSION_DENIED);
    }

    /**
     * 兜底：未知异常 — 返回500
     * 场景：代码bug、数据库连接失败等未预料到的异常
     * 记录完整堆栈日志，方便排查
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
