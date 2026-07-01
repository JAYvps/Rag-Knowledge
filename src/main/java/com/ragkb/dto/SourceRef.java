// ============ dto/SourceRef.java ============
package com.ragkb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 引用来源
 * 每条引用指向一个检索到的文档分块
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SourceRef {

    /** 序号 */
    private int index;

    /** 来源标签: "企业知识库" 或 "我的文档" */
    private String sourceLabel;

    /** 来源文档名 */
    private String docTitle;

    /** 分块内容摘要（前200字） */
    private String snippet;

    /** 相似度分数 */
    private double score;
}
