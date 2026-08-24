package com.sping.order.repository;

import com.sping.common.entity.AfterSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AfterSaleRepository extends JpaRepository<AfterSale, Integer> {

    List<AfterSale> findByUserIdOrderByCreateTimeDesc(Integer userId);

    List<AfterSale> findAllByOrderByCreateTimeDesc();

    List<AfterSale> findByStatusInOrderByCreateTimeDesc(List<String> statuses);

    List<AfterSale> findByOrderId(Integer orderId);

    AfterSale findByOrderIdAndStatusIn(Integer orderId, List<String> statuses);
}
