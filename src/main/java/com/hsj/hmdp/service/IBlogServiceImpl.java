package com.hsj.hmdp.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.BlogMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.dto.UserDto;
import com.hsj.hmdp.pojo.Blog;
import com.hsj.hmdp.pojo.User;
import com.hsj.hmdp.utils.Constants;
import com.hsj.hmdp.utils.UserContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IBlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override
    public Result queryHotBlog(Integer current) {
        //根据用户查询
        Page<Blog> page = query().orderByDesc("liked")
                .page(new Page<>(current, Constants.MAX_PAGE_SIZE));
        //获取当前页数据
        List<Blog> records = page.getRecords();

        //查询用户,放入Blog对象中(因为查看时不仅要查看笔记内容还要查看是谁发的)
        records.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getBaseMapper().selectById(id);
        if(blog == null){
            return Result.fail("笔记不存在!");
        }
        //将当前用户信息放进Blog对象中
        queryBlogUser(blog);
        //查看当前用户是否点赞过该笔记
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    //根据当前用户设置拿到的blog对象的isLiked属性--所有拿到Blog对象返回到前端的都要做--可以做成AOP
    private void isBlogLiked(Blog blog) {
        //1.拿到当前用户
        UserDto user = UserContext.getUser();
        //用户未登录直接返回
        if(user == null)return;
        Long userId = user.getId();

        //2.判断当前登录用户是否点赞过
        String key = Constants.BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key,userId.toString());
        blog.setIsLike(score != null);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNick_name());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result likeBlog(Long id) {
        //1.拿到当前用户
        UserDto user = UserContext.getUser();
        Long userId = user.getId();

        //2.判断当前登录用户是否点赞过
        String key = Constants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key,userId.toString());
        if(score == null)
        {
            //3.如果未点赞则可以点赞
            //3.1 数据库点赞数+1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id",id).update();
            //3.2 更新成功保存用户到Redis的Set集合
            if(isSuccess)
            {
                //插入SortedSet中，以时间戳为score == zadd key value score
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
            return Result.ok("点赞成功");
        }
        else
        {
            //4.如果已点赞，取消点赞
            //4.1 数据库点赞数-1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id",id).update();
            //4.2 把用户从Redis的Set集合移除
            if(isSuccess)
            {
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
            return Result.ok("取消点赞成功");
        }
    }

    //查询笔记点赞时间前五的用户
    @Override
    public Result queryBlogLikes(Long id) {
        String key = Constants.BLOG_LIKED_KEY + id;
        //1.查询Top5的点赞用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(top5 == null || top5.isEmpty())
        {
            return Result.ok(Collections.emptyList());
        }
        //2.解析其中的用户id--使用流处理
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        //3.根据用户id查询用户-用MyBatisplus实现的列表查询
        List<UserDto> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user,UserDto.class))
                .collect(Collectors.toList());
        //4.返回用户列表
        return Result.ok(users);
    }

    @Override
    public Result saveBlog(Blog blog) {
        //获取登录用户
        UserDto user = UserContext.getUser();
        blog.setUserId(user.getId());
        //保存探店博文
        saveOrUpdate(blog);
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        return null;
    }
}
