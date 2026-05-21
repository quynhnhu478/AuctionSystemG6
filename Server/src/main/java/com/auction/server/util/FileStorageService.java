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
            String baseUploadDir = "uploads/";
            String fullTargetDir = baseUploadDir + subFolder + "/";
            File folder = new File(fullTargetDir);
            if (!folder.exists()) {
                folder.mkdir();
            }
            String newFileName = UUID.randomUUID().toString() + ".jpg";
            String fileSavePath = fullTargetDir + newFileName;
            byte[] imageBytes = Base64.getDecoder().decode(image);

            try (OutputStream stream = new FileOutputStream(fileSavePath)) {
                stream.write(imageBytes);
            }
            return "/" + fileSavePath;
        } catch (Exception e) {
            throw new RuntimeException("Error system: cannot save image " + e.getMessage());
        }
    }
    public void deleteImage(String image){
        if (image == null || image.isEmpty()){
            return;
        }
        try{
            String fixedPath = image.startsWith("/") ? image.substring(1) : image;
            Path path = Paths.get(fixedPath);
            Files.deleteIfExists(path);
        }catch (Exception e){
            System.out.println("Error: cannot delete image " + e.getMessage());
        }
    }
}