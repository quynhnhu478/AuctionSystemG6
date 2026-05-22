package com.auction.client.controller.seller;

import com.auction.client.service.AppContext;
import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ElectronicsRequest;
import com.auction.common.payload.ArtRequest;
import com.auction.common.payload.VehicleRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;


import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public class AddProductController {
    @FXML
    private TextField listingTitleField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField startingPriceField;
    @FXML
    private ChoiceBox<String> categoryChoiceBox;
    @FXML
    private Button createListingButton;
    @FXML
    private Button cancelButton;
    @FXML
    private TextField bidIncrementField;
    @FXML
    private TextField startingTimeField;
    @FXML
    private TextField endTimeField;
    @FXML
    private ImageView productImageView;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private File selectedImageFile;

    @FXML
    public void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    //Them du lieu vao choicebox
    @FXML
    public void initialize() {
        categoryChoiceBox.getItems().setAll("ELECTRONICS", "VEHICLE", "ART");
    }

    //Phuong thuc de tai anh len
    @FXML
    public void handleSelectedImageFile(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select product photo");

        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp, *.gif)",
                "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            Image image = new Image(selectedImageFile.toURI().toString());
            productImageView.setImage(image);
        }
    }

    //xu ly gom du lieu va gui xuong Server
    @FXML
    public void handleCreateListing(ActionEvent event) {
        try{
            //kiem tra xem da nhap du chua
            if(isInputInvalid()){
                showAlert(Alert.AlertType.ERROR, "Input error", "Please fill in all required fields.");
                return;
            }

            //kiểm tra và chuyển đổi định dạng ngày tháng
            LocalDateTime startingTime = parseDateTime(startingTimeField.getText(), "Starting Time");
            LocalDateTime endTime = parseDateTime(endTimeField.getText(), "End Time");

            if(startingTime.isAfter(endTime)){
                showAlert(Alert.AlertType.ERROR, "Timing error", "Invalid time!");
                return;
            }

            //khởi tạo đối tượng Request dựa trên danh mục được chọn
            String selectedCategory = categoryChoiceBox.getValue();
            ItemRequest itemRequest;

            if("ELECTRONICS".equals(selectedCategory)){
                itemRequest = new ElectronicsRequest();
            }
            else if("VEHICLE".equals(selectedCategory)){
                itemRequest = new VehicleRequest();
            }
            else if("ART".equals(selectedCategory)){
                itemRequest = new ArtRequest();
            }else
                itemRequest = new ItemRequest();

            //gom dữ liệu từ FXML vào itemRequest
            itemRequest.setCategories(Categories.valueOf(selectedCategory));
            itemRequest.setName(listingTitleField.getText());
            itemRequest.setDescription(descriptionField.getText());
            itemRequest.setPrice(Double.parseDouble(startingPriceField.getText()));
            itemRequest.setBidIncrement(Double.parseDouble(bidIncrementField.getText()));
            itemRequest.setStartingTime(startingTime);
            itemRequest.setEndTime(endTime);

            //chuyển ảnh thành chuỗi base64
            String base64 = "";
            if(selectedImageFile != null){
                byte[] fileContent = Files.readAllBytes(selectedImageFile.toPath());
                base64 = Base64.getEncoder().encodeToString(fileContent);
            }
            itemRequest.setImageBase64(base64);

            //tiến hành gửi request lên server
            sendRequestToServer(itemRequest);
        }catch (NumberFormatException e){
            showAlert(Alert.AlertType.ERROR, "Number format error", "Starting price and required bidding step");
        }catch (DateTimeParseException e){
        }catch (Exception e){
            showAlert(Alert.AlertType.ERROR, "System error", "An error occurred: " + e.getMessage());
        }
    }

    //CÁC HÀM PHỤ TRỢ
    private boolean isInputInvalid(){
        return listingTitleField.getText().trim().isEmpty() ||
                categoryChoiceBox.getValue() == null ||
                descriptionField.getText().trim().isEmpty() ||
                startingPriceField.getText().trim().isEmpty() ||
                bidIncrementField.getText().trim().isEmpty() ||
                startingTimeField.getText().trim().isEmpty() ||
                endTimeField.getText().trim().isEmpty();
    }

    private LocalDateTime parseDateTime(String dateTime, String fieldName){
        try{
            return LocalDateTime.parse(dateTime, dateTimeFormatter);
        }catch (DateTimeParseException e){
            showAlert(Alert.AlertType.ERROR, "Format error", fieldName + "Incorrect format DD/MM/YYYY HH:MM");
            throw e;
        }
    }

    private void clearForm(){
        listingTitleField.clear();
        descriptionField.clear();
        startingPriceField.clear();
        categoryChoiceBox.setValue(null);
        bidIncrementField.clear();
        startingTimeField.clear();
        endTimeField.clear();
    }

    private void sendRequestToServer(ItemRequest itemRequest) throws Exception{
        //Dùng Jackson để parse Object thành JSON String
        //Cần đăng ký JavaTimeModule để Jackson hiểu được kiểu dữ liệu LocalDateTime
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        try{
            String jsonBody = objectMapper.writeValueAsString(itemRequest);

            //Tạo HttpClient và HttpRequest
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/items"))  //gửi đến địa chỉ server
                    .header("Content-Type", "application/json") //ghi chú
                    .header("Seller-ID", String.valueOf(AppContext.getInstance().getUserId()))  //Thêm token bảo mật
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))   //gửi bằng phương thức POST
                    .build();

            //Gửi bất đồng bộ
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {   //đoạn code chỉ chạy khi Server trả về kết quả
                        if(response.statusCode() == 201){
                            //Đang ở luồng ngầm -> phải về Platform.runLater để quay về luồng giao diện
                            javafx.application.Platform.runLater(() -> {
                                showAlert(Alert.AlertType.INFORMATION, "Success", "Successfully created product auction!");
                                clearForm();
                            });
                        }
                        else {
                            javafx.application.Platform.runLater(() -> {
                                showAlert(Alert.AlertType.ERROR, "Server error", "Error code: " + response.statusCode() + "\nDetail: " + response.body());
                            });
                        }
                    })
                    .exceptionally(ex -> {   //đoạn code này chỉ chạy khi bị lỗi mạng
                        javafx.application.Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Connection error", "Unable to connect to the server.\nDetail: " + ex.getMessage());
                        });
                        return null;
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void showAlert(Alert.AlertType type, String title, String message){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
