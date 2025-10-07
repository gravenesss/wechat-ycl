package com.dhu.ycl.mq;

import com.dhu.ycl.pojo.netty.DataContent;
import com.dhu.ycl.utils.JsonUtils;
import com.dhu.ycl.utils.UserChannelSession;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RabbitMQConnectUtils {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConnectUtils.class);
    private final int maxConnection = 20;
    // 开发环境 dev
    private final String host = "127.0.0.1";
    private final int port = 5672;
    private final String username = "admin";
    private final String password = "ycl0823";
    private final String virtualHost = "wechat-ycl";
    public ConnectionFactory factory;
    private final BlockingQueue<Connection> connectionPool = new ArrayBlockingQueue<>(maxConnection);
    private final String EXCHANGE_NAME_DEFAULT = MessagePublisher.EXCHANGE_NETTY;

    // 1、MQ 连接工厂
    public ConnectionFactory getMQFactory() {
        if (factory == null) {
            factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(virtualHost);
        }
        return factory;
    }

    // 2、获取连接：从连接池中获取
    public Connection getConnection() throws Exception {
        getMQFactory(); // 初始化连接工厂
        Connection conn = connectionPool.poll();
        if (conn == null || !conn.isOpen()) {
            return factory.newConnection();
        }
        return conn;
    }

    // 2、释放连接：加入到连接池
    public void returnConnection(Connection connection) {
        if (connection.isOpen() && connectionPool.size() < maxConnection) {
            connectionPool.offer(connection);
        }
    }

    /**
     * 3、监听队列，自动确认消息接收。启动持续监听模式，有新消息时自动触发 handleDelivery。neety的消息自动发送给其他节点，消费者在里面已经定义了。
     *
     * @param exchangeName 交换机名称
     * @param exchangeType 交换机类型
     * @param queueName    队列名称
     * @param routingKey   路由键
     * @throws Exception
     */
    public void listen(String exchangeName, BuiltinExchangeType exchangeType, String queueName, String routingKey) throws Exception {
        Connection connection = getConnection();
        Channel channel = connection.createChannel(); // MQ的channel
        // 声明一个交换机，第一个参数是交换机的名称。 BuiltinExchangeType.FANOUT: 交换机类型为 fanout（发布/订阅模式）。
        // 持久化true: 交换机将在服务器重启后仍然存在。非自动删除false: 交换机不会在没有队列绑定时自动删除。
        // 非内部使用false: 交换机可以被客户端直接使用。参数null: 额外的参数配置，这里使用默认配置。
        channel.exchangeDeclare(exchangeName, exchangeType,
                true, false, false, null);
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, exchangeName, routingKey);// 绑定队列到交换机并指定路由键

        Consumer consumer = new DefaultConsumer(channel) {
            /**
             * 重写 handleDelivery 方法来处理接收到的消息
             * @param consumerTag 消息的标签（标识）
             * @param envelope  信封（一些信息，比如交换机路由等等信息）
             * @param properties 配置信息
             * @param body 收到的消息数据
             * @throws IOException
             */
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String msg = new String(body);
                String exchange = envelope.getExchange();
                log.info("Netty Received message: {}", msg);
                log.info("Netty Received message from exchange: {}", exchange);
                if (EXCHANGE_NAME_DEFAULT.equalsIgnoreCase(exchange)) {
                    // 实现聊天消息的实时广播和多设备同步功能。
                    DataContent dataContent = JsonUtils.jsonToPojo(msg, DataContent.class);
                    String senderId = dataContent.getChatMsg().getSenderId();
                    //广播至集群的其他节点并且发送给用户聊天信息  netty 的 channel
                    String receiverId = dataContent.getChatMsg().getReceiverId();
                    List<io.netty.channel.Channel> receiverChannels = UserChannelSession.getMultiChannels(receiverId);
                    UserChannelSession.sendToTarget(receiverChannels, dataContent);
                    // 广播至集群的其他节点并且同步给自己其他设备聊天信息
                    String currentChannelId = dataContent.getExtend();
                    List<io.netty.channel.Channel> senderChannels = UserChannelSession.getMyOtherChannels(senderId, currentChannelId);
                    UserChannelSession.sendToTarget(senderChannels, dataContent);
                }
            }
        };
        // 将消费者注册到指定队列，autoAck=true: 自动确认消息接收。启动持续监听模式，有新消息时自动触发 handleDelivery
        channel.basicConsume(queueName, true, consumer);
    }


    // 4、发送消息-交换机路由模式：支持多种交换机类型（Fanout、Direct、Topic等），可以实现发布/订阅模式
    public void sendMsg(String message, String exchange, String routingKey) throws Exception {
        Connection connection = getConnection();
        Channel channel = connection.createChannel();       // MQ 通道
        // 生产者 → 指定交换机 → 根据路由键匹配 → 路由到符合条件的队列 → 消费者。  routingKey决定了消息如何从交换机路由到队列。
        channel.basicPublish(exchange, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
        channel.close();
        returnConnection(connection);
    }

    // 发送消息-点对点队列模式：不使用交换机，直接发送消息到指定队列
    public void sendMsg(String message, String queue) throws Exception {
        Connection connection = getConnection();
        Channel channel = connection.createChannel();    // MQ 通道
        // ""表示使用默认交换机（Default Exchange）：生产者 → 默认交换机 → 直接路由到指定队列 → 消费者。只会被一个消费者消费
        channel.basicPublish("", queue, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
        channel.close();
        returnConnection(connection);
    }

    // 获取消息：未使用，因为使用了持续监听模式，有新消息时自动触发 handleDelivery
    public GetResponse basicGet(String queue, boolean autoAck) throws Exception {
        GetResponse getResponse = null;
        Connection connection = getConnection();
        Channel channel = connection.createChannel();     // MQ 通道
        // 主动拉取：消费者主动从指定队列中获取消息，非阻塞操作：如果没有消息，立即返回null，不会阻塞等待
        // autoAck: 是否自动确认消息。true-自动确认，消息被获取后立即从队列中删除。false-手动确认，需要调用 basicAck 才会从队列中删除。
        getResponse = channel.basicGet(queue, autoAck);   // MQ 主动拉取消息
        channel.close();
        returnConnection(connection);
        return getResponse;
    }

}
