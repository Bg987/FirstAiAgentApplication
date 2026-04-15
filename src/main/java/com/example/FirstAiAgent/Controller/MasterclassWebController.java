package com.example.FirstAiAgent.Controller;

import com.example.FirstAiAgent.Entity.MasterclassPost;
import com.example.FirstAiAgent.Service.MasterclassWebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/masterclass")
@CrossOrigin(origins = "https://java-blond-ten.vercel.app") // Connects to your Angular port
public class MasterclassWebController {

    @Autowired
    private MasterclassWebService webService;

    // API 1: Fetch all posts for the List View
    @GetMapping
    public List<MasterclassPost> getAllPosts() {
        return webService.getAllMasterclasses();
    }

    // API 2: Fetch a single post for the Detail View
    @GetMapping("/{id}")
    public ResponseEntity<MasterclassPost> getPostById(@PathVariable Long id) {
        return webService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}