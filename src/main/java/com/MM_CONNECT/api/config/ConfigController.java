package com.MM_CONNECT.api.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/config")
public class ConfigController {

    private final PlatformSettingRepository settingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigController(PlatformSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAppConfig() {
        List<PlatformSetting> settings = settingRepository.findAll();
        Map<String, Object> response = new HashMap<>();

        for (PlatformSetting setting : settings) {
            try {
                // Parse directly into plain Java Map/Object instead of JsonNode
                Object parsedJson = objectMapper.readValue(setting.getConfigValue(), Object.class);
                response.put(setting.getConfigKey(), parsedJson);
            } catch (JsonProcessingException e) {
                response.put(setting.getConfigKey(), setting.getConfigValue());
            }
        }

        return ResponseEntity.ok(response);
    }
}