//package com.MM_CONNECT.api.config;
//
//import com.MM_CONNECT.api.auth.JwtAuthFilter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//@Configuration
//public class SecurityRuleConfig {
//
//    private final JwtAuthFilter tokenFilter;
//
//    public SecurityRuleConfig(JwtAuthFilter tokenFilter) {
//        this.tokenFilter = tokenFilter;
//    }
//
//    @Bean
//    public SecurityFilterChain defineSecurityGrid(HttpSecurity http) throws Exception {
//        http
//                .csrf(AbstractHttpConfigurer::disable)
//                .cors(AbstractHttpConfigurer::disable)
//                // APIs are stateless, no backend session cookies used
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(rules -> rules
//                        // Allow these endpoints without authentication
//                        .requestMatchers("/v1/auth/**").permitAll()
//                        .requestMatchers("/v1/config").permitAll() // Mobile app needs this on launch
//                        .requestMatchers("/actuator/health").permitAll()
//                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
//                        .requestMatchers("/").permitAll()
//                        .requestMatchers("/index.html").permitAll()
//                        .requestMatchers("/phone.html").permitAll()
//                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
//
//                        // Block EVERYTHING else
//                        .anyRequest().authenticated()
//                )
//                // Inject our custom filter right before the default Spring password filter
//                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOriginPatterns(List.of("*")); // Allow any browser origin for dev
//        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        //need to remove later
//        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "ngrok-skip-browser-warning", "Accept", "Origin"));
//        config.setMaxAge(3600L);
//        ///
//        config.setAllowedHeaders(List.of("*"));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }
//}


package com.MM_CONNECT.api.config;

import com.MM_CONNECT.api.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityRuleConfig {

    private final JwtAuthFilter tokenFilter;

    public SecurityRuleConfig(JwtAuthFilter tokenFilter) {
        this.tokenFilter = tokenFilter;
    }

    @Bean
    public SecurityFilterChain defineSecurityGrid(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Uses the CorsConfigurationSource bean defined below
                .cors(Customizer.withDefaults())
                // APIs are stateless, no backend session cookies used
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rules -> rules
                        // CORS preflight requests must never require authentication
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow these endpoints without authentication
                        .requestMatchers("/v1/auth/**").permitAll()
                        .requestMatchers("/v1/config").permitAll() // Mobile app needs this on launch
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/index.html").permitAll()
                        .requestMatchers("/phone.html").permitAll()

                        // Block EVERYTHING else
                        .anyRequest().authenticated()
                )
                // Inject our custom filter right before the default Spring password filter
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // Dev only: tighten before deploying
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}