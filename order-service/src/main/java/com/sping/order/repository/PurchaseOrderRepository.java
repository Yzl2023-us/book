package com.sping.order.repository;

import com.sping.common.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    List<PurchaseOrder> findByUserIdOrderByCreateTimeDesc(Integer userId);

    List<PurchaseOrder> findAllByOrderByCreateTimeDesc();

    List<PurchaseOrder> findByStatusOrderByCreateTimeDesc(String status);

    List<PurchaseOrder> findByStatusInOrderByCreateTimeDesc(List<String> statuses);
}
