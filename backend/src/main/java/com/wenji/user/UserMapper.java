package com.wenji.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE users SET level = FLOOR((points + #{points}) / 100) + 1, " +
            "points = points + #{points} WHERE id = #{userId}")
    int addPoints(@Param("userId") Long userId, @Param("points") int points);
}
