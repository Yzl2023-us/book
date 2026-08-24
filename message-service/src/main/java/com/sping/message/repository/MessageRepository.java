package com.sping.message.repository;

import com.sping.common.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findBySenderIdAndReceiverIdOrderByCreateTimeAsc(Integer senderId, Integer receiverId);

    List<Message> findByReceiverIdOrderByCreateTimeDesc(Integer receiverId);

    List<Message> findByBookIdOrderByCreateTimeAsc(Integer bookId);

    long countByReceiverIdAndIsRead(Integer receiverId, Integer isRead);

    List<Message> findByReceiverIdAndBookIdIsNullOrderByCreateTimeDesc(Integer receiverId);
}