package com.auction.server.service;
import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public Map upload(MultipartFile file)  {
        try{
            Map data = this.cloudinary.uploader().upload(file.getBytes(), Map.of("resource_type", "image"));
            return data;
        }catch (IOException io){
            throw new RuntimeException("Image upload fail");
        }
    }

    public String uploadBase64Image(String image, String subFolder) {
        try {
            String cleanImage = image.contains(",") ? image.substring(image.indexOf(",") + 1) : image;
            byte[] imageBytes = Base64.getDecoder().decode(cleanImage);
            Map data = this.cloudinary.uploader().upload(
                    imageBytes,
                    Map.of(
                            "resource_type", "image",
                            "folder", "auction/" + subFolder
                    )
            );
            Object secureUrl = data.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new RuntimeException("Cloudinary response does not contain secure_url");
            }
            return secureUrl.toString();
        } catch (Exception e) {
            throw new RuntimeException("Image upload to Cloudinary failed: " + e.getMessage(), e);
        }
    }
}
