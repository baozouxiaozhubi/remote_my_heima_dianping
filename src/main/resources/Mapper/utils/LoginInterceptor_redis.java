package Mapper.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

}
