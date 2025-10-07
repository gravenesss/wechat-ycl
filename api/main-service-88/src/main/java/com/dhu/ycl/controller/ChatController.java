package com.dhu.ycl.controller;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.pojo.netty.NettyServerNode;
import com.dhu.ycl.service.ChatMessageService;
import com.dhu.ycl.utils.PagedGridResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController extends BaseInfoProperties {
    @Resource
    private ChatMessageService chatMessageService;

    // 获取未读消息数量
    @PostMapping("/getMyUnReadCounts")
    public GraceJSONResult getMyUnReadCounts(String myId) {
        Map map = redis.hgetall(CHAT_MSG_LIST + ":" + myId);
        return GraceJSONResult.ok(map);
    }

    // 清空当前好友的未读消息数量
    @PostMapping("/clearMyUnReadCounts")
    public GraceJSONResult clearMyUnReadCounts(String myId, String oppositeId) {
        redis.setHashValue(CHAT_MSG_LIST + ":" + myId, oppositeId, "0");
        return GraceJSONResult.ok();
    }

    @PostMapping("list/{senderId}/{receiverId}")
    public GraceJSONResult list(@PathVariable("senderId") String senderId, @PathVariable("receiverId") String receiverId,
                                Integer page, Integer pageSize) {
        if (page == null) page = 1;
        if (pageSize == null) page = 20;
        PagedGridResult gridResult = chatMessageService.queryChatMsgList(senderId, receiverId, page, pageSize);
        return GraceJSONResult.ok(gridResult);
    }

    @PostMapping("signRead/{msgId}")
    public GraceJSONResult signRead(@PathVariable("msgId") String msgId) {
        chatMessageService.updateMsgSignRead(msgId);
        return GraceJSONResult.ok();
    }

    @PostMapping("/getNettyOnlineInfo")
    public GraceJSONResult getNettyOnlineInfo() throws UnknownHostException {
        NettyServerNode minNode = new NettyServerNode();
        minNode.setIp("127.0.0.1");
        minNode.setPort(875);
        minNode.setOnlineCounts(0);
        return GraceJSONResult.ok(minNode);
    }
}
