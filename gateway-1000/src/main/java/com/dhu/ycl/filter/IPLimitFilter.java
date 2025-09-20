package com.dhu.ycl.filter;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.grace.result.ResponseStatusEnum;
import com.dhu.ycl.utils.IPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * 临时ip：限制次数和秒数，自动过期，需要使用 redis  => 整合redis。
 * 需求：判断某个请求的ip在20秒内的请求次数是否超过3次，如果超过3次，则限制访问30秒后再恢复访问。
 */


@Order(1)  // 过滤器的顺序，数字越小则优先级越大
@Slf4j
@Component
@RefreshScope
public class IPLimitFilter extends BaseInfoProperties implements GlobalFilter {
    private static final String COMMON_IP_PREFIX = "gateway-ip:";
    private static final String Limit_IP_PREFIX = "gateway-ip:limit:";
    // 连续请求的次数
    @Value("${blackIp.continueCounts}")
    private Integer continueCounts;
    // 时间间隔
    @Value("${blackIp.timeInterval}")
    private Integer timeInterval;
    // 限制时间
    @Value("${blackIp.limitTimes}")
    private Integer limitTimes;

    IPLimitFilter() {
        log.info("IPLimitFilter init...");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.debug("IPLimitFilter processing request: {}", exchange.getRequest().getURI());
        log.info("continueCounts: {}", continueCounts);
        log.info("timeInterval: {}", timeInterval);
        log.info("limitTimes: {}", limitTimes);
        return doLimit(exchange, chain);
        // return chain.filter(exchange);  // 默认放行请求到后续的路由/服务
    }

    // 限制ip请求次数的判断
    public Mono<Void> doLimit(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1、根据request获得请求ip
        ServerHttpRequest request = exchange.getRequest();
        String ip = IPUtil.getIP(request);
        // 正常ip
        final String commonIpRedisKey = COMMON_IP_PREFIX + ip;
        // 黑名单ip，如果在redis中存在，则表示目前被关小黑屋
        final String limitIpRedisKey = Limit_IP_PREFIX + ip;

        // 2、黑名单ip：限制时间大于0，直接终止。
        long limitLeftTimes = redis.ttl(limitIpRedisKey);
        if (limitLeftTimes > 0) {
            return ErrorRenderUtils.renderErrorMsg(exchange, ResponseStatusEnum.SYSTEM_ERROR_BLACK_IP);
        }

        // 3、正常ip：次数累加
        long requestCounts = redis.increment(commonIpRedisKey, 1);
        // 正常ip：第一次进来设置过期时间 timeInterval 连续请求的次数的间隔时间
        if (requestCounts == 1) {
            redis.expire(commonIpRedisKey, timeInterval);
        }
        // 正常ip：超过连续请求的次数，则进行限制，将其加入黑名单。
        if (requestCounts > continueCounts) {
            // 变为黑名单ip，直接终止请求
            redis.set(limitIpRedisKey, limitIpRedisKey, limitTimes);
            return ErrorRenderUtils.renderErrorMsg(exchange, ResponseStatusEnum.SYSTEM_ERROR_BLACK_IP);
        }

        return chain.filter(exchange);
    }
}