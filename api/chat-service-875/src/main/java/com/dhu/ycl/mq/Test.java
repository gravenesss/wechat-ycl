package com.dhu.ycl.mq;

import com.dhu.ycl.pojo.netty.ChatMsg;
import com.dhu.ycl.utils.JsonUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Test {
    // 定义交换机的名字
    public static final String TEST_EXCHANGE = "test_exchange";
    // 定义保存消息的路由键
    public static final String ROUTING_KEY = "save.message";
    // 定义队列名称
    public static final String QUEUE_NAME = "message_queue";

    public static void main(String[] args) throws Exception {
        // 测试发送消息
        ChatMsg chatMsg = new ChatMsg();
        chatMsg.setMsgId("1001");
        chatMsg.setMsg("Hello RabbitMQ!");
        String pendingMsg = JsonUtils.objectToJson(chatMsg);
        sendMsg(pendingMsg);
        System.out.println("消息发送成功: " + pendingMsg);
    }

    // 通过MQ发送消息到队列
    public static void sendMsg(String msg) throws Exception {
        // 1、创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        // 1.1 设置连接参数
        factory.setHost("127.0.0.1");
        factory.setPort(5672);  // 修正端口号，与RabbitMQConnectUtils保持一致
        factory.setVirtualHost("wechat-ycl");
        factory.setUsername("admin");
        factory.setPassword("ycl0823");
        // 1.2 建立连接\创建通道channel
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            try {
                // 3、声明交换机、定义队列
                channel.exchangeDeclare(TEST_EXCHANGE, "direct", true);
                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                // 4、绑定队列到交换机，并发送消息
                channel.queueBind(QUEUE_NAME, TEST_EXCHANGE, ROUTING_KEY);
                channel.basicPublish(TEST_EXCHANGE, ROUTING_KEY, null, msg.getBytes("UTF-8"));
                System.out.println("消息已发送到队列: " + msg);
            } finally {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
                if (connection.isOpen()) {
                    connection.close();
                }
            }
        }
    }
}
