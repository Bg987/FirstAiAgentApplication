package com.example.FirstAiAgent.Service;

import com.example.FirstAiAgent.Entity.MasterclassPost;
import com.example.FirstAiAgent.Repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MasterclassWebService {

    @Autowired
    private PostRepository repository;

    public List<MasterclassPost> getAllMasterclasses() {
        // Fetches everything from your PostgreSQL table
        return repository.findAll();
    }

    public Optional<MasterclassPost> getPostById(Long id) {
        return repository.findById(id);
    }
}