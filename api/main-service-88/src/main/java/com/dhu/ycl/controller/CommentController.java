package com.dhu.ycl.controller;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.pojo.bo.CommentBO;
import com.dhu.ycl.pojo.vo.CommentVO;
import com.dhu.ycl.service.CommentService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
public class CommentController extends BaseInfoProperties {
    @Resource
    private CommentService commentService;

    @RequestMapping("/create")
    public GraceJSONResult create(@RequestBody CommentBO friendCircleBO) {
        CommentVO commentVO = commentService.createComment(friendCircleBO);
        return GraceJSONResult.ok(commentVO);
    }

    @RequestMapping("/query")
    public GraceJSONResult queryAll(String friendCircleId) {
        return GraceJSONResult.ok(commentService.queryAll(friendCircleId));
    }

    @RequestMapping("/delete")
    public GraceJSONResult delete(String commentUserId, String commentId, String friendCircleId) {
        if (StringUtils.isBlank(commentUserId) || StringUtils.isBlank(commentId) || StringUtils.isBlank(friendCircleId)) {
            return GraceJSONResult.error();
        }
        commentService.deleteComment(commentUserId, commentId, friendCircleId);
        return GraceJSONResult.ok();
    }
}
