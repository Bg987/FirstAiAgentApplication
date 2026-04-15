package com.example.FirstAiAgent.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "masterclass_posts")
@Data
public class MasterclassPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;

    // Remove @Lob! Use columnDefinition = "text" for Postgres
    @Column(columnDefinition = "text")
    private String instagramCaption;

    @Column(columnDefinition = "text")
    private String vastInformation;

    private String imageUrl;
    private String instagramPermalink;
    private LocalDateTime createdAt = LocalDateTime.now();
}