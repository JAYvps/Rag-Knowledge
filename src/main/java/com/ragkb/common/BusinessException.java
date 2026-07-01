package com.ragkb.common;

import lombok.Getter;

/**
 * 业务异常
 *
 * 在Service层抛出，由GlobalExceptionHandler统一捕获并返回给前端
 * 用法：
 *   throw new BusinessException("用户名已存在");
 *   throw new BusinessException(ResultCode.DATA_NOT_FOUND);
 *   throw new BusinessException(ResultCode.OPERATION_FAILED, "同步失败：超时");
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final int code;

    /**
     * 只传message，code默认400
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST.getCode();
    }

    /**
     * 传ResultCode枚举，message取枚举中的默认值
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 传ResultCode枚举 + 自定义message，覆盖枚举默认值
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
