package Mapper.utils;

import com.hsj.hmdp.dto.UserDto;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginIntercepter implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取session
        HttpSession session = request.getSession();

        //2.从session中获取用户
        UserDto user = (UserDto) session.getAttribute("user");

        //3.判断用户是否存在
        if (user == null) {
            //4.不存在，拦截,返回401状态码未授权
            response.setStatus(401);
            return false;
        }
        //5.存在，保存用户信息到ThreadLocal
        UserContext.saveUser(user);
        //6.放行
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
