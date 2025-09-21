package com.dhu.ycl.service;

import com.dhu.ycl.pojo.bo.NewFriendRequestBO;
import com.dhu.ycl.utils.PagedGridResult;

// 好友请求 服务类
public interface FriendRequestService {
    // 查询新朋友的请求记录列表
    PagedGridResult queryNewFriendList(String userId, Integer page, Integer pageSize);

    // 新增添加好友的请求
    void addNewRequest(NewFriendRequestBO friendRequestBO);

    // 通过好友请求：就是请求的id，不是发送方或接收方的用户id。
    void passNewFriend(String friendRequestId, String friendRemark);
}
