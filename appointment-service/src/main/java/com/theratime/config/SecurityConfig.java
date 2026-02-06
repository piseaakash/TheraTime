package com.theratime.config;

import com.theratime.security.TenantContextFilter;
import com.theratime.security.TokenValidatorFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final TokenValidatorFilter tokenValidatorFilter;
    private final TenantContextFilter tenantContextFilter;

    @Bean
    public RestTemplate restTemplate(
            @Value("${rest.client.connect-timeout-ms:2000}") int connectTimeout,
            @Value("${rest.client.read-timeout-ms:2000}") int readTimeout
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
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
                        .requestMatchers( "/admin/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(tokenValidatorFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantContextFilter, TokenValidatorFilter.class)
                .build();

    }

}
