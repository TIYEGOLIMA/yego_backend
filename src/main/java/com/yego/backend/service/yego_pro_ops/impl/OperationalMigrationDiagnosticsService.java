package com.yego.backend.service.yego_pro_ops.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OperationalMigrationDiagnosticsService {

    private static final String MIGRATION_017_RESOURCE = "db/migration/017_operational_automatic_shift_mirror.sql";

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public MigrationDiagnostics inspect() {
        boolean tripFactsExists = tableExists("operational_trip_facts");
        boolean shiftSessionsExists = tableExists("operational_shift_sessions");
        boolean shiftEventsExists = tableExists("operational_shift_events");
        String historyTableName = resolveHistoryTableName();
        boolean historyTableExists = historyTableName != null;
        String ddlAuto = resolveDdlAuto();
        boolean migrationScriptPresent = getClass().getClassLoader().getResource(MIGRATION_017_RESOURCE) != null;
        String migrationMechanism = resolveMigrationMechanism(ddlAuto, migrationScriptPresent);
        boolean migration017Applied = tripFactsExists && shiftSessionsExists && shiftEventsExists;
        boolean safeToApplyManually = Arrays.stream(environment.getActiveProfiles())
                .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("local") || profile.equals("dev") || profile.equals("staging"));

        return new MigrationDiagnostics(
                tripFactsExists,
                shiftSessionsExists,
                shiftEventsExists,
                migration017Applied,
                migrationMechanism,
                historyTableExists,
                historyTableExists ? historyTableName : expectedHistoryTableName(migrationMechanism),
                migrationScriptPresent,
                ddlAuto,
                safeToApplyManually);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = current_schema()
                          AND table_name = ?
                        """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private String resolveHistoryTableName() {
        return jdbcTemplate.query(
                """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = current_schema()
                          AND table_name IN ('flyway_schema_history', 'databasechangelog')
                        ORDER BY table_name
                        """,
                rs -> rs.next() ? rs.getString(1) : null);
    }

    private String resolveDdlAuto() {
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        if (ddlAuto == null || ddlAuto.isBlank()) {
            ddlAuto = environment.getProperty("spring.jpa.properties.hibernate.hbm2ddl.auto", "unknown");
        }
        return ddlAuto;
    }

    private String resolveMigrationMechanism(String ddlAuto, boolean migrationScriptPresent) {
        ClassLoader classLoader = getClass().getClassLoader();
        boolean flywayPresent = isPresent("org.flywaydb.core.Flyway", classLoader);
        boolean liquibasePresent = isPresent("liquibase.Liquibase", classLoader);

        if (flywayPresent) {
            return "Flyway";
        }
        if (liquibasePresent) {
            return "Liquibase";
        }
        if ("update".equalsIgnoreCase(ddlAuto) || "create".equalsIgnoreCase(ddlAuto) || "create-drop".equalsIgnoreCase(ddlAuto)) {
            return migrationScriptPresent ? "Manual SQL + Hibernate auto" : "Hibernate auto";
        }
        return migrationScriptPresent ? "Manual SQL" : "Unknown";
    }

    private String expectedHistoryTableName(String migrationMechanism) {
        if ("Flyway".equalsIgnoreCase(migrationMechanism)) {
            return "flyway_schema_history";
        }
        if ("Liquibase".equalsIgnoreCase(migrationMechanism)) {
            return "databasechangelog";
        }
        return "N/A";
    }

    private boolean isPresent(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public record MigrationDiagnostics(
            boolean operationalTripFactsTableExists,
            boolean operationalShiftSessionsTableExists,
            boolean operationalShiftEventsTableExists,
            boolean migration017Applied,
            String migrationMechanism,
            boolean migrationHistoryTableExists,
            String migrationHistoryTableName,
            boolean migration017ScriptPresentInRepo,
            String ddlAutoMode,
            boolean safeToApplyManuallyInCurrentProfile) {

        public boolean allOperationalTablesExist() {
            return operationalTripFactsTableExists
                    && operationalShiftSessionsTableExists
                    && operationalShiftEventsTableExists;
        }
    }
}
