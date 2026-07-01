// ============ dto/ChatMessage.java ============
package com.ragkb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息（返回给前端）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    private Long id;
    private Long conversationId;
    private String role;          // user / assistant
    private String content;
    private java.util.List<SourceRef> references;
    private LocalDateTime createdAt;
}
