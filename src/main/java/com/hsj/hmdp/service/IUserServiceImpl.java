package com.hsj.hmdp.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.UserMapper;
import com.hsj.hmdp.dto.LoginFormDTO;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.dto.UserDto;
import com.hsj.hmdp.pojo.User;
import com.hsj.hmdp.utils.Constants;
import com.hsj.hmdp.utils.RegexUtils;
import com.hsj.hmdp.utils.SnowflakeSingleton;
import com.hsj.hmdp.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class IUserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate; //注入专用于Redis的BaseMapper

    //从前端获取用户输入的手机号并生成验证码返回的功能-基于session已经实现
    //@Override
    public Result sendCode_session(String phone, HttpSession session) {
        //1. 校验手机号是否合法
        if(RegexUtils.isPhoneInvalid(phone))
        {
            //2. 不合法返回错误信息
            return Result.fail("手机号不合法");
        }
        //3. 合法生成验证码
        String code= RandomUtil.randomNumbers(6);

        //4. 保存生成的验证码到session
        //原版黑马点评只存code,会出现先输入自己的手机号获取验证码，然后输入正确验证码和另一个手机号也成功登陆的bug
        //这里改成存Map
        Map<String,String> codeMap = new HashMap<>();
        codeMap.put(phone,code);
        if(session!=null) session.setAttribute(Constants.CODE,codeMap);

        //5.发送验证码
        log.debug("验证码成功生成，为："+code);
        return Result.ok();
    }

    //从前端获取用户输入的手机号并生成验证码返回的功能-基于redis已实现
    @Override
    public Result sendCode(String phone, HttpSession session) {

        //1. 校验手机号是否合法
        if(RegexUtils.isPhoneInvalid(phone))
        {
            //2. 不合法返回错误信息
            return Result.fail("手机号不合法");
        }
        //3. 合法生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4. 保存生成的验证码到redis并设置过期时间
        stringRedisTemplate.opsForValue().set(Constants.LOGIN_CODE_HEADER +phone,
                code, Constants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5.发送验证码
        log.debug("验证码成功生成，为："+code);
        return Result.ok();
    }

    //用户输入手机号+密码/验证码 后端登陆功能+未注册用户注册-基于redis已实现
    @Override
    public Result login(LoginFormDTO loginFormDTO, HttpSession session) {
        //1.校验手机号
        String phone = loginFormDTO.getPhone();
        if(RegexUtils.isPhoneInvalid(phone))
        {
            //2.手机号不合法报错
            return Result.fail("手机号不合法");
        }
        //3.从redis获取验证码并校验验证码
        String code = loginFormDTO.getCode();
        String cacheCode = stringRedisTemplate.opsForValue().get(Constants.LOGIN_CODE_HEADER +phone);
        if(cacheCode==null || !cacheCode.equals(code))
        {
            //4.不一致，报错
            return Result.fail("验证码错误");
        }
        //5.一致，根据手机号查询用户，判断用户是否存在
        User user = baseMapper.selectOne(new QueryWrapper<User>().eq("phone",phone));
        //6.不存在，创建用户并保存
        if(user==null)
        {
            user = createUserWithPhone(phone);
        }
        //7.1 随机生成token作为登陆令牌(UUID或者雪花算法) UUID.randomUUID().toString();
        Snowflake snowflake = SnowflakeSingleton.getSnowflake(); //通过单例类获取雪花算法执行器
        String token = snowflake.nextIdStr(); //获取雪花ID
        //7.2 将User对象转为HashMap存储 过滤敏感信息:使用hutool的BeanUtil.copyProperties类
        UserDto userDto = BeanUtil.copyProperties(user,UserDto.class);
        Map<String,Object> userDtoMapObject = BeanUtil.beanToMap(userDto);
        // 借助工具类完成Map<String,Object>到Map<String,String>类型转换,因为stringRedisTemplate.opsForHash()
        // putAll只能存Map<String,String>
        // 这一步也可以放在Template中实现
        Map<String, String> userDtoMap = new HashMap<>();
        userDtoMapObject.forEach((key, value) -> userDtoMap.put(key, String.valueOf(value)));
        //7.3 存储用户信息到redis中
        stringRedisTemplate.opsForHash().putAll(Constants.LOGIN_TOKEN_HEADER+token,
                userDtoMap);
        //7.4 设置token有效期
        stringRedisTemplate.expire(Constants.LOGIN_TOKEN_HEADER+token,
                Constants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);

        //8.将token显式的返回给用户--放入['Authorization']中
        return Result.ok(token);
    }

    @Override
    public Result logout(String token){
        stringRedisTemplate.delete(Constants.LOGIN_TOKEN_HEADER+token);//用于登出基于Redis实现的session的登录
        UserContext.removeUser();
        return Result.ok();
    }

    //用户输入手机号+密码/验证码 后端登陆功能+未注册用户注册-基于session已经实现
    //@Override
    public Result login_session(LoginFormDTO loginFormDTO, HttpSession session) {
        //1.校验手机号
        String phone = loginFormDTO.getPhone();
        if(RegexUtils.isPhoneInvalid(phone))
        {
            //2.手机号不合法报错
            return Result.fail("手机号不合法");
        }
        //3.校验验证码
        Map<String,String> map = session.getAttribute(Constants.CODE)==null?new HashMap<>():(Map<String,String>)session.getAttribute(Constants.CODE);
        String code = loginFormDTO.getCode();
        String cacheCode = map.get(phone);
        if(cacheCode==null || !cacheCode.equals(code))
        {
            //4.不一致，报错
            return Result.fail("验证码错误");
        }
        //5.一致，根据手机号查询用户，判断用户是否存在
        User user = baseMapper.selectOne(new QueryWrapper<User>().eq("phone",phone));
        //6.不存在，创建用户并保存
        if(user==null)
        {
            user = createUserWithPhone(phone);
        }
        //7.存在，保存用户信息到session中
        session.setAttribute(Constants.USER,user);
        return null;
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_"+RandomUtil.randomString(10));
        baseMapper.insert(user);
        return user;
    }

    //利用BitMap实现签到
    @Override
    public Result sign() {
        //1.获取当前用户
        UserDto user = UserContext.getUser();
        Long userId = user.getId();
        //2.获取日期
        LocalDateTime now = LocalDateTime.now();
        //3.拼接Key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = Constants.USER_SIGN_KEY + userId + keySuffix;
        //4.获取今天是几号
        int dayOfMonth = now.getDayOfMonth();
        //5.设置 Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth-1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //1.获取当前用户
        UserDto user = UserContext.getUser();
        Long userId = user.getId();
        //2.获取日期
        LocalDateTime now = LocalDateTime.now();
        //3.拼接Key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = Constants.USER_SIGN_KEY + userId + keySuffix;
        //4.获取本月至今为止的所有签到记录，返回的是一个十进制数字
        int dayOfMonth = now.getDayOfMonth();
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands
                                .BitFieldType
                                .unsigned(dayOfMonth))
                                .valueAt(0)
        );
        if(result==null || result.isEmpty())return Result.ok(0);
        Long num = result.get(0); //获得代表签到记录的十进制数字
        //5.循环遍历并统计连续签到数
        int cnt = 0;
        while(num>0) {
            //5.1 &1得到数字最后一个bit位 判断是否为0
            if((num & 1) ==0)
            {
                //5.2 为0则说明连续签到断了，直接结束
                break;
            }
            else
            {
                //5.3 否则连续签到计数器+1
                cnt++;
                //5.4 把数字右移一位继续循环
                num = num >> 1;
            }
        }
        return Result.ok(cnt);
    }
}
