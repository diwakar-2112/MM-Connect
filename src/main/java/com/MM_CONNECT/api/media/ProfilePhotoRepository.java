package com.MM_CONNECT.api.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, UUID> {
    long countByUserId(UUID userId);

    @Query("""
        SELECT p FROM ProfilePhoto p
        WHERE p.userId = :userId
        ORDER BY p.isPrimary DESC, p.sortOrder ASC, p.createdAt ASC, p.id ASC
    """)
    List<ProfilePhoto> findProfilePhotos(@Param("userId") UUID userId);
}
