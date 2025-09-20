package com.dhu.ycl.interceptor;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.exceptions.GraceException;
import com.dhu.ycl.grace.result.ResponseStatusEnum;
import com.dhu.ycl.utils.IPUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;


// 下面的代码在此处没有实际效果-不会生效，controller中是根据手机号设置的key，并且也进行了时间验证，仅测试。
@Slf4j
public class SMSInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 拦截请求，在controller调用方法之前
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userIp = IPUtil.getRequestIp(request);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(BaseInfoProperties.MOBILE_SMSCODE + ":" + userIp))) {
            log.error("短信发送频率太高了~~！！！");
            GraceException.display(ResponseStatusEnum.SMS_NEED_WAIT_ERROR);
            return false;
        }
        return true;
    }
}
