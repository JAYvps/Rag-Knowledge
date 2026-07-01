// ============ entity/YuqueRepo.java ============
package com.ragkb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 语雀知识库配置
 *
 * repo_source 字段区分数据来源：
 * - OWN:   自己语雀账户中的知识库
 * - PUBLIC: 互联网上任意公开知识库
 */
@Data
@TableName("yuque_repo")
public class YuqueRepo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 语雀知识库ID */
    private Long yuqueRepoId;

    /** 语雀slug（URL标识） */
    private String yuqueRepoSlug;

    /** 展示名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 图标URL */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 数据来源: OWN / PUBLIC */
    private String repoSource;

    /** 语雀命名空间（用户名或组织名） */
    private String namespace;

    /** 同步状态: 0未同步 1同步中 2已完成 3失败 */
    private Integer syncStatus;

    /** 最后同步时间 */
    private LocalDateTime lastSyncAt;

    /** 最后同步消息 */
    private String lastSyncMsg;

    /** 已同步文档数 */
    private Integer docCount;

    /** 已同步分块数 */
    private Integer chunkCount;

    /** 1启用 0禁用 */
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
