package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hsj.hmdp.dto.LoginFormDTO;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.User;

import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {
    public Result sendCode(String phone, HttpSession session);
    public Result login(LoginFormDTO loginFormDTO, HttpSession session);
}
