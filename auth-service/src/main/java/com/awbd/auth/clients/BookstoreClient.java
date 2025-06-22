// src/main/java/com/awbd/auth/clients/BookstoreClient.java
package com.awbd.auth.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

//@FeignClient(name = "bookstore-service")
@FeignClient(name = "bookstore-service", url = "${bookstore.service.url:http://localhost:8082}")
public interface BookstoreClient {

    @PostMapping("/api/cart/create")
    String createCart(@RequestParam Long userId);

    @PostMapping("/api/wishlists/create")
    String createWishlist(@RequestParam Long userId);
}