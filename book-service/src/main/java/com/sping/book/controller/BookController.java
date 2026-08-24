package com.sping.book.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.sping.book.service.BookService;
import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.BookRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/book")
public class BookController {

    private final BookService bookService;

    @Value("${upload.path}")
    private String uploadPath;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ===== 用户发布图书 =====
    @PostMapping("/publish")
    public ApiResponse<?> publishBook(@RequestBody BookRequest request,
                                      @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }
        return bookService.publishBook(request, userId);
    }

    // ===== 用户查看自己的发布 =====
    @GetMapping("/my-published")
    public ApiResponse<?> getMyPublished(@RequestHeader(value = "X-User-Id", required = false) Integer userId,
                                          Pageable pageable) {
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }
        return bookService.getMyPublished(userId, pageable);
    }

    // ===== 管理员查看待审核列表 =====
    @GetMapping("/pending")
    public ApiResponse<?> getPendingBooks(Pageable pageable) {
        return bookService.getPendingBooks(pageable);
    }

    // ===== 管理员审核 =====
    @PutMapping("/{bookId}/review")
    public ApiResponse<?> reviewBook(@PathVariable("bookId") Integer bookId,
                                     @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return bookService.reviewBook(bookId, status);
    }

    @PostMapping("/add")
    @SentinelResource(value = "addBook", blockHandler = "addBookBlockHandler")
    public ApiResponse<?> addBook(@RequestBody BookRequest request) {
        return bookService.addBook(request);
    }

    public ApiResponse<?> addBookBlockHandler(BookRequest request,
                                              com.alibaba.csp.sentinel.slots.block.BlockException e) {
        return ApiResponse.error(429, "添加过于频繁，请稍后再试");
    }

    @PutMapping("/{bookId}")
    public ApiResponse<?> update(@PathVariable("bookId") Integer bookId,
                                 @RequestBody BookRequest request) {
        return bookService.update(bookId, request);
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<?> delete(@PathVariable("bookId") Integer bookId) {
        return bookService.delete(bookId);
    }

    @GetMapping("/{bookId}")
    public ApiResponse<?> getById(@PathVariable("bookId") Integer bookId) {
        return bookService.getById(bookId);
    }

    @GetMapping("/search")
    @SentinelResource(value = "searchBook", blockHandler = "searchBlockHandler")
    public ApiResponse<?> search(@RequestParam(name = "keyword", required = false) String keyword,
                                 @RequestParam(name = "bookTypeId", required = false) Integer bookTypeId,
                                 Pageable pageable) {
        return bookService.search(keyword, bookTypeId, pageable);
    }

    // ===== 管理员查看所有图书（含审核中/已拒绝）=====
    @GetMapping("/admin/all")
    public ApiResponse<?> adminSearch(@RequestParam(name = "keyword", required = false) String keyword,
                                       @RequestParam(name = "bookTypeId", required = false) Integer bookTypeId,
                                       Pageable pageable) {
        return bookService.adminSearch(keyword, bookTypeId, pageable);
    }

    public ApiResponse<?> searchBlockHandler(String keyword, Integer bookTypeId, Pageable pageable,
                                             com.alibaba.csp.sentinel.slots.block.BlockException e) {
        return ApiResponse.error(429, "搜索请求过多，请稍后再试");
    }

    @GetMapping("/hot")
    public ApiResponse<?> getHotBooks() {
        return bookService.getHotBooks();
    }

    @GetMapping("/types")
    public ApiResponse<?> getBookTypes() {
        return bookService.getBookTypes();
    }

    @PostMapping("/upload-cover")
    public ApiResponse<?> uploadCover(@RequestParam("file") MultipartFile file) {
        return bookService.uploadCover(file);
    }

    @GetMapping("/cover/{filename}")
    public ResponseEntity<Resource> getCover(@PathVariable("filename") String filename) {
        Path filePath = Paths.get(uploadPath, "books", filename);
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        // 根据扩展名设置 Content-Type
        String contentType = "image/jpeg";
        String name = filename.toLowerCase();
        if (name.endsWith(".png")) contentType = "image/png";
        else if (name.endsWith(".gif")) contentType = "image/gif";
        else if (name.endsWith(".webp")) contentType = "image/webp";
        else if (name.endsWith(".bmp")) contentType = "image/bmp";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }
}