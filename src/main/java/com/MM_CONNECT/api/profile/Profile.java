package com.MM_CONNECT.api.profile;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "profile_created_for", length = 20, nullable = false)
    private String profileCreatedFor;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "gender", length = 20, nullable = false)
    private String gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "mother_tongue", length = 100)
    private String motherTongue;

    @Column(name = "marital_status", length = 20, nullable = false)
    private String maritalStatus;

    @Column(name = "religion", length = 50)
    private String religion;

    @Column(name = "community", length = 50)
    private String community;

    @Column(name = "sub_community", length = 100)
    private String subCommunity;

    // Required by API validation; nullable in storage for existing profiles.
    @Column(name = "paternal_gotra", length = 100)
    private String paternalGotra;

    @Column(name = "maternal_gotra", length = 100)
    private String maternalGotra;

    @Column(name = "sampradaya", length = 100)
    private String sampradaya;

    @Column(name = "show_community", nullable = false)
    private boolean showCommunity = true;

    @Column(name = "highest_qualification", length = 100)
    private String highestQualification;

    @Column(name = "field_of_study", length = 100)
    private String fieldOfStudy;

    @Column(name = "profession_domain", length = 100)
    private String professionDomain;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "annual_income")
    private Long annualIncome;

    @Column(name = "current_city", length = 100, nullable = false)
    private String currentCity;

    @Column(name = "current_state", length = 100)
    private String currentState;

    @Column(name = "native_place", length = 100)
    private String nativePlace;

    @Column(name = "native_district", length = 100)
    private String nativeDistrict;

    @Column(name = "work_city", length = 100)
    private String workCity;

    @Column(name = "relocation_preference", length = 30)
    private String relocationPreference;

    @Column(name = "family_type", length = 20)
    private String familyType;

    @Column(name = "father_occupation", length = 100)
    private String fatherOccupation;

    @Column(name = "mother_occupation", length = 100)
    private String motherOccupation;

    @Column(name = "siblings", length = 30)
    private String siblings;

    @Column(name = "weekend_preference", length = 100)
    private String weekendPreference;

    @Column(name = "food_preference", length = 30)
    private String foodPreference;

    @Column(name = "smoking", length = 30)
    private String smoking;

    @Column(name = "alcohol", length = 30)
    private String alcohol;

    @JdbcTypeCode(SqlTypes.ARRAY) // Maps to Postgres TEXT[]
    @Column(name = "interests", columnDefinition = "text[]")
    private List<String> interests;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // Default constructor
    public Profile() {}

    // Getters and Setters
    // (You can use your IDE to Auto-Generate the Getters and Setters here!)

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getProfileCreatedFor() { return profileCreatedFor; }
    public void setProfileCreatedFor(String profileCreatedFor) { this.profileCreatedFor = profileCreatedFor; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public String getMotherTongue() { return motherTongue; }
    public void setMotherTongue(String motherTongue) { this.motherTongue = motherTongue; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }

    public String getSubCommunity() { return subCommunity; }
    public void setSubCommunity(String subCommunity) { this.subCommunity = subCommunity; }

    public String getPaternalGotra() { return paternalGotra; }
    public void setPaternalGotra(String paternalGotra) { this.paternalGotra = paternalGotra; }

    public String getMaternalGotra() { return maternalGotra; }
    public void setMaternalGotra(String maternalGotra) { this.maternalGotra = maternalGotra; }

    public String getSampradaya() { return sampradaya; }
    public void setSampradaya(String sampradaya) { this.sampradaya = sampradaya; }

    public boolean isShowCommunity() { return showCommunity; }
    public void setShowCommunity(boolean showCommunity) { this.showCommunity = showCommunity; }

    public String getHighestQualification() { return highestQualification; }
    public void setHighestQualification(String highestQualification) { this.highestQualification = highestQualification; }

    public String getFieldOfStudy() { return fieldOfStudy; }
    public void setFieldOfStudy(String fieldOfStudy) { this.fieldOfStudy = fieldOfStudy; }

    public String getProfessionDomain() { return professionDomain; }
    public void setProfessionDomain(String professionDomain) { this.professionDomain = professionDomain; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public Long getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(Long annualIncome) { this.annualIncome = annualIncome; }

    public String getCurrentCity() { return currentCity; }
    public void setCurrentCity(String currentCity) { this.currentCity = currentCity; }

    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }

    public String getNativePlace() { return nativePlace; }
    public void setNativePlace(String nativePlace) { this.nativePlace = nativePlace; }

    public String getNativeDistrict() { return nativeDistrict; }
    public void setNativeDistrict(String nativeDistrict) { this.nativeDistrict = nativeDistrict; }

    public String getWorkCity() { return workCity; }
    public void setWorkCity(String workCity) { this.workCity = workCity; }

    public String getRelocationPreference() { return relocationPreference; }
    public void setRelocationPreference(String relocationPreference) { this.relocationPreference = relocationPreference; }

    public String getFamilyType() { return familyType; }
    public void setFamilyType(String familyType) { this.familyType = familyType; }

    public String getFatherOccupation() { return fatherOccupation; }
    public void setFatherOccupation(String fatherOccupation) { this.fatherOccupation = fatherOccupation; }

    public String getMotherOccupation() { return motherOccupation; }
    public void setMotherOccupation(String motherOccupation) { this.motherOccupation = motherOccupation; }

    public String getSiblings() { return siblings; }
    public void setSiblings(String siblings) { this.siblings = siblings; }

    public String getWeekendPreference() { return weekendPreference; }
    public void setWeekendPreference(String weekendPreference) { this.weekendPreference = weekendPreference; }

    public String getFoodPreference() { return foodPreference; }
    public void setFoodPreference(String foodPreference) { this.foodPreference = foodPreference; }

    public String getSmoking() { return smoking; }
    public void setSmoking(String smoking) { this.smoking = smoking; }

    public String getAlcohol() { return alcohol; }
    public void setAlcohol(String alcohol) { this.alcohol = alcohol; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
