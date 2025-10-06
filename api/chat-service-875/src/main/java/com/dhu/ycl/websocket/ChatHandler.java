package com.dhu.ycl.websocket;


import com.a3test.component.idworker.IdWorkerConfigBean;
import com.a3test.component.idworker.Snowflake;
import com.dhu.ycl.enums.MsgTypeEnum;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.pojo.netty.ChatMsg;
import com.dhu.ycl.pojo.netty.DataContent;
import com.dhu.ycl.pojo.netty.NettyServerNode;
import com.dhu.ycl.utils.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.time.LocalDateTime;


/**
 * 创建自定义助手类
 * SimpleChannelInboundHandler: 对于请求来说，相当于入站(入境)
 * TextWebSocketFrame: 用于为websocket专门处理的文本数据对象，Frame是数据(消息)的载体
 */
@Slf4j
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    // 用于记录和管理所有客户端的channel组
    public static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static final String IS_BLACK_URK = "http://127.0.0.1:1000/friendship/isBlack";

    // 当客户端通过WebSocket连接向服务器发送文本消息时触发(发送消息)，当消息是 TextWebSocketFrame 类型时才会触发
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 1、获取客户端发来的消息并且解析
        String content = msg.text();
        log.debug("接收到的数据：{}", content);
        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        if (dataContent == null) {
            log.debug("数据解析失败，dataContent 为 null，原始数据为：{}", content);
            return;
        }
        ChatMsg chatMsg = dataContent.getChatMsg();
        String receiverId = chatMsg.getReceiverId();
        String senderId = chatMsg.getSenderId();
        GraceJSONResult isBlackResult = OkHttpUtil.get(IS_BLACK_URK + "?friendId1st=" + receiverId + "&friendId2nd=" + senderId);
        boolean isBlank = (Boolean) isBlackResult.getData();
        if (isBlank) {
            log.debug("用户 {} 被 {} 加入了黑名单", receiverId, senderId);
            return;
        }
        chatMsg.setChatTime(LocalDateTime.now());  // 时间校准，以服务器的时间为准

        // 2、获取channel，并根据不同的消息类型来处理不同的业务
        Channel currentChannel = ctx.channel();
        String currentChannelId = currentChannel.id().asLongText();
        Integer msgType = chatMsg.getMsgType();
        if (msgType == MsgTypeEnum.CONNECT_INIT.type) {
            // 当websocket初次open的时候，初始化channel、把channel和用户userid关联起来
            UserChannelSession.putMultiChannels(senderId, currentChannel);
            UserChannelSession.putUserChannelIdRelation(currentChannelId, senderId);
            NettyServerNode minNode = dataContent.getServerNode();
            // 初次连接后，该节点下的在线人数累加
            // ZookeeperRegister.incrementOnlineCounts(minNode); // TODO zk
            // 获得ip+端口，在redis中设置关系，以便在前端设备断线后减少在线人数
            Jedis jedis = JedisPoolUtils.getJedis();
            jedis.set(senderId, JsonUtils.objectToJson(minNode));
        } else if (msgType == MsgTypeEnum.WORDS.type || msgType == MsgTypeEnum.IMAGE.type
                || msgType == MsgTypeEnum.VIDEO.type || msgType == MsgTypeEnum.VOICE.type
        ) {
            // 此处为 mq异步解耦，保存信息到数据库，数据库无法获得信息的主键id，所以此处可以用snowflake直接生成唯一的主键id
            Snowflake snowflake = new Snowflake(new IdWorkerConfigBean());
            String sid = snowflake.nextId();        // String iid = IdWorker.getIdStr();  mybatisplus.core.toolkit
            chatMsg.setMsgId(sid);
            log.debug("生成唯一主键sid：{}", sid);
            if (msgType == MsgTypeEnum.VOICE.type) {
                chatMsg.setIsRead(false);
            }
            dataContent.setChatMsg(chatMsg);
            String chatTimeFormat = LocalDateUtils.format(chatMsg.getChatTime(), LocalDateUtils.DATETIME_PATTERN_2);
            dataContent.setChatTime(chatTimeFormat);
            // 先把聊天信息发送给其他netty服务器，再把聊天信息发送给mq  TODO：mq 异步解耦
            // MessagePublisher.sendMsgToOtherNettyServer(JsonUtils.objectToJson(dataContent));
            // MessagePublisher.sendMsgToSave(chatMsg);
        }
        UserChannelSession.printLogMulti();
    }

    // 客户端连接到服务端之后(打开链接)
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        Channel currentChannel = ctx.channel();
        String currentChannelId = currentChannel.id().asLongText();
        log.info("客户端建立连接，channel对应的短id为：{}", currentChannelId);
        clients.add(currentChannel);  // 获得客户端的channel，并且存入到ChannelGroup中进行管理(作为一个客户端群组)
    }

    // 关闭连接触发时：移除channel
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        Channel currentChannel = ctx.channel();
        String currentChannelId = currentChannel.id().asLongText();
        log.info("客户端关闭连接，channel对应的短id为：{}", currentChannelId);
        // 移除多余的会话
        String userId = UserChannelSession.getUserIdByChannelId(currentChannelId);
        UserChannelSession.removeUselessChannels(userId, currentChannelId);
        clients.remove(currentChannel);
        // zk中在线人数累减
        // Jedis jedis = JedisPoolUtils.getJedis();
        // NettyServerNode minNode = JsonUtils.jsonToPojo(jedis.get(userId), NettyServerNode.class);
        // ZookeeperRegister.decrementOnlineCounts(minNode);  // TODO zk
    }

    // 捕获异常：移除channel
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel currentChannel = ctx.channel();
        String currentChannelId = currentChannel.id().asLongText();
        log.error("发生异常捕获，channel对应长id为：{}", currentChannelId);
        // 发生异常之后关闭连接(channel),随后从ChannelGroup中移除对应的channel; 并移除多余的会话
        ctx.channel().close();
        clients.remove(currentChannel);
        String userId = UserChannelSession.getUserIdByChannelId(currentChannelId);
        UserChannelSession.removeUselessChannels(userId, currentChannelId);
        // zk中在线人数累减
        // Jedis jedis = JedisPoolUtils.getJedis();
        // NettyServerNode minNode = JsonUtils.jsonToPojo(jedis.get(userId), NettyServerNode.class);
        // ZookeeperRegister.decrementOnlineCounts(minNode);  // TODO zk
    }
}
