package com.awbd.bookstore.exceptions.cart;

public class EmptyCartException  extends RuntimeException{
    public EmptyCartException(String message) {
        super(message);
    }
}
