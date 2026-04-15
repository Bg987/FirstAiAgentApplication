package com.example.FirstAiAgent.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${groq.api.key}")
    private String apiKey;

    private final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final RestTemplate restTemplate = new RestTemplate();

    public String getJavaSuggestions(String topic) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = "You are a World-Class Java Architect. Generate a three-part Masterclass for the topic: '" + topic + "'.\n\n" +

                "1. CODE_START: Provide a comprehensive industry-standard Java snippet demonstrating EVERY major variation of the concept. " +
                "Include clean comments for logic flow. (RAW TEXT ONLY, NO MARKDOWN BACKTICKS).\n\n" +

                "2. INSTAGRAM_START: Provide a technical summary (STRICTLY UNDER 1800 characters) for social media. Use this structure:\n" +
                "   - **The Deep Dive**: Brief internal mechanics.\n" +
                "   - 🌍 **Use-Case**: Real-world mission-critical scenario.\n" +
                "   - ⚡ **The 'Why'**: Brief comparison with alternatives.\n" +
                "   - 🛠 **Architecture**: Stack/Heap and Scalability impact.\n" +
                "   - 🎯 **Pro-Tip**: Rare insight or pitfall.\n" +
                "   - 🏷 5 engagement hashtags.\n\n" +

                "3. BLOG_START: Provide an exhaustive technical deep-dive. AVOID future trends or resource lists. FOCUS ONLY ON:\n" +
                "   - **Simplified Definition**: Explain it like I'm 15, then explain it like a Senior Engineer.\n" +
                "   - **Real-Life vs. Software**: A concrete analogy from the physical world paired with its exact mission-critical software counterpart.\n" +
                "   - **The 'Internal' Secret**: Detail what happens in the JVM (Stack/Heap/JIT) or Bytecode that is usually 'invisible' to developers.\n" +
                "   - **Edge-Case Complexity**: Big O analysis not just for the best case, but how it behaves under heavy GC (Garbage Collection) pressure.\n" +
                "   - **Secret**: something special about this topic which not accessible easily but you know\n\n" +
                "CRITICAL FORMATTING RULES:\n" +
                "\n- The Java snippet MUST NOT exceed 30 lines." +
                "\n- If the logic is long, use '...' to omit standard getters/setters." +
                "\n- Focus only on the core logic to keep the image height readable on mobile."+
                "- DO NOT use asterisks (*) for bolding or lists.\n" +
                "- Use plain CAPITAL LETTERS for headings.\n" +
                "- Use simple dashes (-) for bullet points.\n" +
                "- Ensure the output is clean, professional, and free of any Markdown symbols.\n\n" +
                "FORMAT YOUR RESPONSE EXACTLY AS: CODE_START: [code] INSTAGRAM_START: [caption] BLOG_START: [long text]";

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", topic)
                ),
                "temperature", 0.5
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(GROQ_URL, entity, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
            return "Error: Empty AI response.";
        } catch (Exception e) {
            return "Error calling Groq: " + e.getMessage();
        }
    }
}