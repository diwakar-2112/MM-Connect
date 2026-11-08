package com.MM_CONNECT.api.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PartnerPreferenceRepository extends JpaRepository<PartnerPreference, UUID> {
}