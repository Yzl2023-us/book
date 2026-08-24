package com.sping.order.config;

import com.sping.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service")
public interface BookFeignClient {

    @GetMapping("/api/book/{bookId}")
    ApiResponse<?> getBookById(@PathVariable("bookId") Integer bookId);
}