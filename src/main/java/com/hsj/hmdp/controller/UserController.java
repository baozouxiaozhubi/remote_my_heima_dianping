package com.hsj.hmdp.controller;

import com.hsj.hmdp.dto.LoginFormDTO;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.service.IUserService;
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

    @PostMapping("login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm,session);
    }
}
