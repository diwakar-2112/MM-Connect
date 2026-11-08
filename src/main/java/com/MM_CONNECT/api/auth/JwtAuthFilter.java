package com.MM_CONNECT.api.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService tokenProcessor;

    public JwtAuthFilter(JwtService tokenProcessor) {
        this.tokenProcessor = tokenProcessor;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Check if token exists in the header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 2. Extract and verify the token
            final String rawToken = authHeader.substring(7);
            DecodedJWT verifiedJwt = tokenProcessor.verifyToken(rawToken);

            // 3. Extract user info
            UUID userId = tokenProcessor.extractUserId(verifiedJwt);
            String role = verifiedJwt.getClaim("role").asString();

            // 4. Create the security session
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, // We store the User UUID as the "principal"
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(role))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 5. Tell Spring Security this request is officially authenticated
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception ex) {
            // If token is expired or fake, reject quietly (Context remains empty, leading to 401)
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}