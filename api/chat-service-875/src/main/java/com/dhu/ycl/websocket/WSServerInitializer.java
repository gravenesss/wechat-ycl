package com.dhu.ycl.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

// 初始化器，channel注册后，会执行里面的相应的初始化方法：→ 自定义1：HeartBeatHandler、自定义2：ChatHandler
public class WSServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // 通过 SocketChannel 获得对应的管道，并通过管道，添加handler处理器
        ChannelPipeline pipeline = socketChannel.pipeline();

        /** ==================== 以下是用于支持http协议相关的handler ====================*/
        /** HttpServerCodec 是由netty提供的助手类，可以理解为管道中的拦截器。到服务端解码+到客户端编码，websocket 基于http协议，所以需要有http的编解码器。
         * ChunkedWriteHandler：添加对大数据流的支持
         * HttpObjectAggregator：对httpMessage进行聚合，聚合成为FullHttpRequest或FullHttpResponse。几乎在netty的编程中，都会使用到此handler
         */
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(1024 * 64));

        /** ==================== 增加心跳支持 start ==================== */
        /** IdleStateHandler：检测客户端空闲连接状态：8秒内没有读操作时触发读空闲/10秒内没有写操作时触发写空闲/300*60秒=5h内没有读写操作时触发读写空闲
         *  自定义1：HeartBeatHandler：自定义处理心跳包，如果读写空闲状态则主动断开连接
         */
        pipeline.addLast(new IdleStateHandler(8, 10, 300 * 60));
        pipeline.addLast(new HeartBeatHandler());

        /** ==================== 支持websocket ==================== */
        /** WebSocketServerProtocolHandler：指定给客户端连接的时候访问路由： /ws
         * 1）WebSocket 握手处理：处理握手请求 + 协议升级为 WebSocket 连接 + 验证握手请求的合法性；
         * 2）协议帧处理：自动处理 WebSocket 控制帧（Close、Ping、Pong）+ 帧的编码和解码；
         * 3）连接管理：管理 WebSocket 协议状态 + 处理连接关闭流程
         * 自定义2：ChatHandler：自定义处理业务逻辑
         */
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
        pipeline.addLast(new ChatHandler());
    }
}
