package com.dhu.ycl.interceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    // 不使用addInterceptor中直接new：每次都是创建新实例，无法进行SMSInterceptor内部的依赖注入；不支持AOP
    @Bean // 支持@Autowired等注入，单例模式，由Spring管理，完整Spring生命周期，支持代理和切面
    public SMSInterceptor smsInterceptor() {
        return new SMSInterceptor();  // 这个new只执行一次
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(smsInterceptor())           // 注册该拦截器
                .addPathPatterns("/userAuth/getSMSCode");   // 指定拦截路径
    }
}
