package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppContext;
import com.auction.client.service.SceneService;
import com.auction.client.service.Session;
import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ElectronicsRequest;
import com.auction.common.payload.ArtRequest;
import com.auction.common.payload.VehicleRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.auction.client.service.AlertService.showAlert;

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
    private DatePicker startingDatePicker;
    @FXML
    private Spinner<Integer> startingHourSpinner;
    @FXML
    private Spinner<Integer> startingMinuteSpinner;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Spinner<Integer> endHourSpinner;
    @FXML
    private Spinner<Integer> endMinuteSpinner;
    @FXML
    private ImageView productImageView;
    @FXML
    private Label uploadHintLabel;

    private final List<File> selectedImageFiles = new ArrayList<>();

    //biến dùng để kết nối với trang chứa card item
    private ItemContainerController itemContainerController;

    private boolean isEditMode = false;   // cờ phân biệt Mode Add và Update
    private Long itemIdForEdit;     //Lưu ID sản phẩm cần sửa
    private CardItemController cardItemController;  //lưu controller của tấm card gốc để update giao diện

    private final ObjectMapper objectMapper = new JsonMapper().builder().addModule(new JavaTimeModule()).build();

    //gọi hàm này khi muốn biến Form thành Form Update
    public void setEditData(Long id, String title, String description, String category, double price, LocalDateTime startingTime, LocalDateTime endTime, String imagePathOrBase64, CardItemController cardItemController) {
        this.isEditMode = true;
        this.itemIdForEdit = id;
        this.cardItemController = cardItemController;

        //Đổ dữ liệu cũ vào các ô giao diện
        listingTitleField.setText(title);
        descriptionField.setText(description);
        categoryChoiceBox.setValue(category);
        startingPriceField.setText(String.valueOf(price));

        startingDatePicker.setValue(startingTime.toLocalDate());
        startingHourSpinner.getValueFactory().setValue(startingTime.getHour());
        startingMinuteSpinner.getValueFactory().setValue(startingTime.getMinute());

        endDatePicker.setValue(endTime.toLocalDate());
        endHourSpinner.getValueFactory().setValue(endTime.getHour());
        endMinuteSpinner.getValueFactory().setValue(endTime.getMinute());

        //xử lý ảnh cũ
        byte[] imageBytes = Base64.getDecoder().decode(imagePathOrBase64);
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        productImageView.setImage(new Image(bais));
        selectedImageFiles.clear();
        uploadHintLabel.setText("Using existing image");
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    public void initialize() {
        //Them du lieu vao choicebox
        categoryChoiceBox.getItems().setAll("ELECTRONICS", "VEHICLE", "ART");
        initTimePickers();
        resolveSellerId();
    }

    private void initTimePickers() {
        startingHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        startingMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1));
        endHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        endMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1));

        startingHourSpinner.setEditable(true);
        startingMinuteSpinner.setEditable(true);
        endHourSpinner.setEditable(true);
        endMinuteSpinner.setEditable(true);
    }

    //Phuong thuc de tai anh len
    @FXML
    public void handleSelectedImageFile(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select product photo");

        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp, *.gif)",
                "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        List<File> pickedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (pickedFiles != null && !pickedFiles.isEmpty()) {
            selectedImageFiles.clear();
            selectedImageFiles.addAll(pickedFiles);
            Image image = new Image(selectedImageFiles.get(0).toURI().toString());
            productImageView.setImage(image);
            uploadHintLabel.setText(selectedImageFiles.size() + " image(s) selected");
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
            LocalDateTime startingTime = buildDateTime(startingDatePicker.getValue(), startingHourSpinner, startingMinuteSpinner, "Starting Time");
            LocalDateTime endTime = buildDateTime(endDatePicker.getValue(), endHourSpinner, endMinuteSpinner, "End Time");

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
            List<String> imageBase64List = new ArrayList<>();
            for (File imageFile : selectedImageFiles) {
                byte[] fileContent = Files.readAllBytes(imageFile.toPath());
                imageBase64List.add(Base64.getEncoder().encodeToString(fileContent));
            }
            itemRequest.setImageBase64List(imageBase64List);
            itemRequest.setImageBase64(imageBase64List.isEmpty() ? "" : imageBase64List.get(0));

            Long sellerId = resolveSellerId();
            if (sellerId == null) {
                showAlert(Alert.AlertType.ERROR, "Login required", "Please login again before creating a listing.");
                return;
            }
            itemRequest.setSellerId(sellerId);

            if(isEditMode){
                //nếu đang là mode edit -> gửi request put
                sendUpdateRequestToServer(itemRequest);
            }
            else {
                //nếu là mode add -> gửi request post lên server
                sendCreateRequestToServer(itemRequest);
            }

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
                startingDatePicker.getValue() == null ||
                endDatePicker.getValue() == null;
    }

    private LocalDateTime buildDateTime(LocalDate date, Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner, String fieldName) {
        if (date == null) {
            showAlert(Alert.AlertType.ERROR, "Input error", fieldName + " is required.");
            throw new DateTimeParseException("Missing date", "", 0);
        }
        Integer hour = hourSpinner.getValue();
        Integer minute = minuteSpinner.getValue();
        if (hour == null || minute == null) {
            showAlert(Alert.AlertType.ERROR, "Input error", fieldName + " is invalid.");
            throw new DateTimeParseException("Missing time", "", 0);
        }
        return date.atTime(hour, minute, 0);
    }

    private void clearForm(){
        listingTitleField.clear();
        descriptionField.clear();
        startingPriceField.clear();
        categoryChoiceBox.setValue(null);
        bidIncrementField.clear();
        startingDatePicker.setValue(null);
        endDatePicker.setValue(null);
        startingHourSpinner.getValueFactory().setValue(0);
        startingMinuteSpinner.getValueFactory().setValue(0);
        endHourSpinner.getValueFactory().setValue(0);
        endMinuteSpinner.getValueFactory().setValue(0);
        selectedImageFiles.clear();
        uploadHintLabel.setText("Click or drag images here");
    }

    private Long resolveSellerId() {
        Long sellerId = AppContext.getInstance().getUserId();
        if (sellerId == null && Session.getUser() != null) {
            sellerId = Session.getUser().getId();
            AppContext.getInstance().setUserId(sellerId);
        }
        return sellerId;
    }

    private void sendCreateRequestToServer(ItemRequest itemRequest) throws Exception{
        Long sellerId = resolveSellerId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Login required", "Please login again before creating a listing.");
            return;
        }

        try{
            String jsonBody = objectMapper.writeValueAsString(itemRequest);

            //Tạo HttpClient và HttpRequest
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/items"))  //gửi đến địa chỉ server
                    .header("Content-Type", "application/json") //ghi chú
                    .header("Seller-ID", String.valueOf(sellerId))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))   //gửi bằng phương thức POST
                    .build();

            //Gửi bất đồng bộ
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {   //đoạn code chỉ chạy khi Server trả về kết quả
                        if(response.statusCode() >= 200 && response.statusCode() < 300){
                            //Đang ở luồng ngầm -> phải về Platform.runLater để quay về luồng giao diện
                            javafx.application.Platform.runLater(() -> {
                                try{
                                    //dùng objectMapper để đọc chuỗi Json được trả về thành JsonNode
                                    JsonNode jsonNode = objectMapper.readTree(response.body());

                                    Long savedItemid = jsonNode.get("id").asLong();

                                    showAlert(Alert.AlertType.INFORMATION, "Success", "Successfully created product auction!");
                                    clearForm();

                                    MainLayoutController mainLayoutController = AppContext.getInstance().getMainLayoutController();
                                    ItemContainerController itemContainerController = AppContext.getInstance().getItemContainerController();
                                    //truyền id thật sang cho conatainer
                                    //mỗi chiếc card item sẽ mang id thật
                                    if(itemContainerController == null){
                                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/item-container-view.fxml"));
                                        Parent itemContainerView = loader.load();  //kích hoạt hàm initialize ở lớp ItemContainerController để setItemControllerLayout của AppContext
                                        mainLayoutController.setCenterView(itemContainerView);
                                        itemContainerController = AppContext.getInstance().getItemContainerController();
                                    }

                                    if(itemContainerController != null){
                                        itemContainerController.addNewCardToGrid(
                                                savedItemid,
                                                itemRequest.getName(),
                                                itemRequest.getDescription(),
                                                itemRequest.getCategories().toString(),
                                                itemRequest.getPrice(),
                                                itemRequest.getStartingTime(),
                                                itemRequest.getEndTime(),
                                                itemRequest.getImageBase64List() != null && !itemRequest.getImageBase64List().isEmpty()
                                                        ? itemRequest.getImageBase64List().get(0)
                                                        : itemRequest.getImageBase64());
                                    }

                                    // Close the current dialog form stage
                                    Stage stage = (Stage) listingTitleField.getScene().getWindow();
                                    stage.close();
                                }catch (Exception e){
                                    e.printStackTrace();
                                    showAlert(Alert.AlertType.ERROR, "Parse Error", "Can not read ID from Server: " + e.getMessage());
                                }
                            });
                        }
                        else {
                            javafx.application.Platform.runLater(() -> {
                                String detail = response.body();
                                if (detail != null && detail.length() > 300) {
                                    detail = detail.substring(0, 300) + "...";
                                }
                                showAlert(Alert.AlertType.ERROR, "Server error", "Error code: " + response.statusCode() + "\n" + detail);
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

    //Hàm gửi request PUT lên server
    private void sendUpdateRequestToServer(ItemRequest itemRequest) {
        HttpClient client = HttpClient.newHttpClient();

        String jsonBody = "";
        try{
            jsonBody = objectMapper.writeValueAsString(itemRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/item/" + itemIdForEdit))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if(response.statusCode() == 200){
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.INFORMATION, "Success", "Product updated successfully!");

                                //Cập nhật trực tiếp trên tấm card gốc
                                if(cardItemController != null){
                                    //Gọi lại hàm setData của chiếc Card để nó tự đổi chữ, nhảy đồng hồ đếm ngược
                                    cardItemController.setData(
                                            itemIdForEdit,
                                            itemRequest.getName(),
                                            itemRequest.getDescription(),
                                            itemRequest.getCategories().toString(),
                                            itemRequest.getPrice(),
                                            itemRequest.getStartingTime(),
                                            itemRequest.getEndTime(),
                                            itemRequest.getImageBase64()
                                    );
                                }

                                //thay đổi nổi dung nút
                                createListingButton.setText("Update Details");

                                //Đóng form
                                Stage stage = (Stage) listingTitleField.getScene().getWindow();
                                stage.close();
                            });
                        }else {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Server error: " + response.statusCode() + "\nDetail: " + response.body()));
                        }
                    }).exceptionally(ex -> {   //đoạn code này chỉ chạy khi bị lỗi mạng
                        javafx.application.Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Connection error", "Unable to connect to the server.\nDetail: " + ex.getMessage());
                        });
                        return null;
                    });
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

}