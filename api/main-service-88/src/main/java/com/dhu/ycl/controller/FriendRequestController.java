package com.dhu.ycl.controller;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.pojo.bo.NewFriendRequestBO;
import com.dhu.ycl.service.FriendRequestService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


// 好友请求 控制器
@RestController
@RequestMapping("/friendRequest")
public class FriendRequestController extends BaseInfoProperties {
    @Resource
    private FriendRequestService friendRequestService;

    // 查询新朋友的请求记录列表  userId=" + userId + "&page=1&pageSize=10
    @PostMapping("/queryNew")
    public GraceJSONResult queryNew(HttpServletRequest request,
                                    @RequestParam(defaultValue = "1", name = "page") Integer page,
                                    @RequestParam(defaultValue = "10", name = "pageSize") Integer pageSize) {
        String userId = request.getHeader(HEADER_USER_ID);
        return GraceJSONResult.ok(friendRequestService.queryNewFriendList(userId, page, pageSize));
    }

    // 新增添加好友的请求
    @PostMapping("/add")
    public GraceJSONResult add(@RequestBody @Valid NewFriendRequestBO friendRequestBO) {
        friendRequestService.addNewRequest(friendRequestBO);
        return GraceJSONResult.ok();
    }

    // 通过好友请求：就是请求的id，不是发送方或接收方的用户id。
    @PostMapping("/pass")
    public GraceJSONResult pass(String friendRequestId, String friendRemark) {
        friendRequestService.passNewFriend(friendRequestId, friendRemark);
        return GraceJSONResult.ok();
    }

}
