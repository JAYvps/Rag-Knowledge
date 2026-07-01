package com.ragkb.common;

/**
 * 统一状态码枚举
 */
public enum ResultCode {

    // 成功状态码
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    // 业务错误码
    BUSINESS_ERROR(1000, "业务异常"),
    VALIDATION_ERROR(1001, "参数校验失败"),
    DATA_NOT_FOUND(1002, "数据不存在"),
    DATA_EXISTS(1003, "数据已存在"),
    OPERATION_FAILED(1004, "操作失败"),

    // 认证授权错误
    AUTH_FAILED(2001, "认证失败"),
    TOKEN_EXPIRED(2002, "令牌已过期"),
    TOKEN_INVALID(2003, "令牌无效"),
    PERMISSION_DENIED(2004, "权限不足"),

    // 系统错误 5xx
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 第三方服务错误
    THIRD_PARTY_ERROR(3001, "第三方服务异常"),
    API_CALL_FAILED(3002, "API调用失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据状态码获取枚举
     */
    public static ResultCode getByCode(int code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode() == code) {
                return resultCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }

    /**
     * 判断是否为成功状态码
     */
    public boolean isSuccess() {
        return this.code >= 200 && this.code < 300;
    }

    /**
     * 判断是否为客户端错误
     */
    public boolean isClientError() {
        return this.code >= 400 && this.code < 500;
    }

    /**
     * 判断是否为服务器错误
     */
    public boolean isServerError() {
        return this.code >= 500 && this.code < 600;
    }
}
