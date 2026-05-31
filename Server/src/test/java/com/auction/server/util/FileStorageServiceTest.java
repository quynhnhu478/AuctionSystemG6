package com.auction.server.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.apache.tomcat.util.http.fileupload.FileUtils.deleteDirectory;
import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {
        private FileStorageService fileStorageService;
        private String currentSubFolder;
        @TempDir
        Path tempDir;

        @BeforeEach
        void setUp(){
            fileStorageService = new FileStorageService();
            currentSubFolder = null;

        }
    @AfterEach
    void tearDown() {
        if (currentSubFolder != null) {
            // Đường dẫn cụ thể đến thư mục con nằm trong uploads/
            File subDir = new File("uploads/" + currentSubFolder);
            if (subDir.exists()) {
                deleteDirectory(subDir);
            }
        }
    }
    @Test
    void testSaveImage_Success() throws Exception {
        // mot file anh/text
        String dummyText = "NguyenVanA";
        String base64Image = Base64.getEncoder().encodeToString(dummyText.getBytes());
        currentSubFolder = "avatars";


        String resultPath = fileStorageService.saveImage(base64Image, currentSubFolder);

        assertNotNull(resultPath, "Đường dẫn trả về không được null");
        assertTrue(resultPath.startsWith("/uploads/avatars/"), "Đường dẫn phải bắt đầu bằng /uploads/avatars/");
        assertTrue(resultPath.endsWith(".jpg"), "File sinh ra phải có đuôi .jpg");


        File physicalFile = new File(resultPath.substring(1));
        assertTrue(physicalFile.exists(), "File thực tế phải tồn tại trên ổ đĩa");


        byte[] fileBytes = Files.readAllBytes(physicalFile.toPath());
        assertEquals(dummyText, new String(fileBytes), "Nội dung file sau khi giải mã Base64 phải trùng khớp");

    }

    @Test
    void testSaveImage_InvalidBase64_ThrowsException() {
        // Chuẩn bị một chuỗi không phải Base64 hợp lệ (chứa ký tự đặc biệt không hợp lệ)
        String invalidBase64 = "Không phải Base64 hợp lệ!";
        currentSubFolder = "errors";

        // Kiểm tra xem hàm có ném ra RuntimeException ko
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.saveImage(invalidBase64, currentSubFolder);
        });

        assertTrue(exception.getMessage().contains("Error system: cannot save image"));

    }
    private void deleteDirectory(File directorytoBeDeleted) {
        File[] allContents = directorytoBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directorytoBeDeleted.delete();
    }

}