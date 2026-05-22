module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.net.http;
    requires org.apache.tomcat.embed.websocket;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    requires Common;
    requires tools.jackson.databind;

    opens com.auction.client to javafx.fxml;
    exports com.auction.client;
    exports com.auction.client.app;
    opens com.auction.client.app to javafx.fxml;
    exports com.auction.client.controller;
    opens com.auction.client.controller to javafx.fxml;
    opens com.auction.client.service to com.fasterxml.jackson.databind;
    exports com.auction.client.controller.admin;
    opens com.auction.client.controller.admin to javafx.fxml;
    exports com.auction.client.controller.seller;
    opens com.auction.client.controller.seller to javafx.fxml;
}