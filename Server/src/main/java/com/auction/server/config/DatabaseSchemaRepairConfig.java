package com.auction.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@Profile("!test")
public class DatabaseSchemaRepairConfig {
    @Bean
    ApplicationRunner repairGeneratedIdColumns(JdbcTemplate jdbcTemplate) {
        return args -> {
            repairAutoIncrement(jdbcTemplate, "auto_bids");
            repairAutoIncrementWithReferencingKeys(jdbcTemplate, "auctions");
            repairBidIncrementColumn(jdbcTemplate);
        };
    }

    private void repairBidIncrementColumn(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("ALTER TABLE auto_bids MODIFY COLUMN bid_increment DOUBLE NULL DEFAULT 1.0");
            log.info("Successfully altered auto_bids.bid_increment to be nullable with default 1.0");
        } catch (Exception ex) {
            log.warn("Note: auto_bids.bid_increment repair skipped or column does not exist: {}", ex.getMessage());
        }
    }

    private void repairAutoIncrement(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            if (isAutoIncrement(jdbcTemplate, tableName)) {
                log.info("Auto-increment id column already verified for table {}", tableName);
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
            log.info("Verified auto-increment id column for table {}", tableName);
        } catch (Exception ex) {
            log.warn("Could not verify auto-increment id column for table {}: {}", tableName, ex.getMessage());
        }
    }

    private void repairAutoIncrementWithReferencingKeys(JdbcTemplate jdbcTemplate, String tableName) {
        if (isAutoIncrement(jdbcTemplate, tableName)) {
            log.info("Auto-increment id column already verified for table {}", tableName);
            return;
        }
        List<Map<String, Object>> foreignKeys = findReferencingForeignKeys(jdbcTemplate, tableName);
        try {
            for (Map<String, Object> foreignKey : foreignKeys) {
                jdbcTemplate.execute("ALTER TABLE " + foreignKey.get("TABLE_NAME")
                        + " DROP FOREIGN KEY " + foreignKey.get("CONSTRAINT_NAME"));
            }

            repairAutoIncrement(jdbcTemplate, tableName);

            for (Map<String, Object> foreignKey : foreignKeys) {
                jdbcTemplate.execute("ALTER TABLE " + foreignKey.get("TABLE_NAME")
                        + " ADD CONSTRAINT " + foreignKey.get("CONSTRAINT_NAME")
                        + " FOREIGN KEY (" + foreignKey.get("COLUMN_NAME") + ")"
                        + " REFERENCES " + tableName + "(id)");
            }
        } catch (Exception ex) {
            log.warn("Could not repair auto-increment id column for table {} with foreign keys: {}", tableName, ex.getMessage());
        }
    }

    private List<Map<String, Object>> findReferencingForeignKeys(JdbcTemplate jdbcTemplate, String referencedTableName) {
        return jdbcTemplate.queryForList("""
                SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND REFERENCED_TABLE_NAME = ?
                  AND REFERENCED_COLUMN_NAME = 'id'
                """, referencedTableName);
    }

    private boolean isAutoIncrement(JdbcTemplate jdbcTemplate, String tableName) {
        String extra = jdbcTemplate.queryForObject("""
                SELECT EXTRA
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = 'id'
                """, String.class, tableName);
        return extra != null && extra.toLowerCase().contains("auto_increment");
    }
}
