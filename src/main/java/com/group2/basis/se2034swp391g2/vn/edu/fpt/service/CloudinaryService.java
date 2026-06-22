package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /*
     * Upload ảnh phòng.
     */
    public Map uploadRoomImage(MultipartFile file) {
        return uploadImage(file, "vihotel/rooms", "Vui lòng chọn ảnh phòng.");
    }

    /*
     * Upload ảnh khuyến mãi.
     */
    public Map uploadPromotionImage(MultipartFile file) {
        return uploadImage(file, "vihotel/promotions", "Vui lòng chọn ảnh khuyến mãi.");
    }

    /*
     * Upload ảnh dịch vụ.
     */
    public String uploadServiceImage(MultipartFile file) {
        Map uploadResult = uploadImage(file, "vihotel/services", "Vui lòng chọn ảnh dịch vụ.");

        Object secureUrl = uploadResult.get("secure_url");

        if (secureUrl == null) {
            throw new IllegalStateException("Cloudinary không trả về URL ảnh dịch vụ.");
        }

        return secureUrl.toString();
    }

    /*
     * Upload ảnh đại diện người dùng.
     */
    public String uploadAvatar(MultipartFile file) {
        Map uploadResult = uploadImage(file, "vihotel/avatars", "Vui lòng chọn ảnh đại diện.");

        Object secureUrl = uploadResult.get("secure_url");

        if (secureUrl == null) {
            throw new IllegalStateException("Cloudinary không trả về URL ảnh.");
        }

        return secureUrl.toString();
    }

    /*
     * Upload ảnh lên Cloudinary theo folder.
     */
    private Map uploadImage(MultipartFile file, String folder, String emptyFileMessage) {
        validateImageFile(file, emptyFileMessage);

        try {
            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException("Tải ảnh lên Cloudinary thất bại.");
        }
    }

    /*
     * Kiểm tra file ảnh trước khi upload.
     */
    private void validateImageFile(MultipartFile file, String emptyFileMessage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(emptyFileMessage);
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File tải lên phải là ảnh.");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Kích thước ảnh không được vượt quá 5MB.");
        }
    }
}