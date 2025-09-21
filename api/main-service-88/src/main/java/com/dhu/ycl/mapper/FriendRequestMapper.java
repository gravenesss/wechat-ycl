package com.dhu.ycl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dhu.ycl.pojo.FriendRequest;
import com.dhu.ycl.pojo.vo.NewFriendsVO;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

// 好友请求记录表 Mapper 接口
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
    Page<NewFriendsVO> queryNewFriendList(@Param("page") Page<NewFriendsVO> page, @Param("paramMap") Map<String, Object> map);
}
