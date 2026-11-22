package com.MM_CONNECT.api.admin.auth;

import com.MM_CONNECT.api.admin.StaffAccessService;
import com.MM_CONNECT.api.auth.UserRepository;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Runs only inside the admin security chain, never the Flutter chain. */
public class StaffAuthFilter extends OncePerRequestFilter {
    private final StaffTokenService tokens;
    private final StaffAccountRepository accounts;
    private final UserRepository users;
    public StaffAuthFilter(StaffTokenService tokens, StaffAccountRepository accounts, UserRepository users) {
        this.tokens = tokens; this.accounts = accounts; this.users = users;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod()) || ("POST".equals(request.getMethod())
                && "/v1/admin/auth/session".equals(path(request)));
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(request, response); return; }
        try {
            var jwt = tokens.verify(header.substring(7));
            UUID userId = UUID.fromString(jwt.getSubject());
            var staff = accounts.findById(userId).orElseThrow(IllegalArgumentException::new);
            var user = users.findById(userId).orElseThrow(IllegalArgumentException::new);
            Long version = jwt.getClaim("auth_version").asLong();
            if (!user.isActive() || user.getDeletedAt() != null || !StaffAccessService.isStaffRole(user.getRole())
                    || !user.getRole().equals(jwt.getClaim("role").asString())
                    || version == null || version != staff.getAuthVersion()) throw new IllegalArgumentException();
            if (staff.isPasswordChangeRequired() && !passwordAction(request)) {
                problem(response, 403, "Forbidden", "Change your temporary password before continuing.", "PASSWORD_CHANGE_REQUIRED");
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority(user.getRole()))));
        } catch (DataAccessException ex) {
            SecurityContextHolder.clearContext();
            problem(response, 503, "Service Unavailable", "Staff authentication is temporarily unavailable.", "STAFF_AUTH_UNAVAILABLE");
            return;
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            problem(response, 401, "Unauthorized", "A valid staff access token is required.", "STAFF_SESSION_REQUIRED");
            return;
        }
        chain.doFilter(request, response);
    }
    private boolean passwordAction(HttpServletRequest request) {
        String path = path(request);
        return ("GET".equals(request.getMethod()) && "/v1/admin/auth/me".equals(path))
                || ("POST".equals(request.getMethod()) && ("/v1/admin/auth/change-password".equals(path)
                || "/v1/admin/auth/logout".equals(path)));
    }
    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
    static void problem(HttpServletResponse response, int status, String title, String detail, String code) throws IOException {
        response.setStatus(status);
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":" + status + ",\"title\":\"" + title
                + "\",\"detail\":\"" + detail + "\",\"code\":\"" + code + "\"}");
    }
}
