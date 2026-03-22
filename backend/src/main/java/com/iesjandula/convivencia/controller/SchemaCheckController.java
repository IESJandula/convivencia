package com.iesjandula.convivencia.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug/schema")
@CrossOrigin(origins = "*")
public class SchemaCheckController {

    private final JdbcTemplate jdbcTemplate;

    public SchemaCheckController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        String currentDb = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);

        List<Map<String, Object>> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                ORDER BY table_name
                """);

        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT table_name, column_name, data_type, is_nullable, column_type
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                ORDER BY table_name, ordinal_position
                """);

        List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList("""
                SELECT table_name, column_name, referenced_table_name, referenced_column_name, constraint_name
                FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE()
                  AND referenced_table_name IS NOT NULL
                ORDER BY table_name, constraint_name
                """);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("database", currentDb);
        response.put("tables", tables);
        response.put("columns", columns);
        response.put("foreignKeys", foreignKeys);
        return response;
    }
}
