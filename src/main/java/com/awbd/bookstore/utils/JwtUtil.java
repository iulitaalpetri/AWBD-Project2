package com.awbd.bookstore.utils;

import com.awbd.bookstore.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static String SECRET_KEY = "your-secret-key";
    private static long TOKEN_VALIDITY = 1000 * 60 * 60 * 10; // 10 hours
    @Autowired
    private TokenBlacklistService tokenBlacklistService;



    public String generateToken(String username, User.Role role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
    public User.Role getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();

            System.out.println("claims: " + claims);

            return User.Role.valueOf(claims.get("role").toString());
        } catch (Exception e) {
            return null;
        }
    }


    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);

            System.out.println("token: " + token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistService.esteInvalid(token);
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }


}