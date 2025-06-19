package com.awbd.bookstore.exceptions.wishlist;


public class BookAlreadyInWishlistException extends RuntimeException {
    public BookAlreadyInWishlistException(String message) {
        super(message);
    }
}
