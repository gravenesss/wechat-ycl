package com.dhu.ycl.zk;

import com.dhu.ycl.pojo.netty.NettyServerNode;
import com.dhu.ycl.utils.JsonUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;

public class ZookeeperRegister {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperRegister.class);
    public static final String DEFAULT_NODE_NAME = "server-list";

    public static String getLocalIp() throws Exception {
        InetAddress addr = InetAddress.getLocalHost();
        String ip = addr.getHostAddress();
        log.info("本机IP地址：{}", ip);
        return ip;
    }

    public static void registerNettyServer(String nodeName, String ip, Integer port) throws Exception {
        CuratorFramework zkClient = CuratorConfig.getClient();
        // 1、创建持久化父节点：检查 /server-list 路径是否存在，如果不存在，则创建持久化节点作为服务器列表的根节点
        String path = "/" + nodeName;
        Stat stat = zkClient.checkExists().forPath(path);
        if (stat == null) {
            zkClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT).forPath(path);
            log.info("registerNettyServer节点创建成功:{}", path);
        } else {
            log.info("registerNettyServer节点已存在:{}", path);
        }
        // 2、注册临时顺序节点：.withMode(CreateMode.EPHEMERAL_SEQUENTIAL): 创建对应的临时_顺序节点,节点具有唯一性和自动清理特性
        // .forPath: 节点命名格式为 /server-list/im- + 序号, 节点数据包含服务器的IP地址和端口信息,(如 /server-list/im-0000000001)
        NettyServerNode serverNode = new NettyServerNode();
        serverNode.setIp(ip);
        serverNode.setPort(port);
        serverNode.setOnlineCounts(0);
        String nodeJson = JsonUtils.objectToJson(serverNode);
        zkClient.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(path + "/im-", nodeJson.getBytes());
    }

    // 增加在线人数
    public static void incrementOnlineCounts(NettyServerNode serverNode) throws Exception {
        dealOnlineCounts(serverNode, 1);
    }

    // 减少在线人数
    public static void decrementOnlineCounts(NettyServerNode serverNode) throws Exception {
        dealOnlineCounts(serverNode, -1);
    }

    // 更新ZooKeeper中Netty服务器节点的在线人数统计
    public static void dealOnlineCounts(NettyServerNode serverNode, Integer counts) throws Exception {
        CuratorFramework zkClient = CuratorConfig.getClient();
        // 1、分布式锁保护: 使用 InterProcessReadWriteLock 确保并发安全
        InterProcessReadWriteLock readWriteLock = new InterProcessReadWriteLock(zkClient, "/rw-locks");
        readWriteLock.writeLock().acquire();
        try {
            // 2、遍历所有子节点，检查是否匹配指定的服务器节点，然后进行在线人数统计更新
            String path = "/" + DEFAULT_NODE_NAME;
            List<String> list = zkClient.getChildren().forPath(path);
            for (String node : list) {
                String pendingNodePath = path + "/" + node;
                String nodeValue = new String(zkClient.getData().forPath(pendingNodePath));
                NettyServerNode pendingNode = JsonUtils.jsonToPojo(nodeValue, NettyServerNode.class);// 注册时放了ip、port、onlineCounts
                // 3、如果ip和端口匹配，则当前路径的节点则需要累加或者累减
                if (pendingNode.getIp().equals(serverNode.getIp()) && (pendingNode.getPort().intValue() == serverNode.getPort().intValue())) {
                    pendingNode.setOnlineCounts(pendingNode.getOnlineCounts() + counts);
                    String nodeJson = JsonUtils.objectToJson(pendingNode);
                    zkClient.setData().forPath(pendingNodePath, nodeJson.getBytes());
                }
            }

        } finally {
            readWriteLock.writeLock().release();
        }
    }

}
