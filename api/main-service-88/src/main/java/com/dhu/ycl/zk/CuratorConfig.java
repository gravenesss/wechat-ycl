package com.dhu.ycl.zk;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.pojo.netty.NettyServerNode;
import com.dhu.ycl.utils.JsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "zookeeper.curator")
public class CuratorConfig extends BaseInfoProperties {
    private String host;                    // 单机/集群的ip:port地址
    private Integer connectionTimeoutMs;    // 连接超时时间
    private Integer sessionTimeoutMs;       // 会话超时时间
    private Integer sleepMsBetweenRetry;    // 每次重试的间隔时间
    private Integer maxRetries;             // 最大重试次数
    private Integer maxSleepMs;             // 最大重试间隔时间
    private String namespace;               // 命名空间（root根节点名称）
    public static final String PATH = "/server-list"; // 与 api-chat中的一直，这里相当于是客户端。
    public static final String NETTY_PORT_KEY = "netty_port";
    public static final String QUEUE_NETTY = "queue_netty_"; // + nettyPort

    @Bean("curatorClient")
    public CuratorFramework curatorClient() {
        RetryPolicy backoffRetry = new ExponentialBackoffRetry(sleepMsBetweenRetry, maxRetries, maxSleepMs);
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(host)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .retryPolicy(backoffRetry)
                .namespace(namespace)
                .build();
        client.start();     // 启动curator客户端
        this.add(PATH, client);  // 注册节点的事件监听
        return client;
    }

    @Autowired
    private RedisTemplate redisTemplate;  // Spring 对 @Autowired 的处理更灵活，尤其是泛型 Bean。
    // 虽然引入了 spring-boot-starter-amqp，但 Spring Boot 并不会默认创建 RabbitAdmin，
    // 除非使用了某些特定功能（比如自动声明队列、交换机等）。如果你只是配置了 RabbitMQ 而没有用到这些功能，Spring Boot 会跳过创建 RabbitAdmin。
    @Autowired
    private RabbitAdmin rabbitAdmin;

    // 注册节点的事件监听
    public void add(String path, CuratorFramework client) {
        CuratorCache curatorCache = CuratorCache.build(client, path);
        // type: 当前监听到的事件类型。oldData: 节点更新前的数据和状态。 data: 节点更新后的数据和状态
        curatorCache.listenable().addListener((type, oldData, data) -> {
            log.info("节点事件类型：{}，节点路径：{}，节点数据：{}", type.name(), data.getPath(), data.getData());
            switch (type.name()) {
                case "NODE_CREATED":
                    log.info("(子)节点创建");
                    break;
                case "NODE_CHANGED":
                    log.info("(子)节点数据变更");
                    break;
                case "NODE_DELETED":
                    log.info("(子)节点删除");
                    NettyServerNode oldNode = JsonUtils.jsonToPojo(new String(oldData.getData()), NettyServerNode.class);
                    log.info("old path: {}, old value: {}", oldData.getPath(), oldNode);
                    String oldPort = oldNode.getPort() + "";
                    String queueName = QUEUE_NETTY + oldPort;
                    redis.hdel(NETTY_PORT_KEY, oldPort);
                    rabbitAdmin.deleteQueue(queueName);
                    break;
                default:
                    log.info("default");
                    break;
            }
        });
        curatorCache.start();
    }

}
