package com.ragkb.common;

import lombok.Data;

/**
 * 统一返回封装
 *
 * 所有Controller接口的返回值都用这个类包装
 * 前端拿到的JSON格式统一为: { "code": 200, "message": "操作成功", "data": ... }
 */
@Data
public class Result<T> {

    /** 状态码 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 返回数据 */
    private T data;

    // ==================== 成功 ====================

    /**
     * 成功（带数据）
     */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    /**
     * 成功（无数据）
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    // ==================== 失败（使用ResultCode枚举） ====================

    /**
     * 失败（枚举，使用枚举默认的message）
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(resultCode.getMessage());
        return r;
    }

    /**
     * 失败（枚举 + 自定义message，覆盖枚举默认message）
     */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(message);
        return r;
    }

    // ==================== 失败（兼容写法） ====================

    /**
     * 失败（只传message，code默认400）
     */
    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.BAD_REQUEST.getCode());
        r.setMessage(message);
        return r;
    }

    /**
     * 失败（自定义code + message）
     * 提供最灵活的失败结果构造方式，允许完全自定义状态码和提示信息
     *
     * @param <T>    返回结果中携带的数据类型
     * @param code   自定义的状态码，通常用于表示特定的业务错误类型
     * @param message 自定义的提示信息，用于向调用方说明具体的错误原因
     * @return 包含自定义状态码和提示信息的失败结果对象
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
