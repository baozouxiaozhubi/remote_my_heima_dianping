package com.hsj.hmdp.config;

import com.hsj.hmdp.utils.LoginInterceptor_redis;
import com.hsj.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    //解决拦截器中无法注入StringRedisTemplate的问题
    @Bean
    public RefreshTokenInterceptor getRefreshTokenInterceptor()
    {
        return new RefreshTokenInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册基于session的拦截器
//        registry.addInterceptor(new LoginIntercepter())
//                .excludePathPatterns(
//                        "/user/code",
//                        "/user/login"
//                );
        //基于刷新Token拦截器
        registry.addInterceptor(getRefreshTokenInterceptor())
            .addPathPatterns("/**").order(0);
        //注册基于redis的拦截器
        registry.addInterceptor(new LoginInterceptor_redis())
            .excludePathPatterns(
                    "/user/code",
                    "/user/login",
                    "/shop/**",
                    "/shop-type/**",
                    "/voucher/**"
            ).order(1);
    }
}
