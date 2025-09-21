package com.dhu.ycl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.mapper.CommentMapper;
import com.dhu.ycl.pojo.Comment;
import com.dhu.ycl.pojo.Users;
import com.dhu.ycl.pojo.bo.CommentBO;
import com.dhu.ycl.pojo.vo.CommentVO;
import com.dhu.ycl.service.CommentService;
import com.dhu.ycl.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl extends BaseInfoProperties implements CommentService {
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private UserService userService;

    @Override
    public CommentVO createComment(CommentBO commentBO) {
        // 1、构造评论并插入数据库
        Comment pendingComment = new Comment();
        BeanUtils.copyProperties(commentBO, pendingComment);
        pendingComment.setCreatedTime(LocalDateTime.now());
        commentMapper.insert(pendingComment);
        // 2、留言后的最新评论数据需要返回给前端（提供前端做的扩展数据）
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(pendingComment, commentVO);
        Users commentUser = userService.getById(commentBO.getCommentUserId());
        commentVO.setCommentUserNickname(commentUser.getNickname());
        commentVO.setCommentUserFace(commentUser.getFace());
        commentVO.setCommentId(pendingComment.getId());
        return commentVO;
    }

    @Override
    public List<CommentVO> queryAll(String friendCircleId) {
        Map<String, Object> map = new HashMap<>();
        map.put("friendCircleId", friendCircleId);
        return commentMapper.queryFriendCircleComments(map);
    }

    @Override
    public void deleteComment(String commentUserId, String commentId, String friendCircleId) {
        QueryWrapper<Comment> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("id", commentId)
                .eq("friend_circle_id", friendCircleId)
                .eq("comment_user_id", commentUserId);
        commentMapper.delete(deleteWrapper);
    }
}
