package com.hsj.hmdp.utils;

import com.hsj.hmdp.dto.UserDto;
import com.hsj.hmdp.pojo.User;

//用于存放保存用户信息的ThreadLocal的类(用户上下文)，一个浏览器的一次访问都在一个线程内，无论怎么跳转
//因此可以使用ThreadLocal保存用户的信息
public class UserContext {
    private static final ThreadLocal<UserDto> tl = new ThreadLocal<>();

    public static void saveUser(UserDto user){
        tl.set(user);
    }

    public static UserDto getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
