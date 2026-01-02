package com.dhu.ycl.zk;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class CuratorConfig {
    private static String host = "127.0.0.1:3191";                 // 单机/集群的ip:port地址
    private static Integer connectionTimeoutMs = 30 * 1000;        // 连接超时时间
    private static Integer sessionTimeoutMs = 30 * 1000;            // 会话超时时间
    private static Integer sleepMsBetweenRetry = 1 * 1000;         // 每次重试的间隔时间
    private static Integer maxRetries = 6;                         // 最大重试次数
    private static Integer maxSleepMs = 10000;                     // 最大重试间隔时间
    private static String namespace = "wechat-im";                 // 命名空间（root根节点名称）

    private static CuratorFramework client;

    static {
        // RetryPolicy retryOneTime = new RetryOneTime(3000);  // 三秒后重连一次，只连一次
        // RetryPolicy retryNTimes = new RetryNTimes(3, 3000); // 每3秒重连一次，重连3次
        // RetryPolicy retryPolicy = new RetryUntilElapsed(10 * 1000, 3000);  // 每3秒重连一次，总等待时间超过10秒则停止重连
        // 随着重试次数的增加，重试的间隔时间也会增加（推荐）：第n次重试等待 sleepMsBetweenRetry * 2^(n-1) 毫秒
        RetryPolicy backoffRetry = new ExponentialBackoffRetry(sleepMsBetweenRetry, maxRetries, maxSleepMs);

        // 声明初始化客户端
        client = CuratorFrameworkFactory.builder()
                .connectString(host)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .retryPolicy(backoffRetry)
                .namespace(namespace)
                .build();
        client.start();     // 启动curator客户端
    }

    public static CuratorFramework getClient() {
        return client;
    }
}
