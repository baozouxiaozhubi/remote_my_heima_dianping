package com.hsj.hmdp.controller;

import com.hsj.hmdp.dto.LoginFormDTO;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.dto.UserDto;
import com.hsj.hmdp.pojo.User;
import com.hsj.hmdp.service.IUserService;
import com.hsj.hmdp.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService userService;

    //发送手机验证码--类之前加'/' 方法之前不加'/'
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone,session);
    }

    //校验手机验证码并保存用户信息(保存一个User对象)到session，其中如果用户第一次登陆还需要创建用户并保存到数据库中
    @PostMapping("login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm,session);
    }

    @GetMapping("/me")
    public Result me() {
        UserDto user = UserContext.getUser();
        return Result.ok(user);
    }
}
