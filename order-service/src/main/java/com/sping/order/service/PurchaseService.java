package com.sping.order.service;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.CheckoutRequest;
import com.sping.common.dto.PayRequest;
import com.sping.common.dto.ReviewRequest;

public interface PurchaseService {

    ApiResponse<?> checkout(CheckoutRequest request, Integer userId);

    ApiResponse<?> pay(Integer orderId, PayRequest request, Integer userId);

    ApiResponse<?> getOrderDetail(Integer orderId);

    ApiResponse<?> getMyOrders(Integer userId);

    ApiResponse<?> getAllOrders();

    ApiResponse<?> reviewOrder(Integer orderId, ReviewRequest request);

    ApiResponse<?> shipOrder(Integer orderId);

    ApiResponse<?> cancelOrder(Integer orderId, Integer userId);
}
