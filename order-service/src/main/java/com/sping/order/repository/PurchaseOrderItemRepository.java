package com.sping.order.repository;

import com.sping.common.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Integer> {

    List<PurchaseOrderItem> findByOrderId(Integer orderId);

    void deleteByOrderId(Integer orderId);
}
