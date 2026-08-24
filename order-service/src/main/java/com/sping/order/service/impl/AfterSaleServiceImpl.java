package com.sping.order.service.impl;

import com.sping.common.dto.AfterSaleRequest;
import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.ReviewRequest;
import com.sping.common.entity.AfterSale;
import com.sping.common.entity.PurchaseOrder;
import com.sping.order.repository.AfterSaleRepository;
import com.sping.order.repository.PurchaseOrderRepository;
import com.sping.order.service.AfterSaleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AfterSaleServiceImpl implements AfterSaleService {

    private final AfterSaleRepository afterSaleRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    private static final Map<String, String> STATUS_TEXT = new LinkedHashMap<>();
    static {
        STATUS_TEXT.put("PENDING_REVIEW", "待审核");
        STATUS_TEXT.put("APPROVED",       "审核通过");
        STATUS_TEXT.put("RETURNED",       "已退货");
        STATUS_TEXT.put("REJECTED",       "审核拒绝");
        STATUS_TEXT.put("REFUNDED",       "已退款");
        STATUS_TEXT.put("CANCELED",       "已取消");
    }

    private static final Map<String, String> TYPE_TEXT = new LinkedHashMap<>();
    static {
        TYPE_TEXT.put("RETURN_REFUND", "退货退款");
        TYPE_TEXT.put("REFUND_ONLY",   "仅退款");
    }

    public AfterSaleServiceImpl(AfterSaleRepository afterSaleRepository,
                                PurchaseOrderRepository purchaseOrderRepository) {
        this.afterSaleRepository = afterSaleRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Override
    @Transactional
    public ApiResponse<?> apply(Integer userId, AfterSaleRequest request) {
        if (request.getOrderId() == null) {
            return ApiResponse.error(400, "订单ID不能为空");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            return ApiResponse.error(400, "售后类型不能为空");
        }
        if (!"RETURN_REFUND".equals(request.getType()) && !"REFUND_ONLY".equals(request.getType())) {
            return ApiResponse.error(400, "售后类型必须为 RETURN_REFUND 或 REFUND_ONLY");
        }
        if (request.getRefundAmount() == null || request.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error(400, "退款金额必须大于0");
        }

        // Validate the order
        Optional<PurchaseOrder> orderOpt = purchaseOrderRepository.findById(request.getOrderId());
        if (orderOpt.isEmpty()) {
            return ApiResponse.error(404, "订单不存在");
        }

        PurchaseOrder order = orderOpt.get();
        if (!order.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权操作此订单");
        }

        // Only allow after-sale for orders that have been paid and passed review
        List<String> allowedStatuses = Arrays.asList("APPROVED", "SHIPPED", "COMPLETED");
        if (!allowedStatuses.contains(order.getStatus())) {
            return ApiResponse.error(400, "当前订单状态不允许申请售后，仅在审核通过/已发货/已完成后可申请");
        }

        if (request.getRefundAmount().compareTo(order.getTotalAmount()) > 0) {
            return ApiResponse.error(400, "退款金额不能超过订单总额 ¥" + order.getTotalAmount());
        }

        // Check for existing active after-sale
        List<String> activeStatuses = Arrays.asList("PENDING_REVIEW", "APPROVED", "RETURNED");
        AfterSale existing = afterSaleRepository.findByOrderIdAndStatusIn(request.getOrderId(), activeStatuses);
        if (existing != null) {
            return ApiResponse.error(400, "该订单已有售后申请在处理中（待审核/审核通过/已退货），请勿重复提交");
        }

        AfterSale afterSale = new AfterSale();
        afterSale.setOrderId(order.getOrderId());
        afterSale.setOrderNo(order.getOrderNo());
        afterSale.setUserId(userId);
        afterSale.setUserName(order.getUserName());
        afterSale.setType(request.getType());
        afterSale.setReason(request.getReason());
        afterSale.setRefundAmount(request.getRefundAmount());
        afterSale.setStatus("PENDING_REVIEW");

        afterSaleRepository.save(afterSale);

        afterSale.setStatusText("待审核");
        afterSale.setTypeText(TYPE_TEXT.getOrDefault(afterSale.getType(), afterSale.getType()));

        Map<String, Object> result = buildDetailMap(afterSale);
        return ApiResponse.success("售后申请已提交，等待管理员审核", result);
    }

    @Override
    public ApiResponse<?> getMyAfterSales(Integer userId) {
        List<AfterSale> list = afterSaleRepository.findByUserIdOrderByCreateTimeDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AfterSale a : list) {
            a.setStatusText(STATUS_TEXT.getOrDefault(a.getStatus(), a.getStatus()));
            a.setTypeText(TYPE_TEXT.getOrDefault(a.getType(), a.getType()));
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("afterSaleId", a.getAfterSaleId());
            map.put("orderId", a.getOrderId());
            map.put("orderNo", a.getOrderNo());
            map.put("type", a.getType());
            map.put("typeText", a.getTypeText());
            map.put("reason", a.getReason());
            map.put("refundAmount", a.getRefundAmount());
            map.put("status", a.getStatus());
            map.put("statusText", a.getStatusText());
            map.put("createTime", a.getCreateTime() != null ? a.getCreateTime().toString() : null);
            result.add(map);
        }
        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<?> getDetail(Integer afterSaleId) {
        Optional<AfterSale> opt = afterSaleRepository.findById(afterSaleId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "售后单不存在");
        }
        AfterSale a = opt.get();
        a.setStatusText(STATUS_TEXT.getOrDefault(a.getStatus(), a.getStatus()));
        a.setTypeText(TYPE_TEXT.getOrDefault(a.getType(), a.getType()));
        return ApiResponse.success(buildDetailMap(a));
    }

    @Override
    @Transactional
    public ApiResponse<?> cancelAfterSale(Integer afterSaleId, Integer userId) {
        Optional<AfterSale> opt = afterSaleRepository.findById(afterSaleId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "售后单不存在");
        }

        AfterSale a = opt.get();
        if (!a.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权操作此售后单");
        }
        if (!"PENDING_REVIEW".equals(a.getStatus()) && !"APPROVED".equals(a.getStatus())) {
            return ApiResponse.error(400, "当前状态不允许取消，仅待审核/审核通过状态可取消");
        }

        a.setStatus("CANCELED");
        a.setCancelTime(LocalDateTime.now());
        afterSaleRepository.save(a);

        a.setStatusText("已取消");
        a.setTypeText(TYPE_TEXT.getOrDefault(a.getType(), a.getType()));

        return ApiResponse.success("售后申请已取消", buildDetailMap(a));
    }

    @Override
    public ApiResponse<?> getAllAfterSales() {
        List<AfterSale> list = afterSaleRepository.findAllByOrderByCreateTimeDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (AfterSale a : list) {
            a.setStatusText(STATUS_TEXT.getOrDefault(a.getStatus(), a.getStatus()));
            a.setTypeText(TYPE_TEXT.getOrDefault(a.getType(), a.getType()));
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("afterSaleId", a.getAfterSaleId());
            map.put("orderId", a.getOrderId());
            map.put("orderNo", a.getOrderNo());
            map.put("userId", a.getUserId());
            map.put("userName", a.getUserName());
            map.put("type", a.getType());
            map.put("typeText", a.getTypeText());
            map.put("reason", a.getReason());
            map.put("refundAmount", a.getRefundAmount());
            map.put("status", a.getStatus());
            map.put("statusText", a.getStatusText());
            map.put("adminRemark", a.getAdminRemark());
            map.put("createTime", a.getCreateTime() != null ? a.getCreateTime().toString() : null);
            map.put("reviewTime", a.getReviewTime() != null ? a.getReviewTime().toString() : null);
            map.put("returnTime", a.getReturnTime() != null ? a.getReturnTime().toString() : null);
            map.put("refundTime", a.getRefundTime() != null ? a.getRefundTime().toString() : null);
            map.put("cancelTime", a.getCancelTime() != null ? a.getCancelTime().toString() : null);
            result.add(map);
        }
        return ApiResponse.success(result);
    }

    @Override
    @Transactional
    public ApiResponse<?> review(Integer afterSaleId, ReviewRequest request) {
        Optional<AfterSale> opt = afterSaleRepository.findById(afterSaleId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "售后单不存在");
        }

        AfterSale a = opt.get();
        if (!"PENDING_REVIEW".equals(a.getStatus())) {
            return ApiResponse.error(400, "当前状态不允许审核");
        }

        String action = request.getAction();
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            return ApiResponse.error(400, "审核动作必须为 APPROVE 或 REJECT");
        }

        if ("APPROVE".equals(action)) {
            // For REFUND_ONLY, skip directly to REFUNDED
            if ("REFUND_ONLY".equals(a.getType())) {
                a.setStatus("REFUNDED");
                a.setRefundTime(LocalDateTime.now());
            } else {
                a.setStatus("APPROVED");
            }
        } else {
            a.setStatus("REJECTED");
            if (request.getRemark() != null && !request.getRemark().isBlank()) {
                a.setAdminRemark(request.getRemark());
            }
        }

        a.setReviewTime(LocalDateTime.now());
        afterSaleRepository.save(a);

        a.setStatusText(STATUS_TEXT.getOrDefault(a.getStatus(), a.getStatus()));
        a.setTypeText(TYPE_TEXT.getOrDefault(a.getType(), a.getType()));

        String msg = "APPROVE".equals(action) ? "审核通过" : "审核已拒绝";
        return ApiResponse.success(msg, buildDetailMap(a));
    }

    @Override
    @Transactional
    public ApiResponse<?> confirmReturn(Integer afterSaleId, Integer userId) {
        Optional<AfterSale> opt = afterSaleRepository.findById(afterSaleId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "售后单不存在");
        }

        AfterSale a = opt.get();
        if (!a.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权操作此售后单");
        }
        if (!"APPROVED".equals(a.getStatus())) {
            return ApiResponse.error(400, "仅在审核通过状态下可确认退货");
        }
        if (!"RETURN_REFUND".equals(a.getType())) {
            return ApiResponse.error(400, "仅退款类型的售后无需退货");
        }

        a.setStatus("RETURNED");
        a.setReturnTime(LocalDateTime.now());
        afterSaleRepository.save(a);

        a.setStatusText("已退货");
        a.setTypeText(TYPE_TEXT.getOrDefault(a.getType(), a.getType()));

        return ApiResponse.success("退货已确认，等待管理员退款", buildDetailMap(a));
    }

    @Override
    @Transactional
    public ApiResponse<?> refund(Integer afterSaleId) {
        Optional<AfterSale> opt = afterSaleRepository.findById(afterSaleId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "售后单不存在");
        }

        AfterSale a = opt.get();
        // For RETURN_REFUND, must be in RETURNED status
        // For REFUND_ONLY, this is handled directly in review() so this endpoint is for RETURN_REFUND
        if ("RETURN_REFUND".equals(a.getType()) && !"RETURNED".equals(a.getStatus())) {
            return ApiResponse.error(400, "请在用户退货后再进行退款操作");
        }

        a.setStatus("REFUNDED");
        a.setRefundTime(LocalDateTime.now());
        afterSaleRepository.save(a);

        a.setStatusText("已退款");
        a.setTypeText(TYPE_TEXT.getOrDefault(a.getType(), a.getType()));

        // Also update the order status to COMPLETED if it was SHIPPED
        Optional<PurchaseOrder> orderOpt = purchaseOrderRepository.findById(a.getOrderId());
        if (orderOpt.isPresent()) {
            PurchaseOrder order = orderOpt.get();
            if ("SHIPPED".equals(order.getStatus())) {
                order.setStatus("COMPLETED");
                order.setCompleteTime(LocalDateTime.now());
                purchaseOrderRepository.save(order);
            }
        }

        return ApiResponse.success("退款成功", buildDetailMap(a));
    }

    private Map<String, Object> buildDetailMap(AfterSale a) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("afterSaleId", a.getAfterSaleId());
        map.put("orderId", a.getOrderId());
        map.put("orderNo", a.getOrderNo());
        map.put("userId", a.getUserId());
        map.put("userName", a.getUserName());
        map.put("type", a.getType());
        map.put("typeText", a.getTypeText());
        map.put("reason", a.getReason());
        map.put("refundAmount", a.getRefundAmount());
        map.put("status", a.getStatus());
        map.put("statusText", a.getStatusText());
        map.put("adminRemark", a.getAdminRemark());
        map.put("createTime", a.getCreateTime() != null ? a.getCreateTime().toString() : null);
        map.put("reviewTime", a.getReviewTime() != null ? a.getReviewTime().toString() : null);
        map.put("returnTime", a.getReturnTime() != null ? a.getReturnTime().toString() : null);
        map.put("refundTime", a.getRefundTime() != null ? a.getRefundTime().toString() : null);
        map.put("cancelTime", a.getCancelTime() != null ? a.getCancelTime().toString() : null);
        return map;
    }
}
