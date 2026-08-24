package com.sping.order.service;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.CartRequest;

public interface CartService {

    ApiResponse<?> addToCart(CartRequest request, Integer userId);

    ApiResponse<?> getMyCart(Integer userId);

    ApiResponse<?> updateQuantity(Integer cartItemId, Integer quantity, Integer userId);

    ApiResponse<?> removeItem(Integer cartItemId, Integer userId);

    ApiResponse<?> clearCart(Integer userId);
}
