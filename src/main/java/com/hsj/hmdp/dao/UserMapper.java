package com.hsj.hmdp.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsj.hmdp.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
