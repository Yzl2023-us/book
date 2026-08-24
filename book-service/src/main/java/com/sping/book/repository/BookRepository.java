package com.sping.book.repository;

import com.sping.common.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    // ===== 状态过滤查询（仅展示已审核通过的图书）=====
    Page<Book> findByBookTypeIdAndStatus(Integer bookTypeId, String status, Pageable pageable);

    Page<Book> findByStatus(String status, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.status = :status AND (b.bookName LIKE %:keyword% OR b.bookAuthor LIKE %:keyword% OR b.bookDesc LIKE %:keyword%)")
    Page<Book> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.bookTypeId = :bookTypeId AND b.status = :status AND (b.bookName LIKE %:keyword% OR b.bookAuthor LIKE %:keyword% OR b.bookDesc LIKE %:keyword%)")
    Page<Book> searchByKeywordAndTypeAndStatus(@Param("keyword") String keyword, @Param("bookTypeId") Integer bookTypeId, @Param("status") String status, Pageable pageable);

    List<Book> findTop10ByStatusOrderByBookIdDesc(String status);

    // ===== 原有查询（保持向后兼容）=====
    Page<Book> findByBookTypeId(Integer bookTypeId, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.bookName LIKE %:keyword% OR b.bookAuthor LIKE %:keyword% OR b.bookDesc LIKE %:keyword%")
    Page<Book> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.bookTypeId = :bookTypeId AND (b.bookName LIKE %:keyword% OR b.bookAuthor LIKE %:keyword% OR b.bookDesc LIKE %:keyword%)")
    Page<Book> searchByKeywordAndType(@Param("keyword") String keyword, @Param("bookTypeId") Integer bookTypeId, Pageable pageable);

    List<Book> findTop10ByOrderByBookIdDesc();

    // ===== 发布者查询 =====
    Page<Book> findBySellerId(Integer sellerId, Pageable pageable);

    // ===== ID 分配 =====
    @Query(value = "SELECT MIN(next_id) FROM (" +
            "SELECT 1 AS next_id WHERE NOT EXISTS (SELECT 1 FROM book_info WHERE bookId = 1) " +
            "UNION ALL " +
            "SELECT t1.bookId + 1 AS next_id FROM book_info t1 " +
            "WHERE t1.bookId + 1 NOT IN (SELECT bookId FROM book_info)" +
            ") t", nativeQuery = true)
    Integer findMinAvailableBookId();
}