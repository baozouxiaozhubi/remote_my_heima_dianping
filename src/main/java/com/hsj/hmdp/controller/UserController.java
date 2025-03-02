package com.hsj.hmdp.controller;

import cn.hutool.core.bean.BeanUtil;
import com.hsj.hmdp.dto.LoginFormDTO;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.dto.UserDto;
import com.hsj.hmdp.pojo.User;
import com.hsj.hmdp.pojo.UserInfo;
import com.hsj.hmdp.service.IUserInfoService;
import com.hsj.hmdp.service.IUserService;
import com.hsj.hmdp.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone,session);
    }

    //校验手机验证码并保存用户信息(保存一个User对象)到session，其中如果用户第一次登陆还需要创建用户并保存到数据库中
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm,session);
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token) {
        return userService.logout(token);
    }

    @GetMapping("/me")
    public Result me() {
        UserDto user = UserContext.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        // 返回
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDto userDto = BeanUtil.copyProperties(user, UserDto.class);
        // 返回
        return Result.ok(userDto);
    }

    //利用BitMap实现签到
    @PostMapping("/sign")
    public Result sign()
    {
        return userService.sign();
    }

    @GetMapping("/sign/count")
    public Result signCount()
    {
        return userService.signCount();
    }
}
