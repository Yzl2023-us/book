package com.sping.order.repository;

import com.sping.common.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUserIdOrderByBorrowTimeDesc(Integer userId);

    List<Order> findByBookId(Integer bookId);

    List<Order> findByBookIdAndReturnTimeIsNull(Integer bookId);
}