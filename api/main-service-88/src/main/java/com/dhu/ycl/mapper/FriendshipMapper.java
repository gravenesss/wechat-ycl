package com.dhu.ycl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dhu.ycl.pojo.Friendship;
import com.dhu.ycl.pojo.vo.ContactsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

// 朋友关系表 Mapper 接口
public interface FriendshipMapper extends BaseMapper<Friendship> {
    List<ContactsVO> queryMyFriends(@Param("paramMap") Map<String, Object> map);
}
