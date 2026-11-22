package com.MM_CONNECT.api.matching;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MatchProfileRepository extends JpaRepository<MatchProfile, UUID> {

    @Query("""
        SELECT m FROM MatchProfile m 
        WHERE m.userId != :myId 
          AND (:seekingGender IS NULL OR m.gender = :seekingGender)
          AND m.active = true
          AND EXISTS (
              SELECT 1 FROM User u WHERE u.id = m.userId
                AND u.active = true AND u.deletedAt IS NULL AND u.verified = true
          )
          AND (:minAge IS NULL OR m.age >= :minAge)
          AND (:maxAge IS NULL OR m.age <= :maxAge)
          AND (:city IS NULL OR m.currentCity = :city)
          AND NOT EXISTS (
              SELECT 1 FROM UserInteraction ui 
              WHERE ui.actorId = :myId 
                AND ui.targetId = m.userId 
                AND ui.action IN ('PASSED', 'BLOCKED')
          )
          AND NOT EXISTS (
              SELECT 1 FROM UserInteraction blocked
              WHERE blocked.actorId = m.userId AND blocked.targetId = :myId
                AND blocked.action = 'BLOCKED'
          )
    """)
    Page<MatchProfile> findDiscoveryFeed(
            @Param("myId") UUID myId,
            @Param("seekingGender") String seekingGender,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("city") String city,
            Pageable pageable
    );
}
