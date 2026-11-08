package com.MM_CONNECT.api.profile.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingRequestValidationTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void acceptsUiSuppliedGotraWithoutLookupAndAllowsOmittedOptionalFields() {
        assertTrue(validator.validate(request("UI supplied gotra", null)).isEmpty());
    }

    @Test
    void requiresPaternalGotraThroughNestedValidation() {
        for (String gotra : new String[]{null, "", "   "}) {
            var violations = validator.validate(request(gotra, null));
            assertEquals(1, violations.size());
            assertEquals("background.paternalGotra",
                    violations.iterator().next().getPropertyPath().toString());
        }
    }

    @Test
    void acceptsFiveInterestsButRejectsSix() {
        assertTrue(validator.validate(request("Agastya",
                List.of("Travel", "Reading", "Music", "Movies", "Fitness"))).isEmpty());

        var violations = validator.validate(request("Agastya",
                List.of("Travel", "Reading", "Music", "Movies", "Fitness", "Art")));
        assertEquals(1, violations.size());
        assertEquals("lifestyle.interests",
                violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void rejectsGotraLongerThanStorageLimit() {
        var violations = validator.validate(request("a".repeat(101), null));
        assertEquals(1, violations.size());
        assertEquals("background.paternalGotra",
                violations.iterator().next().getPropertyPath().toString());
    }

    private OnboardingRequest request(String paternalGotra, List<String> interests) {
        return new OnboardingRequest(
                "BROTHER",
                new OnboardingRequest.BasicDetails("Example Member", "MAN",
                        LocalDate.of(1995, 1, 1), null, "NEVER_MARRIED", null),
                new OnboardingRequest.Background(null, null, true,
                        null, paternalGotra, null, null),
                new OnboardingRequest.EducationAndCareer(null, null, null, null, null, null),
                new OnboardingRequest.LocationAndFamily("Pune", null, null, null, null, null,
                        null, null, null),
                new OnboardingRequest.Lifestyle(null, null, null, interests, null),
                new OnboardingRequest.PartnerPreferencesDto(21, 60, null, null,
                        null, null, null, null));
    }
}
