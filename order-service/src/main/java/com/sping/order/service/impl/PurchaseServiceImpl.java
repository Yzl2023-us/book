package com.sping.order.service.impl;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.CheckoutRequest;
import com.sping.common.dto.PayRequest;
import com.sping.common.dto.ReviewRequest;
import com.sping.common.entity.*;
import com.sping.order.repository.*;
import com.sping.order.service.PurchaseService;
import com.sping.order.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;

    private static final Map<String, String> STATUS_TEXT = new LinkedHashMap<>();
    static {
        STATUS_TEXT.put("PENDING_PAYMENT", "待支付");
        STATUS_TEXT.put("PENDING_REVIEW",  "待审核");
        STATUS_TEXT.put("APPROVED",        "审核通过");
        STATUS_TEXT.put("REJECTED",        "审核拒绝");
        STATUS_TEXT.put("SHIPPED",         "已发货");
        STATUS_TEXT.put("COMPLETED",       "已完成");
        STATUS_TEXT.put("CANCELED",        "已取消");
    }

    public PurchaseServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                               PurchaseOrderItemRepository purchaseOrderItemRepository,
                               PaymentRepository paymentRepository,
                               CartRepository cartRepository,
                               CartItemRepository cartItemRepository,
                               CartService cartService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
    }

    @Override
    @Transactional
    public ApiResponse<?> checkout(CheckoutRequest request, Integer userId) {
        if (request.getRecipientName() == null || request.getRecipientName().isBlank()) {
            return ApiResponse.error(400, "收件人姓名不能为空");
        }
        if (request.getRecipientPhone() == null || request.getRecipientPhone().isBlank()) {
            return ApiResponse.error(400, "收件人电话不能为空");
        }
        if (request.getRecipientAddress() == null || request.getRecipientAddress().isBlank()) {
            return ApiResponse.error(400, "收件地址不能为空");
        }

        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) {
            return ApiResponse.error(400, "购物车为空");
        }

        String userName = cartOpt.get().getUserName();

        // Use CartService to get enriched items (with prices from book-service)
        ApiResponse<?> cartResp = cartService.getMyCart(userId);
        if (cartResp.getCode() != 200 || !(cartResp.getData() instanceof List)) {
            return ApiResponse.error(400, "购物车为空");
        }
        @SuppressWarnings("unchecked")
        List<CartItem> cartItems = (List<CartItem>) cartResp.getData();
        if (cartItems.isEmpty()) {
            return ApiResponse.error(400, "购物车为空");
        }

        String orderNo = generateOrderNo();

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setUserName(userName);
        order.setRecipientName(request.getRecipientName().trim());
        order.setRecipientPhone(request.getRecipientPhone().trim());
        order.setRecipientAddress(request.getRecipientAddress().trim());
        order.setStatus("PENDING_PAYMENT");
        order.setTotalAmount(BigDecimal.ZERO);

        purchaseOrderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Map<String, Object>> itemList = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            PurchaseOrderItem orderItem = new PurchaseOrderItem();
            orderItem.setOrderId(order.getOrderId());
            orderItem.setBookId(cartItem.getBookId());
            orderItem.setBookName(cartItem.getBookName());
            orderItem.setBookPrice(cartItem.getBookPrice() != null ? cartItem.getBookPrice() : BigDecimal.ZERO);
            orderItem.setQuantity(cartItem.getQuantity());
            purchaseOrderItemRepository.save(orderItem);

            totalAmount = totalAmount.add(orderItem.getBookPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));

            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("itemId", orderItem.getItemId());
            itemMap.put("bookId", orderItem.getBookId());
            itemMap.put("bookName", orderItem.getBookName());
            itemMap.put("bookPrice", orderItem.getBookPrice());
            itemMap.put("quantity", orderItem.getQuantity());
            itemList.add(itemMap);
        }

        order.setTotalAmount(totalAmount);
        purchaseOrderRepository.save(order);

        // Clear cart after checkout
        cartItemRepository.deleteByCartId(cartOpt.get().getCartId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("orderNo", order.getOrderNo());
        result.put("totalAmount", totalAmount);
        result.put("recipientName", order.getRecipientName());
        result.put("recipientPhone", order.getRecipientPhone());
        result.put("recipientAddress", order.getRecipientAddress());
        result.put("status", order.getStatus());
        result.put("statusText", "待支付");
        result.put("items", itemList);

        return ApiResponse.success("下单成功", result);
    }

    @Override
    @Transactional
    public ApiResponse<?> pay(Integer orderId, PayRequest request, Integer userId) {
        Optional<PurchaseOrder> opt = purchaseOrderRepository.findById(orderId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "订单不存在");
        }

        PurchaseOrder order = opt.get();
        if (!order.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权操作此订单");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            return ApiResponse.error(400, "订单状态不允许支付");
        }

        String payMethod = (request.getPayMethod() != null) ? request.getPayMethod() : "BALANCE";

        // Simulate payment - always success
        String transactionId = "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);

        Payment payment = new Payment();
        payment.setOrderId(order.getOrderId());
        payment.setOrderNo(order.getOrderNo());
        payment.setPayAmount(order.getTotalAmount());
        payment.setPayMethod(payMethod);
        payment.setTransactionId(transactionId);
        payment.setStatus("SUCCESS");
        paymentRepository.save(payment);

        order.setStatus("PENDING_REVIEW");
        order.setPayTime(LocalDateTime.now());
        purchaseOrderRepository.save(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("orderNo", order.getOrderNo());
        result.put("payAmount", order.getTotalAmount());
        result.put("transactionId", transactionId);
        result.put("status", order.getStatus());
        result.put("statusText", "待审核");
        result.put("payTime", order.getPayTime().toString());

        return ApiResponse.success("支付成功，等待管理员审核", result);
    }

    @Override
    public ApiResponse<?> getOrderDetail(Integer orderId) {
        Optional<PurchaseOrder> opt = purchaseOrderRepository.findById(orderId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "订单不存在");
        }

        PurchaseOrder order = opt.get();
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByOrderId(orderId);
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);

        Map<String, Object> result = buildOrderDetailMap(order, items, paymentOpt.orElse(null));
        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<?> getMyOrders(Integer userId) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findByUserIdOrderByCreateTimeDesc(userId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (PurchaseOrder order : orders) {
            order.setStatusText(STATUS_TEXT.getOrDefault(order.getStatus(), order.getStatus()));
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderId", order.getOrderId());
            map.put("orderNo", order.getOrderNo());
            map.put("totalAmount", order.getTotalAmount());
            map.put("status", order.getStatus());
            map.put("statusText", order.getStatusText());
            map.put("createTime", order.getCreateTime() != null ? order.getCreateTime().toString() : null);
            map.put("payTime", order.getPayTime() != null ? order.getPayTime().toString() : null);
            list.add(map);
        }
        return ApiResponse.success(list);
    }

    @Override
    public ApiResponse<?> getAllOrders() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllByOrderByCreateTimeDesc();
        List<Map<String, Object>> list = new ArrayList<>();
        for (PurchaseOrder order : orders) {
            order.setStatusText(STATUS_TEXT.getOrDefault(order.getStatus(), order.getStatus()));
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderId", order.getOrderId());
            map.put("orderNo", order.getOrderNo());
            map.put("userId", order.getUserId());
            map.put("userName", order.getUserName());
            map.put("totalAmount", order.getTotalAmount());
            map.put("recipientName", order.getRecipientName());
            map.put("recipientPhone", order.getRecipientPhone());
            map.put("recipientAddress", order.getRecipientAddress());
            map.put("status", order.getStatus());
            map.put("statusText", order.getStatusText());
            map.put("reviewRemark", order.getReviewRemark());
            map.put("createTime", order.getCreateTime() != null ? order.getCreateTime().toString() : null);
            map.put("payTime", order.getPayTime() != null ? order.getPayTime().toString() : null);
            map.put("reviewTime", order.getReviewTime() != null ? order.getReviewTime().toString() : null);
            map.put("shipTime", order.getShipTime() != null ? order.getShipTime().toString() : null);
            map.put("cancelTime", order.getCancelTime() != null ? order.getCancelTime().toString() : null);
            list.add(map);
        }
        return ApiResponse.success(list);
    }

    @Override
    @Transactional
    public ApiResponse<?> reviewOrder(Integer orderId, ReviewRequest request) {
        Optional<PurchaseOrder> opt = purchaseOrderRepository.findById(orderId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "订单不存在");
        }

        PurchaseOrder order = opt.get();
        if (!"PENDING_REVIEW".equals(order.getStatus())) {
            return ApiResponse.error(400, "当前订单状态不允许审核");
        }

        String action = request.getAction();
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            return ApiResponse.error(400, "审核动作必须为 APPROVE 或 REJECT");
        }

        if ("APPROVE".equals(action)) {
            order.setStatus("APPROVED");
        } else {
            order.setStatus("REJECTED");
            if (request.getRemark() != null && !request.getRemark().isBlank()) {
                order.setReviewRemark(request.getRemark());
            }
        }

        order.setReviewTime(LocalDateTime.now());
        purchaseOrderRepository.save(order);

        order.setStatusText(STATUS_TEXT.getOrDefault(order.getStatus(), order.getStatus()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("statusText", order.getStatusText());
        result.put("reviewRemark", order.getReviewRemark());
        result.put("reviewTime", order.getReviewTime().toString());

        String msg = "APPROVE".equals(action) ? "审核通过" : "审核已拒绝";
        return ApiResponse.success(msg, result);
    }

    @Override
    @Transactional
    public ApiResponse<?> shipOrder(Integer orderId) {
        Optional<PurchaseOrder> opt = purchaseOrderRepository.findById(orderId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "订单不存在");
        }

        PurchaseOrder order = opt.get();
        if (!"APPROVED".equals(order.getStatus())) {
            return ApiResponse.error(400, "只有审核通过的订单才能发货");
        }

        order.setStatus("SHIPPED");
        order.setShipTime(LocalDateTime.now());
        purchaseOrderRepository.save(order);

        order.setStatusText(STATUS_TEXT.getOrDefault(order.getStatus(), order.getStatus()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("statusText", order.getStatusText());
        result.put("shipTime", order.getShipTime().toString());

        return ApiResponse.success("发货成功", result);
    }

    @Override
    @Transactional
    public ApiResponse<?> cancelOrder(Integer orderId, Integer userId) {
        Optional<PurchaseOrder> opt = purchaseOrderRepository.findById(orderId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "订单不存在");
        }

        PurchaseOrder order = opt.get();
        if (!order.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权操作此订单");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus()) && !"PENDING_REVIEW".equals(order.getStatus())) {
            return ApiResponse.error(400, "当前订单状态不允许取消，仅待支付/待审核订单可取消");
        }

        order.setStatus("CANCELED");
        order.setCancelTime(LocalDateTime.now());
        purchaseOrderRepository.save(order);

        order.setStatusText(STATUS_TEXT.getOrDefault(order.getStatus(), order.getStatus()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("statusText", order.getStatusText());
        result.put("cancelTime", order.getCancelTime().toString());

        return ApiResponse.success("订单已取消", result);
    }

    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%06d", new Random().nextInt(1000000));
        return datePart + randomPart;
    }

    private Map<String, Object> buildOrderDetailMap(PurchaseOrder order, List<PurchaseOrderItem> items, Payment payment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("orderNo", order.getOrderNo());
        map.put("userId", order.getUserId());
        map.put("userName", order.getUserName());
        map.put("totalAmount", order.getTotalAmount());
        map.put("recipientName", order.getRecipientName());
        map.put("recipientPhone", order.getRecipientPhone());
        map.put("recipientAddress", order.getRecipientAddress());
        map.put("status", order.getStatus());
        map.put("statusText", STATUS_TEXT.getOrDefault(order.getStatus(), order.getStatus()));
        map.put("reviewRemark", order.getReviewRemark());
        map.put("createTime", order.getCreateTime() != null ? order.getCreateTime().toString() : null);
        map.put("payTime", order.getPayTime() != null ? order.getPayTime().toString() : null);
        map.put("reviewTime", order.getReviewTime() != null ? order.getReviewTime().toString() : null);
        map.put("shipTime", order.getShipTime() != null ? order.getShipTime().toString() : null);
        map.put("cancelTime", order.getCancelTime() != null ? order.getCancelTime().toString() : null);

        List<Map<String, Object>> itemList = new ArrayList<>();
        for (PurchaseOrderItem item : items) {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("itemId", item.getItemId());
            im.put("bookId", item.getBookId());
            im.put("bookName", item.getBookName());
            im.put("bookPrice", item.getBookPrice());
            im.put("quantity", item.getQuantity());
            im.put("subtotal", item.getBookPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            itemList.add(im);
        }
        map.put("items", itemList);

        if (payment != null) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("paymentId", payment.getPaymentId());
            pm.put("transactionId", payment.getTransactionId());
            pm.put("payAmount", payment.getPayAmount());
            pm.put("payMethod", payment.getPayMethod());
            pm.put("status", payment.getStatus());
            pm.put("payTime", payment.getPayTime() != null ? payment.getPayTime().toString() : null);
            map.put("payment", pm);
        }

        return map;
    }
}
