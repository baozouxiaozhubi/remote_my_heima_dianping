package com.hsj.hmdp;

import com.hsj.hmdp.dao.UserMapper;
import com.hsj.hmdp.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

@SpringBootTest
public class UserMapperTest {
    @Autowired
    private UserMapper userMapper;

    @Test
    void userMapperTest() {
        User user = new User();
        user.setId(1L).setPhone("18050285529");
        userMapper.insert(user);
    }
}
