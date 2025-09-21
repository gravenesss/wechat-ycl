package com.dhu.ycl.service;

import com.dhu.ycl.pojo.FriendCircleLiked;
import com.dhu.ycl.pojo.bo.FriendCircleBO;
import com.dhu.ycl.utils.PagedGridResult;

import java.util.List;

public interface FriendCircleService {
    // 发布朋友圈图文数据，保存到数据库
    void publish(FriendCircleBO friendCircleBO);

    // 分页查询朋友圈图文列表
    PagedGridResult queryList(String userId, Integer page, Integer pageSize);

    // 点赞朋友圈
    void like(String friendCircleId, String userId);

    // 取消(删除)点赞朋友圈
    void unlike(String friendCircleId, String userId);

    // 查询朋友圈的点赞列表
    List<FriendCircleLiked> queryLikedFriends(String friendCircleId);

    // 判断当前用户是否点赞过朋友圈
    boolean doILike(String friendCircleId, String userId);

    // 删除朋友圈发布的文案
    void delete(String friendCircleId, String userId);
}
