package com.auction.client.service;

import com.auction.client.config.ApiConfig;
import com.fasterxml.jackson.databind.ObjectMapper; // Hoặc dùng Gson tùy dự án
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

public class ImageUploadService {

    public String uploadFile(File file) {
        // Kết nối bằng cách lấy BASE_URL (http://localhost:8080) nối với Endpoint của Server
        String serverUrl = ApiConfig.BASE_URL + "/cloudinary/upload";
        String boundary = "JavaFX-" + UUID.randomUUID().toString();

        try {
            HttpClient client = HttpClient.newHttpClient();
            ArrayList<byte[]> byteArrays = new ArrayList<>();
            String mimeType = Files.probeContentType(file.toPath());

            // Cấu hình Multipart Form-Data khớp với @RequestParam("image") ở Server
            String header = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"image\"; filename=\"" + file.getName() + "\"\r\n" +
                    "Content-Type: " + mimeType + "\r\n\r\n";

            byteArrays.add(header.getBytes());
            byteArrays.add(Files.readAllBytes(file.toPath()));
            byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
                    .build();

            // Gửi dữ liệu tới Spring Boot Server
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Đọc chuỗi JSON trả về bằng ObjectMapper để lấy secure_url
                ObjectMapper mapper = new ObjectMapper();
                var res = mapper.readValue(response.body(), java.util.Map.class);
                return (String) res.get("secure_url"); // Trả về link ảnh cuối cùng
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}