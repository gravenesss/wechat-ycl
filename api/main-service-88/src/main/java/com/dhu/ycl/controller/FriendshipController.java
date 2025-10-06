package com.dhu.ycl.controller;


import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.enums.YesOrNo;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.service.FriendshipService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/friendship")
public class FriendshipController extends BaseInfoProperties {
    @Resource
    private FriendshipService friendshipService;

    // 通讯录界面-展示好友
    @PostMapping("/queryMyFriends")
    public GraceJSONResult queryMyFriends(HttpServletRequest request) {
        String myId = request.getHeader(HEADER_USER_ID);
        return GraceJSONResult.ok(friendshipService.queryMyFriends(myId, false));
    }

    // 通讯录界面-黑名单
    @PostMapping("/queryMyBlackList")
    public GraceJSONResult queryMyBlackList(HttpServletRequest request) {
        String myId = request.getHeader(HEADER_USER_ID);
        return GraceJSONResult.ok(friendshipService.queryMyFriends(myId, true));
    }

    // 通讯录界面：点击头像进入资料设置
    @PostMapping("/getFriendship")
    public GraceJSONResult pass(String friendId, HttpServletRequest request) {
        String myId = request.getHeader(HEADER_USER_ID);
        return GraceJSONResult.ok(friendshipService.getFriendship(myId, friendId));
    }

    // 好友信息界面-资料设置：修改好友备注
    @PostMapping("/updateFriendRemark")
    public GraceJSONResult updateFriendRemark(HttpServletRequest request, String friendId, String friendRemark) {
        if (StringUtils.isBlank(friendId) || StringUtils.isBlank(friendRemark)) {
            return GraceJSONResult.error();
        }
        String myId = request.getHeader(HEADER_USER_ID);
        friendshipService.updateFriendRemark(myId, friendId, friendRemark);
        return GraceJSONResult.ok();
    }

    // 好友信息界面-资料设置：拉黑
    @PostMapping("/tobeBlack")
    public GraceJSONResult tobeBlack(HttpServletRequest request, String friendId) {
        if (StringUtils.isBlank(friendId)) {
            return GraceJSONResult.error();
        }
        String myId = request.getHeader(HEADER_USER_ID);
        friendshipService.updateBlackList(myId, friendId, YesOrNo.YES);
        return GraceJSONResult.ok();
    }

    // 好友信息界面-资料设置：移出黑名单
    @PostMapping("/moveOutBlack")
    public GraceJSONResult moveOutBlack(HttpServletRequest request, String friendId) {
        if (StringUtils.isBlank(friendId)) {
            return GraceJSONResult.error();
        }
        String myId = request.getHeader(HEADER_USER_ID);
        friendshipService.updateBlackList(myId, friendId, YesOrNo.NO);
        return GraceJSONResult.ok();
    }

    // 好友信息界面-资料设置：删除
    @PostMapping("/delete")
    public GraceJSONResult delete(HttpServletRequest request, String friendId) {
        if (StringUtils.isBlank(friendId)) {
            return GraceJSONResult.error();
        }
        String myId = request.getHeader(HEADER_USER_ID);
        friendshipService.delete(myId, friendId);
        return GraceJSONResult.ok();
    }

    // 前端未使用：判断两个朋友之前的关系是否拉黑。com.dhu.ycl.websocket.ChatHandler中使用。
    @GetMapping("/isBlack")
    public GraceJSONResult isBlack(String friendId1st, String friendId2nd) {
        // 需要进行两次查询，A拉黑B，B拉黑A，AB相互拉黑。只需要符合其中的一个条件，就表示双发发送消息不可送达
        return GraceJSONResult.ok(friendshipService.isBlackEachOther(friendId1st, friendId2nd));
    }
}
