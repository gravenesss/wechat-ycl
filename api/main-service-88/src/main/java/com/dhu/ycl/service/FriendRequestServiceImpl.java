package com.dhu.ycl.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.enums.FriendRequestVerifyStatus;
import com.dhu.ycl.enums.YesOrNo;
import com.dhu.ycl.mapper.FriendRequestMapper;
import com.dhu.ycl.mapper.FriendshipMapper;
import com.dhu.ycl.pojo.FriendRequest;
import com.dhu.ycl.pojo.Friendship;
import com.dhu.ycl.pojo.bo.NewFriendRequestBO;
import com.dhu.ycl.pojo.vo.NewFriendsVO;
import com.dhu.ycl.utils.PagedGridResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class FriendRequestServiceImpl extends BaseInfoProperties implements FriendRequestService {
    @Resource
    private FriendRequestMapper friendRequestMapper;

    @Resource
    private FriendshipMapper friendshipMapper;

    // 查询新朋友的请求记录列表：MyBatis-Plus 的分页插件会拦截你的 SQL 查询
    @Override
    public PagedGridResult queryNewFriendList(String userId, Integer page, Integer pageSize) {
        Map<String, Object> map = new HashMap<>();
        map.put("mySelfId", userId);
        Page<NewFriendsVO> pageInfo = new Page<>(page, pageSize);
        friendRequestMapper.queryNewFriendList(pageInfo, map);
        return setterPagedGridPlus(pageInfo); // BaseInfoProperties 封装
    }

    // 新增添加好友的请求
    @Transactional
    @Override
    public void addNewRequest(NewFriendRequestBO friendRequestBO) {
        // 先删除以前的记录：可能不是第一次添加，删除后又进行了添加
        QueryWrapper deleteWrapper = new QueryWrapper<FriendRequest>()
                .eq("my_id", friendRequestBO.getMyId())
                .eq("friend_id", friendRequestBO.getFriendId());
        int deleteCount = friendRequestMapper.delete(deleteWrapper);
        log.info("添加好友时先删除了之前的申请记录：{}-{}:{}", friendRequestBO.getMyId(), friendRequestBO.getFriendId(), deleteCount);
        // 再新增记录
        FriendRequest pendingFriendRequest = new FriendRequest();
        BeanUtils.copyProperties(friendRequestBO, pendingFriendRequest);
        pendingFriendRequest.setVerifyStatus(FriendRequestVerifyStatus.WAIT.type);
        pendingFriendRequest.setRequestTime(LocalDateTime.now());
        friendRequestMapper.insert(pendingFriendRequest);
    }

    // 通过好友请求：就是请求的id，不是发送方或接收方的用户id。
    @Transactional
    @Override
    public void passNewFriend(String friendRequestId, String friendRemark) {
        // 1、FriendShip表
        FriendRequest friendRequest = getSingle(friendRequestId);
        String mySelfId = friendRequest.getFriendId();  // 接受方：通过方的用户id
        String myFriendId = friendRequest.getMyId();    // 请求方：被通过方的用户id
        // 创建双方的好友关系：自己的角度
        LocalDateTime nowTime = LocalDateTime.now();
        Friendship friendshipSelf = new Friendship();
        friendshipSelf.setMyId(mySelfId);
        friendshipSelf.setFriendId(myFriendId);
        friendshipSelf.setFriendRemark(friendRemark);   // 自己给对方的备注-传入的方式指定
        friendshipSelf.setIsBlack(YesOrNo.NO.type);
        friendshipSelf.setIsMsgIgnore(YesOrNo.NO.type);
        friendshipSelf.setCreatedTime(nowTime);
        friendshipSelf.setUpdatedTime(nowTime);
        // 创建对方好友关系：对方的角度
        Friendship friendshipOpposite = new Friendship();
        friendshipOpposite.setMyId(myFriendId);
        friendshipOpposite.setFriendId(mySelfId);
        friendshipOpposite.setFriendRemark(friendRequest.getFriendRemark());// 对方给自己的备注-来自对方-已经记录到了请求表
        friendshipOpposite.setIsBlack(YesOrNo.NO.type);
        friendshipOpposite.setIsMsgIgnore(YesOrNo.NO.type);
        friendshipOpposite.setCreatedTime(nowTime);
        friendshipOpposite.setUpdatedTime(nowTime);
        // 好友关系表中插入两条关系记录
        friendshipMapper.insert(friendshipSelf);
        friendshipMapper.insert(friendshipOpposite);

        // 2、FriendRequest表
        // 情况1：A通过B的请求之后，需要通过请求id设置为“通过”
        friendRequest.setVerifyStatus(FriendRequestVerifyStatus.SUCCESS.type);
        friendRequestMapper.updateById(friendRequest);
        // 情况2：A添加B，B没有通过则过期。但是过期后，B向A发起好友请求，所以B被A通过后，那么之前的过期请求也需要设置为“通过”
        QueryWrapper updateWrapper = new QueryWrapper<FriendRequest>()
                .eq("my_id", myFriendId)
                .eq("friend_id", mySelfId);
        FriendRequest requestOpposite = new FriendRequest();
        requestOpposite.setVerifyStatus(FriendRequestVerifyStatus.SUCCESS.type);
        friendRequestMapper.update(requestOpposite, updateWrapper);
    }

    // 根据请求 id 获取指定好友请求
    private FriendRequest getSingle(String friendRequestId) {
        return friendRequestMapper.selectById(friendRequestId);
    }
}
