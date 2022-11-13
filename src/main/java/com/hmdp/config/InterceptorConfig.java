package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Resource
    StringRedisTemplate template;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /*
        使用两个拦截器，一个拦截所有请求，但不具有拦截功能。用来确保token刷新。同时完成登录判断
        另一个完成拦截功能
        * */
        //order值越小，拦截器优先级越高
        registry.addInterceptor(new RefreshTokenInterceptor(template)).addPathPatterns("/**").order(0);

        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/shop/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/hot",
                "/voucher/**",
                "/user/code",
                "/user/login"
        ).order(1);
    }
}
