package com.ragkb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话记录
 */
@Data
@TableName("conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 对话标题（默认取第一条问题） */
    private String title;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
