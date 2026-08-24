package com.sping.order.service;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.OrderRequest;

public interface OrderService {

    ApiResponse<?> borrowBook(OrderRequest request, Integer userId);

    ApiResponse<?> returnBook(Integer borrowId, Integer userId);

    ApiResponse<?> getBorrowById(Integer borrowId);

    ApiResponse<?> getMyBorrows(Integer userId);
}