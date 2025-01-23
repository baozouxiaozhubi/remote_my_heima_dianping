package com.hsj.hmdp.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.UserMapper;
import com.hsj.hmdp.dto.LoginFormDTO;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.User;
import com.hsj.hmdp.utils.Constants;
import com.hsj.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class IUserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    //从前端获取用户输入的手机号并生成验证码返回的功能-已经实现
    @Override
    public Result sendCode(String phone, HttpSession session) {
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

    //用户输入手机号+密码/验证码 后端登陆功能+未注册用户注册
    @Override
    public Result login(LoginFormDTO loginFormDTO, HttpSession session) {
        //1.校验手机号
        String phone = loginFormDTO.getPhone();
        if(RegexUtils.isPhoneInvalid(phone))
        {
            //2.手机号不合法报错
            return Result.fail("手机号不合法");
        }
        //3.校验验证码
        Map<String,String> map = session.getAttribute(Constants.CODE)==null ? new HashMap<>() : (Map<String,String>)session.getAttribute(Constants.CODE);
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
        user.setNick_name("user_"+RandomUtil.randomString(10));
        baseMapper.insert(user);
        return user;
    }
}
