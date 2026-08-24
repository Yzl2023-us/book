package com.sping.message.config;

import com.sping.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/api/user/info/{userId}")
    ApiResponse<?> getUserInfo(@PathVariable("userId") Integer userId);
}