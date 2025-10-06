package com.dhu.ycl.utils;

import com.dhu.ycl.pojo.netty.DataContent;
import com.dhu.ycl.websocket.ChatHandler;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;

// 会话管理：使用两个 map 管理 userId-channel一对多、channelId-userId一对一的关系。 仅此。
public class UserChannelSession {
    private static final Logger log = LoggerFactory.getLogger(UserChannelSession.class);
    // 用于多端同时接受消息，允许同一个账号在多个设备同时在线，比如iPad、iPhone、Mac等设备同时收到消息 key: userId, value: 多个用户的channel
    private static Map<String, List<Channel>> multiSession = new HashMap<>();
    private static Map<String, String> userChannelIdRelation = new HashMap<>();

    // 添加 userId 和 channelId 的1对n的关联关系
    public static void putMultiChannels(String userId, Channel channel) {
        List<Channel> channels = getMultiChannels(userId);
        if (CollectionUtils.isEmpty(channels)) {
            channels = new ArrayList<>();
        }
        channels.add(channel);
        multiSession.put(userId, channels);
    }

    // 添加 channelId 和 userId 的1对1的关联关系
    public static void putUserChannelIdRelation(String channelId, String userId) {
        userChannelIdRelation.put(channelId, userId);
    }

    // 通过 userId 获取 channelId 列表
    public static List<Channel> getMultiChannels(String userId) {
        return multiSession.get(userId);
    }

    // 获取当前用户除了当前设备以外的所有设备 !channelId
    public static List<Channel> getMyOtherChannels(String userId, String channelId) {
        List<Channel> channels = getMultiChannels(userId);
        if (CollectionUtils.isEmpty(channels) || StringUtils.isBlank(channelId)) {
            return null;
        }
        List<Channel> myOtherChannels = new ArrayList<>();
        for (Channel tempChannel : channels) {
            if (!tempChannel.id().asLongText().equals(channelId)) {
                myOtherChannels.add(tempChannel);
            }
        }
        return myOtherChannels;
    }

    // 通过 channelId 获取 userId
    public static String getUserIdByChannelId(String channelId) {
        return userChannelIdRelation.get(channelId);
    }

    // 输出 multiSession 中的所有关联关系：一个id对应多个channel
    public static void printLogMulti() {
        log.debug("++++++++++++++++++++++++++++++++++++");
        for (Map.Entry<String, List<Channel>> entry : multiSession.entrySet()) {
            log.debug("UserId: {}", entry.getKey());
            for (Channel c : entry.getValue()) {
                log.debug("\t\t ChannelId: {}", c.id().asLongText());
            }
            log.debug("----------*----------");
        }
        log.debug("++++++++++++++++++++++++++++++++++++");
    }

    // 删除 multiSession 中的无用的channel：当用户关闭了一个设备的连接，需要从 multiSession 中删除对应的 channel
    public static void removeUselessChannels(String userId, String channelId) {
        List<Channel> channels = getMultiChannels(userId);
        if (CollectionUtils.isEmpty(channels) || StringUtils.isBlank(channelId)) {
            return;
        }
        // channels.removeIf(tempChannel -> tempChannel.id().asLongText().equals(channelId));
        Iterator<Channel> iterator = channels.iterator();
        while (iterator.hasNext()) {
            Channel tempChannel = iterator.next();
            if (tempChannel.id().asLongText().equals(channelId)) {
                iterator.remove();
            }
        }
        multiSession.put(userId, channels);
    }

    // 发送消息到指定的 channel 列表-receiverChannels
    public static void sendToTarget(List<Channel> receiverChannels, DataContent dataContent) {
        ChannelGroup clients = ChatHandler.clients;
        if (CollectionUtils.isEmpty(receiverChannels) || dataContent == null) {
            return;
        }
        for (Channel c : receiverChannels) {
            Channel findChannel = clients.find(c.id());
            if (findChannel != null) {
                findChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));
            }
        }
    }
}
