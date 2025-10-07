package com.dhu.ycl.mq;


import com.dhu.ycl.pojo.netty.ChatMsg;
import com.dhu.ycl.utils.JsonUtils;
import com.rabbitmq.client.BuiltinExchangeType;

public class MessagePublisher {
    // fanout_exchange-广播消息：多个消费者都收到消息；direct_exchange只有绑定到指定路由键的队列会收到消息；topic_exchange 模糊匹配路由键的队列会收到消息
    public static final String EXCHANGE_NETTY = "exchange_netty";
    public static final BuiltinExchangeType EXCHANGE_NETTY_TYPE = BuiltinExchangeType.FANOUT;
    public static final String QUEUE_NETTY = "queue_netty_"; // + nettyPort
    public static final String ROUTING_KEY_NETTY = "";

    /**
     * 微信消息的交换机和队列--base模块：
     *
     * @see com.dhu.ycl.mq.RabbitMQTestConfig#EXCHANGE_MSG
     */
    public static final String EXCHANGE_MSG = "exchange_msg";
    public static final String ROUTING_KEY_MSG = "wechat.msg.send";// 发送信息到消息队列接受并且保存到数据库的路由地址

    // netty 监听队列: 交换机和队列都是netty相关的。
    public static void listen(Integer nettyPort) throws Exception {
        RabbitMQConnectUtils mqConnectUtils = new RabbitMQConnectUtils();
        mqConnectUtils.listen(EXCHANGE_NETTY, EXCHANGE_NETTY_TYPE, QUEUE_NETTY + nettyPort, ROUTING_KEY_NETTY);
    }

    // 发送消息到其他netty服务器：dataContent已经提前转为json了。 netty的交换机和队列都是netty相关的。
    public static void sendMsgToOtherNettyServer(String msg) throws Exception {
        RabbitMQConnectUtils rabbitMQConnectUtils = new RabbitMQConnectUtils();
        rabbitMQConnectUtils.sendMsg(msg, EXCHANGE_NETTY, ROUTING_KEY_NETTY);
    }

    // 发送并保存消息：msg就是字符串类型并未转换为json。 在main模块中进行保存。
    public static void sendMsgToSave(ChatMsg msg) throws Exception {
        RabbitMQConnectUtils rabbitMQConnectUtils = new RabbitMQConnectUtils();
        rabbitMQConnectUtils.sendMsg(JsonUtils.objectToJson(msg), EXCHANGE_MSG, ROUTING_KEY_MSG);
    }
}
