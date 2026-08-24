package com.sping.message.service.impl;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.MessageRequest;
import com.sping.common.entity.Message;
import com.sping.message.config.BookFeignClient;
import com.sping.message.config.UserFeignClient;
import com.sping.message.repository.MessageRepository;
import com.sping.message.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserFeignClient userFeignClient;
    private final BookFeignClient bookFeignClient;

    public MessageServiceImpl(MessageRepository messageRepository,
                              UserFeignClient userFeignClient,
                              BookFeignClient bookFeignClient) {
        this.messageRepository = messageRepository;
        this.userFeignClient = userFeignClient;
        this.bookFeignClient = bookFeignClient;
    }

    @Override
    public ApiResponse<?> sendMessage(MessageRequest request, Integer senderId) {
        if (request.getReceiverId() == null) {
            return ApiResponse.error(400, "接收者ID不能为空");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            return ApiResponse.error(400, "消息内容不能为空");
        }
        if (request.getReceiverId().equals(senderId)) {
            return ApiResponse.error(400, "不能给自己发消息");
        }

        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(request.getReceiverId());
        message.setBookId(request.getBookId());
        message.setContent(request.getContent());

        // 冗余写入用户名
        message.setUserName(fetchUserName(senderId));

        // 冗余写入书名
        if (request.getBookId() != null) {
            message.setBookName(fetchBookName(request.getBookId()));
        }

        messageRepository.save(message);
        message.setSenderName(message.getUserName());
        return ApiResponse.success("发送成功", message);
    }

    @Override
    public ApiResponse<?> getConversation(Integer senderId, Integer receiverId) {
        List<Message> messages = messageRepository
                .findBySenderIdAndReceiverIdOrderByCreateTimeAsc(senderId, receiverId);
        messages.forEach(this::enrichSenderName);
        return ApiResponse.success(messages);
    }

    @Override
    public ApiResponse<?> getMyMessages(Integer userId) {
        List<Message> messages = messageRepository.findByReceiverIdOrderByCreateTimeDesc(userId);
        messages.forEach(this::enrichSenderName);
        return ApiResponse.success(messages);
    }

    @Override
    public ApiResponse<?> getBookMessages(Integer bookId) {
        List<Message> messages = messageRepository.findByBookIdOrderByCreateTimeAsc(bookId);
        messages.forEach(this::enrichSenderName);
        return ApiResponse.success(messages);
    }

    @Override
    public ApiResponse<?> markAsRead(Integer messageId, Integer userId) {
        Optional<Message> opt = messageRepository.findById(messageId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "消息不存在");
        }
        Message message = opt.get();
        if (!message.getReceiverId().equals(userId)) {
            return ApiResponse.error(403, "无权操作");
        }
        message.setIsRead(1);
        messageRepository.save(message);
        return ApiResponse.success("已读", null);
    }

    @Override
    public ApiResponse<?> getUnreadCount(Integer userId) {
        long count = messageRepository.countByReceiverIdAndIsRead(userId, 0);
        return ApiResponse.success(count);
    }

    @Override
    public ApiResponse<?> broadcastAnnouncement(String content, Integer adminId) {
        if (content == null || content.isBlank()) {
            return ApiResponse.error(400, "公告内容不能为空");
        }

        // Verify admin
        try {
            ApiResponse<?> resp = userFeignClient.getUserInfo(adminId);
            if (resp.getCode() == 200 && resp.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userMap = (Map<String, Object>) resp.getData();
                Object isAdminObj = userMap.get("isAdmin");
                boolean isAdmin = isAdminObj instanceof Boolean
                        ? (Boolean) isAdminObj
                        : isAdminObj instanceof Number && ((Number) isAdminObj).intValue() == 1;
                if (!isAdmin) {
                    return ApiResponse.error(403, "仅管理员可以发布公告");
                }
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "验证管理员身份失败");
        }

        Message announcement = new Message();
        announcement.setSenderId(adminId);
        announcement.setReceiverId(0);       // 0 = 系统公告
        announcement.setBookId(null);        // null = 非图书留言
        announcement.setContent(content);
        announcement.setUserName(fetchUserName(adminId));

        messageRepository.save(announcement);
        announcement.setSenderName(announcement.getUserName());
        return ApiResponse.success("公告已发布", announcement);
    }

    @Override
    public ApiResponse<?> getAnnouncements() {
        List<Message> announcements = messageRepository
                .findByReceiverIdAndBookIdIsNullOrderByCreateTimeDesc(0);
        announcements.forEach(this::enrichSenderName);
        return ApiResponse.success(announcements);
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

    private String fetchBookName(Integer bookId) {
        try {
            ApiResponse<?> resp = bookFeignClient.getBookById(bookId);
            if (resp.getCode() == 200 && resp.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bookMap = (Map<String, Object>) resp.getData();
                return (String) bookMap.get("bookName");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void enrichSenderName(Message message) {
        // 优先使用冗余列，为空时通过Feign获取
        if (message.getUserName() != null && !message.getUserName().isEmpty()) {
            message.setSenderName(message.getUserName());
            return;
        }
        try {
            ApiResponse<?> resp = userFeignClient.getUserInfo(message.getSenderId());
            if (resp.getCode() == 200 && resp.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userMap = (Map<String, Object>) resp.getData();
                String name = (String) userMap.get("userName");
                message.setSenderName(name);
                message.setUserName(name);
            }
        } catch (Exception ignored) {
        }
    }
}