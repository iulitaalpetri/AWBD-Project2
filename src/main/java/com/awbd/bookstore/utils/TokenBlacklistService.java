package com.awbd.bookstore.utils;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private Set<String> tokenInvalidate = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void invalidateToken(String token) {
        tokenInvalidate.add(token);
    }

    public boolean esteInvalid(String token) {
        return tokenInvalidate.contains(token);
    }
    public Set<String> getBlacklist() {
        return tokenInvalidate;
    }
}