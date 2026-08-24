package com.sping.order.controller;

import com.sping.common.dto.AfterSaleRequest;
import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.ReviewRequest;
import com.sping.order.service.AfterSaleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/after-sale")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    public AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PostMapping("/apply")
    public ApiResponse<?> apply(@RequestBody AfterSaleRequest request,
                                @RequestHeader("X-User-Id") Integer userId) {
        return afterSaleService.apply(userId, request);
    }

    @GetMapping("/my")
    public ApiResponse<?> getMyAfterSales(@RequestHeader("X-User-Id") Integer userId) {
        return afterSaleService.getMyAfterSales(userId);
    }

    @GetMapping("/{afterSaleId}")
    public ApiResponse<?> getDetail(@PathVariable Integer afterSaleId) {
        return afterSaleService.getDetail(afterSaleId);
    }

    @PutMapping("/{afterSaleId}/cancel")
    public ApiResponse<?> cancel(@PathVariable Integer afterSaleId,
                                 @RequestHeader("X-User-Id") Integer userId) {
        return afterSaleService.cancelAfterSale(afterSaleId, userId);
    }

    @GetMapping("/admin/list")
    public ApiResponse<?> getAllAfterSales() {
        return afterSaleService.getAllAfterSales();
    }

    @PutMapping("/admin/{afterSaleId}/review")
    public ApiResponse<?> review(@PathVariable Integer afterSaleId,
                                 @RequestBody ReviewRequest request) {
        return afterSaleService.review(afterSaleId, request);
    }

    @PutMapping("/{afterSaleId}/return")
    public ApiResponse<?> confirmReturn(@PathVariable Integer afterSaleId,
                                        @RequestHeader("X-User-Id") Integer userId) {
        return afterSaleService.confirmReturn(afterSaleId, userId);
    }

    @PutMapping("/admin/{afterSaleId}/refund")
    public ApiResponse<?> refund(@PathVariable Integer afterSaleId) {
        return afterSaleService.refund(afterSaleId);
    }
}
