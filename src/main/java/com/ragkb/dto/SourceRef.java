// ============ dto/SourceRef.java ============
package com.ragkb.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 引用来源
 * 每条引用指向一个检索到的文档分块
 */
@Data
@NoArgsConstructor
public class SourceRef {

    /** 序号 */
    private int index;

    /** 来源标签: "企业知识库" / "全局文档" / "我的文档" */
    private String sourceLabel;

    /** 来源文档名 */
    private String docTitle;

    /** 分块内容摘要（前200字） */
    private String snippet;

    /** 相似度分数 */
    private double score;

    /** 来源类型: "yuque" / "user" */
    private String sourceType;

    /** 来源ID: yuque_doc.id 或 user_document.id */
    private Long sourceId;

    /** 知识库ID（仅语雀文档有值，用于跳转到知识库页面） */
    private Long repoId;

    /** 是否全局文档 */
    private Boolean isGlobal;
}
