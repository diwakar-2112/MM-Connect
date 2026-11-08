package com.MM_CONNECT.api.matching;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "match_profiles")
public class MatchProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    // --- My actual details ---
    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "marital_status", nullable = false)
    private String maritalStatus;

    @Column(name = "religion")
    private String religion;

    @Column(name = "community")
    private String community;

    @Column(name = "current_city", nullable = false)
    private String currentCity;

    // --- What I am looking for ---
    @Column(name = "pref_age_min", nullable = false)
    private Integer prefAgeMin;

    @Column(name = "pref_age_max", nullable = false)
    private Integer prefAgeMax;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "pref_locations", columnDefinition = "text[]")
    private List<String> prefLocations;

    // --- System Status ---
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public MatchProfile() {}

    // --- Getters and Setters ---
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }

    public String getCurrentCity() { return currentCity; }
    public void setCurrentCity(String currentCity) { this.currentCity = currentCity; }

    public Integer getPrefAgeMin() { return prefAgeMin; }
    public void setPrefAgeMin(Integer prefAgeMin) { this.prefAgeMin = prefAgeMin; }

    public Integer getPrefAgeMax() { return prefAgeMax; }
    public void setPrefAgeMax(Integer prefAgeMax) { this.prefAgeMax = prefAgeMax; }

    public List<String> getPrefLocations() { return prefLocations; }
    public void setPrefLocations(List<String> prefLocations) { this.prefLocations = prefLocations; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}