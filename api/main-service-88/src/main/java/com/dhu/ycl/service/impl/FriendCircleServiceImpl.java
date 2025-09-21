package com.dhu.ycl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.mapper.FriendCircleLikedMapper;
import com.dhu.ycl.mapper.FriendCircleMapper;
import com.dhu.ycl.pojo.FriendCircle;
import com.dhu.ycl.pojo.FriendCircleLiked;
import com.dhu.ycl.pojo.Users;
import com.dhu.ycl.pojo.bo.FriendCircleBO;
import com.dhu.ycl.pojo.vo.FriendCircleVO;
import com.dhu.ycl.service.FriendCircleService;
import com.dhu.ycl.service.UserService;
import com.dhu.ycl.utils.PagedGridResult;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FriendCircleServiceImpl extends BaseInfoProperties implements FriendCircleService {
    @Resource
    private FriendCircleMapper friendCircleMapper;

    @Resource
    private FriendCircleLikedMapper circleLikedMapper;

    @Resource
    private UserService userService;

    // 发布朋友圈图文数据，保存到数据库。图片和视频上传都是在file模块中完成。
    @Override
    public void publish(FriendCircleBO friendCircleBO) {
        FriendCircle pendingFriendCircle = new FriendCircle();
        BeanUtils.copyProperties(friendCircleBO, pendingFriendCircle);
        friendCircleMapper.insert(pendingFriendCircle);
    }

    // 分页查询朋友圈图文列表
    @Override
    public PagedGridResult queryList(String userId, Integer page, Integer pageSize) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        // 设置分页参数
        Page<FriendCircleVO> pageInfo = new Page<>(page, pageSize);
        friendCircleMapper.queryFriendCircleList(pageInfo, map);
        return setterPagedGridPlus(pageInfo);
    }

    // 点赞朋友圈
    @Override
    public void like(String friendCircleId, String userId) {
        // 根据朋友圈主键ID查询-发布人；根据用户主键ID查询-点赞人
        FriendCircle friendCircle = friendCircleMapper.selectById(friendCircleId);
        Users users = userService.getById(userId);
        // 1、创建点赞关系并插入数据库
        FriendCircleLiked circleLiked = new FriendCircleLiked();
        circleLiked.setFriendCircleId(friendCircleId);
        circleLiked.setBelongUserId(friendCircle.getUserId());
        circleLiked.setLikedUserId(userId);
        circleLiked.setLikedUserName(users.getNickname());
        circleLiked.setCreatedTime(LocalDateTime.now());
        circleLikedMapper.insert(circleLiked);
        // 2、点赞过后，朋友圈的对应点赞数累加1: friend_circle_liked_counts:id。也可以通过数据库count来实现
        redis.increment(REDIS_FRIEND_CIRCLE_LIKED_COUNTS + ":" + friendCircleId, 1);
        // 标记哪个用户点赞过该朋友圈: does_user_like_friend_circle:id:userId
        redis.setnx(REDIS_DOES_USER_LIKE_FRIEND_CIRCLE + ":" + friendCircleId + ":" + userId, userId);
    }

    // 取消点赞朋友圈
    @Override
    public void unlike(String friendCircleId, String userId) {
        // 1、从数据库中删除点赞关系
        QueryWrapper<FriendCircleLiked> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("friend_circle_id", friendCircleId).eq("liked_user_id", userId);
        circleLikedMapper.delete(deleteWrapper);
        // 2、取消点赞过后，朋友圈的对应点赞数累减1，并删除标记的那个用户点赞过的朋友圈。对应 like 方法
        redis.decrement(REDIS_FRIEND_CIRCLE_LIKED_COUNTS + ":" + friendCircleId, 1);
        redis.del(REDIS_DOES_USER_LIKE_FRIEND_CIRCLE + ":" + friendCircleId + ":" + userId);
    }

    // 查询朋友圈的点赞列表
    @Override
    public List<FriendCircleLiked> queryLikedFriends(String friendCircleId) {
        QueryWrapper<FriendCircleLiked> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("friend_circle_id", friendCircleId);
        return circleLikedMapper.selectList(queryWrapper);
    }

    // 判断当前用户是否点赞过朋友圈
    @Override
    public boolean doILike(String friendCircleId, String userId) {
        String isExist = redis.get(REDIS_DOES_USER_LIKE_FRIEND_CIRCLE + ":" + friendCircleId + ":" + userId);
        return StringUtils.isNotBlank(isExist);
    }

    // 删除朋友圈文案
    @Override
    public void delete(String friendCircleId, String userId) {
        QueryWrapper<FriendCircle> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("id", friendCircleId).eq("user_id", userId);
        friendCircleMapper.delete(deleteWrapper);
    }
}
