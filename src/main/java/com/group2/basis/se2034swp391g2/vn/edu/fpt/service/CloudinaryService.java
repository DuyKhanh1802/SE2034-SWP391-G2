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

    /*
     * Upload ảnh phòng lên Cloudinary.
     */
    public Map uploadRoomImage(MultipartFile file) {
        return uploadImage(file, "vihotel/rooms", "Vui lòng chọn ảnh phòng.");
    }

    /*
     * Upload ảnh khuyến mãi lên Cloudinary.
     */
    public Map uploadPromotionImage(MultipartFile file) {
        return uploadImage(file, "vihotel/promotions", "Vui lòng chọn ảnh khuyến mãi.");
    }

    /*
     * Hàm upload dùng chung cho nhiều loại ảnh.
     * Mỗi loại ảnh sẽ được lưu vào một folder riêng trên Cloudinary.
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
     * Kiểm tra file trước khi upload.
     * Chỉ cho phép file ảnh và dung lượng tối đa 5MB.
     */
    private void validateImageFile(MultipartFile file, String emptyFileMessage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(emptyFileMessage);
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File tải lên phải là ảnh.");
        }

        long maxSize = 5 * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Kích thước ảnh không được vượt quá 5MB.");
        }
    }
}