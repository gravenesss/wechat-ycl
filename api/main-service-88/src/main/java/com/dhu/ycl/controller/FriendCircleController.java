package com.dhu.ycl.controller;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.pojo.FriendCircleLiked;
import com.dhu.ycl.pojo.bo.FriendCircleBO;
import com.dhu.ycl.pojo.vo.CommentVO;
import com.dhu.ycl.pojo.vo.FriendCircleVO;
import com.dhu.ycl.service.CommentService;
import com.dhu.ycl.service.FriendCircleService;
import com.dhu.ycl.utils.PagedGridResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/friendCircle")
public class FriendCircleController extends BaseInfoProperties {
    @Resource
    private FriendCircleService friendCircleService;
    @Resource
    private CommentService commentService;

    @RequestMapping("/queryList")
    public GraceJSONResult publish(String userId, @RequestParam(defaultValue = "1", name = "page") Integer page,
                                   @RequestParam(defaultValue = "10", name = "pageSize") Integer pageSize) {
        if (StringUtils.isBlank(userId)) return GraceJSONResult.error();
        // 获取文案列表
        PagedGridResult gridResult = friendCircleService.queryList(userId, page, pageSize);
        List<FriendCircleVO> list = (List<FriendCircleVO>) gridResult.getRows();
        for (FriendCircleVO friendCircleVO : list) {
            String friendCircleId = friendCircleVO.getFriendCircleId();
            // 点赞列表
            List<FriendCircleLiked> likedList = friendCircleService.queryLikedFriends(friendCircleId);
            friendCircleVO.setLikedFriends(likedList);
            // 判断当前用户是否点赞，用于特别标识
            boolean res = friendCircleService.doILike(friendCircleId, userId);
            friendCircleVO.setDoILike(res);
            // 获取评论列表
            List<CommentVO> commentList = commentService.queryAll(friendCircleId);
            friendCircleVO.setCommentList(commentList);
        }
        return GraceJSONResult.ok(gridResult);
    }

    @RequestMapping("/publish")
    public GraceJSONResult publish(@RequestBody FriendCircleBO friendCircleBO, HttpServletRequest request) {
        friendCircleBO.setUserId(request.getHeader(HEADER_USER_ID));
        friendCircleBO.setPublishTime(LocalDateTime.now());
        friendCircleService.publish(friendCircleBO);
        return GraceJSONResult.ok();
    }

    @RequestMapping("/delete")
    public GraceJSONResult delete(String friendCircleId, HttpServletRequest request) {
        friendCircleService.delete(friendCircleId, request.getHeader(HEADER_USER_ID));
        return GraceJSONResult.ok();
    }

    @RequestMapping("/like")
    public GraceJSONResult like(String friendCircleId, HttpServletRequest request) {
        friendCircleService.like(friendCircleId, request.getHeader(HEADER_USER_ID));
        return GraceJSONResult.ok();
    }

    @RequestMapping("/unlike")
    public GraceJSONResult unlike(String friendCircleId, HttpServletRequest request) {
        friendCircleService.unlike(friendCircleId, request.getHeader(HEADER_USER_ID));
        return GraceJSONResult.ok();
    }

    @RequestMapping("/likedFriends")
    public GraceJSONResult likedFriends(String friendCircleId) {
        List<FriendCircleLiked> likedList = friendCircleService.queryLikedFriends(friendCircleId);
        return GraceJSONResult.ok(likedList);
    }
}