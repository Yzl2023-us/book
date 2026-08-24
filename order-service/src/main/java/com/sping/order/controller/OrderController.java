package com.sping.order.controller;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.OrderRequest;
import com.sping.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/borrow")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/borrow")
    public ApiResponse<?> borrowBook(@RequestBody OrderRequest request,
                                     @RequestHeader("X-User-Id") Integer userId) {
        return orderService.borrowBook(request, userId);
    }

    @PutMapping("/{borrowId}/return")
    public ApiResponse<?> returnBook(@PathVariable("borrowId") Integer borrowId,
                                     @RequestHeader("X-User-Id") Integer userId) {
        return orderService.returnBook(borrowId, userId);
    }

    @GetMapping("/{borrowId}")
    public ApiResponse<?> getBorrowById(@PathVariable("borrowId") Integer borrowId) {
        return orderService.getBorrowById(borrowId);
    }

    @GetMapping("/my")
    public ApiResponse<?> getMyBorrows(@RequestHeader("X-User-Id") Integer userId) {
        return orderService.getMyBorrows(userId);
    }
}