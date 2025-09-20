package com.dhu.ycl.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "file-service")
public interface FileMicroServiceFeign {
    // 注册用户和更新用户名时会调用
    @PostMapping("/file/generatorQrCode")
    public String generatorQrCode(@RequestParam("wechatNumber") String wechatNumber, @RequestParam("userId") String userId);
}
