package com.awbd.bookstore.exceptions.cart;

public class CartNotFoundException  extends RuntimeException{
    public CartNotFoundException(String message) {
        super(message);
    }
}
