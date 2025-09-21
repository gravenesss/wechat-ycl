package com.dhu.ycl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.enums.YesOrNo;
import com.dhu.ycl.mapper.FriendshipMapper;
import com.dhu.ycl.pojo.Friendship;
import com.dhu.ycl.pojo.vo.ContactsVO;
import com.dhu.ycl.service.FriendshipService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FriendshipServiceImpl extends BaseInfoProperties implements FriendshipService {
    @Resource
    private FriendshipMapper friendshipMapper;

    @Override
    public Friendship getFriendship(String myId, String friendId) {
        QueryWrapper<Friendship> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("my_id", myId).eq("friend_id", friendId);
        return friendshipMapper.selectOne(queryWrapper);
    }

    @Override
    public List<ContactsVO> queryMyFriends(String myId, boolean needBlack) {
        Map<String, Object> map = new HashMap<>();
        map.put("myId", myId);
        map.put("needBlack", needBlack);
        // 查询好友列表：拉黑/未拉黑
        return friendshipMapper.queryMyFriends(map);
    }

    @Override
    public void updateFriendRemark(String myId, String friendId, String friendRemark) {
        QueryWrapper<Friendship> updateWrapper = new QueryWrapper<>();
        updateWrapper.eq("my_id", myId).eq("friend_id", friendId);
        // 修改备注
        Friendship friendship = new Friendship();
        friendship.setFriendRemark(friendRemark);
        friendship.setUpdatedTime(LocalDateTime.now());
        friendshipMapper.update(friendship, updateWrapper);
    }

    @Override
    public void updateBlackList(String myId, String friendId, YesOrNo yesOrNo) {
        QueryWrapper<Friendship> updateWrapper = new QueryWrapper<>();
        updateWrapper.eq("my_id", myId).eq("friend_id", friendId);
        // 修改黑名单状态
        Friendship friendship = new Friendship();
        friendship.setIsBlack(yesOrNo.type);
        friendship.setUpdatedTime(LocalDateTime.now());
        friendshipMapper.update(friendship, updateWrapper);
    }

    @Transactional
    @Override
    public void delete(String myId, String friendId) {
        // 删除 A 对 B 的好友关系
        QueryWrapper<Friendship> deleteWrapper1 = new QueryWrapper<>();
        deleteWrapper1.eq("my_id", myId).eq("friend_id", friendId);
        friendshipMapper.delete(deleteWrapper1);
        // 删除 B 对 A 的好友关系
        QueryWrapper<Friendship> deleteWrapper2 = new QueryWrapper<>();
        deleteWrapper2.eq("my_id", friendId).eq("friend_id", myId);
        friendshipMapper.delete(deleteWrapper2);
    }

    @Override
    public boolean isBlackEachOther(String friendId1st, String friendId2nd) {
        // 判断 A 是否对 B 拉黑
        QueryWrapper<Friendship> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("my_id", friendId1st)
                .eq("friend_id", friendId2nd)
                .eq("is_black", YesOrNo.YES.type);
        Friendship friendship1st = friendshipMapper.selectOne(queryWrapper1);
        // 判断 B 是否对 A 拉黑
        QueryWrapper<Friendship> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("my_id", friendId2nd)
                .eq("friend_id", friendId1st)
                .eq("is_black", YesOrNo.YES.type);
        Friendship friendship2nd = friendshipMapper.selectOne(queryWrapper2);
        return friendship1st != null || friendship2nd != null;
    }
}
