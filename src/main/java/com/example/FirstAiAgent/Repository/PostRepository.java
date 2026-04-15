package com.example.FirstAiAgent.Repository;

import com.example.FirstAiAgent.Entity.MasterclassPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<MasterclassPost, Long> {
}