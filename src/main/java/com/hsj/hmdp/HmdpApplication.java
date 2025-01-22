package com.hsj.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.hsj.hmdp.dao")
@SpringBootApplication
public class HmdpApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmdpApplication.class, args);
    }

}
