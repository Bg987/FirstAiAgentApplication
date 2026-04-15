package com.example.FirstAiAgent.Service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import static java.util.Map.entry;

@Service
public class CodeImageService {

    private final String CARBONARA_URL = "https://carbonara.solopov.dev/api/cook";
    private final RestTemplate restTemplate = new RestTemplate();

    public byte[] generateCodeImage(String javaCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Fixed using ofEntries to support more than 10 parameters
        Map<String, Object> body = Map.ofEntries(
                entry("code", javaCode),
                entry("backgroundColor", "#1e1e1e"),
                entry("theme", "monokai"),
                entry("language", "java"),
                entry("paddingVertical", "40px"),   // Reduced padding to save space
                entry("paddingHorizontal", "40px"), // Reduced padding to save space
                entry("fontFamily", "JetBrains Mono"),
                entry("fontSize", "14px"),          // Smaller font helps fit more code
                entry("lineNumbers", true),
                entry("exportSize", "1x"),          // Changed from 2x to 1x to keep file size down
                entry("windowTheme", "sharp"),
                entry("width", 800),                // FIXED WIDTH prevents horizontal stretching
                entry("widthAdjustment", false),    // Disable auto-width to stay within limits
                entry("dropShadow", true),
                entry("dropShadowBlurRadius", "30px"),
                entry("dropShadowOffsetY", "10px")
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.postForObject(CARBONARA_URL, entity, byte[].class);
        } catch (Exception e) {
            System.err.println("❌ Carbonara API Error: " + e.getMessage());
            return null;
        }
    }
}