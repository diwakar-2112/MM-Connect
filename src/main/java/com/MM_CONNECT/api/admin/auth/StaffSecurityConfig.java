package com.MM_CONNECT.api.admin.auth;

import com.MM_CONNECT.api.auth.JwtAuthFilter;
import com.MM_CONNECT.api.auth.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.time.Clock;

@Configuration
public class StaffSecurityConfig {
    @Bean public Clock staffClock() { return Clock.systemUTC(); }
    @Bean public PasswordEncoder staffPasswordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
    @Bean public StaffTokenService staffTokenService(@Value("${security.jwt.secret-key}") String secret,
            @Value("${security.staff.access-token-minutes:15}") long minutes, Clock staffClock) {
        return new StaffTokenService(secret, minutes, staffClock);
    }
    @Bean @Order(1)
    public SecurityFilterChain staffSecurityChain(HttpSecurity http, StaffTokenService tokens,
                                                 StaffAccountRepository accounts, UserRepository users) throws Exception {
        http.securityMatcher("/v1/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/admin/auth/session").permitAll()
                        .requestMatchers("/v1/admin/staff", "/v1/admin/staff/**").hasRole("SUPERADMIN")
                        .anyRequest().hasAnyRole("ADMIN", "SUPERADMIN"))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, failure) -> StaffAuthFilter.problem(response,
                                401, "Unauthorized", "A valid staff access token is required.", "STAFF_SESSION_REQUIRED"))
                        .accessDeniedHandler((request, response, failure) -> StaffAuthFilter.problem(response,
                                403, "Forbidden", "Staff permission required.", "STAFF_PERMISSION_REQUIRED")))
                .addFilterBefore(new StaffAuthFilter(tokens, accounts, users), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    // The existing component must run in its security chain only, not as a second
    // servlet-container filter on staff routes. Its Flutter implementation is untouched.
    @Bean public FilterRegistrationBean<JwtAuthFilter> memberJwtRegistration(JwtAuthFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
