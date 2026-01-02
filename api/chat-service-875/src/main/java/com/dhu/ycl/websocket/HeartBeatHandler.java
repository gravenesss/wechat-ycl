package com.dhu.ycl.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

// 心跳助手类：读空闲/写空闲/读写空闲后的触发操作。
@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断evt是否是IdleStateEvent(空闲事件状态，包含 读空闲/写空闲/读写空闲)
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                // log.info("进入读空闲...");
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // log.info("进入写空闲..."); // ctx.writeAndFlush(new PingWebSocketFrame()); // 发送心跳包维持连接
            } else if (event.state() == IdleState.ALL_IDLE) {
                // log.info("channel 关闭前，clients的数量为：{}", ChatHandler.clients.size());
                Channel channel = ctx.channel();
                channel.close(); // 关闭无用的channel，以防资源浪费
                // log.info("channel 关闭后，clients的数量为：{}", ChatHandler.clients.size());
            }
        }
    }
}
