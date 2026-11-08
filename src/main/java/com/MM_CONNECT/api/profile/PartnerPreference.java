package com.MM_CONNECT.api.profile;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "partner_preferences")
public class PartnerPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "pref_age_min", nullable = false)
    private Integer prefAgeMin = 21;

    @Column(name = "pref_age_max", nullable = false)
    private Integer prefAgeMax = 60;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "pref_locations", columnDefinition = "text[]")
    private List<String> prefLocations;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "pref_education", columnDefinition = "text[]")
    private List<String> prefEducation;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "pref_marital_statuses", columnDefinition = "text[]")
    private List<String> preferredMaritalStatuses;

    @Column(name = "exclude_paternal_gotra")
    private Boolean excludePaternalGotra;

    @Column(name = "exclude_maternal_gotra")
    private Boolean excludeMaternalGotra;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "pref_sub_communities", columnDefinition = "text[]")
    private List<String> preferredSubCommunities;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public PartnerPreference() {}

    // Getters and Setters

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Integer getPrefAgeMin() { return prefAgeMin; }
    public void setPrefAgeMin(Integer prefAgeMin) { this.prefAgeMin = prefAgeMin; }

    public Integer getPrefAgeMax() { return prefAgeMax; }
    public void setPrefAgeMax(Integer prefAgeMax) { this.prefAgeMax = prefAgeMax; }

    public List<String> getPrefLocations() { return prefLocations; }
    public void setPrefLocations(List<String> prefLocations) { this.prefLocations = prefLocations; }

    public List<String> getPrefEducation() { return prefEducation; }
    public void setPrefEducation(List<String> prefEducation) { this.prefEducation = prefEducation; }

    public List<String> getPreferredMaritalStatuses() { return preferredMaritalStatuses; }
    public void setPreferredMaritalStatuses(List<String> preferredMaritalStatuses) { this.preferredMaritalStatuses = preferredMaritalStatuses; }

    public Boolean getExcludePaternalGotra() { return excludePaternalGotra; }
    public void setExcludePaternalGotra(Boolean excludePaternalGotra) { this.excludePaternalGotra = excludePaternalGotra; }

    public Boolean getExcludeMaternalGotra() { return excludeMaternalGotra; }
    public void setExcludeMaternalGotra(Boolean excludeMaternalGotra) { this.excludeMaternalGotra = excludeMaternalGotra; }

    public List<String> getPreferredSubCommunities() { return preferredSubCommunities; }
    public void setPreferredSubCommunities(List<String> preferredSubCommunities) { this.preferredSubCommunities = preferredSubCommunities; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
