package com.MM_CONNECT.api.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.dao.DataAccessException;
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
    private final UserRepository users;

    public JwtAuthFilter(JwtService tokenProcessor, UserRepository users) {
        this.tokenProcessor = tokenProcessor;
        this.users = users;
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
            String tokenType = verifiedJwt.getClaim("token_type").asString();
            // Legacy access tokens carry a role. Legacy refresh tokens do not.
            if (!("access".equals(tokenType)
                    || (tokenType == null && verifiedJwt.getClaim("role").asString() != null))) {
                throw new IllegalArgumentException("Access token required");
            }
            User user = users.findById(userId)
                    .filter(u -> u.isActive() && u.getDeletedAt() == null)
                    .orElseThrow(() -> new IllegalArgumentException("Account unavailable"));
            String role = user.getRole();
            if (!"ROLE_USER".equals(role)
                    && !com.MM_CONNECT.api.admin.StaffAccessService.isStaffRole(role)) {
                throw new IllegalArgumentException("Unsupported role");
            }
            // Promoting an existing member must not upgrade its already-issued JWT.
            if (com.MM_CONNECT.api.admin.StaffAccessService.isStaffRole(role)
                    && (user.getFirebaseUid() == null
                    || !user.getFirebaseUid().equals(verifiedJwt.getClaim("firebase_uid").asString())
                    || !role.equals(verifiedJwt.getClaim("role").asString()))) {
                throw new IllegalArgumentException("Staff must sign in again");
            }

            // 4. Create the security session
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, // We store the User UUID as the "principal"
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(role))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 5. Tell Spring Security this request is officially authenticated
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (DataAccessException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(503);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":503,\"title\":\"Service Unavailable\",\"detail\":\"Account service is temporarily unavailable.\"}");
            return;
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            // If token is expired or fake, reject quietly (Context remains empty, leading to 401)
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
