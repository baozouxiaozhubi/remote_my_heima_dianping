package Mapper.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hsj.hmdp.dto.UserDto;
import jodd.util.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//专门用于刷新Token,不论是否登陆用户都放行
@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.从请求头获取token
        String token = request.getHeader("authorization");
        //2.token为空直接放行，不用刷新token
        if(StringUtil.isBlank(token))return true;
        //3.否则根据token从redis中获取用户
        Map<Object,Object> userDtoMap = stringRedisTemplate.opsForHash().entries(Constants.LOGIN_TOKEN_HEADER+token);
        //4.判断用户是否存在
        if (userDtoMap.isEmpty()) {
            //4.不存在，说明登陆过期，直接放行
            return true;
        }
        //5.否则将HashMap数据转换成UserDto对象
        UserDto user = BeanUtil.fillBeanWithMap(userDtoMap, new UserDto(),false);
        //6.保存用户信息到ThreadLocal
        UserContext.saveUser(user);
        //7.刷新token有效期
        stringRedisTemplate.expire(Constants.LOGIN_TOKEN_HEADER+token, Constants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        //8.放行
        System.out.println("token已被刷新");
        return true;
    }

}
