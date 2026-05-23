package com.auction.client.controller.seller;

import com.auction.common.payload.SellerRegistrationRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

/* 
- Controller điều khiển giao diện Đăng ký thông tin Người bán (Register as a Seller)
- Quản lý việc tải ảnh Căn cước công dân (mặt trước/mặt sau) và xử lý chuyển đổi giữa các Dialog
 */
public class RegisterSellerDialogController {

    @FXML
    private ImageView frontImageView; // ô hiển thị hình ảnh mặt trước ID Card
    @FXML
    private ImageView backImageView; // ô hiển thị hình ảnh mặt sau ID Card
    @FXML
    private ImageView frontPlaceholderImage; // icon upload mặt trước
    @FXML
    private ImageView backPlaceholderImage; // icon upload mặt sau
    @FXML
    private Label frontUploadLabel;
    @FXML
    private Label backUploadLabel;
    @FXML
    private Label uploadErrorLabel; // dòng thông báo lỗi khi xác thực dữ liệu đầu vào
    @FXML
    private Button submitButton; // nút gửi đơn đăng ký (Submit)


    private File frontImageFile; // lưu trữ file ảnh mặt trước được chọn từ máy tính
    private File backImageFile; // lưu trữ file ảnh mặt sau được chọn từ máy tính
    private Stage parentStage; // lưu tham chiếu của Stage cha (Cửa sổ nhập form thông tin trước đó)

    private String base64ImageFront;
    private String base64ImageBack;
    private SellerRegistrationRequest request;
    private boolean submitPressed = false;
    // Hàm nhận request từ lớp trung tâm truyền sang
    public void setRegistrationRequest(SellerRegistrationRequest request) {
        this.request = request;
    }

    /*
    - Xử lý sự kiện khi người dùng ấn vào khu vực tải ảnh Mặt Trước ID Card
    - Mở hộp thoại chọn tệp tin và cập nhật hình ảnh lên giao diện
     */
    @FXML
    private void handleUploadFront() {
        File file = chooseImageFile(); //hàm mở hộp thoại FileChooser
        if (file != null) {
            try{
                frontImageFile = file; //lưu trữ file phục vụ cho việc gửi dữ liệu sau này
                frontImageView.setImage(new Image(file.toURI().toString())); //chuyển file thành chuỗi URI để hiển thị lên ImageView
                frontPlaceholderImage.setVisible(false);
                frontUploadLabel.setVisible(false);

                byte[] fileContent = Files.readAllBytes(file.toPath());
                base64ImageFront = Base64.getEncoder().encodeToString(fileContent);
                request.setIdentifiedImageFront(base64ImageFront);

                uploadErrorLabel.setText("");//xóa dòng cảnh báo lỗi cũ nếu có

        }catch(Exception e){
            e.printStackTrace();

            uploadErrorLabel.setText("Upload Failed");
            }
        }
    }

    /*
    - Xử lý sự kiện khi người dùng ấn vào khu vực tải ảnh Mặt Sau ID Card
    - Mở hộp thoại chọn tệp tin và cập nhật hình ảnh lên giao diện
     */
    @FXML
    private void handleUploadBack() {
        File file = chooseImageFile();
        if (file != null) {
            try {
                backImageFile = file;
                backImageView.setImage(new Image(file.toURI().toString()));
                backPlaceholderImage.setVisible(false);
                backUploadLabel.setVisible(false);

                byte[] fileContent = Files.readAllBytes(file.toPath());
                base64ImageBack = Base64.getEncoder().encodeToString(fileContent);
                request.setIdentifiedImageBehind(base64ImageBack);

                uploadErrorLabel.setText("");

            }catch(Exception e){
                e.printStackTrace();

                uploadErrorLabel.setText("Upload Failed");
            }
        }
    }

    public String getBase64Front() { return base64ImageFront; }
    public String getBase64Behind() { return base64ImageBack; }
    public boolean isSubmitPressed() { return submitPressed; }

    /*
    - Thiết lập tham chiếu Stage cha cho Controller này
    - Được gọi từ Controller trước đó khi chuyển tiếp sang màn hình upload ảnh
     */
    public void setParentStage(Stage stage) {
        this.parentStage = stage;
    }

    /*
     - Hàm dùng chung để mở hộp thoại hệ thống FileChooser
     - Giới hạn người dùng chỉ được lựa chọn định dạng file là định dạng hình ảnh PNG
    */
    private File chooseImageFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select PNG image");
        
        //cấu hình bộ lọc định dạng tệp tin, buộc chỉ hiển thị file có các đuôi này
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp, *.gif)",
                "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        
        //lấy Stage hiện tại của nút bấm để làm điểm neo (Owner Window) cho hộp thoại
        Stage stage = (Stage) submitButton.getScene().getWindow();
        return chooser.showOpenDialog(stage); //mở hộp thoại ở chế độ đồng bộ (Blocking call)
    }

    /*
    - Xử lý sự kiện khi nhấn nút "Submit"
    - Xác thực xem người dùng đã tải lên đầy đủ 2 mặt ảnh chưa, nếu đạt điều kiện sẽ tiếp tục bước tiếp theo
     */
    @FXML
    private void handleSubmit(ActionEvent event) {
        //check điều kiện bắt buộc: phải chọn đủ file cho cả mặt trước và mặt sau
        if (request.getIdentifiedImageFront() == null || request.getIdentifiedImageBehind() == null) {
            uploadErrorLabel.setText("You must upload both PNG images before submitting."); //thông báo lỗi
            return; //không cho gửi đơn
        }
        this.submitPressed = true;
//        openApplicationSubmittedDialog(); //mở Dialog thông báo nộp đơn thành công
        closeDialog(event); //đóng cửa sổ upload ảnh hiện tại
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
    }

    //Xử lý sự kiện khi người dùng nhấn nút "Back" để quay lại form nhập liệu trước
    @FXML
    private void handleBack(ActionEvent event) {
        closeDialog(event); //chỉ đóng cửa sổ hiện tại (cửa sổ cha vẫn đang hiển thị phía sau)
    }

    //Hàm dùng chung để đóng nhanh một cửa sổ Stage hiện tại dựa trên sự kiện kích hoạt
    private void closeDialog(ActionEvent event) {
        //truy vết từ Node (nút bấm) phát ra sự kiện -> lấy Scene -> lấy Stage đang chứa Scene đó
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close(); //đóng Stage
    }
}