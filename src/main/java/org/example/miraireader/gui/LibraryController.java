package org.example.miraireader.gui;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.example.miraireader.core.Azw3Metadata;
import org.example.miraireader.core.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class LibraryController {
    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);
    @FXML
    protected BorderPane mainContainer;

    @FXML
    protected AnchorPane bookContainer;

    protected BookReader bookReader;

    @FXML
    protected void onOpenBook() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("AZW3 files", "*.azw3")
        );
        File selectedFile = fileChooser.showOpenDialog(mainContainer.getScene().getWindow());
        log.info(selectedFile.getName());

        Task<Book> openBookTask = new Task<>() {
            @Override
            protected Book call() throws IOException {
                Azw3Metadata metadata = Azw3Metadata.of(selectedFile);
                return new Book(selectedFile, metadata);
            }
        };
        openBookTask.setOnFailed(e -> {
            Throwable exception = openBookTask.getException();
            PauseTransition hideNotification = new PauseTransition(Duration.seconds(10));
            if (exception instanceof IOException) {
                Label warning = new Label("Error while reading the file. Please try again!");
                HBox hbox = new HBox(warning);
                hbox.setBackground(new Background(new BackgroundFill(Paint.valueOf("red"), null, null)));
                mainContainer.setBottom(hbox);
            } else {
                Label warning = new Label("Unknown error!");
                log.error("Found exception ", exception);
                HBox hbox = new HBox(warning);
                hbox.setBackground(new Background(new BackgroundFill(Paint.valueOf("red"), null, null)));
                mainContainer.setBottom(hbox);
            }
            hideNotification.setOnFinished(event -> mainContainer.setBottom(null));
            hideNotification.play();
        });

        openBookTask.setOnSucceeded(event -> {
            Book book = openBookTask.getValue();
            bookReader = new BookReader(bookContainer, book.getTitle(), book);
            bookContainer.getChildren().add(bookReader);
        });

        new Thread(openBookTask).start();
    }
}