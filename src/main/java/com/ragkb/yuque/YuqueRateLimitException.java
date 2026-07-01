// ============ yuque/YuqueRateLimitException.java ============
package com.ragkb.yuque;

/**
 * 语雀API频率限制异常（HTTP 429）
 */
public class YuqueRateLimitException extends RuntimeException {

    public YuqueRateLimitException(String message) {
        super(message);
    }
}
