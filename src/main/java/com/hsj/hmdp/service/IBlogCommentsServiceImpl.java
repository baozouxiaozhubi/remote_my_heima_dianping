package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.BlogCommentsMapper;
import com.hsj.hmdp.pojo.BlogComments;
import org.springframework.stereotype.Service;

@Service
public class IBlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {
}
