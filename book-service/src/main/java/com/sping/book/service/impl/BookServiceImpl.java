package com.sping.book.service.impl;

import com.sping.book.repository.BookRepository;
import com.sping.book.repository.BookTypeRepository;
import com.sping.book.service.BookService;
import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.BookRequest;
import com.sping.common.entity.Book;
import com.sping.common.entity.BookType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookTypeRepository bookTypeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${upload.path}")
    private String uploadPath;

    private static final String HOT_BOOKS_KEY = "hot_books";

    public BookServiceImpl(BookRepository bookRepository,
                           BookTypeRepository bookTypeRepository,
                           RedisTemplate<String, Object> redisTemplate) {
        this.bookRepository = bookRepository;
        this.bookTypeRepository = bookTypeRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 为图书列表填充分类名称
     */
    private void fillBookTypeNames(List<Book> books) {
        if (books == null || books.isEmpty()) return;
        Map<Integer, String> typeMap = bookTypeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(BookType::getBookTypeId, BookType::getBookTypeName));
        for (Book book : books) {
            String typeName = typeMap.get(book.getBookTypeId());
            book.setBookTypeName(typeName != null ? typeName : "未分类");
        }
    }

    /**
     * 为单个图书填充分类名称
     */
    private void fillBookTypeName(Book book) {
        if (book == null) return;
        fillBookTypeNames(Collections.singletonList(book));
    }

    @Override
    public ApiResponse<?> addBook(BookRequest request) {
        if (request.getBookName() == null || request.getBookName().isBlank()) {
            return ApiResponse.error(400, "书名不能为空");
        }
        if (request.getBookPrice() == null) {
            return ApiResponse.error(400, "价格不能为空");
        }

        Book book = new Book();
        book.setBookId(bookRepository.findMinAvailableBookId());
        book.setBookName(request.getBookName());
        book.setBookAuthor(request.getBookAuthor());
        book.setBookPrice(request.getBookPrice());
        book.setBookTypeId(request.getBookTypeId());
        book.setBookDesc(request.getBookDesc());
        book.setBookImg(request.getBookImg());
        if (request.getBookStock() != null) book.setBookStock(request.getBookStock());
        book.setStatus("APPROVED"); // 管理员添加直接通过

        bookRepository.save(book);
        clearHotCache();
        fillBookTypeName(book);
        return ApiResponse.success("添加成功", book);
    }

    @Override
    public ApiResponse<?> publishBook(BookRequest request, Integer userId) {
        if (request.getBookName() == null || request.getBookName().isBlank()) {
            return ApiResponse.error(400, "书名不能为空");
        }
        if (request.getBookPrice() == null) {
            return ApiResponse.error(400, "价格不能为空");
        }

        Book book = new Book();
        book.setBookId(bookRepository.findMinAvailableBookId());
        book.setBookName(request.getBookName());
        book.setBookAuthor(request.getBookAuthor());
        book.setBookPrice(request.getBookPrice());
        book.setBookTypeId(request.getBookTypeId());
        book.setBookDesc(request.getBookDesc());
        book.setBookImg(request.getBookImg());
        if (request.getBookStock() != null) book.setBookStock(request.getBookStock());
        book.setSellerId(userId);
        book.setStatus("PENDING_REVIEW"); // 用户发布需审核

        bookRepository.save(book);
        fillBookTypeName(book);
        return ApiResponse.success("发布成功，请等待管理员审核", book);
    }

    @Override
    public ApiResponse<?> update(Integer bookId, BookRequest request) {
        Optional<Book> opt = bookRepository.findById(bookId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "图书不存在");
        }
        Book book = opt.get();

        if (request.getBookName() != null) book.setBookName(request.getBookName());
        if (request.getBookAuthor() != null) book.setBookAuthor(request.getBookAuthor());
        if (request.getBookPrice() != null) book.setBookPrice(request.getBookPrice());
        if (request.getBookTypeId() != null) book.setBookTypeId(request.getBookTypeId());
        if (request.getBookDesc() != null) book.setBookDesc(request.getBookDesc());
        if (request.getBookImg() != null) book.setBookImg(request.getBookImg());
        if (request.getBookStock() != null) book.setBookStock(request.getBookStock());

        bookRepository.save(book);
        fillBookTypeName(book);
        return ApiResponse.success("更新成功", book);
    }

    @Override
    public ApiResponse<?> delete(Integer bookId) {
        Optional<Book> opt = bookRepository.findById(bookId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "图书不存在");
        }
        bookRepository.deleteById(bookId);
        clearHotCache();
        return ApiResponse.success("删除成功", null);
    }

    @Override
    public ApiResponse<?> getById(Integer bookId) {
        Optional<Book> opt = bookRepository.findById(bookId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "图书不存在");
        }
        Book book = opt.get();
        fillBookTypeName(book);
        return ApiResponse.success(book);
    }

    @Override
    public ApiResponse<?> search(String keyword, Integer bookTypeId, Pageable pageable) {
        String status = "APPROVED"; // 前台搜索仅返回已审核通过的图书
        Page<Book> page;
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "bookId"));

        if (keyword != null && !keyword.isBlank() && bookTypeId != null) {
            page = bookRepository.searchByKeywordAndTypeAndStatus(keyword, bookTypeId, status, sorted);
        } else if (keyword != null && !keyword.isBlank()) {
            page = bookRepository.searchByKeywordAndStatus(keyword, status, sorted);
        } else if (bookTypeId != null) {
            page = bookRepository.findByBookTypeIdAndStatus(bookTypeId, status, sorted);
        } else {
            page = bookRepository.findByStatus(status, sorted);
        }

        List<Book> content = page.getContent();
        fillBookTypeNames(content);

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("number", page.getNumber());
        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<?> adminSearch(String keyword, Integer bookTypeId, Pageable pageable) {
        Page<Book> page;
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "bookId"));

        if (keyword != null && !keyword.isBlank() && bookTypeId != null) {
            page = bookRepository.searchByKeywordAndType(keyword, bookTypeId, sorted);
        } else if (keyword != null && !keyword.isBlank()) {
            page = bookRepository.searchByKeyword(keyword, sorted);
        } else if (bookTypeId != null) {
            page = bookRepository.findByBookTypeId(bookTypeId, sorted);
        } else {
            page = bookRepository.findAll(sorted);
        }

        List<Book> content = page.getContent();
        fillBookTypeNames(content);

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("number", page.getNumber());
        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<?> getHotBooks() {
        try {
            @SuppressWarnings("unchecked")
            List<Book> cached = (List<Book>) redisTemplate.opsForValue().get(HOT_BOOKS_KEY);
            if (cached != null) {
                fillBookTypeNames(cached);
                return ApiResponse.success(cached);
            }
        } catch (Exception ignored) {
            // Redis 不可用时直接查数据库
        }

        List<Book> hotBooks = bookRepository.findTop10ByStatusOrderByBookIdDesc("APPROVED");
        fillBookTypeNames(hotBooks);
        try {
            redisTemplate.opsForValue().set(HOT_BOOKS_KEY, hotBooks, 10, TimeUnit.MINUTES);
        } catch (Exception ignored) {
            // Redis 不可用时不影响返回结果
        }
        return ApiResponse.success(hotBooks);
    }

    @Override
    public ApiResponse<?> getBookTypes() {
        List<BookType> types = bookTypeRepository.findAll();
        return ApiResponse.success(types);
    }

    @Override
    public ApiResponse<?> reviewBook(Integer bookId, String status) {
        Optional<Book> opt = bookRepository.findById(bookId);
        if (opt.isEmpty()) {
            return ApiResponse.error(404, "图书不存在");
        }
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            return ApiResponse.error(400, "审核状态无效，仅支持 APPROVED 或 REJECTED");
        }
        Book book = opt.get();
        book.setStatus(status);
        bookRepository.save(book);
        clearHotCache();
        String msg = "APPROVED".equals(status) ? "审核通过" : "已拒绝";
        return ApiResponse.success(msg, null);
    }

    @Override
    public ApiResponse<?> getPendingBooks(Pageable pageable) {
        Page<Book> page = bookRepository.findByStatus("PENDING_REVIEW", pageable);
        fillBookTypeNames(page.getContent());
        Map<String, Object> result = new HashMap<>();
        result.put("content", page.getContent());
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("number", page.getNumber());
        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<?> getMyPublished(Integer userId, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "bookId"));
        Page<Book> page = bookRepository.findBySellerId(userId, sorted);
        fillBookTypeNames(page.getContent());
        Map<String, Object> result = new HashMap<>();
        result.put("content", page.getContent());
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("number", page.getNumber());
        return ApiResponse.success(result);
    }

    private void clearHotCache() {
        try {
            redisTemplate.delete(HOT_BOOKS_KEY);
        } catch (Exception ignored) {
            // Redis 不可用时不影响
        }
    }

    @Override
    public ApiResponse<?> uploadCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ApiResponse.error(400, "文件名不能为空");
        }

        // 校验文件类型
        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            ext = originalFilename.substring(dotIndex).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)$")) {
            return ApiResponse.error(400, "仅支持 jpg、jpeg、png、gif、webp、bmp 格式的图片");
        }

        try {
            // 生成唯一文件名
            String newFilename = UUID.randomUUID().toString() + ext;

            // 使用配置的绝对路径创建上传目录
            Path uploadDir = Paths.get(uploadPath, "books");
            Files.createDirectories(uploadDir);

            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            file.transferTo(filePath.toFile());

            // 返回访问路径
            String url = "/api/book/cover/" + newFilename;
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return ApiResponse.success("上传成功", data);
        } catch (IOException e) {
            return ApiResponse.error(500, "图片上传失败: " + e.getMessage());
        }
    }
}