package com.dhu.ycl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dhu.ycl.pojo.FriendCircle;
import com.dhu.ycl.pojo.vo.FriendCircleVO;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

// 朋友圈表 Mapper 接口
public interface FriendCircleMapper extends BaseMapper<FriendCircle> {
    Page<FriendCircleVO> queryFriendCircleList(@Param("page") Page<FriendCircleVO> page, @Param("paramMap") Map<String, Object> map);
}
