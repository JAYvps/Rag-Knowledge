// ============ mapper/UserMapper.java ============
package com.ragkb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragkb.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // BaseMapper已提供: selectById, insert, updateById, deleteById, selectOne 等
    // 自定义查询写在XML或用@Select注解
}
