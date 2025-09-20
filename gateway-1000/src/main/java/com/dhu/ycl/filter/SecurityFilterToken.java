package com.dhu.ycl.filter;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.grace.result.ResponseStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Order(-1)
@Slf4j
@Component
public class SecurityFilterToken extends BaseInfoProperties implements GlobalFilter {
    @Resource
    private ExcludeUrlProperties excludeUrlProperties;

    // 路径匹配规则器
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取当前请求的URL
        String url = exchange.getRequest().getURI().getPath();
        log.info("SecurityFilterToken url = {}", url);

        // 2. 校验并且排除 excludeList、静态资源服务static
        List<String> excludeList = excludeUrlProperties.getUrls();
        if (excludeList != null && !excludeList.isEmpty()) {
            for (String excludeUrl : excludeList) {
                if (antPathMatcher.matchStart(excludeUrl, url)) {
                    // 如果匹配到，则直接放行，表示当前的url是不需要被拦截校验的
                    return chain.filter(exchange);
                }
            }
        }
        String fileStart = excludeUrlProperties.getFileStart();
        if (StringUtils.isNotBlank(fileStart)) {
            if (antPathMatcher.matchStart(fileStart, url)) {
                return chain.filter(exchange);
            }
        }

        // 3. 从header中获得用户的id以及token
        log.info("当前请求的路径[{}]被拦截...", url);
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String userId = headers.getFirst(HEADER_USER_ID);
        String userToken = headers.getFirst(HEADER_USER_TOKEN);
        log.info("userId = {}", userId);
        log.info("userToken = {}", userToken);

        // 4. 判断header中是否有token，对用户请求进行判断拦截
        if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(userToken)) {
            // 限制只能单设备登录 每个id都会有一个token，id是固定的，token是变化的，后面的token就会覆盖前面的，导致只能有一个token生效
            // String redisToken = redis.get(REDIS_USER_TOKEN + ":" + userId);
            // if (redisToken.equals(userToken)) {
            //     return chain.filter(exchange);
            // }

            // 允许多设备登录：每个token可以不一样，但是id是固定的，每次可以匹配上。
            String userIdRedis = redis.get(REDIS_USER_TOKEN + ":" + userToken);
            if (userIdRedis.equals(userId)) {
                return chain.filter(exchange);
            }
        }

        // 默认不放行
        return ErrorRenderUtils.renderErrorMsg(exchange, ResponseStatusEnum.UN_LOGIN);
    }
}
