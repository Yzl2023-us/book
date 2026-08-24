package com.sping.message.service;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.MessageRequest;

public interface MessageService {

    ApiResponse<?> sendMessage(MessageRequest request, Integer senderId);

    ApiResponse<?> getConversation(Integer senderId, Integer receiverId);

    ApiResponse<?> getMyMessages(Integer userId);

    ApiResponse<?> getBookMessages(Integer bookId);

    ApiResponse<?> markAsRead(Integer messageId, Integer userId);

    ApiResponse<?> getUnreadCount(Integer userId);

    ApiResponse<?> broadcastAnnouncement(String content, Integer adminId);

    ApiResponse<?> getAnnouncements();
}