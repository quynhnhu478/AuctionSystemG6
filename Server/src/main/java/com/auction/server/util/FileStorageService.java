package com.auction.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    // Khởi tạo Logger theo chuẩn SLF4J cho Spring Boot Service Utility
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    public String saveImage(String image, String subFolder) {
        try {
            String baseUploadDir = "uploads";
            String fullTargetDir = baseUploadDir + File.separator + subFolder + File.separator;
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

            String webPath = "/" + fileSavePath.replace("\\", "/");
            log.info("Lưu trữ tệp tin ảnh Base64 thành công. Web URL: {}", webPath);
            return webPath;
        } catch (Exception e) {
            log.error("Lỗi hệ thống: Không thể xử lý giải mã và lưu hình ảnh. Chi tiết: {}", e.getMessage(), e);
            throw new RuntimeException("Error system: cannot save image " + e.getMessage(), e);
        }
    }

    public void deleteImage(String image){
        if (image == null || image.isEmpty()) {
            log.info("Đường dẫn ảnh rỗng hoặc null, bỏ qua thao tác xóa file.");
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

            log.info("Hệ thống đang tìm để dọn dẹp file vật lý tại đường dẫn: {}", fullPath.toAbsolutePath());

            boolean isDeleted = Files.deleteIfExists(fullPath);

            if (isDeleted) {
                log.info("==> XÓA ẢNH THÀNH CÔNG KHỎI THƯ MỤC UPLOADS!");
            } else {
                log.warn("==> KHÔNG tìm thấy ảnh tại đường dẫn trên. Vui lòng kiểm tra lại cấu trúc bản ghi lưu trữ trong Cơ sở dữ liệu.");
            }

        } catch (Exception e) {
            log.error("Gặp lỗi ngoại lệ của hệ thống trong quá trình dọn dẹp, xóa file ảnh vật lý: {}", e.getMessage(), e);
        }
    }
}