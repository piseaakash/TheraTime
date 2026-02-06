package com.theratime.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Wraps the application DataSource with TenantAwareDataSource so each request
 * uses the correct PostgreSQL schema (tenant_1, tenant_2, ...) via search_path.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource targetDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSource targetDataSource) {
        return new TenantAwareDataSource(targetDataSource);
    }
}
