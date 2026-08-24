package com.sping.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "book_info")
public class Book {

    @Id
    @Column(name = "bookId")
    private Integer bookId;

    @Column(name = "bookName", nullable = false, length = 50)
    private String bookName;

    @Column(name = "bookAuthor", nullable = false, length = 50)
    private String bookAuthor;

    @Column(name = "bookPrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal bookPrice;

    @Column(name = "bookTypeId", nullable = false)
    private Integer bookTypeId;

    @Column(name = "bookDesc", nullable = false, length = 255)
    private String bookDesc;

    @Column(name = "bookImg", length = 255)
    private String bookImg;

    @Column(name = "bookStock", nullable = false)
    private Integer bookStock = 0;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "APPROVED";

    @Transient
    private String bookTypeName;

    public Book() {
    }

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }
    public BigDecimal getBookPrice() { return bookPrice; }
    public void setBookPrice(BigDecimal bookPrice) { this.bookPrice = bookPrice; }
    public Integer getBookTypeId() { return bookTypeId; }
    public void setBookTypeId(Integer bookTypeId) { this.bookTypeId = bookTypeId; }
    public String getBookDesc() { return bookDesc; }
    public void setBookDesc(String bookDesc) { this.bookDesc = bookDesc; }
    public String getBookImg() { return bookImg; }
    public void setBookImg(String bookImg) { this.bookImg = bookImg; }
    public Integer getBookStock() { return bookStock; }
    public void setBookStock(Integer bookStock) { this.bookStock = bookStock; }
    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBookTypeName() { return bookTypeName; }
    public void setBookTypeName(String bookTypeName) { this.bookTypeName = bookTypeName; }
}