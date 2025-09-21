package com.dhu.ycl.service;

import com.dhu.ycl.enums.YesOrNo;
import com.dhu.ycl.pojo.Friendship;
import com.dhu.ycl.pojo.vo.ContactsVO;

import java.util.List;


public interface FriendshipService {
    // 获得我和B的朋友关系
    Friendship getFriendship(String myId, String friendId);

    // 查询我的好友列表(通讯录)：needBlack=true表示查询黑名单
    List<ContactsVO> queryMyFriends(String myId, boolean needBlack);

    // 修改我的好友的备注名
    void updateFriendRemark(String myId, String friendId, String friendRemark);

    // 拉黑或者恢复好友
    void updateBlackList(String myId, String friendId, YesOrNo yesOrNo);

    // 删除好友(删除好友之间的两个记录)
    void delete(String myId, String friendId);

    // 判断两个朋友之前的关系是否拉黑
    boolean isBlackEachOther(String friendId1st, String friendId2nd);
}
