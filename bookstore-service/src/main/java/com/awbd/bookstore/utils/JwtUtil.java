package com.awbd.bookstore.utils;

import com.awbd.bookstore.enums.Role; // SCHIMBĂ IMPORTUL
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static String SECRET_KEY = "mySecretKeyForJWTThatIsAtLeast256BitsLongForHS256AlgorithmSecurity";
    private static long TOKEN_VALIDITY = 1000 * 60 * 60 * 10; // 10 hours
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    // Această metodă nu mai e necesară în bookstore-service
    // Doar auth-service generează token-uri
    public String generateToken(String username, Role role) {
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

    public Role getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();

            System.out.println("claims: " + claims);

            // Convertește la Role din bookstore-service
            return Role.valueOf(claims.get("role").toString());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            logger.info("=== Token Validation Debug ===");
            logger.info("Token to validate: {}", token.substring(0, 20) + "...");
            logger.info("SECRET_KEY used: {}", SECRET_KEY.substring(0, 20) + "...");

            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();

            logger.info("Token validation SUCCESS");
            logger.info("Claims: {}", claims);
            logger.info("Subject: {}", claims.getSubject());
            logger.info("Role: {}", claims.get("role"));
            logger.info("Issued at: {}", claims.getIssuedAt());
            logger.info("Expires at: {}", claims.getExpiration());

            return true;
        } catch (Exception e) {
            logger.error("Token validation FAILED: {}", e.getMessage());
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                logger.error("Cause: {}", e.getCause().getMessage());
            }
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