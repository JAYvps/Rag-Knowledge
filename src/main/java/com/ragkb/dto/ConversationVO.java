// ============ dto/ConversationVO.java ============
package com.ragkb.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话列表项
 */
@Data
public class ConversationVO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
