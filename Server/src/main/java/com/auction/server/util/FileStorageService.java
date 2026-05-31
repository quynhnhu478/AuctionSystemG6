package com.auction.server.util;

import com.auction.server.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.image-storage:cloudinary}")
    private String imageStorage;

    @Autowired(required = false)
    private CloudinaryService cloudinaryService;

    public String saveImage(String image, String subFolder) {
        try {
            if (image == null || image.isBlank()) {
                throw new IllegalArgumentException("Image content is empty");
            }
            if (shouldUseCloudinary()) {
                String cloudinaryUrl = cloudinaryService.uploadBase64Image(image, subFolder);
                log.info("Saved image to Cloudinary. URL: {}", cloudinaryUrl);
                return cloudinaryUrl;
            }
            Path targetDir = Paths.get(resolveUploadDir()).resolve(subFolder).toAbsolutePath().normalize();
            File folder = targetDir.toFile();
            if (!folder.exists() && !folder.mkdirs()) {
                throw new IllegalStateException("Cannot create upload directory: " + targetDir);
            }

            String newFileName = UUID.randomUUID() + ".jpg";
            Path fileSavePath = targetDir.resolve(newFileName);
            String cleanImage = image.contains(",") ? image.substring(image.indexOf(",") + 1) : image;
            byte[] imageBytes = Base64.getDecoder().decode(cleanImage);

            try (OutputStream stream = new FileOutputStream(fileSavePath.toFile())) {
                stream.write(imageBytes);
            }

            String webPath = "/uploads/" + subFolder + "/" + newFileName;
            log.info("Saved image. Web URL: {}", webPath);
            return webPath;
        } catch (Exception e) {
            log.error("Cannot save image: {}", e.getMessage(), e);
            throw new RuntimeException("Error system: cannot save image " + e.getMessage(), e);
        }
    }

    public void deleteImage(String image) {
        if (image == null || image.isEmpty()) {
            return;
        }
        if (image.startsWith("http://") || image.startsWith("https://")) {
            return;
        }
        try {
            String relativePath = image.startsWith("/") ? image.substring(1) : image;
            if (relativePath.startsWith("uploads/")) {
                relativePath = relativePath.substring("uploads/".length());
            }

            Path fullPath = Paths.get(resolveUploadDir()).resolve(relativePath).toAbsolutePath().normalize();
            boolean isDeleted = Files.deleteIfExists(fullPath);
            if (isDeleted) {
                log.info("Deleted image: {}", fullPath);
            } else {
                log.warn("Image not found for deletion: {}", fullPath);
            }
        } catch (Exception e) {
            log.error("Cannot delete image: {}", e.getMessage(), e);
        }
    }

    private String resolveUploadDir() {
        return uploadDir == null || uploadDir.isBlank() ? "uploads" : uploadDir;
    }

    private boolean shouldUseCloudinary() {
        return cloudinaryService != null && "cloudinary".equalsIgnoreCase(imageStorage);
    }
}
