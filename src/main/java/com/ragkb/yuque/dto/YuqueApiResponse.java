// ============ yuque/dto/YuqueApiResponse.java ============
package com.ragkb.yuque.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 语雀API统一响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YuqueApiResponse<T> {

    private T data;
    private boolean ok;
    private String message;
    private Integer status;
}
