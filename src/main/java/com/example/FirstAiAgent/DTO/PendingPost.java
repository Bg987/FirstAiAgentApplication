package com.example.FirstAiAgent.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingPost {
    private String topic;
    private String prompt;
    private String caption;
    private String imageUrl;
}
