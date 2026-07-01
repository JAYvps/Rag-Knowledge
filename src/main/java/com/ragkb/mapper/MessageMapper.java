// ============ mapper/MessageMapper.java ============
package com.ragkb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragkb.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 获取对话最近N条消息（用于多轮对话上下文）
     * 按创建时间降序取N条，再反转为正序
     */
    @Select("""
        SELECT * FROM (
            SELECT * FROM message
            WHERE conversation_id = #{convId}
            ORDER BY created_at DESC
            LIMIT #{limit}
        ) tmp
        ORDER BY created_at ASC
        """)
    List<Message> selectRecent(@Param("convId") Long convId, @Param("limit") int limit);
}
