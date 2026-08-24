package com.sping.book.service;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.BookRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface BookService {

    // 管理员直接添加（自动 APPROVED）
    ApiResponse<?> addBook(BookRequest request);

    // 普通用户发布（PENDING_REVIEW）
    ApiResponse<?> publishBook(BookRequest request, Integer userId);

    ApiResponse<?> update(Integer bookId, BookRequest request);

    ApiResponse<?> delete(Integer bookId);

    ApiResponse<?> getById(Integer bookId);

    // 前台搜索（仅返回 APPROVED）
    ApiResponse<?> search(String keyword, Integer bookTypeId, Pageable pageable);

    // 热门图书（仅返回 APPROVED）
    ApiResponse<?> getHotBooks();

    ApiResponse<?> getBookTypes();

    ApiResponse<?> uploadCover(MultipartFile file);

    // 管理员审核（APPROVED/REJECTED）
    ApiResponse<?> reviewBook(Integer bookId, String status);

    // 管理员查看待审核列表
    ApiResponse<?> getPendingBooks(Pageable pageable);

    // 用户查看自己的发布
    ApiResponse<?> getMyPublished(Integer userId, Pageable pageable);
    // 管理员查看所有图书（不过滤状态）
    ApiResponse<?> adminSearch(String keyword, Integer bookTypeId, Pageable pageable);
}