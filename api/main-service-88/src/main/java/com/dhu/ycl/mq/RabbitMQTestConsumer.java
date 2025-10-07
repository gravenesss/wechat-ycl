package com.dhu.ycl.mq;

import com.dhu.ycl.pojo.netty.ChatMsg;
import com.dhu.ycl.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;


// 这里仅仅是记录日志(未使用@Conponent)，不做业务处理  QUEUE_MSG \ ROUTING_KEY_TEST
@Slf4j
public class RabbitMQTestConsumer {
    @RabbitListener(queues = {RabbitMQMsgConfig.QUEUE_MSG})
    public void watchQueue(String payload, Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("RabbitMQTestConsumer_watchQueue_payload = {}", payload);          // 查看消息内容
        log.info("RabbitMQTestConsumer_watchQueue_routingKey = {}", routingKey);    // 查看路由键
        if (RabbitMQMsgConfig.ROUTING_KEY_TEST.equals(routingKey)) {
            ChatMsg chatMsg = JsonUtils.jsonToPojo(payload, ChatMsg.class);
            log.info("RabbitMQTestConsumer_watchQueue_chatMsg = {}", chatMsg.toString());
        }
    }
}