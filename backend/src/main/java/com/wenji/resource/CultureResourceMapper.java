package com.wenji.resource;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CultureResourceMapper extends BaseMapper<CultureResource> {

    @Update("UPDATE culture_resources SET view_count = view_count + 1 WHERE id = #{id} AND status = 'PUBLISHED'")
    int incrementViewCount(Long id);

    @Update("UPDATE culture_resources SET favorite_count = favorite_count + 1 WHERE id = #{id} AND status = 'PUBLISHED'")
    int incrementFavoriteCount(Long id);

    @Update("UPDATE culture_resources SET favorite_count = CASE WHEN favorite_count > 0 THEN favorite_count - 1 ELSE 0 END WHERE id = #{id}")
    int decrementFavoriteCount(Long id);
}
