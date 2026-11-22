package com.MM_CONNECT.api.verification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ProfileReviewRepository extends JpaRepository<ProfileReview, UUID> {
    Optional<ProfileReview> findFirstByUserIdOrderBySubmittedAtDescIdDesc(UUID userId);
    Optional<ProfileReview> findByUserIdAndStatus(UUID userId, ReviewStatus status);
    Page<ProfileReview> findByStatus(ReviewStatus status, Pageable pageable);

    @Query("select r.userId from ProfileReview r where r.id = :id")
    Optional<UUID> findReviewUserId(@Param("id") UUID id);
}
