package com.dhu.ycl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dhu.ycl.pojo.Comment;
import com.dhu.ycl.pojo.vo.CommentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CommentMapper extends BaseMapper<Comment> {
    List<CommentVO> queryFriendCircleComments(@Param("paramMap") Map<String, Object> map);
}
