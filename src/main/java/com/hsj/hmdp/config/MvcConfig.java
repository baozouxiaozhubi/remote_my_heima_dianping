package com.hsj.hmdp.config;

import com.hsj.hmdp.utils.LoginInterceptor_redis;
import com.hsj.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private RefreshTokenInterceptor refreshTokenInterceptor;

    @Resource
    private LoginInterceptor_redis loginInterceptorRedis;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册基于session的拦截器
//        registry.addInterceptor(new LoginIntercepter())
//                .excludePathPatterns(
//                        "/user/code",
//                        "/user/login"
//                );
        //基于刷新Token拦截器
        registry.addInterceptor(refreshTokenInterceptor)
            .addPathPatterns("/**").order(0);
        //注册基于redis的拦截器
        registry.addInterceptor(loginInterceptorRedis)
            .excludePathPatterns(
                    "/shop/**",
                    "/voucher/**",
                    "/shop-type/**",
                    "/upload/**",
                    "/blog/hot",
                    "/user/code",
                    "/user/login"
            ).order(1);
    }
}
