package com.yego.backend.service.yego_pro_ops.mobile;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class DatabaseLockService {

    private final JdbcTemplate jdbcTemplate;

    /** Acquires transaction-scoped PostgreSQL locks in stable order to prevent concurrent duplicates. */
    public void acquireAll(Collection<String> keys) {
        keys.stream().sorted().forEach(this::acquire);
    }

    private void acquire(String key) {
        jdbcTemplate.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                Object.class,
                key
        );
    }
}
