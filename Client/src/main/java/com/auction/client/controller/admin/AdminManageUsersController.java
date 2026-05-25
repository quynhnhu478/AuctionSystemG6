package com.auction.client.controller.admin;

import com.auction.client.controller.seller.SellerRegistrationViewController;
import com.auction.client.service.AlertService;
import com.auction.client.service.SceneService;
import com.auction.common.payload.UserResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;

public class AdminManageUsersController {

    @FXML
    public void switchToManageAuctionButton(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminDashboard.fxml");
    }

    @FXML
    private TableView<UserResponse> tblUsers;
    @FXML
    private TableColumn<UserResponse, Long> colUserId;
    @FXML
    private TableColumn<UserResponse, String> colUsername;
    @FXML
    private TableColumn<UserResponse, String> colEmail;
    @FXML
    private TableColumn<UserResponse, String> colRole;
    @FXML
    private TableColumn<UserResponse, String> colBalance;
    @FXML
    private TableColumn<UserResponse, String>colSellerStatus;

    private ObservableList<UserResponse> userList = FXCollections.observableArrayList();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colSellerStatus.setCellValueFactory(new PropertyValueFactory<>("sellerStatus"));


        colRole.setCellValueFactory(cellData -> {
            Set<String> roles = cellData.getValue().getRoles();
            if (roles == null || roles.isEmpty()) {
                return new javafx.beans.property.SimpleStringProperty("Not have roles!");
            }

            String rolesString = String.join(", ", roles);
            return new javafx.beans.property.SimpleStringProperty(rolesString);
        });


        tblUsers.setItems(userList);

        loadDataFromServer();
    }

    private void loadDataFromServer() {

        new Thread(() -> {
            try {
                String apiUrl = "http://localhost:8080/api/admin/user_list";



                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());



                if (response.statusCode() == 200) {

                    List<UserResponse> serverUsers = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<UserResponse>>() {}
                    );


                    Platform.runLater(() -> {
                        userList.clear();
                        userList.addAll(serverUsers);
                        System.out.println("Pour data into TableView successfully!");
                    });
                } else {
                    System.err.println("Error Server returned code: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Unable to connect to Server: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    @FXML
    private void viewRequest(ActionEvent event) {
        UserResponse selectedUser = tblUsers.getSelectionModel().getSelectedItem();
        if (selectedUser == null){
            AlertService.showAlert(Alert.AlertType.WARNING, "WARN", "Please select a user!");
            return;
        }
        if (selectedUser.getSellerStatus()==null||selectedUser.getSellerStatus().equals("")){
            AlertService.showAlert(Alert.AlertType.WARNING, "WARN", "This user do not have a registration!");
            return;
        }
        if ("APPROVED".equalsIgnoreCase(selectedUser.getSellerStatus())){
            AlertService.showAlert(Alert.AlertType.WARNING, "WARN", "This user's registration is already approved!");
            return;
        }

        if ("REJECTED".equalsIgnoreCase(selectedUser.getSellerStatus())){
            AlertService.showAlert(Alert.AlertType.WARNING, "WARN", "This user's registration is already rejected!");
            return;
        }
        System.out.println("Get the user id"+ selectedUser.getId());
        openRegistrationDialod(selectedUser.getId());
    }
    public void openRegistrationDialod(Long UserId){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/admin/ReviewSellerRequest.fxml"));

            Parent root = fxmlLoader.load();
            ReviewSellerRequestController controller = fxmlLoader.<ReviewSellerRequestController>getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Seller Registration");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            controller.initData(UserId);
            dialogStage.showAndWait();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}