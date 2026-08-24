package com.sping.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "book_type")
public class BookType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookTypeId")
    private Integer bookTypeId;

    @Column(name = "bookTypeName", nullable = false, length = 20)
    private String bookTypeName;

    @Column(name = "bookTypeDesc", nullable = false, length = 255)
    private String bookTypeDesc;

    public BookType() {
    }

    public Integer getBookTypeId() { return bookTypeId; }
    public void setBookTypeId(Integer bookTypeId) { this.bookTypeId = bookTypeId; }
    public String getBookTypeName() { return bookTypeName; }
    public void setBookTypeName(String bookTypeName) { this.bookTypeName = bookTypeName; }
    public String getBookTypeDesc() { return bookTypeDesc; }
    public void setBookTypeDesc(String bookTypeDesc) { this.bookTypeDesc = bookTypeDesc; }
}