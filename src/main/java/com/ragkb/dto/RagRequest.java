package com.ragkb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RAG问答请求
 */
@Data
public class RagRequest {

    /** 用户问题 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 对话ID（为空则新建对话） */
    private Long conversationId;
}
