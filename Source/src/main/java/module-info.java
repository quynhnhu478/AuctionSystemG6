module org.example.source {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.desktop;

    opens org.example.views to javafx.graphics, javafx.fxml;
    opens org.example.controllers to javafx.fxml;
    opens org.example.source to javafx.fxml, javafx.graphics;

    exports org.example.source;
    exports org.example.views;
    exports org.example.controllers;
}