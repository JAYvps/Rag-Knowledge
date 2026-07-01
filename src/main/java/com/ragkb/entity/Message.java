// ============ entity/Message.java ============
package com.ragkb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息记录
 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属对话ID */
    private Long conversationId;

    /** 角色: user / assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /**
     * 引用来源 (JSON格式)
     * 注意：不能用 references 作为字段名，是MySQL保留关键字
     * Java字段名用 refData，MyBatis-Plus自动映射到 ref_data 列
     */
    private String refData;

    /** 消耗的token数 */
    private Integer tokenUsed;

    private LocalDateTime createdAt;
}
