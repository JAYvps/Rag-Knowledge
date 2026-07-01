// ============ yuque/dto/YuqueDocDetail.java ============
package com.ragkb.yuque.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 语雀文档详情（含正文）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YuqueDocDetail {

    private Long id;
    private String slug;
    private String title;
    private String body;

    @JsonProperty("body_html")
    private String bodyHtml;

    @JsonProperty("word_count")
    private Integer wordCount;

    @JsonProperty("book_id")
    private Long bookId;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("created_at")
    private String createdAt;
}
