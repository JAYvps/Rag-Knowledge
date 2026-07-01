// ============ mapper/YuqueDocSyncMapper.java ============
package com.ragkb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragkb.entity.YuqueDocSync;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface YuqueDocSyncMapper extends BaseMapper<YuqueDocSync> {

    /**
     * 查询某个知识库下的所有文档记录
     */
    @Select("SELECT * FROM yuque_doc WHERE yuque_repo_id = #{repoId}")
    List<YuqueDocSync> selectByRepoId(@Param("repoId") Long repoId);
}
