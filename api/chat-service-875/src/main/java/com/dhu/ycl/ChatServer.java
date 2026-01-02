package com.dhu.ycl;

import com.dhu.ycl.mq.MessagePublisher;
import com.dhu.ycl.utils.JedisPoolUtils;
import com.dhu.ycl.websocket.WSServerInitializer;
import com.dhu.ycl.zk.ZookeeperRegister;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ChatServer {
    public static final Integer NETTY_DEFAULT_PORT = 875;
    public static final Integer PORT_ADD = 10;
    public static final String NETTY_PORT_KEY = "netty_port";
    public static final String INIT_ONLINE_COUNTS = "0";

    // 核心：①创建主从线程组，分别进行接受连接和处理任务，并需要保持运行状态。
    // ②zk监听服务端节点，并保存服务端节点信息到zk中；保证服务端节点信息的实时性。③mq监听队列，根据队列中的消息进行处理。
    public static void main(String[] args) throws Exception {
        // 定义主从线程组：主线程组用于接受客户端的连接，但是不做任何处理。从属线程组处理主线程池交过来的任务。
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // 此时先使用固定的端口，com.dhu.ycl.controller.ChatController.getNettyOnlineInfo 才能匹配
        Integer nettyPort = selectPort(NETTY_DEFAULT_PORT); // 875;  ip 和 端口都是动态设置的，然后再 main 模块中动态获取使用。
        // 注册当前netty服务到zookeeper中
        ZookeeperRegister.registerNettyServer(ZookeeperRegister.DEFAULT_NODE_NAME, ZookeeperRegister.getLocalIp(), nettyPort);
        MessagePublisher.listen(nettyPort);  // 启动消费者进行监听，队列可以根据动态生成的端口号进行拼接

        try {
            // 构建Netty服务器，设置线程组+通道+处理器
            ServerBootstrap server = new ServerBootstrap();     // 服务的启动类
            server.group(bossGroup, workerGroup)                // 把主从线程池组放入到启动类中
                    .channel(NioServerSocketChannel.class)      // 设置Nio的双向通道
                    .childHandler(new WSServerInitializer());   // == 自定义处理器，用于处理 workerGroup ==
            // 启动server并绑定端口号875，加 sync 保证启动Netty服务器并保持其运行状态。  http://127.0.0.1:875
            ChannelFuture channelFuture = server.bind(nettyPort).sync();  // ChannelFuture 对象，代表异步的I/O操作结果
            // 获取实际服务器Channel、监听关闭-服务器Channel关闭时完成的Future、sync()阻塞主线程，直到服务器Channel被关闭
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            removePort(nettyPort);
        }
    }


    /**
     * 从 redis 中获取可用的端口号：redis 中为空则返回默认端口；否则取已有端口的最大值加10作为端口号
     *
     * @param port 默认端口
     * @return 可用的端口号
     */
    public static Integer selectPort(Integer port) {
        // 1、从redis中获取所有的端口号
        try (Jedis jedis = JedisPoolUtils.getJedis()) {
            Map<String, String> portMap = jedis.hgetAll(NETTY_PORT_KEY);
            log.info("selectPort_begin_portMap: {}", portMap);
            List<Integer> portList = portMap.entrySet().stream()
                    .map(entry -> Integer.valueOf(entry.getKey())).collect(Collectors.toList());
            log.info("selectPort_get_portList: {}", portList);
            // 2、如果portList为空，则在redis中添加端口号与初始化在线人数的关系。否则，则从portList中获取最大的端口号并加10作为端口号。
            Integer nettyPort;
            if (CollectionUtils.isEmpty(portList)) {
                nettyPort = port;
                jedis.hset(NETTY_PORT_KEY, nettyPort + "", INIT_ONLINE_COUNTS);
            } else {
                Optional<Integer> maxInteger = portList.stream().max(Integer::compareTo);
                nettyPort = maxInteger.get() + PORT_ADD;
                jedis.hset(NETTY_PORT_KEY, nettyPort + "", INIT_ONLINE_COUNTS);
            }
            return nettyPort;
        }
    }

    public static void removePort(Integer port) {
        try (Jedis jedis = JedisPoolUtils.getJedis()) {
            jedis.hdel(NETTY_PORT_KEY, port + "");
        }
    }
}
