package com.sping.order.controller;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.CheckoutRequest;
import com.sping.common.dto.PayRequest;
import com.sping.common.dto.ReviewRequest;
import com.sping.order.service.PurchaseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/checkout")
    public ApiResponse<?> checkout(@RequestBody CheckoutRequest request,
                                   @RequestHeader("X-User-Id") Integer userId) {
        return purchaseService.checkout(request, userId);
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<?> pay(@PathVariable Integer orderId,
                              @RequestBody PayRequest request,
                              @RequestHeader("X-User-Id") Integer userId) {
        return purchaseService.pay(orderId, request, userId);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<?> getOrderDetail(@PathVariable Integer orderId) {
        return purchaseService.getOrderDetail(orderId);
    }

    @GetMapping("/my")
    public ApiResponse<?> getMyOrders(@RequestHeader("X-User-Id") Integer userId) {
        return purchaseService.getMyOrders(userId);
    }

    @GetMapping("/admin/list")
    public ApiResponse<?> getAllOrders() {
        return purchaseService.getAllOrders();
    }

    @PutMapping("/admin/{orderId}/review")
    public ApiResponse<?> reviewOrder(@PathVariable Integer orderId,
                                      @RequestBody ReviewRequest request) {
        return purchaseService.reviewOrder(orderId, request);
    }

    @PutMapping("/admin/{orderId}/ship")
    public ApiResponse<?> shipOrder(@PathVariable Integer orderId) {
        return purchaseService.shipOrder(orderId);
    }

    @PutMapping("/{orderId}/cancel")
    public ApiResponse<?> cancelOrder(@PathVariable Integer orderId,
                                      @RequestHeader("X-User-Id") Integer userId) {
        return purchaseService.cancelOrder(orderId, userId);
    }
}
