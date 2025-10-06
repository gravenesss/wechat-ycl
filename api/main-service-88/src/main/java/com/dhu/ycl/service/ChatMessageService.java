package com.dhu.ycl.service;


import com.dhu.ycl.pojo.netty.ChatMsg;
import com.dhu.ycl.utils.PagedGridResult;

public interface ChatMessageService {
    // 保存聊天信息
    void saveMsg(ChatMsg chatMsg);

    // 查询聊天信息列表
    PagedGridResult queryChatMsgList(String senderId, String receiverId, Integer page, Integer pageSize);

    // 标记语音聊天信息的签收已读
    void updateMsgSignRead(String msgId);
}
