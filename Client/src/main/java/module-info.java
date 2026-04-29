module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    opens com.auction.client to javafx.fxml;
    exports com.auction.client;
    exports com.auction.client.app;
    opens com.auction.client.app to javafx.fxml;
    exports com.auction.client.controller;
    opens com.auction.client.controller to javafx.fxml;
}