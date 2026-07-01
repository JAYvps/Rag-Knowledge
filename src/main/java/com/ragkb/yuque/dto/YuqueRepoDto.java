// ============ yuque/dto/YuqueRepoDto.java ============
package com.ragkb.yuque.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 语雀知识库DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YuqueRepoDto {

    private Long id;
    private String slug;
    private String name;
    private String description;

    @JsonProperty("public")
    private Integer isPublic;

    private Integer itemsCount;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("created_at")
    private String createdAt;
}
