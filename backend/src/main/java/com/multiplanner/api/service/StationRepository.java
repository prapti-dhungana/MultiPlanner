package com.multiplanner.api.service;

import com.multiplanner.api.model.Station;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for station autocomplete lookups (PostgreSQL).
 * - Uses pg_trgm for ranked fuzzy search, with a LIKE fallback so very short queries still return something useful.
 */
@Repository
public class StationRepository {

    private static final int DEFAULT_LIMIT = 5;

    private static final String SEARCH_SQL =
            "SELECT atco_code AS code, name " +
            "FROM stations " +
            "WHERE lower(name) % ? OR lower(name) LIKE '%' || ? || '%' " +
            "ORDER BY similarity(lower(name), ?) DESC " +
            "LIMIT ?";

    private final JdbcTemplate jdbcTemplate;

    public StationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //Search stations by name (case-insensitive).
    //Returns an empty list for blank queries to avoid scanning the full table.
    public List<Station> search(String query) {
        String q = (query == null) ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            return List.of();
        }

        return jdbcTemplate.query(
                SEARCH_SQL,
                (rs, rowNum) -> new Station(
                        rs.getString("code"),
                        rs.getString("name")
                ),
                q, // trigram % match
                q, // LIKE fallback
                q,  // similarity() ranking
                DEFAULT_LIMIT   // limit
        );
    }
}
