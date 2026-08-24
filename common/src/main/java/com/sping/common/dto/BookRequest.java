package com.sping.common.dto;

import java.math.BigDecimal;

public class BookRequest {

    private String bookName;
    private String bookAuthor;
    private BigDecimal bookPrice;
    private Integer bookTypeId;
    private String bookDesc;
    private String bookImg;
    private Integer bookStock;

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
}