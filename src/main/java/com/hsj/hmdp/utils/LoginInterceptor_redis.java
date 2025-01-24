package com.hsj.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hsj.hmdp.dto.UserDto;
import jodd.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//只用于拦截针对需要登陆的页面的访问
@Component
public class LoginInterceptor_redis implements HandlerInterceptor {

    //基于redis实现
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.根据ThreaLocal中是否有用户信息判断是否拦截即可
        if(UserContext.getUser()==null)
        {
            response.setStatus(401);
            return false;
        }
        //2.放行
        System.out.println("拦截器成功放行");
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //postHandle只有
        UserContext.removeUser();
    }

}
