module org.example.azw3reader {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.slf4j;
    requires java.management;


    opens org.example.azw3reader to javafx.fxml;
    exports org.example.azw3reader;
    exports org.example.azw3reader.gui;
    opens org.example.azw3reader.gui to javafx.fxml;
}