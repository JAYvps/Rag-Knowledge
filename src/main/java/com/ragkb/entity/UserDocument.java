// ============ entity/UserDocument.java ============
package com.ragkb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户上传的文档
 *
 * 注意：不使用全局的 status 逻辑删除配置
 * status字段含义：0待处理 1处理中 2就绪 3失败
 */
@Data
@TableName("user_document")
public class UserDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上传用户ID */
    private Long userId;

    /** 原始文件名 */
    private String fileName;

    /** pdf / txt / md */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 存储路径 */
    private String filePath;

    /** 展示标题 */
    private String title;

    /**
     * 处理状态
     * 0=待处理  1=处理中  2=就绪  3=失败
     *
     * 用 @TableField 明确标注不是逻辑删除字段
     */
    @TableField("status")
    private Integer status;

    /** 失败原因 */
    private String errorMsg;

    /** 分块数 */
    private Integer chunkCount;

    /** 字数 */
    private Integer wordCount;

    /** 逻辑删除标记（0正常 1已删除） */
    //@TableLogic
    private Integer isDeleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
