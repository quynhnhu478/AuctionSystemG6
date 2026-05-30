package com.auction.client.controller.seller;

import com.auction.client.config.ApiConfig;
import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppContext;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.auction.client.service.AlertService.showAlert;
import com.auction.client.service.AppEventBus;

public class AddProductController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(AddProductController.class.getName());

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
    private Spinner<Integer> startingSecondSpinner;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Spinner<Integer> endHourSpinner;
    @FXML
    private Spinner<Integer> endMinuteSpinner;
    @FXML
    private Spinner<Integer> endSecondSpinner;
    @FXML
    private ImageView productImageView;
    @FXML
    private Label uploadHintLabel;

    private final List<File> selectedImageFiles = new ArrayList<>();

    // Bộ điều khiển kết nối với container chứa các thẻ sản phẩm (item cards)
    private ItemContainerController itemContainerController;

    private boolean isEditMode = false;   // Cờ phân biệt giữa chế độ Thêm mới và Cập nhật
    private Long itemIdForEdit;     // Lưu trữ ID của sản phẩm đang được chỉnh sửa
    private CardItemController cardItemController;  // Lưu tham chiếu đến bộ điều khiển card gốc để cập nhật lại UI

    private final ObjectMapper objectMapper = new JsonMapper().builder().addModule(new JavaTimeModule()).build();
    private String existingImageBase64;

    // Gọi phương thức này để chuyển Form sang Chế độ Cập nhật
    public void setEditData(Long id, String title, String description, String category, double price, double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, String imagePathOrBase64, CardItemController cardItemController) {
        this.isEditMode = true;
        this.itemIdForEdit = id;
        this.cardItemController = cardItemController;
        this.existingImageBase64 = imagePathOrBase64;

        // Đổ dữ liệu hiện có vào các thành phần điều khiển trên UI
        listingTitleField.setText(title);
        descriptionField.setText(description);
        categoryChoiceBox.setValue(category);
        startingPriceField.setText(String.valueOf(price));
        bidIncrementField.setText(String.valueOf(bidIncrement));

        startingDatePicker.setValue(startingTime.toLocalDate());
        startingHourSpinner.getValueFactory().setValue(startingTime.getHour());
        startingMinuteSpinner.getValueFactory().setValue(startingTime.getMinute());
        startingSecondSpinner.getValueFactory().setValue(startingTime.getSecond());

        endDatePicker.setValue(endTime.toLocalDate());
        endHourSpinner.getValueFactory().setValue(endTime.getHour());
        endMinuteSpinner.getValueFactory().setValue(endTime.getMinute());
        endSecondSpinner.getValueFactory().setValue(endTime.getSecond());

        createListingButton.setText("Update Details");

        // Xử lý hình ảnh cũ/hiện tại
        if (imagePathOrBase64 != null && !imagePathOrBase64.isBlank()) {
            try {
                if (imagePathOrBase64.startsWith("http")) {
                    productImageView.setImage(new Image(imagePathOrBase64, true));
                } else if (imagePathOrBase64.startsWith("/")) {
                    productImageView.setImage(new Image(ApiConfig.BASE_URL + imagePathOrBase64, true));
                } else if (imagePathOrBase64.length() > 100) {
                    byte[] imageBytes = Base64.getDecoder().decode(imagePathOrBase64);
                    productImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                } else {
                    File file = new File(imagePathOrBase64);
                    if (file.exists()) {
                        productImageView.setImage(new Image(file.toURI().toString()));
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Cannot load existing image", e);
            }
        }

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
        // Thêm các danh mục dữ liệu vào hộp chọn danh mục choice box
        categoryChoiceBox.getItems().setAll("ELECTRONICS", "VEHICLE", "ART");
        initTimePickers();
        resolveSellerId();
    }

    private void initTimePickers() {
        startingHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        startingMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1));
        startingSecondSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1));
        endHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        endMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1));
        endSecondSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1));

        startingHourSpinner.setEditable(true);
        startingMinuteSpinner.setEditable(true);
        startingSecondSpinner.setEditable(true);
        endHourSpinner.setEditable(true);
        endMinuteSpinner.setEditable(true);
        endSecondSpinner.setEditable(true);
    }

    // Phương thức xử lý việc chọn tệp hình ảnh và tải lên
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

    // Thu thập tất cả thông tin payload từ form và gửi xuống phía Server
    @FXML
    public void handleCreateListing(ActionEvent event) {
        try{
            // Kiểm tra xem tất cả các trường bắt buộc đã được nhập đầy đủ chưa
            if(isInputInvalid()){
                showAlert(Alert.AlertType.ERROR, "Input error", "Please fill in all required fields.");
                return;
            }

            // Phân tích cú pháp và cấu trúc lại các thành phần ngày-giờ
            LocalDateTime startingTime = buildDateTime(startingDatePicker.getValue(), startingHourSpinner, startingMinuteSpinner, startingSecondSpinner, "Starting Time");
            LocalDateTime endTime = buildDateTime(endDatePicker.getValue(), endHourSpinner, endMinuteSpinner, endSecondSpinner, "End Time");

            if(startingTime.isAfter(endTime)){
                showAlert(Alert.AlertType.ERROR, "Timing error", "End time must be after starting time.");
                return;
            }

            // Khởi tạo đúng thực thể Request Model tương ứng với loại danh mục được chọn
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

            // Ràng buộc các giá trị lấy từ các trường FXML vào thực thể itemRequest mục tiêu
            itemRequest.setCategories(Categories.valueOf(selectedCategory));
            itemRequest.setName(listingTitleField.getText());
            itemRequest.setDescription(descriptionField.getText());
            itemRequest.setPrice(Double.parseDouble(startingPriceField.getText()));
            itemRequest.setBidIncrement(Double.parseDouble(bidIncrementField.getText()));
            itemRequest.setStartingTime(startingTime);
            itemRequest.setEndTime(endTime);

            // Mã hóa các tệp tin cục bộ đã chọn sang nội dung dữ liệu base64 để đóng gói vào payload
            List<String> imageBase64List = new ArrayList<>();
            itemRequest.setImageBase64List(imageBase64List);
            for (File imageFile : selectedImageFiles) {
                if (imageBase64List.isEmpty() && isEditMode) {
                    itemRequest.setImageBase64(null);
                } else {
                    itemRequest.setImageBase64(imageBase64List.isEmpty() ? "" : imageBase64List.get(0));
                }
                byte[] fileContent = Files.readAllBytes(imageFile.toPath());
                imageBase64List.add(Base64.getEncoder().encodeToString(fileContent));
            }

            if (imageBase64List.isEmpty() && isEditMode) {
                itemRequest.setImageBase64(null); // không gửi ảnh cũ lên server
            } else {
                itemRequest.setImageBase64(imageBase64List.isEmpty() ? "" : imageBase64List.get(0));
            }
            Long sellerId = resolveSellerId();
            if (sellerId == null) {
                showAlert(Alert.AlertType.ERROR, "Login required", "Please login again before creating a listing.");
                return;
            }
            itemRequest.setSellerId(sellerId);

            if(isEditMode){
                // Điều hướng sang xử lý tác vụ cập nhật bằng phương thức PUT
                sendUpdateRequestToServer(itemRequest);
            }
            else {
                // Điều hướng sang xử lý tác vụ khởi tạo mới bằng phương thức POST
                sendCreateRequestToServer(itemRequest);
            }

        }catch (NumberFormatException e){
            showAlert(Alert.AlertType.ERROR, "Number format error", "Starting price and required bidding step must be numeric values.");
        }catch (DateTimeParseException e){
            // Đã được xử lý hoặc ghi lại thông qua các tham số thay thế nếu cần thiết
        }catch (Exception e){
            logger.log(Level.SEVERE, "Gặp lỗi trong quá trình xử lý dữ liệu form sản phẩm: ", e);
            showAlert(Alert.AlertType.ERROR, "System error", "An error occurred: " + e.getMessage());
        }
    }

    // CÁC HÀM TIỆN ÍCH TRỢ GIÚP PHỤ TRỢ
    private boolean isInputInvalid(){
        return listingTitleField.getText().trim().isEmpty() ||
                categoryChoiceBox.getValue() == null ||
                descriptionField.getText().trim().isEmpty() ||
                startingPriceField.getText().trim().isEmpty() ||
                bidIncrementField.getText().trim().isEmpty() ||
                startingDatePicker.getValue() == null ||
                endDatePicker.getValue() == null;
    }

    private LocalDateTime buildDateTime(LocalDate date, Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner, Spinner<Integer> secondSpinner, String fieldName) {
        if (date == null) {
            showAlert(Alert.AlertType.ERROR, "Input error", fieldName + " is required.");
            throw new DateTimeParseException("Missing date", "", 0);
        }
        Integer hour = hourSpinner.getValue();
        Integer minute = minuteSpinner.getValue();
        Integer second = secondSpinner.getValue();
        if (hour == null || minute == null || second == null) {
            showAlert(Alert.AlertType.ERROR, "Input error", fieldName + " is invalid.");
            throw new DateTimeParseException("Missing time", "", 0);
        }
        return date.atTime(hour, minute, second);
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
        startingSecondSpinner.getValueFactory().setValue(0);
        endHourSpinner.getValueFactory().setValue(0);
        endMinuteSpinner.getValueFactory().setValue(0);
        endSecondSpinner.getValueFactory().setValue(0);
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
        String displayImage = itemRequest.getImageBase64();
        if ((displayImage == null || displayImage.isBlank()) && existingImageBase64 != null) {
            displayImage = existingImageBase64;
        }
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Login required", "Please login again before creating a listing.");
            return;
        }

        try{
            String jsonBody = objectMapper.writeValueAsString(itemRequest);

            // Thiết lập HttpClient và xây dựng các thành phần HttpRequest
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/items"))  // Điểm cuối endpoint đích mục tiêu
                    .header("Content-Type", "application/json")
                    .header("Seller-ID", String.valueOf(sellerId))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))   // Được phái đi thông qua kỹ thuật POST
                    .build();

            // Đường ống truyền tải bất đồng bộ (asynchronous) không gây nghẽn luồng (non-blocking)
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {   // Hàm callback được kích hoạt ngay sau khi có xác nhận phản hồi từ server
                        if(response.statusCode() >= 200 && response.statusCode() < 300){
                            // Chuyển quyền thực thi quay trở lại ngữ cảnh luồng UI Thread của ứng dụng JavaFX
                            javafx.application.Platform.runLater(() -> {
                                try{
                                    // Phân tích cú pháp payload phản hồi thành các phần tử cấu trúc có thể truy cập được
                                    JsonNode jsonNode = objectMapper.readTree(response.body());

                                    Long savedItemid = jsonNode.get("id").asLong();

                                    showAlert(Alert.AlertType.INFORMATION, "Success", "Successfully created product auction!");
                                    clearForm();

                                    MainLayoutController mainLayoutController = AppContext.getInstance().getMainLayoutController();
                                    ItemContainerController itemContainerController = AppContext.getInstance().getItemContainerController();

                                    if(itemContainerController == null){
                                        MainLayoutController.switchCenterView("/com/auction/client/fxml/seller/my-listings-view.fxml");
                                        itemContainerController = AppContext.getInstance().getItemContainerController();
                                    }

                                    if (itemContainerController != null) {
                                        com.auction.common.payload.ItemResponse created = new com.auction.common.payload.ItemResponse();
                                        created.setId(savedItemid);
                                        created.setName(itemRequest.getName());
                                        created.setDescription(itemRequest.getDescription());
                                        created.setCategories(itemRequest.getCategories());
                                        created.setPrice(itemRequest.getPrice());
                                        created.setStartingTime(itemRequest.getStartingTime());
                                        created.setBidIncrement(itemRequest.getBidIncrement());
                                        created.setEndTime(itemRequest.getEndTime());
                                        created.setSellerId(jsonNode.has("sellerId") && !jsonNode.get("sellerId").isNull()
                                                ? jsonNode.get("sellerId").asLong()
                                                : sellerId);
                                        created.setImageUrl(jsonNode.has("imageUrl") && !jsonNode.get("imageUrl").isNull()
                                                ? jsonNode.get("imageUrl").asText()
                                                : itemRequest.getImageBase64List() != null && !itemRequest.getImageBase64List().isEmpty()
                                                  ? itemRequest.getImageBase64List().get(0)
                                                  : itemRequest.getImageBase64());

                                        AppEventBus.emit("ITEM_CREATED", created);

                                        Stage stage = (Stage) listingTitleField.getScene().getWindow();
                                        stage.close();
                                    }
                                }catch (Exception e){
                                    logger.log(Level.SEVERE, "Gặp lỗi khi phân tích phản hồi tạo sản phẩm từ phía server: ", e);
                                    showAlert(Alert.AlertType.ERROR, "Parse Error", "Cannot read ID from Server: " + e.getMessage());
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
                    .exceptionally(ex -> {   // Được thực thi nghiêm ngặt trong trường hợp xảy ra nghẽn hoặc lỗi kết nối mạng
                        javafx.application.Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Connection error", "Unable to connect to the server.\nDetail: " + ex.getMessage());
                        });
                        return null;
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Xử lý việc truyền tải các yêu cầu cập nhật bằng phương thức PUT lên môi trường máy chủ backend
    private void sendUpdateRequestToServer(ItemRequest itemRequest) {
        HttpClient client = HttpClient.newHttpClient();

        String jsonBody = "";
        try{
            jsonBody = objectMapper.writeValueAsString(itemRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/items/" + itemIdForEdit))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            Platform.runLater(() -> {
                                try {
                                    String displayImage = itemRequest.getImageBase64();

                                    if ((displayImage == null || displayImage.isBlank()) && existingImageBase64 != null) {
                                        displayImage = existingImageBase64;
                                    }

                                    JsonNode jsonNode = null;
                                    if (response.body() != null && !response.body().isBlank() && !"null".equals(response.body())) {
                                        jsonNode = objectMapper.readTree(response.body());
                                    }

                                    LocalDateTime serverStartTime = jsonNode != null && jsonNode.has("startingTime") && !jsonNode.get("startingTime").isNull()
                                            ? objectMapper.convertValue(jsonNode.get("startingTime"), LocalDateTime.class)
                                            : itemRequest.getStartingTime();

                                    LocalDateTime serverEndTime = jsonNode != null && jsonNode.has("endTime") && !jsonNode.get("endTime").isNull()
                                            ? objectMapper.convertValue(jsonNode.get("endTime"), LocalDateTime.class)
                                            : itemRequest.getEndTime();

                                    LocalDateTime serverTime = jsonNode != null && jsonNode.has("serverTime") && !jsonNode.get("serverTime").isNull()
                                            ? objectMapper.convertValue(jsonNode.get("serverTime"), LocalDateTime.class)
                                            : LocalDateTime.now();

                                    String serverAuctionStatus = jsonNode != null && jsonNode.has("auctionStatus") && !jsonNode.get("auctionStatus").isNull()
                                            ? jsonNode.path("auctionStatus").asText()
                                            : resolveAuctionStatus(serverStartTime, serverEndTime);

                                    if (cardItemController != null) {
                                        cardItemController.setData(
                                                itemIdForEdit,
                                                jsonNode != null ? jsonNode.path("name").asText(itemRequest.getName()) : itemRequest.getName(),
                                                jsonNode != null ? jsonNode.path("description").asText(itemRequest.getDescription()) : itemRequest.getDescription(),
                                                jsonNode != null ? jsonNode.path("categories").asText(itemRequest.getCategories().toString()) : itemRequest.getCategories().toString(),
                                                jsonNode != null ? jsonNode.path("price").asDouble(itemRequest.getPrice()) : itemRequest.getPrice(),
                                                jsonNode != null ? jsonNode.path("bidIncrement").asDouble(itemRequest.getBidIncrement()) : itemRequest.getBidIncrement(),
                                                serverStartTime,
                                                serverEndTime,
                                                displayImage,
                                                cardItemController.getCurrentBidCount(),
                                                serverTime,
                                                serverAuctionStatus
                                        );
                                    }

                                    showAlert(Alert.AlertType.INFORMATION, "Success", "Product updated successfully!");

                                    Stage stage = (Stage) listingTitleField.getScene().getWindow();
                                    stage.close();

                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, "Cannot update card after server response", e);
                                    showAlert(Alert.AlertType.ERROR, "Update UI Error", e.getMessage());
                                }
                            });
                        }
                        else {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Server error: " + response.statusCode() + "\nDetail: " + response.body()));
                        }
                    }).exceptionally(ex -> {
                        javafx.application.Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Connection error", "Unable to connect to the server.\nDetail: " + ex.getMessage());
                        });
                        return null;
                    });
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private String resolveAuctionStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && now.isBefore(startTime)) {
            return "OPEN";
        }
        if (endTime != null && now.isAfter(endTime)) {
            return "FINISHED";
        }
        return "RUNNING";
    }

}
