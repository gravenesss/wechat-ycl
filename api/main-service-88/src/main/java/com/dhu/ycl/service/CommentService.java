package com.dhu.ycl.service;

import com.dhu.ycl.pojo.bo.CommentBO;
import com.dhu.ycl.pojo.vo.CommentVO;

import java.util.List;


public interface CommentService {
    // 创建发表评论
    CommentVO createComment(CommentBO commentBO);

    // 查询朋友圈的列表
    List<CommentVO> queryAll(String friendCircleId);

    // 删除朋友圈的评论
    void deleteComment(String commentUserId, String commentId, String friendCircleId);
}
