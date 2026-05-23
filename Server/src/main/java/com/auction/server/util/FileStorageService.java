package com.auction.server.util;


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

    public String saveImage(String image, String subFolder) {
        try {
            String baseUploadDir = "uploads";
            String fullTargetDir = baseUploadDir +File.separator+ subFolder +File.separator;
            File folder = new File(fullTargetDir);

            if (!folder.exists()) {
                folder.mkdirs();
            }
            String newFileName = UUID.randomUUID().toString() + ".jpg";
            String fileSavePath = fullTargetDir + newFileName;
            byte[] imageBytes = Base64.getDecoder().decode(image);

            try (OutputStream stream = new FileOutputStream(fileSavePath)) {
                stream.write(imageBytes);
            }
            return "/" + fileSavePath.toString().replace("\\", "/");
        } catch (Exception e) {
            throw new RuntimeException("Error system: cannot save image " + e.getMessage());
        }
    }
    public void deleteImage(String image){
        if (image == null || image.isEmpty()) {
            System.out.println("Đường dẫn ảnh rỗng hoặc null, bỏ qua xóa.");
            return;
        }
        try {
            String relativePath = image.startsWith("/") ? image.substring(1) : image;

            Path currentPath = Paths.get("").toAbsolutePath();
            Path fullPath;

            // Nếu hệ thống đang trỏ vào module Server, ta dùng .getParent() để lùi 1 cấp ra thư mục gốc lớn
            if (currentPath.toString().endsWith("Server")) {
                fullPath = currentPath.getParent().resolve(relativePath);
            } else {
                fullPath = currentPath.resolve(relativePath);
            }

            System.out.println("System đang tìm xóa file tại: " + fullPath.toAbsolutePath());

            boolean isDeleted = Files.deleteIfExists(fullPath);

            if (isDeleted) {
                System.out.println("==> XÓA ẢNH THÀNH CÔNG KHỎI THƯ MỤC UPLOADS!");
            } else {
                System.out.println("==> KHÔNG tìm thấy ảnh tại đường dẫn trên. Vui lòng check lại DB lưu gì.");
            }

        } catch (Exception e) {
            System.out.println("Lỗi hệ thống khi xóa ảnh: " + e.getMessage());
            e.printStackTrace();
        }
    }
}