package com.workproofpay.backend.shared.bootstrap;

import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.model.JobType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JobConstraintInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        if (!isPostgreSql() || !jobsTableExists()) {
            return;
        }
        refreshColumnChecks("job_type", allowedJobTypes());
        refreshColumnChecks("reference_kind", allowedReferenceKinds());
    }

    private void refreshColumnChecks(String columnName, String allowedValues) {
        List<String> constraintNames = jdbcTemplate.query(
                """
                select c.conname
                from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                join pg_namespace n on n.oid = t.relnamespace
                where c.contype = 'c'
                  and t.relname = 'jobs'
                  and n.nspname = current_schema()
                  and pg_get_constraintdef(c.oid) like ?
                """,
                (rs, rowNum) -> rs.getString(1),
                "%" + columnName + "%"
        );
        for (String constraintName : constraintNames) {
            jdbcTemplate.execute("alter table jobs drop constraint if exists " + constraintName);
        }
        jdbcTemplate.execute("alter table jobs add constraint jobs_%s_check check (%s in (%s))"
                .formatted(columnName, columnName, allowedValues));
    }

    private boolean jobsTableExists() {
        Integer matches = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = current_schema()
                  and table_name = 'jobs'
                """,
                Integer.class
        );
        return matches != null && matches > 0;
    }

    private boolean isPostgreSql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("postgresql");
        }
    }

    private String allowedJobTypes() {
        return Arrays.stream(JobType.values())
                .map(JobType::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));
    }

    private String allowedReferenceKinds() {
        return Arrays.stream(JobReferenceKind.values())
                .map(JobReferenceKind::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));
    }
}
