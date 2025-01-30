package com.hsj.hmdp;


import com.hsj.hmdp.service.IUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class codetest {
    @Autowired
    //@Qualifier("iUserServiceImpl")
    private IUserServiceImpl userServiceImpl;

    @Test
    public void sendcode()
    {
        userServiceImpl.sendCode("18050285529",null);
    }
}
