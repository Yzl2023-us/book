package com.sping.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "borrow")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "borrowId")
    private Integer borrowId;

    @Column(name = "userId", nullable = false)
    private Integer userId;

    @Column(name = "bookId", nullable = false)
    private Integer bookId;

    @Column(name = "borrowTime", nullable = false)
    private LocalDateTime borrowTime;

    @Column(name = "returnTime")
    private LocalDateTime returnTime;

    @Transient
    private String bookName;
    @Transient
    private String userName;

    public Order() {
    }

    @PrePersist
    protected void onCreate() {
        borrowTime = LocalDateTime.now();
    }

    public Integer getBorrowId() { return borrowId; }
    public void setBorrowId(Integer borrowId) { this.borrowId = borrowId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }
    public LocalDateTime getBorrowTime() { return borrowTime; }
    public void setBorrowTime(LocalDateTime borrowTime) { this.borrowTime = borrowTime; }
    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}