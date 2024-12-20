module org.example.miraireader {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.slf4j;
    requires java.management;


    opens org.example.miraireader to javafx.fxml;
    exports org.example.miraireader;
    exports org.example.miraireader.gui;
    opens org.example.miraireader.gui to javafx.fxml;
}