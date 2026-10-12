package com.liyun.item.mapper;

import com.liyun.item.domain.pojo.Favorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户收藏 Mapper 接口
 * </p>
 *
 * @author liyun
 * @since 2026-05-28
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

}
