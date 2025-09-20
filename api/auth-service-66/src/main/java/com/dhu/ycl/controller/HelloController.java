package com.dhu.ycl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试网关访问：127.0.0.1:66/a/hello
 * @see com.dhu.ycl.filter.IPLimitFilter
 */
@RestController
@RequestMapping("/a")
public class HelloController {

    @GetMapping("/hello")
    public String hello(){
        return "Hello World!";
    }
}
