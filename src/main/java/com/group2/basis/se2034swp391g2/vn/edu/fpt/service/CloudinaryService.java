package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map uploadRoomImage(MultipartFile file) {
        try {
            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "vihotel/rooms",
                            "resource_type", "image"
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException("Upload image failed");
        }
    }

    public String uploadAvatar(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "vihotel/avatars",
                            "resource_type", "image"
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");

            if (secureUrl == null) {
                throw new IllegalStateException("Cloudinary did not return secure_url.");
            }

            return secureUrl.toString();

        } catch (IOException e) {
            throw new IllegalStateException("Upload avatar failed.", e);
        }
    }
}