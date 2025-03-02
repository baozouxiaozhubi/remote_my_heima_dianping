package com.hsj.hmdp.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.BlogMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.dto.ScrollResult;
import com.hsj.hmdp.dto.UserDto;
import com.hsj.hmdp.pojo.Blog;
import com.hsj.hmdp.pojo.Follow;
import com.hsj.hmdp.pojo.User;
import com.hsj.hmdp.utils.Constants;
import com.hsj.hmdp.utils.UserContext;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IBlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Resource
    private IFollowService followService;

    @Resource
    private IBlogService blogService;

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
        long userId = user.getId();

        //2.判断当前登录用户是否点赞过
        String key = Constants.BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, Long.toString(userId));
        blog.setIsLike(score != null);
    }

    //把笔记作者信息保存到笔记类中
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result likeBlog(Long id) {
        //1.拿到当前用户
        UserDto user = UserContext.getUser();
        long userId = user.getId();

        //2.判断当前登录用户是否点赞过
        String key = Constants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, Long.toString(userId));
        if(score == null)
        {
            //3.如果未点赞则可以点赞
            //3.1 数据库点赞数+1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id",id).update();
            //3.2 更新成功保存用户到Redis的Set集合
            if(isSuccess)
            {
                //插入SortedSet中，以时间戳为score == zadd key value score
                stringRedisTemplate.opsForZSet().add(key, Long.toString(userId),System.currentTimeMillis());
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
                stringRedisTemplate.opsForZSet().remove(key, Long.toString(userId));
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

        //3.根据用户id查询用户-用MyBatisplus实现的列表查询1---会导致乱序
//        List<UserDto> users = userService.listByIds(ids)
//                .stream()
//                .map(user -> BeanUtil.copyProperties(user,UserDto.class))
//                .collect(Collectors.toList());

        String idStr = StrUtil.join(",", ids);
        //3.根据用户id查询用户-用MyBatisplus实现的列表查询2---解决乱序 WHERE id IN (5,1) ORDER BY FIELD (id, 5, 1)
        List<UserDto> users = userService.query()
                .in("id",ids).last("ORDER BY FIELD(id," + idStr + ")").list()
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
        Long userId = user.getId();
        blog.setUserId(userId);
        //保存探店博文--用当前线程的AOP事务代理来执行对数据库的修改操作(对象内方法自调用会导致事务失效)
        IBlogService proxy = (IBlogService) AopContext.currentProxy();
        boolean isSuccess = proxy.saveOrUpdate(blog);
        if(!isSuccess)return Result.fail("保存笔记失败");
        //查询笔记作者(当前登录用户)的所有粉丝(为了推送到他们的【收件箱】)
        List<Long> fansIds = followService.query().eq("follow_user_id",userId).list()
                .stream().map(Follow::getUserId).collect(Collectors.toList());
        //推送笔记Id给所有粉丝
        Long blogId = blog.getId();
        for (Long id : fansIds) {
            //推送到收件箱(每个粉丝独立)
            String key = Constants.BLOG_PUSH_KEY + id;
            stringRedisTemplate.opsForZSet().add(key, blogId.toString(), System.currentTimeMillis());
        }
        //返回博客Id
        return Result.ok(blogId);
    }

    //查看登录用户所关注用户发的笔记(Feed流推模式)
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //1. 找到当前用户
        Long userId = UserContext.getUser().getId();

        //2.拿到收件箱对应的KEY
        String key = Constants.BLOG_PUSH_KEY + userId;

        //3.查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, Constants.DEFAULT_PAGE_SIZE);
        if(typedTuples == null || typedTuples.isEmpty())return Result.ok();

        //4.解析数据：blogId,minTime(时间戳),offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os=1;
        //4.1 遍历的时候统计和最小值相等的元素有几个，作为下一次访问的offset
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            //4.2 获取blogId
            ids.add(Long.valueOf(Objects.requireNonNull(typedTuple.getValue())));
            //4.3 获取分数(时间戳)
            long time = Objects.requireNonNull(typedTuple.getScore()).longValue();
            if(time == minTime)os++;
            else
            {
                minTime = time;
                os = 1;
            }
        }

        //5.根据blogId查blog-处理ZSET到数据库乱序问题
        String idStr = StrUtil.join(",", ids); //手动指定返回的顺序
        List<Blog> blogs = blogService.query().in("id", ids).
                last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream().map(blog -> {
                    //把笔记作者信息保存进笔记实体类对象中
                    queryBlogUser(blog);
                    //查询blog是否被当前用户点赞
                    isBlogLiked(blog);
                    return blog;
                })
                .collect(Collectors.toList());

        //6.封装并返回 下次访问以minTime为max，跳过offset个
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);

        return Result.ok(r);
    }

    @Override
    public Result queryBlogByUserId(Integer current, Long id) {
        Page<Blog> page= query().eq("user_id", id).page(new Page<>(current, Constants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }
}
