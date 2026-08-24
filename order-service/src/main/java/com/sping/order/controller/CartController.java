package com.sping.order.controller;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.CartRequest;
import com.sping.order.service.CartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ApiResponse<?> addToCart(@RequestBody CartRequest request,
                                    @RequestHeader("X-User-Id") Integer userId) {
        return cartService.addToCart(request, userId);
    }

    @GetMapping("/my")
    public ApiResponse<?> getMyCart(@RequestHeader("X-User-Id") Integer userId) {
        return cartService.getMyCart(userId);
    }

    @PutMapping("/{cartItemId}")
    public ApiResponse<?> updateQuantity(@PathVariable Integer cartItemId,
                                         @RequestParam Integer quantity,
                                         @RequestHeader("X-User-Id") Integer userId) {
        return cartService.updateQuantity(cartItemId, quantity, userId);
    }

    @DeleteMapping("/{cartItemId}")
    public ApiResponse<?> removeItem(@PathVariable Integer cartItemId,
                                     @RequestHeader("X-User-Id") Integer userId) {
        return cartService.removeItem(cartItemId, userId);
    }

    @DeleteMapping("/clear")
    public ApiResponse<?> clearCart(@RequestHeader("X-User-Id") Integer userId) {
        return cartService.clearCart(userId);
    }
}
