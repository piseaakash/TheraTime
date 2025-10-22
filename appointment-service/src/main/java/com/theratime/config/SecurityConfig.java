package com.theratime.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChainForActuator(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/**")
            .authorizeHttpRequests(
                auth -> auth
                .requestMatchers("/actuator/prometheus", "/actuator/health", "/actuator/metrics", "/actuator/info").permitAll()
                .anyRequest().authenticated()
        )
                .httpBasic( basic -> {} )
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher( "/appointments/**", "/calendar/**", "/admin/**")
                .csrf( AbstractHttpConfigurer::disable )
                .authorizeHttpRequests( auth -> auth
                        .requestMatchers(HttpMethod.POST, "/appointments/book" ).permitAll()
                        .requestMatchers( "/calendar/block").permitAll()
                        .requestMatchers( "/admin/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic( basic -> {} )
                .build();

    }
}
