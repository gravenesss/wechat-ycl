package com.dhu.ycl.mq;

import com.dhu.ycl.pojo.netty.ChatMsg;
import com.dhu.ycl.service.ChatMessageService;
import com.dhu.ycl.utils.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


// 收到消息后判断路由键，并保存消息。 QUEUE_MSG \ ROUTING_KEY_MSG
@Slf4j
@Component
public class RabbitMQMsgConsumer {
    @Resource
    private ChatMessageService chatMessageService;

    @RabbitListener(queues = {RabbitMQMsgConfig.QUEUE_MSG})
    public void watchQueue(String payload, Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("RabbitMQMsgConsumer_watchQueue_routingKey = {}", routingKey);
        if (RabbitMQMsgConfig.ROUTING_KEY_MSG.equals(routingKey)) {
            ChatMsg chatMsg = JsonUtils.jsonToPojo(payload, ChatMsg.class);
            chatMessageService.saveMsg(chatMsg);
        }
    }

}