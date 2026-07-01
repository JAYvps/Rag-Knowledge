// ============ mapper/DocumentChunkMapper.java ============
package com.ragkb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragkb.entity.DocumentChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    @Select("SELECT * FROM document_chunk WHERE source_type = #{type} AND source_id = #{id}")
    List<DocumentChunk> selectBySource(@Param("type") String type, @Param("id") Long id);

    @Delete("DELETE FROM document_chunk WHERE source_type = #{type} AND source_id = #{id}")
    int deleteBySource(@Param("type") String type, @Param("id") Long id);
}
