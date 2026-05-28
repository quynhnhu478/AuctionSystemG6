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
import java.util.logging.Level;
import java.util.logging.Logger;

/* - Controller điều khiển giao diện Đăng ký thông tin Người bán (Register as a Seller)
- Quản lý việc tải ảnh Căn cước công dân (mặt trước/mặt sau) và xử lý chuyển đổi giữa các Dialog
 */
public class RegisterSellerDialogController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(RegisterSellerDialogController.class.getName());

    @FXML
    private ImageView frontImageView; // Ô hiển thị hình ảnh mặt trước của thẻ ID/CCCD
    @FXML
    private ImageView backImageView; // Ô hiển thị hình ảnh mặt sau của thẻ ID/CCCD
    @FXML
    private ImageView frontPlaceholderImage; // Icon hiển thị mặc định (placeholder) khi chưa upload mặt trước
    @FXML
    private ImageView backPlaceholderImage; // Icon hiển thị mặc định (placeholder) khi chưa upload mặt sau
    @FXML
    private Label frontUploadLabel;
    @FXML
    private Label backUploadLabel;
    @FXML
    private Label uploadErrorLabel; // Nhãn hiển thị thông báo cảnh báo khi dữ liệu đầu vào không hợp lệ
    @FXML
    private Button submitButton; // Nút xác nhận gửi đơn đăng ký thông tin người bán


    private File frontImageFile; // Lưu trữ đối tượng file ảnh mặt trước được chọn từ ổ đĩa cục bộ
    private File backImageFile; // Lưu trữ đối tượng file ảnh mặt sau được chọn từ ổ đĩa cục bộ
    private Stage parentStage; // Lưu tham chiếu đến cửa sổ Stage cha đang quản lý form trước đó

    private String base64ImageFront;
    private String base64ImageBack;
    private SellerRegistrationRequest request;
    private boolean submitPressed = false;

    // Nhận dữ liệu request context được truyền xuống từ bộ điều phối controller trung tâm
    public void setRegistrationRequest(SellerRegistrationRequest request) {
        this.request = request;
    }

    /*
    - Xử lý sự kiện kích hoạt khi người dùng nhấn chuột vào vùng tải ảnh Mặt Trước ID/CCCD
    - Hiển thị hộp thoại chọn file hệ thống và ánh dẫn hình ảnh đã chọn lên khung nhìn UI
     */
    @FXML
    private void handleUploadFront() {
        File file = chooseImageFile(); // Gọi hàm hiển thị hộp thoại FileChooser của hệ thống
        if (file != null) {
            try {
                frontImageFile = file; // Lưu lại file asset để chuẩn bị đóng gói vào payload request gửi đi
                frontImageView.setImage(new Image(file.toURI().toString())); // Chuyển đổi đường dẫn file hệ thống thành nút Image để hiển thị lên UI
                frontPlaceholderImage.setVisible(false);
                frontUploadLabel.setVisible(false);

                byte[] fileContent = Files.readAllBytes(file.toPath());
                base64ImageFront = Base64.getEncoder().encodeToString(fileContent);
                request.setIdentifiedImageFront(base64ImageFront);

                uploadErrorLabel.setText(""); // Xóa bỏ dòng thông báo lỗi cũ nếu có trước đó

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Gặp ngoại lệ khi phân tích siêu dữ liệu mảng byte của tài liệu mặt trước CCCD.", e);
                uploadErrorLabel.setText("Upload Failed");
            }
        }
    }

    /*
    - Xử lý sự kiện kích hoạt khi người dùng nhấn chuột vào vùng tải ảnh Mặt Sau ID/CCCD
    - Hiển thị hộp thoại chọn file hệ thống và ánh dẫn hình ảnh đã chọn lên khung nhìn UI
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

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Gặp ngoại lệ khi phân tích siêu dữ liệu mảng byte của tài liệu mặt sau CCCD.", e);
                uploadErrorLabel.setText("Upload Failed");
            }
        }
    }

    public String getBase64Front() { return base64ImageFront; }
    public String getBase64Behind() { return base64ImageBack; }
    public boolean isSubmitPressed() { return submitPressed; }

    /*
    - Thiết lập tham chiếu Stage cha để theo dõi ranh giới ngữ cảnh của các cửa sổ window
    - Được gọi từ tầng controller logic trước đó trước khi chuyển tiếp màn hình upload ảnh tại đây
     */
    public void setParentStage(Stage stage) {
        this.parentStage = stage;
    }

    /*
     - Hàm tiện ích chung dùng để mở thành phần FileChooser tiêu chuẩn của hệ thống
     - Lọc định dạng asset nhằm đảm bảo mục tiêu chọn lựa của người dùng khớp với các định dạng ảnh máy tính thông dụng
    */
    private File chooseImageFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image File");

        // Thiết lập cấu hình bộ lọc đuôi mở rộng để chỉ cho phép hiển thị các tệp hình ảnh một cách tường minh
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp, *.gif)",
                "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));

        // Truy vết stage chứa nút bấm hiện tại để làm điểm neo (Owner Window) định vị cho hộp thoại overlay
        Stage stage = (Stage) submitButton.getScene().getWindow();
        return chooser.showOpenDialog(stage); // Lệnh gọi đồng bộ (Blocking call) để hiển thị hộp thoại lựa chọn file
    }

    /*
    - Xử lý tín hiệu kích hoạt khi người dùng nhấn nút xác nhận gửi (Submit) trên workflow giao diện
    - Xác thực sự hiện diện của các file ảnh trong bộ nhớ trước khi đưa các phần tử vào đường ống xử lý tiếp theo
     */
    @FXML
    private void handleSubmit(ActionEvent event) {
        // Bắt buộc điều kiện dữ liệu: Phải chọn đủ file cho cả 2 vị trí trường dữ liệu trong request entity payload
        if (request.getIdentifiedImageFront() == null || request.getIdentifiedImageBehind() == null) {
            uploadErrorLabel.setText("You must upload both images before submitting.");
            return; // Hủy bỏ tiến trình gửi dữ liệu ra ngoài do thiếu tham số bắt buộc
        }
        this.submitPressed = true;
        closeDialog(event); // Tắt bớt khung hiển thị đính kèm thẻ đang hoạt động để làm sạch layout ngữ cảnh
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
    }

    // Xử lý các kích hoạt quay lui (Rollback) trạng thái khi người dùng nhấn nút quay lại (Back) trên giao diện
    @FXML
    private void handleBack(ActionEvent event) {
        closeDialog(event); // Đóng cửa sổ layout hiện tại một cách an toàn (giữ nguyên form nhập liệu gốc nằm ẩn phía sau)
    }

    // Hàm trợ giúp trừu tượng xử lý nhanh quy trình giải phóng và đóng thực thể cửa sổ Stage cục bộ dựa trên event
    private void closeDialog(ActionEvent event) {
        // Truy vết nguồn phát sinh sự kiện origin -> lấy ra node scene tương ứng -> tìm đến window stage bọc ngoài cùng
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close(); // Chấm dứt vòng lặp thực thi của cửa sổ window ngữ cảnh
    }
}