package com.theratime.auth.repository;

import com.theratime.auth.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
    Optional<Credentials> findByEmail(String email);

    Optional<Credentials> findByRefreshToken(String refreshToken);

    @Modifying
    @Query("""
            UPDATE Credentials c
            SET c.refreshToken = NULL,
                c.refreshTokenExpiry = NULL
            WHERE c.refreshToken = :refreshToken
            """)
    int clearRefreshToken(@Param("refreshToken") String refreshToken);
}
