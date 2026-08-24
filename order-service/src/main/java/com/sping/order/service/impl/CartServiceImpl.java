package com.sping.order.service.impl;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.CartRequest;
import com.sping.common.entity.Cart;
import com.sping.common.entity.CartItem;
import com.sping.order.config.BookFeignClient;
import com.sping.order.config.UserFeignClient;
import com.sping.order.repository.CartItemRepository;
import com.sping.order.repository.CartRepository;
import com.sping.order.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookFeignClient bookFeignClient;
    private final UserFeignClient userFeignClient;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           BookFeignClient bookFeignClient,
                           UserFeignClient userFeignClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookFeignClient = bookFeignClient;
        this.userFeignClient = userFeignClient;
    }

    @Override
    @Transactional
    public ApiResponse<?> addToCart(CartRequest request, Integer userId) {
        if (request.getBookId() == null) {
            return ApiResponse.error(400, "图书ID不能为空");
        }

        int quantity = (request.getQuantity() == null || request.getQuantity() < 1) ? 1 : request.getQuantity();

        // 验证图书是否存在，并获取书名
        String bookName = null;
        try {
            ApiResponse<?> bookResp = bookFeignClient.getBookById(request.getBookId());
            if (bookResp.getCode() != 200 || bookResp.getData() == null) {
                return ApiResponse.error(404, "图书不存在");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> bookMap = (Map<String, Object>) bookResp.getData();
            bookName = (String) bookMap.get("bookName");
        } catch (Exception e) {
            return ApiResponse.error(404, "图书不存在");
        }

        // 获取或创建用户的购物车（含用户名）
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            newCart.setUserName(fetchUserName(userId));
            newCart.setCreateTime(LocalDateTime.now());
            return cartRepository.save(newCart);
        });

        // 检查购物车中是否已有该书
        Optional<CartItem> existing = cartItemRepository.findByCartIdAndBookId(cart.getCartId(), request.getBookId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
            return ApiResponse.success("已更新购物车数量", null);
        }

        CartItem item = new CartItem();
        item.setCartId(cart.getCartId());
        item.setBookId(request.getBookId());
        item.setBookName(bookName);
        item.setQuantity(quantity);
        item.setAddTime(LocalDateTime.now());
        cartItemRepository.save(item);

        return ApiResponse.success("已添加到购物车", null);
    }

    @Override
    public ApiResponse<?> getMyCart(Integer userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        List<CartItem> items = cartItemRepository.findByCartId(cartOpt.get().getCartId());
        items.forEach(this::enrichCartItem);
        return ApiResponse.success(items);
    }

    @Override
    @Transactional
    public ApiResponse<?> updateQuantity(Integer cartItemId, Integer quantity, Integer userId) {
        if (quantity == null || quantity < 1) {
            return ApiResponse.error(400, "数量必须大于0");
        }

        Optional<CartItem> opt = cartItemRepository.findById(cartItemId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "购物车项不存在");
        }

        CartItem item = opt.get();
        // 验证归属
        Cart cart = cartRepository.findById(item.getCartId()).orElse(null);
        if (cart == null || !cart.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权操作");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return ApiResponse.success("更新成功", null);
    }

    @Override
    @Transactional
    public ApiResponse<?> removeItem(Integer cartItemId, Integer userId) {
        Optional<CartItem> opt = cartItemRepository.findById(cartItemId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "购物车项不存在");
        }

        CartItem item = opt.get();
        Cart cart = cartRepository.findById(item.getCartId()).orElse(null);
        if (cart == null || !cart.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权操作");
        }

        cartItemRepository.delete(item);
        return ApiResponse.success("已移除", null);
    }

    @Override
    @Transactional
    public ApiResponse<?> clearCart(Integer userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isPresent()) {
            cartItemRepository.deleteByCartId(cartOpt.get().getCartId());
        }
        return ApiResponse.success("购物车已清空", null);
    }

    private String fetchUserName(Integer userId) {
        try {
            ApiResponse<?> resp = userFeignClient.getUserInfo(userId);
            if (resp.getCode() == 200 && resp.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userMap = (Map<String, Object>) resp.getData();
                return (String) userMap.get("userName");
            }
        } catch (Exception ignored) {
        }
        return "用户" + userId;
    }

    @SuppressWarnings("unchecked")
    private void enrichCartItem(CartItem item) {
        try {
            ApiResponse<?> bookResp = bookFeignClient.getBookById(item.getBookId());
            if (bookResp.getCode() == 200 && bookResp.getData() != null) {
                Map<String, Object> bookMap = (Map<String, Object>) bookResp.getData();
                item.setBookName((String) bookMap.get("bookName"));
                item.setBookAuthor((String) bookMap.get("bookAuthor"));
                if (bookMap.get("bookPrice") != null) {
                    item.setBookPrice(new BigDecimal(bookMap.get("bookPrice").toString()));
                }
                item.setBookImg((String) bookMap.get("bookImg"));
            }
        } catch (Exception ignored) {
        }
    }
}
