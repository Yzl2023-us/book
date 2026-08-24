package com.sping.message.controller;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.MessageRequest;
import com.sping.message.service.MessageService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public ApiResponse<?> sendMessage(@RequestBody MessageRequest request,
                                      @RequestHeader("X-User-Id") Integer userId) {
        return messageService.sendMessage(request, userId);
    }

    @GetMapping("/conversation/{receiverId}")
    public ApiResponse<?> getConversation(@RequestHeader("X-User-Id") Integer userId,
                                          @PathVariable("receiverId") Integer receiverId) {
        return messageService.getConversation(userId, receiverId);
    }

    @GetMapping("/my")
    public ApiResponse<?> getMyMessages(@RequestHeader("X-User-Id") Integer userId) {
        return messageService.getMyMessages(userId);
    }

    @GetMapping("/book/{bookId}")
    public ApiResponse<?> getBookMessages(@PathVariable("bookId") Integer bookId) {
        return messageService.getBookMessages(bookId);
    }

    @PutMapping("/{messageId}/read")
    public ApiResponse<?> markAsRead(@PathVariable("messageId") Integer messageId,
                                     @RequestHeader("X-User-Id") Integer userId) {
        return messageService.markAsRead(messageId, userId);
    }

    @GetMapping("/unread-count")
    public ApiResponse<?> getUnreadCount(@RequestHeader("X-User-Id") Integer userId) {
        return messageService.getUnreadCount(userId);
    }

    @PostMapping("/broadcast")
    public ApiResponse<?> broadcast(@RequestBody MessageRequest request,
                                    @RequestHeader("X-User-Id") Integer userId) {
        return messageService.broadcastAnnouncement(request.getContent(), userId);
    }

    @GetMapping("/announcements")
    public ApiResponse<?> getAnnouncements() {
        return messageService.getAnnouncements();
    }
}