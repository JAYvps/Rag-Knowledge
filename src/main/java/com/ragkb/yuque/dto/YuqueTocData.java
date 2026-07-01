// ============ yuque/dto/YuqueTocData.java ============
package com.ragkb.yuque.dto;

import lombok.Data;
import java.util.List;

/**
 * 语雀TOC接口返回的数据部分
 */
@Data
public class YuqueTocData {

    /** 目录树节点列表 */
    private List<YuqueTocItem> toc;
}
