package com.hsj.hmdp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @RequestMapping("/1")
    public String hello() {
        return "hello";
    }
}
