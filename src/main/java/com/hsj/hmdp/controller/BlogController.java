package com.hsj.hmdp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.dto.UserDto;
import com.hsj.hmdp.pojo.Blog;
import com.hsj.hmdp.service.IBlogService;
import com.hsj.hmdp.utils.Constants;
import com.hsj.hmdp.utils.UserContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    //保存笔记到数据库，并且用推模式实现Feed流(数据结构：SortedSet)
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id){
        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDto user = UserContext.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, Constants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    //获取某条笔记的点赞用户排行榜
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id){
        return blogService.queryBlogLikes(id);
    }

    //根据用户Id查询笔记，以GET形式传参
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {return blogService.queryBlogByUserId(current,id);}

    //查看登录用户所关注用户发的笔记(Feed流推模式)
    @GetMapping("/of/follow")
    public Result queryBlogofFollow(@RequestParam("lastId") Long max, @RequestParam(value = "offset",defaultValue = "0") Integer offset)
    {
        return blogService.queryBlogOfFollow(max, offset);
    }
}
