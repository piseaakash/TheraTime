package com.theratime.config;

import com.theratime.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Schema-per-tenant: sets PostgreSQL search_path to the current tenant's schema
 * when a connection is obtained, so all subsequent queries run in that schema.
 */
@Slf4j
public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        setSchema(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = super.getConnection(username, password);
        setSchema(conn);
        return conn;
    }

    private void setSchema(Connection conn) throws SQLException {
        String schema = TenantContext.getCurrentSchema();
        try (var stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + schema);
            log.trace("Set search_path to {}", schema);
        }
    }
}
