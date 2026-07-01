// ============ entity/DocumentChunk.java ============
package com.ragkb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档分块记录
 *
 * 不管是语雀同步的还是用户上传的，分块后都记录在这张表
 * 通过 sourceType + sourceId 定位到原始文档
 */
@Data
@TableName("document_chunk")
public class DocumentChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceType;      // yuque / user
    private Long sourceId;          // 对应表的ID
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private String vectorId;        // 向量数据库中的ID
    private LocalDateTime createdAt;
}
