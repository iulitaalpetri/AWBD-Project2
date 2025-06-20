package com.awbd.bookstore.exceptions.cart;

public class BookAlreadyInCartException  extends RuntimeException{
    public BookAlreadyInCartException(String message) {
        super(message);
    }
}
