package com.multiplanner.api.service;

import com.multiplanner.api.model.Station;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StationRepository {

    private final JdbcTemplate jdbcTemplate;

    public StationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Station> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            return List.of();
        }

        // Rank by trigram similarity (pg_trgm)
        // Also keep a broad LIKE so short queries still return something.
        String sql =
            "SELECT atco_code AS code, name, town " +
            "FROM stations " +
            "WHERE lower(name) % ? OR lower(name) LIKE '%' || ? || '%' " +
            "ORDER BY similarity(lower(name), ?) DESC " +
            "LIMIT 5";

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new Station(
                rs.getString("code"),
                rs.getString("name")
            ),
            q,   //for % trigram match
            q,   // for LIKE
            q    // for similarity()
        );
    }
}
