package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.UserInfoMapper;
import com.hsj.hmdp.pojo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IUserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService{

}
