package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.Follow;

public interface IFollowService extends IService<Follow> {
    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}
