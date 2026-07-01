// ============ yuque/dto/YuqueTocItem.java ============
package com.ragkb.yuque.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 语雀目录树节点
 *
 * data直接是数组: [ { "id":..., "title":..., "doc_id":..., "type":... }, ... ]
 * 其中 type="DOC" 是文档节点，type="TITLE" 是纯目录节点
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YuqueTocItem {

    private Long id;
    private String uuid;

    /** 文档ID，为空说明是纯目录节点 */
    @JsonProperty("doc_id")
    private Long docId;

    /** 文档标题 */
    private String title;

    /** 文档URL标识 */
    private String slug;

    /** 目录层级深度 */
    private Integer depth;

    /** 语雀文档URL */
    private String url;

    /** 节点类型: DOC=文档, TITLE=目录 */
    private String type;

    @JsonProperty("parent_uuid")
    private String parentUuid;
}
