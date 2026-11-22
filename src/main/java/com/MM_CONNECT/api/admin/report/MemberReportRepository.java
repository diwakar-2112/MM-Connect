package com.MM_CONNECT.api.admin.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Bounded page queries: no per-member profile/review lookups. */
@Repository
public class MemberReportRepository {
    private static final String MEMBERS = """
            FROM users u
            WHERE u.role = 'ROLE_USER' AND u.deleted_at IS NULL
              AND (u.firebase_uid IS NOT NULL OR u.last_login_at IS NOT NULL)
            """;
    private static final String PAGE_SQL = """
            SELECT u.id, COALESCE(NULLIF(p.full_name, ''), u.sign_in_name) AS member_name,
                   u.email, u.created_at, u.last_login_at, (p.user_id IS NOT NULL) AS onboarded,
                   COALESCE(latest.status, CASE WHEN u.is_verified THEN 'APPROVED'
                       ELSE 'NOT_SUBMITTED' END) AS onboarding_status, u.is_verified
            FROM users u
            LEFT JOIN profiles p ON p.user_id = u.id
            LEFT JOIN LATERAL (
                SELECT r.status FROM profile_reviews r WHERE r.user_id = u.id
                ORDER BY r.submitted_at DESC, r.id DESC LIMIT 1
            ) latest ON TRUE
            WHERE u.role = 'ROLE_USER' AND u.deleted_at IS NULL
              AND (u.firebase_uid IS NOT NULL OR u.last_login_at IS NOT NULL)
            ORDER BY u.created_at DESC, u.id DESC LIMIT ? OFFSET ?
            """;
    private final JdbcTemplate jdbc;
    public MemberReportRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record MemberRow(UUID id, String name, String email, Instant createdAt,
                            Instant lastLoginAt, boolean onboarded, String onboardingStatus,
                            boolean verified) {}

    public long countMembers() {
        Long count = jdbc.queryForObject("SELECT count(*) " + MEMBERS, Long.class);
        return count == null ? 0 : count;
    }
    public List<MemberRow> findMembers(int size, long offset) {
        return jdbc.query(PAGE_SQL, (rs, row) -> new MemberRow(
                rs.getObject("id", UUID.class), rs.getString("member_name"), rs.getString("email"),
                instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("last_login_at")),
                rs.getBoolean("onboarded"), rs.getString("onboarding_status"), rs.getBoolean("is_verified")),
                size, offset);
    }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
