package com.hsj.hmdp.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.FollowMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.dto.UserDto;
import com.hsj.hmdp.pojo.Follow;
import com.hsj.hmdp.utils.Constants;
import com.hsj.hmdp.utils.UserContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IFollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    //关注和取关，在数据库和Redis同时更新数据
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //1.获取当前登录用户
        UserDto user = UserContext.getUser();
        Long userId = user.getId();
        String key = Constants.FOLLOW_KEY + userId;
        //2.根据isFollow判断是要取关还是关注：true关注 false取关
        if(BooleanUtil.isTrue(isFollow))
        {
            //3.true则添加关注，在中间表插入一行
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if(isSuccess)
            {
                //把followUserId放进KEY=userId的集合中去
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        }
        else
        {
            //4.false则取消关注，在中间表删除一行
            boolean isSuccess = remove(new QueryWrapper<Follow>().eq("user_id",userId).eq("follow_user_id",followUserId));
            if(isSuccess) {
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            };
        }
        return Result.ok();
    }

    //查询当前用户是否关注指定用户
    @Override
    public Result isFollow(Long followUserId) {
        UserDto user = UserContext.getUser();
        Long userId = user.getId();
        Integer count = query().eq("user_id",userId).eq("follow_user_id",followUserId).count();
        return Result.ok(count>0);
    }

    //返回当前用户与ID=id的用户的共同关注列表
    @Override
    public Result followCommons(Long id) {
        //获取当前登录用户
        UserDto user = UserContext.getUser();
        Long userId = user.getId();
        String currentUserKey = Constants.FOLLOW_KEY + userId;

        //获取要查询共同关注的用户的KEY
        String userKey = Constants.FOLLOW_KEY + id;

        //查交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(currentUserKey,userKey);

        //交集为空直接返回
        if(intersect == null || intersect.isEmpty())return Result.ok();

        //解析Id集合转换成Long类型
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        //查询用户
        List<UserDto> users = userService.listByIds(ids).stream()
                .map(uuser->{return BeanUtil.copyProperties(uuser,UserDto.class);})
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}
