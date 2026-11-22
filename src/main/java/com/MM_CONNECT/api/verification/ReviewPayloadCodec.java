package com.MM_CONNECT.api.verification;

import com.MM_CONNECT.api.profile.dto.OnboardingRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

/** Dedicated mapper keeps persisted review payloads independent of HTTP mapper settings. */
@Component
public class ReviewPayloadCodec {
    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    public String encode(OnboardingRequest request) {
        try { return mapper.writeValueAsString(request); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize review submission", e); }
    }

    public OnboardingRequest decode(String payload) {
        try { return mapper.readValue(payload, OnboardingRequest.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot read review submission", e); }
    }
}
