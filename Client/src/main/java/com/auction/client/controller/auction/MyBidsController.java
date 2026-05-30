package com.auction.client.controller.auction;

import com.auction.client.config.ApiConfig;
import com.auction.client.service.AppEventBus;
import com.auction.client.service.Session;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.auction.client.service.WebsocketConfigService;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MyBidsController {
    @FXML
    private VBox bidsList;
    @FXML
    private Label emptyLabel;
    @FXML
    private Button refreshButton;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String currentCategoryFilter;
    private final Set<Long> subscribedAuctionIds = new HashSet<>();
    private final PauseTransition reloadDebounce = new PauseTransition(Duration.millis(250));
    private boolean initialized;

    public void setCategoryFilter(String category) {
        boolean changed = !sameCategory(currentCategoryFilter, category);
        this.currentCategoryFilter = category;
        if (initialized && changed) {
            loadMyBids();
        }
    }

    @FXML
    private void initialize() {
        initialized = true;
        AppEventBus.on("USER_AUCTION_UPDATED", message -> {
            reloadDebounce.setOnFinished(e -> loadMyBids());
            reloadDebounce.playFromStart();
        });
        loadMyBids();
    }

    @FXML
    private void loadMyBids() {
        if (Session.getUser() == null || Session.getUser().getId() == null) {
            showEmpty("Session expired. Please login again.");
            return;
        }
        refreshButton.setDisable(true);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildMyBidsUrl(Session.getUser().getId())))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> render(response)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        refreshButton.setDisable(false);
                        showEmpty("Could not connect to server.");
                    });
                    return null;
                });
    }

    private String buildMyBidsUrl(Long userId) {
        String url = "http://localhost:8080/api/bids/user/" + userId;
        if (currentCategoryFilter != null && !currentCategoryFilter.isBlank()) {
            url += "?category=" + currentCategoryFilter.trim().toUpperCase();
        }
        return url;
    }

    private void render(HttpResponse<String> response) {
        refreshButton.setDisable(false);
        bidsList.getChildren().clear();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            showEmpty("Failed to load your bids. Code: " + response.statusCode());
            return;
        }
        try {
            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                showEmpty("You have not placed any bids yet.");
                return;
            }
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);

            Map<Long, JsonNode> latestBidByAuctionId = new LinkedHashMap<>();
            for (JsonNode bid : root) {
                try {
                    String category = bid.path("categories").asText("");

                    if (currentCategoryFilter != null
                            && !currentCategoryFilter.isBlank()
                            && !currentCategoryFilter.equalsIgnoreCase(category)) {
                        continue;
                    }
                    Long auctionId = bid.path("auctionId").isMissingNode() || bid.path("auctionId").isNull()
                            ? null
                            : bid.path("auctionId").asLong();

                    if (auctionId != null && subscribedAuctionIds.add(auctionId)) {
                        WebsocketConfigService.getInstance().subscribeAuctionRoom(auctionId, message -> {
                            reloadDebounce.setOnFinished(e -> loadMyBids());
                            reloadDebounce.playFromStart();
                        });                    }

                    if (auctionId != null) {
                        JsonNode current = latestBidByAuctionId.get(auctionId);
                        if (current == null || isBetterBidRow(bid, current)) {
                            latestBidByAuctionId.put(auctionId, bid);
                        }
                    }
                } catch (Exception rowError) {
                    rowError.printStackTrace();
                }
            }

            for (JsonNode bid : latestBidByAuctionId.values()) {
                bidsList.getChildren().add(createBidRow(bid));
            }

            if (latestBidByAuctionId.isEmpty()) {
                showEmpty("You have not placed any bids in this category yet.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showEmpty("Failed to parse bid data.");
        }

    }

    private boolean isBetterBidRow(JsonNode candidate, JsonNode current) {
        double candidateAmount = candidate.path("bidAmount").asDouble(0);
        double currentAmount = current.path("bidAmount").asDouble(0);
        if (candidateAmount != currentAmount) {
            return candidateAmount > currentAmount;
        }

        LocalDateTime candidateTime = parseTime(candidate.path("bidTime").asText(""));
        LocalDateTime currentTime = parseTime(current.path("bidTime").asText(""));
        if (candidateTime == null) {
            return false;
        }
        if (currentTime == null) {
            return true;
        }
        return candidateTime.isAfter(currentTime);
    }

    private HBox createBidRow(JsonNode bid) {
        String itemName = bid.path("itemName").asText("Auction #" + bid.path("auctionId").asText(""));
        double bidAmount = bid.path("bidAmount").asDouble(0);
        double currentPrice = bid.path("currentPrice").asDouble(bidAmount);
        boolean winning = bid.path("winningBid").asBoolean(false);
        String imageUrl = bid.path("imageUrl").asText("");
        ImageView imageView = new ImageView();
        imageView.setFitWidth(90);
        imageView.setFitHeight(70);
        imageView.setPreserveRatio(true);

        String fullUrl = normalizeImageUrl(imageUrl);
        if (!fullUrl.isBlank()) {
            imageView.setImage(new Image(fullUrl, true));
        }
        VBox textBox = new VBox(4);
        Label title = new Label(itemName);
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label meta = new Label("Bid time: " + formatTime(bid.path("bidTime").asText("")));
        meta.setStyle("-fx-font-size: 12; -fx-text-fill: #64748B;");
        textBox.getChildren().addAll(title, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox priceBox = new VBox(4);
        priceBox.setStyle("-fx-alignment: center-right;");
        Label amount = new Label(String.format("Your bid: $%.2f", bidAmount));
        amount.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #10B981;");
        Label status = new Label(winning ? "Winning" : String.format("Current: $%.2f", currentPrice));
        status.setStyle(winning
                ? "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold;"
                : "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold;");
        priceBox.getChildren().addAll(amount, status);

        HBox row = new HBox(12, imageView, textBox, spacer, priceBox);
        row.setStyle("-fx-background-color: white; -fx-border-color: #d9c9be; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12 14 12 14;");
        return row;

    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        if (imageUrl.startsWith("http")) {
            return imageUrl;
        }
        if (imageUrl.startsWith("/")) {
            return ApiConfig.BASE_URL + imageUrl;
        }
        return ApiConfig.BASE_URL + "/uploads/items/" + imageUrl;
    }

    private String formatTime(String raw) {
        LocalDateTime parsed = parseTime(raw);
        if (parsed != null) {
            return parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        return raw == null ? "" : raw;
    }

    private LocalDateTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.replace(" ", "T"));
        } catch (Exception e) {
            return null;
        }
    }

    private void showEmpty(String message) {
        bidsList.getChildren().clear();
        emptyLabel.setText(message);
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }

    private boolean sameCategory(String first, String second) {
        String a = first == null ? "" : first.trim();
        String b = second == null ? "" : second.trim();
        return a.equalsIgnoreCase(b);
    }
}
