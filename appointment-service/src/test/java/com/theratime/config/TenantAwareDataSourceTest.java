package com.theratime.config;

import com.theratime.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TenantAwareDataSourceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getConnection_setsSearchPath() throws SQLException {
        TenantContext.setTenantId(2L);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(conn.createStatement()).thenReturn(stmt);

        DataSource target = mock(DataSource.class);
        when(target.getConnection()).thenReturn(conn);

        TenantAwareDataSource ds = new TenantAwareDataSource(target);
        Connection result = ds.getConnection();

        assertThat(result).isSameAs(conn);
        verify(stmt).execute("SET search_path TO tenant_2");
    }

    @Test
    void getConnection_withUsernamePassword_setsSearchPath() throws SQLException {
        TenantContext.setTenantId(3L);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(conn.createStatement()).thenReturn(stmt);

        DataSource target = mock(DataSource.class);
        when(target.getConnection("user", "pass")).thenReturn(conn);

        TenantAwareDataSource ds = new TenantAwareDataSource(target);
        Connection result = ds.getConnection("user", "pass");

        assertThat(result).isSameAs(conn);
        verify(stmt).execute("SET search_path TO tenant_3");
    }
}
