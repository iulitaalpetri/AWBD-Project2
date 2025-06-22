package com.awbd.bookstore.utils;

import com.awbd.bookstore.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        logger.info("=== JWT Filter Debug ===");
        logger.info("Authorization header: {}", authorizationHeader);
        logger.info("Request URL: {}", request.getRequestURL());

        String username = null;
        String jwt = null;
        Role role = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            logger.info("JWT token extracted: {}", jwt.substring(0, 20) + "...");

            if (tokenBlacklistService.esteInvalid(jwt)) {
                logger.warn("Token găsit în lista neagră");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            try {
                if (jwtUtil.validateToken(jwt)) {
                    username = jwtUtil.getUsernameFromToken(jwt);
                    role = jwtUtil.getRoleFromToken(jwt);
                    logger.info("Token valid - User: {}, Role: {}", username, role);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Authentication set successfully");
                } else {
                    logger.error("Token validation failed");
                }
            } catch (Exception e) {
                logger.error("JWT validation error: {}", e.getMessage());
            }
        } else {
            logger.info("No Authorization header found");
        }

        filterChain.doFilter(request, response);
    }
}