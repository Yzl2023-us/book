package com.sping.order.service.impl;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.OrderRequest;
import com.sping.common.entity.Order;
import com.sping.order.config.BookFeignClient;
import com.sping.order.config.UserFeignClient;
import com.sping.order.repository.OrderRepository;
import com.sping.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final BookFeignClient bookFeignClient;
    private final UserFeignClient userFeignClient;

    public OrderServiceImpl(OrderRepository orderRepository,
                            BookFeignClient bookFeignClient,
                            UserFeignClient userFeignClient) {
        this.orderRepository = orderRepository;
        this.bookFeignClient = bookFeignClient;
        this.userFeignClient = userFeignClient;
    }

    @Override
    @Transactional
    public ApiResponse<?> borrowBook(OrderRequest request, Integer userId) {
        if (request.getBookId() == null) {
            return ApiResponse.error(400, "图书ID不能为空");
        }

        ApiResponse<?> bookResp = bookFeignClient.getBookById(request.getBookId());
        if (bookResp.getCode() != 200 || bookResp.getData() == null) {
            return ApiResponse.error(404, "图书不存在");
        }

        List<Order> unreturned = orderRepository.findByBookIdAndReturnTimeIsNull(request.getBookId());
        if (!unreturned.isEmpty()) {
            return ApiResponse.error(400, "该图书已被借出");
        }

        Order order = new Order();
        order.setBookId(request.getBookId());
        order.setUserId(userId);

        orderRepository.save(order);
        enrichOrder(order);
        return ApiResponse.success("借阅成功", order);
    }

    @Override
    @Transactional
    public ApiResponse<?> returnBook(Integer borrowId, Integer userId) {
        Optional<Order> opt = orderRepository.findById(borrowId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "借阅记录不存在");
        }
        Order order = opt.get();

        if (order.getReturnTime() != null) {
            return ApiResponse.error(400, "该图书已归还");
        }
        if (!order.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权操作此借阅记录");
        }

        order.setReturnTime(LocalDateTime.now());
        orderRepository.save(order);

        enrichOrder(order);
        return ApiResponse.success("归还成功", order);
    }

    @Override
    public ApiResponse<?> getBorrowById(Integer borrowId) {
        Optional<Order> opt = orderRepository.findById(borrowId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "借阅记录不存在");
        }
        Order order = opt.get();
        enrichOrder(order);
        return ApiResponse.success(order);
    }

    @Override
    public ApiResponse<?> getMyBorrows(Integer userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByBorrowTimeDesc(userId);
        orders.forEach(this::enrichOrder);
        return ApiResponse.success(orders);
    }

    private void enrichOrder(Order order) {
        try {
            ApiResponse<?> bookResp = bookFeignClient.getBookById(order.getBookId());
            if (bookResp.getCode() == 200 && bookResp.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bookMap = (Map<String, Object>) bookResp.getData();
                order.setBookName((String) bookMap.get("bookName"));
            }
        } catch (Exception ignored) {
        }
        try {
            ApiResponse<?> userResp = userFeignClient.getUserInfo(order.getUserId());
            if (userResp.getCode() == 200 && userResp.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userMap = (Map<String, Object>) userResp.getData();
                order.setUserName((String) userMap.get("userName"));
            }
        } catch (Exception ignored) {
        }
    }
}