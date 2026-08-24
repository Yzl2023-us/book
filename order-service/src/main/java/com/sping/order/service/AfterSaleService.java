package com.sping.order.service;

import com.sping.common.dto.AfterSaleRequest;
import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.ReviewRequest;

public interface AfterSaleService {

    ApiResponse<?> apply(Integer userId, AfterSaleRequest request);

    ApiResponse<?> getMyAfterSales(Integer userId);

    ApiResponse<?> getDetail(Integer afterSaleId);

    ApiResponse<?> cancelAfterSale(Integer afterSaleId, Integer userId);

    ApiResponse<?> getAllAfterSales();

    ApiResponse<?> review(Integer afterSaleId, ReviewRequest request);

    ApiResponse<?> confirmReturn(Integer afterSaleId, Integer userId);

    ApiResponse<?> refund(Integer afterSaleId);
}
