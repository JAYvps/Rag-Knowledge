// ============ entity/YuqueDocSync.java ============
package com.ragkb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 语雀文档同步记录
 * 每一篇从语雀同步过来的文档都有一条记录
 * contentHash 用于增量对比：下次同步时如果hash没变就跳过
 */
@Data
@TableName("yuque_doc")
public class YuqueDocSync {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 yuque_repo.id */
    private Long yuqueRepoId;

    /** 语雀文档ID */
    private Long yuqueDocId;

    /** 文档标题 */
    private String title;

    /** 内容MD5哈希，增量同步判断用 */
    private String contentHash;

    /** 字数 */
    private Integer wordCount;

    /** 分块数 */
    private Integer chunkCount;

    /** 同步状态: 1成功 0失败 */
    private Integer syncStatus;

    /** 语雀侧最后更新时间 */
    private LocalDateTime yuqueUpdatedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
