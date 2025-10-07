package com.dhu.ycl.mq;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQMsgConfig {
    // 定义交换机的名字：
    public static final String EXCHANGE_MSG = "exchange_msg";
    // 发送信息到消息队列接受并且保存到数据库的路由地址
    public static final String ROUTING_KEY_MSG = "wechat.msg.send";
    // 测试的路由地址
    public static final String ROUTING_KEY_TEST = "wechat.msg.test";
    // 定义队列的名字
    public static final String QUEUE_MSG = "queue_msg";

    // 创建交换机：主题交换机(topic exchange)；设置交换机为持久化状态，服务器重启后仍存在；
    @Bean(EXCHANGE_MSG)
    public Exchange exchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_MSG).durable(true).build();
    }

    // 创建队列：持久化状态，服务器重启后仍存在；
    @Bean(QUEUE_MSG)
    public Queue queue() {
        return QueueBuilder.durable(QUEUE_MSG).build();
    }

    // 定义队列绑定到交换机的关系:com.dhu.ycl.mq.RabbitMQConnectUtils.listen是一步一步创建绑定的;
    // 这里通过 Bean 的方式绑定。通过 @Qualifier 注解注入之前创建的交换机和队列Bean
    @Bean
    public Binding binding(@Qualifier(EXCHANGE_MSG) Exchange exchange, @Qualifier(QUEUE_MSG) Queue queue) {
        // 使用路由模式 "wechat.msg.#"，匹配所有以 wechat.msg. 开头的路由键
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with("wechat.msg.#")
                .noargs();  // 执行绑定关系
    }

}
