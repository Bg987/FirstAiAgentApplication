package com.example.FirstAiAgent.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(byte[] imageBytes) {
        try {
            Map params = ObjectUtils.asMap(
                    "folder", "agent",
                    "resource_type", "image"
            );
            Map result = cloudinary.uploader().upload(imageBytes, params);
            return (String) result.get("secure_url");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}