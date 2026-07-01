// ============ mapper/YuqueRepoMapper.java ============
package com.ragkb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragkb.entity.YuqueRepo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface YuqueRepoMapper extends BaseMapper<YuqueRepo> {

    /**
     * 查询所有启用的知识库，按排序字段升序
     */
    @Select("SELECT * FROM yuque_repo WHERE status = 1 ORDER BY sort_order ASC")
    List<YuqueRepo> selectEnabled();
}
