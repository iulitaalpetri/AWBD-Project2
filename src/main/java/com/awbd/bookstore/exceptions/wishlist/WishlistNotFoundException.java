package com.awbd.bookstore.exceptions.wishlist;


public class WishlistNotFoundException extends RuntimeException {
    public WishlistNotFoundException(String message) {
        super(message);
    }
}
