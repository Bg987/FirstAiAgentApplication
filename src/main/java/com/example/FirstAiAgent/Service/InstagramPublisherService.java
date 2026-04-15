package com.example.FirstAiAgent.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class InstagramPublisherService {

    @Value("${instagram.access.token}")
    private String accessToken;

    @Value("${instagram.account.id}")
    private String instagramId;

    private final String BASE_URL = "https://graph.facebook.com/v22.0/";
    private final RestTemplate restTemplate = new RestTemplate();

    public String publishToInstagram(String imageUrl, String caption) {
        try {

            // ✅ Step 0: Validate image URL
            if (imageUrl == null || imageUrl.isEmpty()) {
                System.err.println("❌ Image URL is empty!");
                return null;
            }

            // ✅ Step 1: Safe caption (Instagram limit ~2200)
            String safeCaption = (caption != null && caption.length() > 2100)
                    ? caption.substring(0, 2097) + "..."
                    : caption;

            // ================================
            // ✅ STEP 1: CREATE MEDIA CONTAINER
            // ================================
            String containerUrl = BASE_URL + instagramId + "/media";

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("image_url", imageUrl);
            body.add("caption", safeCaption);
            body.add("access_token", accessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> containerResponse =
                    restTemplate.postForEntity(containerUrl, request, Map.class);

            if (containerResponse.getBody() == null || !containerResponse.getBody().containsKey("id")) {
                System.err.println("❌ Container creation failed: " + containerResponse.getBody());
                return null;
            }

            String creationId = containerResponse.getBody().get("id").toString();
            System.out.println("✅ Container created: " + creationId);

            // ================================
            // ✅ STEP 2: PUBLISH MEDIA
            // ================================
            String publishUrl = BASE_URL + instagramId + "/media_publish";

            MultiValueMap<String, String> publishBody = new LinkedMultiValueMap<>();
            publishBody.add("creation_id", creationId);
            publishBody.add("access_token", accessToken);

            HttpEntity<MultiValueMap<String, String>> publishRequest =
                    new HttpEntity<>(publishBody, headers);

            ResponseEntity<Map> publishResponse =
                    restTemplate.postForEntity(publishUrl, publishRequest, Map.class);

            if (publishResponse.getBody() == null || !publishResponse.getBody().containsKey("id")) {
                System.err.println("❌ Publish failed: " + publishResponse.getBody());
                return null;
            }

            String postId = publishResponse.getBody().get("id").toString();
            System.out.println("✅ Successfully posted! Post ID: " + postId);

            return postId;

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("❌ Meta API Error: " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.err.println("❌ General Error: " + e.getMessage());
            return null;
        }
    }

    public String getPostPermalink(String postId) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL + postId)
                    .queryParam("fields", "permalink")
                    .queryParam("access_token", accessToken)
                    .build()
                    .toUri();

            ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
            return response.getBody().get("permalink").toString();
        } catch (Exception e) {
            // Fallback: If the API call fails, construct a basic search link
            return "https://www.instagram.com/reels/videos/";
        }
    }

    public String makeInstagramReady(String publicUrl) {
        if (publicUrl == null) return null;

        // c_pad: Adds padding to reach the target size
        // ar_4:5: Forces a 4:5 Portrait aspect ratio (best for long code)
        // w_1080: Sets standard Instagram width
        // b_rgb:1e1e1e: Matches the dark code editor background
        return publicUrl.replace("/upload/", "/upload/c_pad,ar_4:5,w_1080,b_rgb:1e1e1e/");
    }
}