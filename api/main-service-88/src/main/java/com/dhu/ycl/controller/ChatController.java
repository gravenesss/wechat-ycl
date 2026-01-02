package com.dhu.ycl.controller;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.pojo.netty.NettyServerNode;
import com.dhu.ycl.service.ChatMessageService;
import com.dhu.ycl.utils.JsonUtils;
import com.dhu.ycl.utils.PagedGridResult;
import com.dhu.ycl.zk.CuratorConfig;
import jakarta.annotation.Resource;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;
import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController extends BaseInfoProperties {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
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

    @PostMapping("/getNettyOnlineInfo1")
    public GraceJSONResult getNettyOnlineInfo1() throws UnknownHostException {
        NettyServerNode minNode = new NettyServerNode();
        minNode.setIp("127.0.0.1");
        minNode.setPort(875);
        minNode.setOnlineCounts(0);
        return GraceJSONResult.ok(minNode);
    }

    @Resource(name = "curatorClient")
    private CuratorFramework zkClient;

    @RequestMapping("/getNettyOnlineInfo")
    public GraceJSONResult getNettyOnlineInfo() throws Exception {
        // 1、从zookeeper中获得当前已经注册的netty服务列表
        String path = CuratorConfig.PATH;
        List<String> registerList = zkClient.getChildren().forPath(path);
        List<NettyServerNode> serverNodeList = new ArrayList<>();
        for (String node : registerList) {
            String nodeValue = new String(zkClient.getData().forPath(path + "/" + node));
            NettyServerNode serverNode = JsonUtils.jsonToPojo(nodeValue, NettyServerNode.class);
            serverNodeList.add(serverNode);
        }
        // 计算当前哪个zk的node是最少连接，获得[ip:port]并且返回给前端
        Optional<NettyServerNode> minNodeOptional = serverNodeList.stream()
                .min(Comparator.comparing(nettyServerNode -> nettyServerNode.getOnlineCounts()));
        NettyServerNode minNode = minNodeOptional.get();
        log.info("minNode: {}", JsonUtils.objectToJson(minNode));
        return GraceJSONResult.ok(minNode);
    }

}
